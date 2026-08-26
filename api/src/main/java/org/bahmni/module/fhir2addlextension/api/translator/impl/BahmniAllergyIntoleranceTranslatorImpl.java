package org.bahmni.module.fhir2addlextension.api.translator.impl;

import javax.annotation.Nonnull;

import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.openmrs.Allergy;
import org.openmrs.Encounter;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.impl.AllergyIntoleranceTranslatorImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Extends the base AllergyIntolerance translator to map the encounter field, which the upstream
 * implementation omits. Required by BAH-4772 so that allergy edits can be routed to the correct
 * encounter session on the backend.
 */
@Component
@Primary
public class BahmniAllergyIntoleranceTranslatorImpl extends AllergyIntoleranceTranslatorImpl {
	
	@Autowired
	private EncounterReferenceTranslator<Encounter> encounterReferenceTranslator;
	
	@Override
	public AllergyIntolerance toFhirResource(@Nonnull Allergy omrsAllergy) {
		AllergyIntolerance fhirAllergy = super.toFhirResource(omrsAllergy);
		if (omrsAllergy.getEncounter() != null) {
			fhirAllergy.setEncounter(encounterReferenceTranslator.toFhirResource(omrsAllergy.getEncounter()));
		}
		return fhirAllergy;
	}
	
	@Override
	public Allergy toOpenmrsType(@Nonnull Allergy existingAllergy, @Nonnull AllergyIntolerance fhirAllergy) {
		existingAllergy = super.toOpenmrsType(existingAllergy, fhirAllergy);
		if (fhirAllergy.hasEncounter()) {
			existingAllergy.setEncounter(encounterReferenceTranslator.toOpenmrsType(fhirAllergy.getEncounter()));
		}
		return existingAllergy;
	}
}
