package org.bahmni.module.fhir2AddlExtension.api.validators.impl;

import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.ValidationException;
import org.openmrs.module.fhir2.api.translators.EncounterLocationTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniEncounterValidatorImplTest {
	
	@Mock
	private EncounterLocationTranslator encounterLocationTranslator;
	
	@Mock
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Mock
	private EncounterReferenceTranslator<Visit> visitReferenceTranslator;
	
	private BahmniEncounterValidatorImpl validator;
	
	private org.openmrs.Encounter existingEncounter;
	
	private Patient existingPatient;
	
	private Patient newPatient;
	
	private Visit visit;
	
	private Location location;
	
	private Encounter fhirEncounter;
	
	private Reference patientReference;
	
	private Reference visitReference;
	
	private Encounter.EncounterLocationComponent locationComponent;
	
	@Before
	public void setup() {
		validator = new BahmniEncounterValidatorImpl();
		validator.setEncounterLocationTranslator(encounterLocationTranslator);
		validator.setPatientReferenceTranslator(patientReferenceTranslator);
		validator.setVisitReferenceTranslator(visitReferenceTranslator);
		
		fhirEncounter = new Encounter();
		patientReference = new Reference("Patient/123");
		visitReference = new Reference("Visit/456");
		locationComponent = new Encounter.EncounterLocationComponent();
		locationComponent.setLocation(new Reference("Location/789"));
		
		fhirEncounter.setSubject(patientReference);
		fhirEncounter.setPartOf(visitReference);
		fhirEncounter.addLocation(locationComponent);
		
		existingEncounter = new org.openmrs.Encounter();
		existingPatient = new Patient();
		existingPatient.setUuid("123");
		newPatient = new Patient();
		newPatient.setUuid("456");
		visit = new Visit();
		visit.setUuid("456");
		location = new Location();
		location.setUuid("789");
		existingEncounter.setPatient(existingPatient);
		existingEncounter.setVisit(visit);
		existingEncounter.setLocation(location);
	}
	
	@Test
	public void shouldValidateSuccessfullyWhenAllReferencesAreValid() {
		when(patientReferenceTranslator.toOpenmrsType(patientReference)).thenReturn(existingPatient);
		when(visitReferenceTranslator.toOpenmrsType(visitReference)).thenReturn(visit);
		when(encounterLocationTranslator.toOpenmrsType(locationComponent)).thenReturn(location);
		
		validator.validate(existingEncounter, fhirEncounter);
	}
	
	@Test
    public void shouldThrowExceptionWhenPatientReferenceDoesNotMatchExistingPatient() {

        when(patientReferenceTranslator.toOpenmrsType(patientReference)).thenReturn(newPatient);
        ValidationException exception = assertThrows(ValidationException.class, () -> validator.validate(existingEncounter, fhirEncounter));
        assertEquals("Patient reference in the encounter does not match the existing patient. Please check the patient reference.", exception.getMessage());
    }
	
	@Test
    public void shouldThrowExceptionWhenVisitReferenceIsInvalid() {
        when(patientReferenceTranslator.toOpenmrsType(patientReference)).thenReturn(existingPatient);
        when(visitReferenceTranslator.toOpenmrsType(visitReference)).thenReturn(null);

        ValidationException exception = assertThrows(ValidationException.class, () -> validator.validate(existingEncounter, fhirEncounter));
        assertEquals("Invalid Visit reference.", exception.getMessage());
    }
	
	@Test
    public void shouldThrowExceptionWhenLocationReferenceIsInvalid() {
        when(patientReferenceTranslator.toOpenmrsType(patientReference)).thenReturn(existingPatient);
        when(visitReferenceTranslator.toOpenmrsType(visitReference)).thenReturn(visit);
        when(encounterLocationTranslator.toOpenmrsType(locationComponent)).thenReturn(null);

        ValidationException exception = assertThrows(ValidationException.class, () -> validator.validate(existingEncounter, fhirEncounter));
        assertEquals("Invalid Location reference.", exception.getMessage());
    }
}
