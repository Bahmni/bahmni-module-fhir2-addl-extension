package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniObsDao;
import org.hl7.fhir.r4.model.Observation;
import org.openmrs.Obs;
import org.openmrs.module.fhir2.api.impl.FhirObservationServiceImpl;
import org.openmrs.module.fhir2.api.util.FhirUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Set;

@Component
@Primary
public class BahmniFhirObservationServiceImpl extends FhirObservationServiceImpl {
	@Getter(value = AccessLevel.PROTECTED)
	@Setter(onMethod_ = @Autowired)
	private BahmniObsDao bahmniObsDao;
	
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
