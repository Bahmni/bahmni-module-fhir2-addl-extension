package org.bahmni.module.fhir2AddlExtension.api.validators.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Diagnosis;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class BahmniEncounterDiagnosisValidatorTest {
	
	private Diagnosis diagnosis;
	
	private Errors errors;
	
	@Before
	public void setUp() {
		diagnosis = new Diagnosis();
		errors = new BindException(diagnosis, "diagnosis");
	}
	
	@Test
	public void validate_shouldFailWhenPatientIsDifferentFromEncounterPatient() {
		Encounter encounter = new Encounter(1);
		encounter.setPatient(new Patient(1));
		diagnosis.setEncounter(encounter);
		diagnosis.setPatient(new Patient(2));
		new BahmniEncounterDiagnosisValidator().validate(diagnosis, errors);
		assertTrue(errors.hasFieldErrors("encounter"));
	}
	
}
