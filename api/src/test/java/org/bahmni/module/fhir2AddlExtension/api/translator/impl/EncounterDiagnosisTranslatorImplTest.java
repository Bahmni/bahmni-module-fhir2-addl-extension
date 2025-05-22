package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.CodedOrFreeText;
import org.openmrs.Concept;
import org.openmrs.ConditionVerificationStatus;
import org.openmrs.Diagnosis;
import org.openmrs.User;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.ConditionVerificationStatusTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;

import java.util.Collections;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EncounterDiagnosisTranslatorImplTest {

    private static final String DIAGNOSIS_UUID = "1e589127-f391-4d0c-8e98-e0a158b4c3f1";
    private static final String PATIENT_UUID = "8adf539e-4b5a-47aa-80c0-ba1025c957fa";
    private static final String ENCOUNTER_UUID = "7209adc9-3e54-4c9f-9d9f-546e5a2d1e98";
    private static final String CREATOR_UUID = "6adf539e-4b5a-47aa-80c0-ba1025c957fa";
    private static final String CONCEPT_UUID = "5adf539e-4b5a-47aa-80c0-ba1025c957fa";
    private static final String NON_CODED_TEXT = "Non-coded diagnosis text";

    @Mock
    private PatientReferenceTranslator patientReferenceTranslator;

    @Mock
    private ConditionVerificationStatusTranslator<ConditionVerificationStatus> verificationStatusTranslator;

    @Mock
    private PractitionerReferenceTranslator<User> practitionerReferenceTranslator;

    @Mock
    private ConceptTranslator conceptTranslator;

    @Mock
    private EncounterReferenceTranslator<org.openmrs.Encounter> encounterReferenceTranslator;

    private EncounterDiagnosisTranslatorImpl translator;

    private Diagnosis diagnosis;
    private org.openmrs.Patient patient;
    private org.openmrs.Encounter encounter;
    private User creator;
    private Concept concept;
    private CodedOrFreeText codedOrFreeText;
    private Reference patientReference;
    private Reference encounterReference;
    private Reference practitionerReference;
    private CodeableConcept codedConcept;
    private CodeableConcept verificationStatusCodeableConcept;

    @Before
    public void setup() {
        translator = new EncounterDiagnosisTranslatorImpl();
        translator.setPatientReferenceTranslator(patientReferenceTranslator);
        translator.setPractitionerReferenceTranslator(practitionerReferenceTranslator);
        translator.setConceptTranslator(conceptTranslator);
        translator.setEncounterReferenceTranslator(encounterReferenceTranslator);
        translator.setVerificationStatusTranslator(verificationStatusTranslator);

        // Create test objects
        patient = new org.openmrs.Patient();
        patient.setUuid(PATIENT_UUID);

        encounter = new org.openmrs.Encounter();
        encounter.setUuid(ENCOUNTER_UUID);

        creator = new User();
        creator.setUuid(CREATOR_UUID);

        concept = new Concept();
        concept.setUuid(CONCEPT_UUID);

        codedOrFreeText = new CodedOrFreeText();
        codedOrFreeText.setCoded(concept);
        codedOrFreeText.setNonCoded(NON_CODED_TEXT);

        diagnosis = new Diagnosis();
        diagnosis.setUuid(DIAGNOSIS_UUID);
        diagnosis.setPatient(patient);
        diagnosis.setEncounter(encounter);
        diagnosis.setCreator(creator);
        diagnosis.setDiagnosis(codedOrFreeText);
        diagnosis.setCertainty(ConditionVerificationStatus.CONFIRMED);
        diagnosis.setDateCreated(new Date());

        // Set up mock responses
        patientReference = new Reference().setReference(FhirConstants.PATIENT + "/" + PATIENT_UUID);
        when(patientReferenceTranslator.toFhirResource(patient)).thenReturn(patientReference);
        when(patientReferenceTranslator.toOpenmrsType(patientReference)).thenReturn(patient);

        encounterReference = new Reference().setReference(FhirConstants.ENCOUNTER + "/" + ENCOUNTER_UUID);
        when(encounterReferenceTranslator.toFhirResource(encounter)).thenReturn(encounterReference);
        when(encounterReferenceTranslator.toOpenmrsType(encounterReference)).thenReturn(encounter);

        practitionerReference = new Reference().setReference(FhirConstants.PRACTITIONER + "/" + CREATOR_UUID);
        when(practitionerReferenceTranslator.toFhirResource(creator)).thenReturn(practitionerReference);
        when(practitionerReferenceTranslator.toOpenmrsType(practitionerReference)).thenReturn(creator);

        codedConcept = new CodeableConcept();
        codedConcept.addCoding(new Coding().setCode(CONCEPT_UUID));
        when(conceptTranslator.toFhirResource(concept)).thenReturn(codedConcept);
        when(conceptTranslator.toOpenmrsType(codedConcept)).thenReturn(concept);

        verificationStatusCodeableConcept = new CodeableConcept();
        verificationStatusCodeableConcept.addCoding(new Coding().setCode("confirmed"));

        when(verificationStatusTranslator.toFhirResource(ConditionVerificationStatus.CONFIRMED))
                .thenReturn(verificationStatusCodeableConcept);
        when(verificationStatusTranslator.toOpenmrsType(verificationStatusCodeableConcept))
                .thenReturn(ConditionVerificationStatus.CONFIRMED);
    }

    @Test
    public void toFhirResource_shouldTranslateDiagnosisToCondition() {
        Condition result = translator.toFhirResource(diagnosis);

        assertNotNull(result);
        assertEquals(DIAGNOSIS_UUID, result.getId());
        assertEquals(patientReference, result.getSubject());
        assertEquals(verificationStatusCodeableConcept, result.getVerificationStatus());
        assertEquals(encounterReference, result.getEncounter());
        assertEquals(practitionerReference, result.getRecorder());
        assertEquals(diagnosis.getDateCreated(), result.getRecordedDate());
        
        // Verify category
        assertThat(result.getCategory(), hasSize(1));
        Coding categoryCoding = result.getCategory().get(0).getCodingFirstRep();
        assertEquals(BahmniFhirConstants.HL7_CONDITION_CATEGORY_CODE_SYSTEM, categoryCoding.getSystem());
        assertEquals(BahmniFhirConstants.HL7_CONDITION_CATEGORY_DIAGNOSIS_CODE, categoryCoding.getCode());
        
        // Verify code
        assertEquals(codedConcept, result.getCode());
        
        // Verify non-coded extension
        Extension nonCodedExtension = result.getExtensionByUrl(FhirConstants.OPENMRS_FHIR_EXT_NON_CODED_CONDITION);
        assertNotNull(nonCodedExtension);
        assertEquals(NON_CODED_TEXT, ((StringType) nonCodedExtension.getValue()).getValue());
    }

    @Test(expected = NullPointerException.class)
    public void toFhirResource_shouldThrowExceptionForNullDiagnosis() {
        translator.toFhirResource(null);
    }

    @Test
    public void toFhirResource_shouldHandleNullCodedOrFreeText() {
        diagnosis.setDiagnosis(null);
        
        Condition result = translator.toFhirResource(diagnosis);
        
        assertNotNull(result);
        assertThat(result.hasCode(), is(false));
        assertThat(result.getExtension(), empty());
    }

    @Test
    public void toFhirResource_shouldHandleNullNonCoded() {
        codedOrFreeText.setNonCoded(null);
        diagnosis.setDiagnosis(codedOrFreeText);
        
        Condition result = translator.toFhirResource(diagnosis);
        
        assertNotNull(result);
        assertEquals(codedConcept, result.getCode());
        assertThat(result.getExtensionByUrl(FhirConstants.OPENMRS_FHIR_EXT_NON_CODED_CONDITION), nullValue());
    }

    @Test
    public void toOpenmrsType_shouldTranslateConditionToNewDiagnosis() {
        Condition condition = new Condition();
        condition.setId(DIAGNOSIS_UUID);
        condition.setSubject(patientReference);
        condition.setVerificationStatus(verificationStatusCodeableConcept);
        condition.setEncounter(encounterReference);
        condition.setRecorder(practitionerReference);
        condition.setCode(codedConcept);
        
        Extension nonCodedExtension = new Extension();
        nonCodedExtension.setUrl(FhirConstants.OPENMRS_FHIR_EXT_NON_CODED_CONDITION);
        nonCodedExtension.setValue(new StringType(NON_CODED_TEXT));
        condition.addExtension(nonCodedExtension);
        
        Diagnosis result = translator.toOpenmrsType(condition);
        
        assertNotNull(result);
        assertEquals(DIAGNOSIS_UUID, result.getUuid());
        assertEquals(patient, result.getPatient());
        assertEquals(ConditionVerificationStatus.CONFIRMED, result.getCertainty());
        assertEquals(encounter, result.getEncounter());
        assertEquals(creator, result.getCreator());
        assertEquals(1, (int)result.getRank());
        
        // Verify coded or free text
        assertNotNull(result.getDiagnosis());
        assertEquals(concept, result.getDiagnosis().getCoded());
        assertEquals(NON_CODED_TEXT, result.getDiagnosis().getNonCoded());
    }

    @Test
    public void toOpenmrsType_shouldUpdateExistingDiagnosis() {
        Diagnosis existingDiagnosis = new Diagnosis();
        existingDiagnosis.setUuid("old-uuid");
        
        Condition condition = new Condition();
        condition.setId(DIAGNOSIS_UUID);
        condition.setSubject(patientReference);
        condition.setVerificationStatus(verificationStatusCodeableConcept);
        condition.setEncounter(encounterReference);
        condition.setRecorder(practitionerReference);
        condition.setCode(codedConcept);
        
        Diagnosis result = translator.toOpenmrsType(existingDiagnosis, condition);
        
        assertNotNull(result);
        assertEquals(DIAGNOSIS_UUID, result.getUuid());
        assertEquals(patient, result.getPatient());
        assertEquals(ConditionVerificationStatus.CONFIRMED, result.getCertainty());
        assertEquals(encounter, result.getEncounter());
        assertEquals(creator, result.getCreator());
        assertEquals(1, (int)result.getRank());
    }

    @Test(expected = NullPointerException.class)
    public void toOpenmrsType_shouldThrowExceptionForNullCondition() {
        translator.toOpenmrsType((Condition) null);
    }

    @Test(expected = NullPointerException.class)
    public void toOpenmrsType_shouldThrowExceptionForNullExistingDiagnosis() {
        Condition condition = new Condition();
        translator.toOpenmrsType(null, condition);
    }

    @Test(expected = NullPointerException.class)
    public void toOpenmrsType_shouldThrowExceptionForNullConditionWhenUpdating() {
        Diagnosis existingDiagnosis = new Diagnosis();
        translator.toOpenmrsType(existingDiagnosis, null);
    }

    @Test
    public void toOpenmrsType_shouldHandleConditionWithoutNonCodedExtension() {
        Condition condition = new Condition();
        condition.setId(DIAGNOSIS_UUID);
        condition.setSubject(patientReference);
        condition.setVerificationStatus(verificationStatusCodeableConcept);
        condition.setEncounter(encounterReference);
        condition.setRecorder(practitionerReference);
        condition.setCode(codedConcept);
        
        Diagnosis result = translator.toOpenmrsType(condition);
        
        assertNotNull(result);
        assertNotNull(result.getDiagnosis());
        assertEquals(concept, result.getDiagnosis().getCoded());
        assertThat(result.getDiagnosis().getNonCoded(), nullValue());
    }
}