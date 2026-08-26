package org.bahmni.module.fhir2addlextension.api.translator.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Allergy;
import org.openmrs.Encounter;
import org.openmrs.User;
import org.openmrs.module.fhir2.api.translators.AllergyIntoleranceCategoryTranslator;
import org.openmrs.module.fhir2.api.translators.AllergyIntoleranceCriticalityTranslator;
import org.openmrs.module.fhir2.api.translators.AllergyIntoleranceReactionComponentTranslator;
import org.openmrs.module.fhir2.api.translators.AllergyIntoleranceSeverityTranslator;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.impl.AllergyIntoleranceTranslatorImpl;
import org.springframework.util.ReflectionUtils;

@RunWith(MockitoJUnitRunner.class)
public class BahmniAllergyIntoleranceTranslatorImplTest {
	
	@Mock
	private EncounterReferenceTranslator<Encounter> encounterReferenceTranslator;
	
	@Mock
	private PractitionerReferenceTranslator<User> practitionerReferenceTranslator;
	
	@Mock
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Mock
	private ConceptTranslator conceptTranslator;
	
	@Mock
	private AllergyIntoleranceSeverityTranslator severityTranslator;
	
	@Mock
	private AllergyIntoleranceCriticalityTranslator criticalityTranslator;
	
	@Mock
	private AllergyIntoleranceCategoryTranslator categoryTranslator;
	
	@Mock
	private AllergyIntoleranceReactionComponentTranslator reactionComponentTranslator;
	
	private BahmniAllergyIntoleranceTranslatorImpl translator;
	
	@Before
	public void setup() {
		translator = new BahmniAllergyIntoleranceTranslatorImpl();
		injectField(AllergyIntoleranceTranslatorImpl.class, "practitionerReferenceTranslator",
		    practitionerReferenceTranslator);
		injectField(AllergyIntoleranceTranslatorImpl.class, "patientReferenceTranslator", patientReferenceTranslator);
		injectField(AllergyIntoleranceTranslatorImpl.class, "conceptTranslator", conceptTranslator);
		injectField(AllergyIntoleranceTranslatorImpl.class, "severityTranslator", severityTranslator);
		injectField(AllergyIntoleranceTranslatorImpl.class, "criticalityTranslator", criticalityTranslator);
		injectField(AllergyIntoleranceTranslatorImpl.class, "categoryTranslator", categoryTranslator);
		injectField(AllergyIntoleranceTranslatorImpl.class, "reactionComponentTranslator", reactionComponentTranslator);
		injectField(BahmniAllergyIntoleranceTranslatorImpl.class, "encounterReferenceTranslator",
		    encounterReferenceTranslator);
	}
	
	@Test
	public void toFhirResource_shouldMapEncounterWhenPresent() {
		Allergy allergy = new Allergy();
		allergy.setUuid("allergy-uuid");
		Encounter encounter = new Encounter();
		encounter.setUuid("encounter-uuid");
		allergy.setEncounter(encounter);
		
		Reference encounterRef = new Reference("Encounter/encounter-uuid");
		when(encounterReferenceTranslator.toFhirResource(encounter)).thenReturn(encounterRef);
		
		AllergyIntolerance result = translator.toFhirResource(allergy);
		
		assertNotNull(result.getEncounter());
		assertEquals("Encounter/encounter-uuid", result.getEncounter().getReference());
	}
	
	@Test
	public void toFhirResource_shouldNotSetEncounterWhenNull() {
		Allergy allergy = new Allergy();
		allergy.setUuid("allergy-uuid");
		
		AllergyIntolerance result = translator.toFhirResource(allergy);
		
		assertFalse(result.hasEncounter());
	}
	
	@Test
	public void toOpenmrsType_shouldSetEncounterFromFhirResource() {
		Allergy existingAllergy = new Allergy();
		AllergyIntolerance fhirAllergy = new AllergyIntolerance();
		Reference encounterRef = new Reference("Encounter/encounter-uuid");
		fhirAllergy.setEncounter(encounterRef);
		
		Encounter encounter = new Encounter();
		when(encounterReferenceTranslator.toOpenmrsType(encounterRef)).thenReturn(encounter);
		
		Allergy result = translator.toOpenmrsType(existingAllergy, fhirAllergy);
		
		assertEquals(encounter, result.getEncounter());
	}
	
	@Test
	public void toOpenmrsType_shouldNotSetEncounterWhenAbsent() {
		Allergy existingAllergy = new Allergy();
		AllergyIntolerance fhirAllergy = new AllergyIntolerance();
		
		Allergy result = translator.toOpenmrsType(existingAllergy, fhirAllergy);
		
		assertNull(result.getEncounter());
		verifyNoInteractions(encounterReferenceTranslator);
	}
	
	private void injectField(Class<?> declaringClass, String fieldName, Object value) {
		Field field = ReflectionUtils.findField(declaringClass, fieldName);
		field.setAccessible(true);
		ReflectionUtils.setField(field, translator, value);
	}
}
