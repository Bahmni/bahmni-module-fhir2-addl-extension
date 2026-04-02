package org.bahmni.module.fhir2addlextension.api.dao;

import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.bahmni.module.fhir2addlextension.api.PrivilegeConstants;
import org.openmrs.Auditable;
import org.openmrs.OpenmrsObject;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.fhir2.api.dao.FhirServiceRequestDao;

public interface BahmniFhirServiceRequestDao<T extends OpenmrsObject & Auditable> extends FhirServiceRequestDao<T> {
	
	@Authorized({ PrivilegeConstants.GET_ORDERS })
	ReferenceAndListParam getEncounterReferencesByNumberOfVisit(NumberParam numberOfVisitsParam,
	        ReferenceParam patientReferenceParam);
	
	@Authorized({ PrivilegeConstants.EDIT_ORDERS })
	T updateOrder(T order);
	
}
