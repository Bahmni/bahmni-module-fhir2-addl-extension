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
	
	public static final String VALIDATION_UUID_REQUIRED = "Uuid cannot be null.";
	
	public static final String VALIDATION_RESOURCE_REQUIRED = "Resource cannot be null.";
	
	public static final String VALIDATION_STUDY_ID_REQUIRED = "ImagingStudy resource is missing id.";
	
	public static final String VALIDATION_ID_MISMATCH = "ImagingStudy id does not match resource id.";
	
	public static final String VALIDATION_ENCOUNTER_REQUIRED = "Encounter is required for quality assessments";
	
	public static final String ERROR_STUDY_NOT_FOUND = "ImagingStudy not found: ";
	
	public static final String ERROR_SEARCH_PARAMS_REQUIRED = "You must specify patient reference or resource _id or basedOn reference!";
	
	public static final String ERROR_MISSING_SEARCH_PARAMS = "Missing patient reference, resource id or basedOn reference for ImagingStudy search";
	
	public static final String WARN_EXTENSION_NOT_REFERENCE = "Quality observation extension value is not a Reference, skipping";
	
	public static final String WARN_INVALID_CONTAINED_REF = "Invalid contained reference: ";
	
	public static final String WARN_RESOURCE_NOT_OBSERVATION = "Referenced contained resource is not an Observation: ";
	
	public static final String DEBUG_UPDATING_QUALITY_ASSESSMENTS = "Updating ImagingStudy with quality assessments";
	
	public static final String DEBUG_VOIDING_QUALITY_OBSERVATION = "Voiding existing quality observation: {}";
	
	public static final String DEBUG_RESOLVING_OBS_MEMBER_REF = "resolving obs.hasMember ref: %s => %s%n";
	
	public static final String DEBUG_NO_OBSERVATION_FOUND = "No existing observation found with UUID: {}";
	
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
			log.error(ERROR_MISSING_SEARCH_PARAMS);
			throw new UnsupportedOperationException(ERROR_SEARCH_PARAMS_REQUIRED);
		}
		return searchQuery.getQueryResults(searchParams.toSearchParameterMap(), imagingStudyDao, imagingStudyTranslator,
		    searchQueryInclude);
	}
	
	@Override
	@Transactional(readOnly = true)
	public ImagingStudy fetchWithQualityAssessment(String uuid) {
		FhirImagingStudy study = imagingStudyDao.get(uuid);
		if (study == null) {
			throw new ResourceNotFoundException(ERROR_STUDY_NOT_FOUND + uuid);
		}
		ImagingStudy resource = imagingStudyTranslator.toFhirResource(study);
		
		addQualityAssessmentsToFhirResource(study, resource);
		
		return resource;
	}
	
	private void addQualityAssessmentsToFhirResource(FhirImagingStudy study, ImagingStudy resource) {
        Optional.ofNullable(study.getAssessment())
                .filter(assessment -> !assessment.isEmpty())
                .ifPresent(assessment -> assessment.forEach(obs -> {
                    Observation fhirObs = observationTranslator.toFhirResource(obs);
                    String containedId = CONTAINED_RESOURCE_PREFIX + obs.getUuid();
                    fhirObs.setId(containedId);

                    resource.addContained(fhirObs);
                    resource.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference(containedId));
                }));
    }
	
	@Override
	public ImagingStudy update(@Nonnull String uuid, @Nonnull ImagingStudy updatedResource) {
		validateUpdateInput(uuid, updatedResource);
		FhirImagingStudy existingStudy = getDao().get(uuid);
		validateExistingStudy(uuid, existingStudy);
		FhirImagingStudy updatedStudy = imagingStudyTranslator.toOpenmrsType(existingStudy, updatedResource);
		validateObject(updatedStudy);
		boolean hasQualityAssessments = hasQualityAssessmentExtensions(updatedResource);
		if (hasQualityAssessments) {
			log.debug(DEBUG_UPDATING_QUALITY_ASSESSMENTS);
			voidExistingQualityObservations(existingStudy);
			processQualityAssessments(updatedResource, updatedStudy);
		}
		
		// Save and return
		FhirImagingStudy savedStudy = imagingStudyDao.createOrUpdate(updatedStudy);
		ImagingStudy result = imagingStudyTranslator.toFhirResource(savedStudy);
		if (hasQualityAssessments) {
			addQualityAssessmentsToFhirResource(savedStudy, result);
		}
		return result;
	}
	
	/**
	 * Validates input parameters for update operation
	 */
	private void validateUpdateInput(String uuid, ImagingStudy updatedResource) {
		if (uuid == null) {
			throw new InvalidRequestException(VALIDATION_UUID_REQUIRED);
		}
		if (updatedResource == null) {
			throw new InvalidRequestException(VALIDATION_RESOURCE_REQUIRED);
		}
		if (updatedResource.getId() == null) {
			throw new InvalidRequestException(VALIDATION_STUDY_ID_REQUIRED);
		}
		if (!updatedResource.getIdElement().getIdPart().equals(uuid)) {
			throw new InvalidRequestException(VALIDATION_ID_MISMATCH);
		}
	}
	
	/**
	 * Validates that the existing study exists
	 */
	private void validateExistingStudy(String uuid, FhirImagingStudy existingStudy) {
		if (existingStudy == null) {
			throw new ResourceNotFoundException(ERROR_STUDY_NOT_FOUND + uuid);
		}
	}
	
	/**
	 * Voids existing quality observations from the imaging study assessment set
	 */
	private void voidExistingQualityObservations(FhirImagingStudy existingStudy) {
		if (existingStudy.getAssessment() == null || existingStudy.getAssessment().isEmpty()) {
			return;
		}
		// Void all observations in assessment set (quality assessments)
		existingStudy.getAssessment().forEach(obs -> {
			log.debug(DEBUG_VOIDING_QUALITY_OBSERVATION, obs.getUuid());
			fhirObservationService.delete(obs.getUuid());
		});
		existingStudy.getAssessment().clear();
	}
	
	/**
	 * Checks if the ImagingStudy has quality assessment extensions and contained observations
	 */
	private boolean hasQualityAssessmentExtensions(ImagingStudy imagingStudy) {
		List<Extension> qualityObsExtensions = imagingStudy.getExtensionsByUrl(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION);
		return !qualityObsExtensions.isEmpty() && imagingStudy.hasContained();
	}
	
	/**
	 * Processes quality assessment observations for an ImagingStudy. Extracts contained
	 * observations from extensions, persists them, and associates with the study.
	 * 
	 * @param imagingStudy The FHIR ImagingStudy resource with quality assessment extensions
	 * @param openmrsStudy The OpenMRS FhirImagingStudy entity to update
	 */
	private void processQualityAssessments(ImagingStudy imagingStudy, FhirImagingStudy openmrsStudy) {
		List<Extension> qualityObsExtensions = imagingStudy.getExtensionsByUrl(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION);
		Map<String, Resource> containedMap = imagingStudy.getContained().stream()
				.filter(r -> r.getId() != null)
				.collect(Collectors.toMap(Resource::getId, r -> r));

		Map<String, Reference> obsReferenceMap = createQualityObservations(qualityObsExtensions, containedMap);
		updateQualityObservationExtensions(imagingStudy, obsReferenceMap);
		Set<Obs> qualityAssessment = new LinkedHashSet<>();
		for (Reference obsRef : obsReferenceMap.values()) {
			Obs obs = observationReferenceTranslator.toOpenmrsType(obsRef);
			if (obs != null) {
				qualityAssessment.add(obs);
			}
		}
		openmrsStudy.setAssessment(qualityAssessment);
	}
	
	private Map<String, Reference> createQualityObservations(List<Extension> qualityObsExtensions,
			Map<String, Resource> containedMap) {

		Map<Observation, String> observationReferenceMap = new HashMap<>();
		List<String> preExistingObservationIds = new ArrayList<>();

		collectObservationsFromExtensions(qualityObsExtensions, containedMap, observationReferenceMap, preExistingObservationIds);

		List<Observation> sortedObservations = ConsultationBundleEntriesHelper.sortObservationsByDepth(
				new ArrayList<>(observationReferenceMap.keySet()));

		return persistObservations(sortedObservations, observationReferenceMap, preExistingObservationIds);
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
			log.warn(WARN_EXTENSION_NOT_REFERENCE);
			return null;
		}
		Reference obsRef = (Reference) ext.getValue();
		String containedId = obsRef.getReference();
		
		if (containedId == null || !containedId.startsWith(CONTAINED_RESOURCE_PREFIX)) {
			log.warn(WARN_INVALID_CONTAINED_REF + containedId);
			return null;
		}
		Resource containedResource = containedMap.get(containedId);
		if (!(containedResource instanceof Observation)) {
			log.warn(WARN_RESOURCE_NOT_OBSERVATION + containedId);
			return null;
		}
		return (Observation) containedResource;
	}
	
	private Map<String, Reference> persistObservations(List<Observation> sortedObservations,
			Map<Observation, String> observationReferenceMap, List<String> preExistingObservationIds) {

		Map<String, Reference> observationsReferenceMap = new HashMap<>();

		for (Observation observation : sortedObservations) {
			String resourceId = Optional.ofNullable(observation.getIdElement().getValue())
					.orElseGet(() -> observationReferenceMap.get(observation));
			String obsEntryId = BahmniFhirUtils.extractId(resourceId);

			prepareObservationForPersistence(observation, observationsReferenceMap);

			Observation persistedObservation = preExistingObservationIds.contains(obsEntryId)
					? fhirObservationService.update(obsEntryId, observation)
					: fhirObservationService.create(observation);

			Reference persistedObsReference = createObservationReference(persistedObservation);
			observationsReferenceMap.put(obsEntryId, persistedObsReference);
		}

		return observationsReferenceMap;
	}
	
	private void prepareObservationForPersistence(Observation observation, Map<String, Reference> observationsReferenceMap) {
		
		if (!observation.hasEncounter() || observation.getEncounter().getReference() == null
		        || observation.getEncounter().getReference().isEmpty()) {
			throw new InvalidRequestException(VALIDATION_ENCOUNTER_REQUIRED);
		}
		
		resolveHasMemberReferences(observation, observationsReferenceMap);
	}
	
	private void resolveHasMemberReferences(Observation observation, Map<String, Reference> observationsReferenceMap) {
		observation.getHasMember().forEach(member -> {
			String memberRefId = extractReferenceId(member.getReference());
			if (memberRefId != null) {
				Reference mappedRef = observationsReferenceMap.get(memberRefId);
				if (mappedRef != null) {
					log.debug(String.format(DEBUG_RESOLVING_OBS_MEMBER_REF, 
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
		imagingStudy.getExtension().removeIf(ext -> FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION.equals(ext.getUrl()));
		imagingStudy.getContained().clear();
		
		for (Reference persistedRef : obsReferenceMap.values()) {
			imagingStudy.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, persistedRef);
		}
	}
	
	private Observation findExistingObservation(String uuid) {
		try {
			return fhirObservationService.get(uuid);
		}
		catch (ResourceNotFoundException e) {
			log.debug(DEBUG_NO_OBSERVATION_FOUND, uuid);
		}
		return null;
	}
	
}
