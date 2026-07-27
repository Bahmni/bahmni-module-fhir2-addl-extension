/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.bahmni.module.fhir2addlextension.api.translator.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hl7.fhir.r4.model.Reference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Condition;
import org.openmrs.ConditionClinicalStatus;
import org.openmrs.ConditionVerificationStatus;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.ConditionClinicalStatusTranslator;
import org.openmrs.module.fhir2.api.translators.ConditionVerificationStatusTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;

/**
 * Unit tests for BahmniConditionTranslatorImpl. Verifies that the encounter reference is correctly
 * set in both toFhirResource (OpenMRS → FHIR) and toOpenmrsType (FHIR → OpenMRS) directions. Uses
 * {@literal @InjectMocks} so the real implementation runs end-to-end with all parent-class
 * dependencies mocked.
 */
@RunWith(MockitoJUnitRunner.class)
public class BahmniConditionTranslatorImplTest {
	
	// Parent class (ConditionTranslatorImpl) dependencies injected via @InjectMocks
	@Mock
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Mock
	private ConditionClinicalStatusTranslator<ConditionClinicalStatus> clinicalStatusTranslator;
	
	@Mock
	private ConditionVerificationStatusTranslator<ConditionVerificationStatus> verificationStatusTranslator;
	
	@Mock
	private PractitionerReferenceTranslator<User> practitionerReferenceTranslator;
	
	@Mock
	private ConceptTranslator conceptTranslator;
	
	// New dependency introduced by BahmniConditionTranslatorImpl
	@Mock
	private EncounterReferenceTranslator<Encounter> encounterReferenceTranslator;
	
	@InjectMocks
	private BahmniConditionTranslatorImpl translator;
	
	// ========== toFhirResource: encounter ==========
	
	@Test
	public void toFhirResource_setsEncounterWhenConditionHasEncounter() {
		Encounter encounter = new Encounter();
		encounter.setUuid("enc-uuid-1234");
		
		Condition condition = buildBaseCondition();
		condition.setEncounter(encounter);
		
		Reference encounterReference = new Reference("Encounter/enc-uuid-1234");
		when(encounterReferenceTranslator.toFhirResource(encounter)).thenReturn(encounterReference);
		
		org.hl7.fhir.r4.model.Condition fhirCondition = translator.toFhirResource(condition);
		
		assertThat(fhirCondition.getEncounter(), notNullValue());
		verify(encounterReferenceTranslator).toFhirResource(encounter);
	}
	
	@Test
	public void toFhirResource_doesNotSetEncounterWhenConditionHasNoEncounter() {
		Condition condition = buildBaseCondition();
		// condition.getEncounter() is null by default
		
		org.hl7.fhir.r4.model.Condition fhirCondition = translator.toFhirResource(condition);
		
		assertThat(fhirCondition.getEncounter().isEmpty(), org.hamcrest.Matchers.equalTo(true));
	}
	
	// ========== toOpenmrsType: encounter ==========
	
	@Test
	public void toOpenmrsType_setsEncounterWhenFhirConditionHasEncounter() {
		Encounter encounter = new Encounter();
		encounter.setUuid("enc-uuid-5678");
		
		org.hl7.fhir.r4.model.Condition fhirCondition = new org.hl7.fhir.r4.model.Condition();
		Reference encounterReference = new Reference("Encounter/enc-uuid-5678");
		fhirCondition.setEncounter(encounterReference);
		
		when(encounterReferenceTranslator.toOpenmrsType(encounterReference)).thenReturn(encounter);
		
		Condition result = translator.toOpenmrsType(new Condition(), fhirCondition);
		
		assertThat(result.getEncounter(), notNullValue());
		verify(encounterReferenceTranslator).toOpenmrsType(encounterReference);
	}
	
	@Test
	public void toOpenmrsType_doesNotSetEncounterWhenFhirConditionHasNoEncounter() {
		org.hl7.fhir.r4.model.Condition fhirCondition = new org.hl7.fhir.r4.model.Condition();
		// fhirCondition.hasEncounter() is false by default
		
		Condition result = translator.toOpenmrsType(new Condition(), fhirCondition);
		
		assertThat(result.getEncounter(), nullValue());
	}
	
	@Test
	public void toFhirResource_setsEncounterToNullWhenTranslatorReturnsNull() {
		Encounter encounter = new Encounter();
		encounter.setUuid("enc-uuid-null");
		
		Condition condition = buildBaseCondition();
		condition.setEncounter(encounter);
		
		when(encounterReferenceTranslator.toFhirResource(encounter)).thenReturn(null);
		
		org.hl7.fhir.r4.model.Condition fhirCondition = translator.toFhirResource(condition);
		
		assertThat(fhirCondition.getEncounter().isEmpty(), org.hamcrest.Matchers.equalTo(true));
		verify(encounterReferenceTranslator).toFhirResource(encounter);
	}
	
	// ========== HELPERS ==========
	
	private Condition buildBaseCondition() {
		Condition condition = new Condition();
		condition.setUuid("condition-uuid-1234");
		condition.setPatient(new Patient());
		return condition;
	}
}
