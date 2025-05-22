package org.bahmni.module.fhir2AddlExtension.api.validators.impl;

import org.openmrs.Diagnosis;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.annotation.Handler;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Handler(supports = { Diagnosis.class }, order = 100)
public class BahmniEncounterDiagnosisValidator implements Validator {
	
	@Override
	public boolean supports(Class<?> clazz) {
		return Diagnosis.class.isAssignableFrom(clazz);
	}
	
	@Override
	public void validate(Object target, Errors errors) {
		Diagnosis diagnosis = (Diagnosis) target;
		if (diagnosis.getEncounter() != null && diagnosis.getPatient() != null) {
			Encounter encounter = diagnosis.getEncounter();
			Patient patient = diagnosis.getPatient();
			if (encounter.getPatient() != patient) {
				errors.rejectValue("encounter", "encounter.patient.mismatch", "The encounter does not belong to the patient");
			}
			
		}
	}
}
