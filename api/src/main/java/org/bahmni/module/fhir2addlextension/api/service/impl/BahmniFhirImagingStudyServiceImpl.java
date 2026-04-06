package org.bahmni.module.fhir2addlextension.api.service.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirImagingStudyDao;
import org.bahmni.module.fhir2addlextension.api.helper.ConsultationBundleEntriesHelper;
import org.bahmni.module.fhir2addlextension.api.model.FhirImagingStudy;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniImagingStudySearchParams;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirImagingStudyService;
import org.bahmni.module.fhir2addlextension.api.translator.BahmniFhirImagingStudyTranslator;
import org.bahmni.module.fhir2addlextension.api.utils.BahmniFhirUtils;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.ImagingStudy;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.openmrs.Obs;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.FhirObservationService;
import org.openmrs.module.fhir2.api.translators.ObservationReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationTranslator;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.impl.BaseFhirService;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants.FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION;

@Component
@Transactional
@Slf4j
public class BahmniFhirImagingStudyServiceImpl extends BaseFhirService<ImagingStudy, FhirImagingStudy> implements BahmniFhirImagingStudyService {
	
	private static final String CONTAINED_RESOURCE_PREFIX = "#";
	
	private final BahmniFhirImagingStudyDao imagingStudyDao;
	
	private final BahmniFhirImagingStudyTranslator imagingStudyTranslator;
	
	private final SearchQueryInclude<ImagingStudy> searchQueryInclude;
	
	private final SearchQuery<FhirImagingStudy, ImagingStudy, BahmniFhirImagingStudyDao, BahmniFhirImagingStudyTranslator, SearchQueryInclude<ImagingStudy>> searchQuery;
	
	private final FhirObservationService fhirObservationService;
	
	private final ObservationTranslator observationTranslator;
	
	private final ObservationReferenceTranslator observationReferenceTranslator;
	
	@Autowired
	public BahmniFhirImagingStudyServiceImpl(
	    BahmniFhirImagingStudyDao imagingStudyDao,
	    BahmniFhirImagingStudyTranslator imagingStudyTranslator,
	    SearchQueryInclude<ImagingStudy> searchQueryInclude,
	    SearchQuery<FhirImagingStudy, ImagingStudy, BahmniFhirImagingStudyDao, BahmniFhirImagingStudyTranslator, SearchQueryInclude<ImagingStudy>> searchQuery,
	    FhirObservationService fhirObservationService, ObservationTranslator observationTranslator,
	    ObservationReferenceTranslator observationReferenceTranslator) {
		this.imagingStudyDao = imagingStudyDao;
		this.imagingStudyTranslator = imagingStudyTranslator;
		this.searchQueryInclude = searchQueryInclude;
		this.searchQuery = searchQuery;
		this.fhirObservationService = fhirObservationService;
		this.observationTranslator = observationTranslator;
		this.observationReferenceTranslator = observationReferenceTranslator;
	}
	
	@Override
	protected FhirDao<FhirImagingStudy> getDao() {
		return imagingStudyDao;
	}
	
	@Override
	protected OpenmrsFhirTranslator<FhirImagingStudy, ImagingStudy> getTranslator() {
		return imagingStudyTranslator;
	}
	
	@Override
	@Transactional(readOnly = true)
	public IBundleProvider searchImagingStudy(BahmniImagingStudySearchParams searchParams) {
		if (!searchParams.hasPatientReference() && !searchParams.hasId() && !searchParams.hasBasedOnReference()) {
			log.error("Missing patient reference, resource id or basedOn reference for ImagingStudy search");
			throw new UnsupportedOperationException(
			        "You must specify patient reference or resource _id or basedOn reference!");
		}
		return searchQuery.getQueryResults(searchParams.toSearchParameterMap(), imagingStudyDao, imagingStudyTranslator,
		    searchQueryInclude);
	}
	
	@Override
	@Transactional(readOnly = true)
	public ImagingStudy fetchWithQualityAssessment(String uuid) {
		FhirImagingStudy study = imagingStudyDao.get(uuid);
		if (study == null) {
			throw new ResourceNotFoundException("ImagingStudy not found: " + uuid);
		}
		
		ImagingStudy resource = imagingStudyTranslator.toFhirResource(study);
		
		addQualityAssessmentsToFhirResource(study, resource);
		
		return resource;
	}
	
