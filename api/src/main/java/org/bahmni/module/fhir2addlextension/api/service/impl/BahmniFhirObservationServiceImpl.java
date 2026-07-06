package org.bahmni.module.fhir2addlextension.api.service.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bahmni.module.fhir2addlextension.api.context.RequestContextHolder;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniObsDao;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirObservationService;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Resource;
import org.openmrs.Obs;
import org.openmrs.module.fhir2.api.impl.FhirObservationServiceImpl;
import org.openmrs.module.fhir2.api.search.param.ObservationSearchParams;
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
public class BahmniFhirObservationServiceImpl extends FhirObservationServiceImpl implements BahmniFhirObservationService {
	
	@Getter(value = AccessLevel.PROTECTED)
	@Setter(onMethod_ = @Autowired)
	private BahmniObsDao bahmniObsDao;
	
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
		
		// Set UUID from FHIR resource id BEFORE validateObject().
		// OpenMRS BaseOpenmrsObject lazily auto-generates UUID on the first getUuid() call
		// (triggered inside validateObject), so we must set it first to honour the client UUID.
		if (newResource.hasId()) {
			openmrsObj.setUuid(newResource.getIdElement().getIdPart());
		}
		
		validateObject(openmrsObj);
		
		// If the UUID maps to an existing obs and there are group members to link,
		// this is an existing parent group obs — do NOT re-create it.
		// Just link the new children via updateObsMember and return the existing obs.
		// Note: when ALL children are deleted the frontend sends DELETE for the parent obs too,
		// so this path is only reached when there are valid new children to link.
		if (newResource.hasId() && !groupMembers.isEmpty()) {
			Obs existingObs = getDao().get(openmrsObj.getUuid());
			if (existingObs != null) {
				bahmniObsDao.updateObsMember(existingObs, groupMembers);
				return getTranslator().toFhirResource(existingObs);
			}
		}
		
		Obs updatedObs = getDao().createOrUpdate(openmrsObj);
		bahmniObsDao.updateObsMember(updatedObs, groupMembers);
		return getTranslator().toFhirResource(updatedObs);
	}
}
