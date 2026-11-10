package org.bahmni.module.fhir2addlextension.api.service;

import org.bahmni.module.fhir2addlextension.api.PrivilegeConstants;
import org.openmrs.Patient;
import org.openmrs.annotation.Authorized;

import java.util.List;

public interface PatientMatchService {
	
	@Authorized({ PrivilegeConstants.GET_PATIENTS })
	List<Patient> findMatches(org.hl7.fhir.r4.model.Patient patient);
}
