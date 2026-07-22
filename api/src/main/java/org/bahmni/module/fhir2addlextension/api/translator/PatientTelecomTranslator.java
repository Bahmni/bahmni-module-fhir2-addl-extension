package org.bahmni.module.fhir2addlextension.api.translator;

import org.hl7.fhir.r4.model.ContactPoint;

import java.util.List;
import java.util.Set;

public interface PatientTelecomTranslator {
	
	List<ContactPoint> getContactPoints(org.openmrs.Patient patient);
	
	void updateAttributes(org.openmrs.Patient patient, List<ContactPoint> telecom);
	
	Set<String> getMappedAttributeTypeNames();
}
