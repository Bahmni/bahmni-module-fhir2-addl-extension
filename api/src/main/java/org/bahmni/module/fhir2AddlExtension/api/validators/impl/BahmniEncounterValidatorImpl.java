package org.bahmni.module.fhir2AddlExtension.api.validators.impl;

import lombok.AccessLevel;
import lombok.Setter;
import org.bahmni.module.fhir2AddlExtension.api.validators.BahmniEncounterValidator;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.ValidationException;
import org.openmrs.module.fhir2.api.translators.EncounterLocationTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Setter(value = AccessLevel.PACKAGE)
public class BahmniEncounterValidatorImpl implements BahmniEncounterValidator {
	
	@Autowired
	private EncounterLocationTranslator encounterLocationTranslator;
	
	@Autowired
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Autowired
	private EncounterReferenceTranslator<Visit> visitReferenceTranslator;
	
	@Override
	public void validate(Encounter existingEncounter, org.hl7.fhir.r4.model.Encounter encounter) {
		validatePatientReference(existingEncounter, encounter);
		validateVisitReference(encounter);
		validateLocationReference(encounter);
	}
	
	private void validatePatientReference(Encounter existingEncounter, org.hl7.fhir.r4.model.Encounter encounter) {
		Patient patient = patientReferenceTranslator.toOpenmrsType(encounter.getSubject());
		if (patient != null && existingEncounter.getPatient() != null && !existingEncounter.getPatient().equals(patient))
			throw new ValidationException(
			        "Patient reference in the encounter does not match the existing patient. Please check the patient reference.");
	}
	
	private void validateVisitReference(org.hl7.fhir.r4.model.Encounter encounter) {
		Visit visit = visitReferenceTranslator.toOpenmrsType(encounter.getPartOf());
		if (visit == null) {
			throw new ValidationException("Invalid Visit reference.");
		}
	}
	
	private void validateLocationReference(org.hl7.fhir.r4.model.Encounter encounter) {
		Location location = encounterLocationTranslator.toOpenmrsType(encounter.getLocationFirstRep());
		if (location == null) {
			throw new ValidationException("Invalid Location reference.");
		}
	}
}
