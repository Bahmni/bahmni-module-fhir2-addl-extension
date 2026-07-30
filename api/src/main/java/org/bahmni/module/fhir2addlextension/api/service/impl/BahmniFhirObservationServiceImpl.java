package org.bahmni.module.fhir2addlextension.api.service.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2addlextension.api.context.RequestContextHolder;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniObsDao;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniObservationSearchParams;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirObservationService;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Resource;
import org.openmrs.Obs;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.impl.FhirObservationServiceImpl;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.search.param.ObservationSearchParams;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;
import org.openmrs.module.fhir2.api.util.FhirUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Component
@Primary
@Slf4j
public class BahmniFhirObservationServiceImpl extends FhirObservationServiceImpl implements BahmniFhirObservationService {
	
	private static final String MISSING_REFERENCE_LOG_MESSAGE = "Missing patient reference or basedOn reference for Observation search";
	
	private static final String MISSING_REFERENCE_ERROR_MESSAGE = "You must specify patient reference or basedOn reference!";
	
	private final BahmniObsDao bahmniObsDao;
	
	private final SearchQueryInclude<Observation> searchQueryInclude;
	
	private final SearchQuery<Obs, Observation, FhirDao<Obs>, OpenmrsFhirTranslator<Obs, Observation>, SearchQueryInclude<Observation>> searchQuery;
	
	@Autowired
	public BahmniFhirObservationServiceImpl(
	    BahmniObsDao bahmniObsDao,
	    SearchQueryInclude<Observation> searchQueryInclude,
	    SearchQuery<Obs, Observation, FhirDao<Obs>, OpenmrsFhirTranslator<Obs, Observation>, SearchQueryInclude<Observation>> searchQuery) {
		this.bahmniObsDao = bahmniObsDao;
		this.searchQueryInclude = searchQueryInclude;
		this.searchQuery = searchQuery;
	}
	
	@Override
	public IBundleProvider searchObservations(BahmniObservationSearchParams searchParams) {
		if (!searchParams.hasPatientReference() && !searchParams.hasBasedOnReference()) {
			log.error(MISSING_REFERENCE_LOG_MESSAGE);
			throw new UnsupportedOperationException(MISSING_REFERENCE_ERROR_MESSAGE);
		}
		return searchQuery.getQueryResults(searchParams.toSearchParameterMap(), bahmniObsDao, getTranslator(),
		    searchQueryInclude);
	}
	
	@Override
	public Bundle fetchAllByEncounter(ReferenceAndListParam encounterReference) {
		ObservationSearchParams searchParams = new ObservationSearchParams();
		searchParams.setEncounter(encounterReference);
		
		IBundleProvider bundleProvider = searchForObservations(searchParams);
		List<IBaseResource> observations = bundleProvider.getResources(0, Integer.MAX_VALUE);
		
		String fhirServerBase = RequestContextHolder.getValue();
		Bundle bundle = new Bundle();
		bundle.setId(FhirUtils.newUuid());
		bundle.setMeta(new Meta());
		bundle.getMeta().setLastUpdated(new Date());
		bundle.setType(Bundle.BundleType.SEARCHSET);
		bundle.setTotal(observations.size());
		for (IBaseResource resource : observations) {
			if (resource instanceof Observation) {
				Observation obs = (Observation) resource;
				bundle.addEntry().setResource(obs).setFullUrl(getFullUrlForEntry(obs, fhirServerBase));
			}
		}
		return bundle;
	}
	
	private String getFullUrlForEntry(Resource resource, String fhirServerBase) {
		if (fhirServerBase != null && !fhirServerBase.isEmpty()) {
			return fhirServerBase.concat("/").concat(resource.getResourceType().name()).concat("/").concat(resource.getId());
		} else {
			return "urn:uuid:".concat(resource.getId());
		}
	}
	
	/**
	 * Creates a brand-new observation via the Bahmni EncounterBundle API.
	 * <p>
	 * Editing an existing obsGroup no longer goes through this method — the frontend sends a real
	 * PUT for that, handled by {@link #applyUpdate(Obs, Observation)}. This method only ever sees
	 * new observations (no client-supplied id), so it always creates and links any group members
	 * via {@code updateObsMember}.
	 */
	@Override
	public Observation create(@Nonnull Observation newResource) {
		if (newResource == null) {
			throw new InvalidRequestException("A resource of type " + resourceClass.getSimpleName() + " must be supplied");
		}
		
		Obs openmrsObj = getTranslator().toOpenmrsType(newResource);
		Set<Obs> groupMembers = openmrsObj.getGroupMembers();
		
		validateObject(openmrsObj);
		
		Obs updatedObs = getDao().createOrUpdate(openmrsObj);
		bahmniObsDao.updateObsMember(updatedObs, groupMembers);
		return getTranslator().toFhirResource(updatedObs);
	}
	
	/**
	 * Merges an incoming FHIR PUT into the existing OpenMRS Obs.
	 * <p>
	 * The inherited default (core {@code FhirObservationServiceImpl#applyUpdate}) translates the
	 * incoming resource into a fresh {@code Obs} from scratch and persists it directly — it never
	 * calls {@code updateObsMember}, so it does not safely, additively re-link an obsGroup's
	 * children the way the EncounterBundle edit contract requires. When the EXISTING obs is itself
	 * an obsGroup, we instead keep the already-loaded existing parent ({@code obs}, supplied by
	 * {@code BaseFhirService#update}) as-is and re-link its children via {@code updateObsMember}.
	 * This is what lets callers use a real PUT to update an existing obsGroup.
	 * <p>
	 * Branching on {@code obs.isObsGrouping()} (the EXISTING obs) rather than on whether the
	 * INCOMING resource's hasMember happens to be non-empty matters: the EncounterBundle edit
	 * contract omits unchanged group members from hasMember as an optimisation (see the frontend's
	 * observationResourceCreator.ts), so a PUT for a group where every remaining member happens to
	 * be unchanged legitimately carries an EMPTY hasMember. {@code updateObsMember} already no-ops
	 * on an empty/null set, so that's safe — but checking the incoming resource for emptiness would
	 * wrongly fall through to the core default path below, which tries to persist the group parent
	 * itself as a valueless, member-less Obs and fails OpenMRS's ObsValidator with "error.noValue".
	 * <p>
	 * For all other cases (the existing obs is a plain leaf) the standard core update path is
	 * taken.
	 */
	@Override
	protected Observation applyUpdate(Obs obs, Observation observation) {
		if (obs.isObsGrouping()) {
			Set<Obs> groupMembers = getTranslator().toOpenmrsType(observation).getGroupMembers();
			bahmniObsDao.updateObsMember(obs, groupMembers);
			return getTranslator().toFhirResource(obs);
		}
		return super.applyUpdate(obs, observation);
	}
}