	private void addQualityAssessmentsToFhirResource(FhirImagingStudy study, ImagingStudy resource) {
		if (study.getResults() != null && !study.getResults().isEmpty()) {
			for (Obs obs : study.getResults()) {
				Observation fhirObs = observationTranslator.toFhirResource(obs);
				String containedId = CONTAINED_RESOURCE_PREFIX + obs.getUuid();
				fhirObs.setId(containedId);
				
				resource.addContained(fhirObs);
				resource.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference(containedId));
			}
		}
	}
	
	@Override
	public ImagingStudy submitQualityAssessment(@Nonnull ImagingStudy imagingStudy) {
		if (imagingStudy.getId() == null) {
			throw new InvalidRequestException("ImagingStudy ID is required for quality assessment submission");
		}
		
		// Get existing imaging study - extract UUID from potential "ImagingStudy/uuid" format
		String extractedUuid = BahmniFhirUtils.extractId(imagingStudy.getId());
		FhirImagingStudy existingStudy = imagingStudyDao.get(extractedUuid);
		if (existingStudy == null) {
			throw new ResourceNotFoundException("ImagingStudy with ID ImagingStudy/" + extractedUuid + " not found");
		}
		
		// Extract quality observation extensions
		List<Extension> qualityObsExtensions = imagingStudy.getExtensionsByUrl(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION);
		if (qualityObsExtensions.isEmpty()) {
			throw new InvalidRequestException("No quality assessment observations found in extensions");
		}
		
		if (!imagingStudy.hasContained()) {
			throw new InvalidRequestException("No contained resources found. Quality observations must be contained resources.");
		}
		
		// Create map of contained resources by ID for lookup
		Map<String, Resource> containedMap = imagingStudy.getContained().stream()
				.filter(r -> r.getId() != null)
				.collect(Collectors.toMap(Resource::getId, r -> r));
		
		// Prepare encounter reference if imaging study has one
		Reference encounterReference = null;
		if (existingStudy.getEncounter() != null) {
			encounterReference = new Reference()
					.setReference(FhirConstants.ENCOUNTER + "/" + existingStudy.getEncounter().getUuid())
					.setType(FhirConstants.ENCOUNTER);
		}
		
		Map<String, Reference> obsReferenceMap = createQualityObservations(qualityObsExtensions, containedMap, encounterReference);
		
		updateQualityObservationExtensions(imagingStudy, obsReferenceMap);
		
		Set<Obs> qualityResults = new LinkedHashSet<>();
		for (Reference obsRef : obsReferenceMap.values()) {
			Obs obs = observationReferenceTranslator.toOpenmrsType(obsRef);
			if (obs != null) {
				qualityResults.add(obs);
			}
		}
		existingStudy.setResults(qualityResults);

		FhirImagingStudy updatedStudy = imagingStudyTranslator.toOpenmrsType(existingStudy, imagingStudy);
		
		FhirImagingStudy savedStudy = imagingStudyDao.createOrUpdate(updatedStudy);
		
		ImagingStudy result = imagingStudyTranslator.toFhirResource(savedStudy);
		addQualityAssessmentsToFhirResource(savedStudy, result);
		
		return result;
	}
	
	private Map<String, Reference> createQualityObservations(List<Extension> qualityObsExtensions, 
			Map<String, Resource> containedMap, Reference encounterReference) {
		
		Map<Observation, String> observationReferenceMap = new HashMap<>();
		List<String> preExistingObservationIds = new ArrayList<>();
		
		collectObservationsFromExtensions(qualityObsExtensions, containedMap, observationReferenceMap, preExistingObservationIds);
		
		List<Observation> sortedObservations = ConsultationBundleEntriesHelper.sortObservationsByDepth(
				new ArrayList<>(observationReferenceMap.keySet()));
		
		return persistObservations(sortedObservations, observationReferenceMap, preExistingObservationIds, encounterReference);
	}
	
	private void collectObservationsFromExtensions(List<Extension> qualityObsExtensions, Map<String, Resource> containedMap,
	        Map<Observation, String> observationReferenceMap, List<String> preExistingObservationIds) {
		
		for (Extension ext : qualityObsExtensions) {
			Observation observation = extractObservationFromExtension(ext, containedMap);
			if (observation == null) {
				continue;
			}
			
			String obsId = BahmniFhirUtils.extractId(observation.getId());
			
			Observation existing = findExistingObservation(obsId);
			if (existing != null) {
				preExistingObservationIds.add(existing.getId());
			}
			
			observationReferenceMap.put(observation, obsId);
		}
	}
	
	private Observation extractObservationFromExtension(Extension ext, Map<String, Resource> containedMap) {
		if (!(ext.getValue() instanceof Reference)) {
			log.warn("Quality observation extension value is not a Reference, skipping");
			return null;
		}
		
		Reference obsRef = (Reference) ext.getValue();
		String containedId = obsRef.getReference();
		
		if (containedId == null || !containedId.startsWith(CONTAINED_RESOURCE_PREFIX)) {
			log.warn("Invalid contained reference: " + containedId);
			return null;
		}
		
		Resource containedResource = containedMap.get(containedId);
		if (!(containedResource instanceof Observation)) {
			log.warn("Referenced contained resource is not an Observation: " + containedId);
			return null;
		}
		
		return (Observation) containedResource;
	}
	
	private Map<String, Reference> persistObservations(List<Observation> sortedObservations,
			Map<Observation, String> observationReferenceMap, List<String> preExistingObservationIds,
			Reference encounterReference) {
		
		Map<String, Reference> observationsReferenceMap = new HashMap<>();
		
		for (Observation observation : sortedObservations) {
			String resourceId = Optional.ofNullable(observation.getIdElement().getValue())
					.orElseGet(() -> observationReferenceMap.get(observation));
			String obsEntryId = BahmniFhirUtils.extractId(resourceId);
			
			prepareObservationForPersistence(observation, encounterReference, observationsReferenceMap);
			
			Observation persistedObservation = preExistingObservationIds.contains(obsEntryId)
					? fhirObservationService.update(obsEntryId, observation)
					: fhirObservationService.create(observation);
			
			Reference persistedObsReference = createObservationReference(persistedObservation);
			observationsReferenceMap.put(obsEntryId, persistedObsReference);
		}
		
		return observationsReferenceMap;
	}
	
	private void prepareObservationForPersistence(Observation observation, Reference encounterReference,
	        Map<String, Reference> observationsReferenceMap) {
		
		// Remove empty encounter references to avoid NullPointerException
		if (observation.hasEncounter() && observation.getEncounter().getReference() != null
		        && observation.getEncounter().getReference().isEmpty()) {
			observation.setEncounter(null);
		}
		
		// Set encounter reference if available
		if (encounterReference != null) {
			observation.setEncounter(encounterReference);
		}
		
		resolveHasMemberReferences(observation, observationsReferenceMap);
	}
	
	private void resolveHasMemberReferences(Observation observation, Map<String, Reference> observationsReferenceMap) {
		observation.getHasMember().forEach(member -> {
			String memberRefId = extractReferenceId(member.getReference());
			if (memberRefId != null) {
				Reference mappedRef = observationsReferenceMap.get(memberRefId);
				if (mappedRef != null) {
					log.debug(String.format("resolving obs.hasMember ref: %s => %s%n", 
							member.getReference(), mappedRef.getReference()));
					member.setReference(mappedRef.getReference());
					member.setResource(null);
				}
			}
		});
	}
	
	private String extractReferenceId(String reference) {
		if (reference == null) {
			return null;
		}
		
		if (reference.startsWith(CONTAINED_RESOURCE_PREFIX)) {
			return reference.substring(1);
		} else if (reference.contains("/")) {
			return BahmniFhirUtils.extractId(reference);
		}
		
		return reference;
	}
	
	private Reference createObservationReference(Observation observation) {
		Reference reference = new Reference().setReference(FhirConstants.OBSERVATION + "/" + observation.getId()).setType(
		    FhirConstants.OBSERVATION);
		reference.setResource(observation);
		return reference;
	}
	
	/**
	 * Updates the ImagingStudy quality observation extensions with persisted references. Replaces
	 * contained references (#uuid) with actual observation references (Observation/uuid).
	 */
	private void updateQualityObservationExtensions(ImagingStudy imagingStudy, Map<String, Reference> obsReferenceMap) {
		// Remove old extensions and contained resources
		imagingStudy.getExtension().removeIf(ext -> FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION.equals(ext.getUrl()));
		imagingStudy.getContained().clear();
		
		// Add new extensions with persisted references
		for (Reference persistedRef : obsReferenceMap.values()) {
			imagingStudy.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, persistedRef);
		}
	}
	
	private Observation findExistingObservation(String uuid) {
		try {
			return fhirObservationService.get(uuid);
		}
		catch (ResourceNotFoundException e) {
			log.debug("No existing observation found with UUID: {}", uuid);
		}
		return null;
	}
	
}
