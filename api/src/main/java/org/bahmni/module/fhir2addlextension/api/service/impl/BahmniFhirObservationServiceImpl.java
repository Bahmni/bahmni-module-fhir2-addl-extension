package org.bahmni.module.fhir2addlextension.api.service.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2addlextension.api.context.RequestContextHolder;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniObsDao;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniObservationDao;
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
import org.springframework.beans.factory.annotation.Qualifier;
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
	
	private final BahmniObservationDao bahmniObservationDao;
	
	private final SearchQueryInclude<Observation> searchQueryInclude;
	
	private final SearchQuery<Obs, Observation, FhirDao<Obs>, OpenmrsFhirTranslator<Obs, Observation>, SearchQueryInclude<Observation>> searchQuery;
	
	@Autowired
	public BahmniFhirObservationServiceImpl(
	    BahmniObsDao bahmniObsDao,
	    BahmniObservationDao bahmniObservationDao,
	    SearchQueryInclude<Observation> searchQueryInclude,
	    SearchQuery<Obs, Observation, FhirDao<Obs>, OpenmrsFhirTranslator<Obs, Observation>, SearchQueryInclude<Observation>> searchQuery) {
		this.bahmniObsDao = bahmniObsDao;
		this.bahmniObservationDao = bahmniObservationDao;
		this.searchQueryInclude = searchQueryInclude;
		this.searchQuery = searchQuery;
	}
	
	@Override
	public IBundleProvider searchObservations(BahmniObservationSearchParams searchParams) {
		if (!searchParams.hasPatientReference() && !searchParams.hasBasedOnReference()) {
			log.error(MISSING_REFERENCE_LOG_MESSAGE);
			throw new UnsupportedOperationException(MISSING_REFERENCE_ERROR_MESSAGE);
		}
		return searchQuery.getQueryResults(searchParams.toSearchParameterMap(), bahmniObservationDao, getTranslator(),
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
	
	@Override
	public Observation create(@Nonnull Observation newResource) {
		if (newResource == null) {
			throw new InvalidRequestException("A resource of type " + resourceClass.getSimpleName() + " must be supplied");
		}
		
		Obs openmrsObj = getTranslator().toOpenmrsType(newResource);
		Set<Obs> groupMembers = openmrsObj.getGroupMembers();
		
		validateObject(openmrsObj);
		
		if (openmrsObj.getUuid() == null) {
			openmrsObj.setUuid(FhirUtils.newUuid());
		}
		
		Obs updatedObs = getDao().createOrUpdate(openmrsObj);
		bahmniObsDao.updateObsMember(updatedObs, groupMembers);
		Observation resource = getTranslator().toFhirResource(updatedObs);
		return resource;
	}
}
