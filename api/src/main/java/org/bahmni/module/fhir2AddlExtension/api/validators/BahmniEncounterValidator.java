package org.bahmni.module.fhir2AddlExtension.api.validators;

import org.openmrs.Encounter;

public interface BahmniEncounterValidator {
	
	void validate(Encounter existingEncounter, org.hl7.fhir.r4.model.Encounter encounter);
}
