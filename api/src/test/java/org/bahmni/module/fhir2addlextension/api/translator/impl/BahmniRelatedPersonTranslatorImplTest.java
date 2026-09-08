package org.bahmni.module.fhir2addlextension.api.translator.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

import org.bahmni.module.fhir2addlextension.api.translator.BahmniRelatedPersonTranslator;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedPerson;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.Relationship;
import org.openmrs.RelationshipType;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.module.fhir2.api.translators.BirthDateTranslator;
import org.openmrs.module.fhir2.api.translators.GenderTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PersonAddressTranslator;
import org.openmrs.module.fhir2.api.translators.PersonNameTranslator;

import java.util.Date;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class BahmniRelatedPersonTranslatorImplTest {
	
	// Test Data Constants
	private static final String PERSON_A_UUID = "person-a-uuid-1234-5678-9012";
	
	private static final String PERSON_B_UUID = "person-b-uuid-1234-5678-9012";
	
	private static final String RELATIONSHIP_UUID = "rel-uuid-1234-5678-9012-3456";
	
	private static final String RELATIONSHIP_TYPE_UUID = "rel-type-uuid-1234-5678-9012";
	
	private static final String A_IS_TO_B = "Parent";
	
	private static final String B_IS_TO_A = "Child";
	
	private static final String RELATED_PATIENT_EXT_URL = "http://fhir.bahmni.org/ext/relatedPatient";
	
	private static final String RELATIONSHIP_TYPE_SYSTEM = "http://fhir.bahmni.org/RelationshipType";
	
	@Mock
	private PersonNameTranslator nameTranslator;
	
	@Mock
	private GenderTranslator genderTranslator;
	
	@Mock
	private BirthDateTranslator birthDateTranslator;
	
	@Mock
	private PersonAddressTranslator addressTranslator;
	
	@Mock
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Mock
	private PersonService personService;
	
	@Mock
	private PatientService patientService;
	
	private BahmniRelatedPersonTranslator translator;
	
	@Before
	public void setup() {
		translator = new BahmniRelatedPersonTranslatorImpl(nameTranslator, genderTranslator, birthDateTranslator,
		        addressTranslator, patientReferenceTranslator, personService, patientService);
	}
	
	// ===============================
	// toFhirResource(Relationship) — DEFAULT PERSPECTIVE TESTS
	// Default: personB is the focal patient, personA is the related person
	// ===============================
	
	@Test
	public void toFhirResource_defaultPerspective_personBIsFocalPatient() {
		// Given — personB is a Patient (getIsPatient() == true), personA is a plain Person
		Patient patientB = buildPatient(PERSON_B_UUID);
		Relationship relationship = buildRelationshipBothPatients(buildPerson(PERSON_A_UUID), patientB);
		Reference patientBRef = buildPatientReference(PERSON_B_UUID);
		
		when(patientService.getPatient(patientB.getPersonId())).thenReturn(patientB);
		when(patientReferenceTranslator.toFhirResource(patientB)).thenReturn(patientBRef);
		
		// When
		RelatedPerson result = translator.toFhirResource(relationship);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.getId(), equalTo(RELATIONSHIP_UUID));
		assertThat(result.getPatient(), equalTo(patientBRef));
	}
	
	@Test
	public void toFhirResource_defaultPerspective_usesAIsToBRelationshipCode() {
		// Given
		Patient patientB = buildPatient(PERSON_B_UUID);
		Relationship relationship = buildRelationshipBothPatients(buildPerson(PERSON_A_UUID), patientB);
		
		when(patientService.getPatient(patientB.getPersonId())).thenReturn(patientB);
		when(patientReferenceTranslator.toFhirResource(patientB)).thenReturn(buildPatientReference(PERSON_B_UUID));
		
		// When
		RelatedPerson result = translator.toFhirResource(relationship);
		
		// Then — aIsToB code because personB is focal, personA is related
		assertThat(result.getRelationship(), hasSize(1));
		CodeableConcept relationshipCode = result.getRelationship().get(0);
		assertThat(relationshipCode.getCoding().get(0).getDisplay(), equalTo(A_IS_TO_B));
		assertThat(relationshipCode.getCoding().get(0).getCode(), equalTo(RELATIONSHIP_TYPE_UUID));
		assertThat(relationshipCode.getCoding().get(0).getSystem(), equalTo(RELATIONSHIP_TYPE_SYSTEM));
	}
	
	@Test
	public void toFhirResource_whenRelatedPersonIsPatient_extensionIsSet() {
		// Given — both personA and personB are Patients so the extension gets populated
		Patient patientA = buildPatient(PERSON_A_UUID);
		Patient patientB = buildPatient(PERSON_B_UUID);
		Relationship relationship = buildRelationshipBothPatients(patientA, patientB);
		Reference patientARef = buildPatientReference(PERSON_A_UUID);
		Reference patientBRef = buildPatientReference(PERSON_B_UUID);
		
		when(patientService.getPatient(patientB.getPersonId())).thenReturn(patientB);
		when(patientReferenceTranslator.toFhirResource(patientB)).thenReturn(patientBRef);
		when(patientService.getPatient(patientA.getPersonId())).thenReturn(patientA);
		when(patientReferenceTranslator.toFhirResource(patientA)).thenReturn(patientARef);
		
		// When
		RelatedPerson result = translator.toFhirResource(relationship);
		
		// Then — extension carries the related patient reference (personA)
		assertThat(result, notNullValue());
		assertThat(result.getExtensionsByUrl(RELATED_PATIENT_EXT_URL), hasSize(1));
		Reference extRef = (Reference) result.getExtensionsByUrl(RELATED_PATIENT_EXT_URL).get(0).getValue();
		assertThat(extRef, equalTo(patientARef));
	}
	
	@Test
	public void toFhirResource_whenRelatedPersonIsNotPatient_noExtensionSet() {
		// Given — personA is a plain Person (not patient) → no extension
		Person personA = buildPerson(PERSON_A_UUID);
		Patient patientB = buildPatient(PERSON_B_UUID);
		Relationship relationship = buildRelationshipBothPatients(personA, patientB);
		
		when(patientService.getPatient(patientB.getPersonId())).thenReturn(patientB);
		when(patientReferenceTranslator.toFhirResource(patientB)).thenReturn(buildPatientReference(PERSON_B_UUID));
		
		// When
		RelatedPerson result = translator.toFhirResource(relationship);
		
		// Then — no extension because personA is not a Patient
		assertThat(result.getExtensionsByUrl(RELATED_PATIENT_EXT_URL), hasSize(0));
	}
	
	// ===============================
	// toFhirResource(Relationship, subjectUuid) TESTS
	// ===============================
	
	@Test
	public void toFhirResource_withSubjectUuidMatchingPersonA_flipsToPersonAIsFocalPatient() {
		// Given — personA is a Patient (so getIsPatient() is true and patient ref is set)
		Patient patientA = buildPatient(PERSON_A_UUID);
		Patient patientB = buildPatient(PERSON_B_UUID);
		Relationship relationship = buildRelationshipBothPatients(patientA, patientB);
		Reference patientARef = buildPatientReference(PERSON_A_UUID);
		Reference patientBRef = buildPatientReference(PERSON_B_UUID);
		
		when(patientService.getPatient(patientA.getPersonId())).thenReturn(patientA);
		when(patientReferenceTranslator.toFhirResource(patientA)).thenReturn(patientARef);
		// personB becomes the relatedPerson (also a Patient → extension added)
		when(patientService.getPatient(patientB.getPersonId())).thenReturn(patientB);
		when(patientReferenceTranslator.toFhirResource(patientB)).thenReturn(patientBRef);
		
		// When
		RelatedPerson result = translator.toFhirResource(relationship, PERSON_A_UUID);
		
		// Then — patient reference is personA (the focal patient)
		assertThat(result, notNullValue());
		assertThat(result.getPatient(), equalTo(patientARef));
	}
	
	@Test
	public void toFhirResource_withSubjectUuidMatchingPersonA_usesBIsToARelationshipCode() {
		// Given
		Patient patientA = buildPatient(PERSON_A_UUID);
		Person personB = buildPerson(PERSON_B_UUID);
		Relationship relationship = buildRelationshipBothPatients(patientA, personB);
		Reference patientARef = buildPatientReference(PERSON_A_UUID);
		
		when(patientService.getPatient(patientA.getPersonId())).thenReturn(patientA);
		when(patientReferenceTranslator.toFhirResource(patientA)).thenReturn(patientARef);
		
		// When
		RelatedPerson result = translator.toFhirResource(relationship, PERSON_A_UUID);
		
		// Then — bIsToA code used when perspective is flipped
		assertThat(result.getRelationship(), hasSize(1));
		assertThat(result.getRelationship().get(0).getCoding().get(0).getDisplay(), equalTo(B_IS_TO_A));
	}
	
	@Test
	public void toFhirResource_withSubjectUuidMatchingPersonB_behavesLikeDefaultPerspective() {
		// Given — subjectUuid == personB.uuid → no flip, same as default
		Patient patientB = buildPatient(PERSON_B_UUID);
		Person personA = buildPerson(PERSON_A_UUID);
		Relationship relationship = buildRelationshipBothPatients(personA, patientB);
		Reference patientBRef = buildPatientReference(PERSON_B_UUID);
		
		when(patientService.getPatient(patientB.getPersonId())).thenReturn(patientB);
		when(patientReferenceTranslator.toFhirResource(patientB)).thenReturn(patientBRef);
		
		// When
		RelatedPerson result = translator.toFhirResource(relationship, PERSON_B_UUID);
		
		// Then — aIsToB code (default), patient is personB
		assertThat(result, notNullValue());
		assertThat(result.getPatient(), equalTo(patientBRef));
		assertThat(result.getRelationship().get(0).getCoding().get(0).getDisplay(), equalTo(A_IS_TO_B));
	}
	
	@Test
	public void toFhirResource_withNullSubjectUuid_behavesLikeDefaultPerspective() {
		// Given
		Patient patientB = buildPatient(PERSON_B_UUID);
		Person personA = buildPerson(PERSON_A_UUID);
		Relationship relationship = buildRelationshipBothPatients(personA, patientB);
		
		when(patientService.getPatient(patientB.getPersonId())).thenReturn(patientB);
		when(patientReferenceTranslator.toFhirResource(patientB)).thenReturn(buildPatientReference(PERSON_B_UUID));
		
		// When
		RelatedPerson result = translator.toFhirResource(relationship, null);
		
		// Then — falls back to default (aIsToB)
		assertThat(result, notNullValue());
		assertThat(result.getRelationship().get(0).getCoding().get(0).getDisplay(), equalTo(A_IS_TO_B));
	}
	
	// ===============================
	// buildPeriod BEHAVIOUR TESTS
	// ===============================
	
	@Test
	public void toFhirResource_whenBothDatesNull_periodNotSet() {
		// Given — no start/end dates
		Patient patientB = buildPatient(PERSON_B_UUID);
		Person personA = buildPerson(PERSON_A_UUID);
		Relationship relationship = buildRelationshipBothPatients(personA, patientB);
		relationship.setStartDate(null);
		relationship.setEndDate(null);
		
		when(patientService.getPatient(patientB.getPersonId())).thenReturn(patientB);
		when(patientReferenceTranslator.toFhirResource(patientB)).thenReturn(buildPatientReference(PERSON_B_UUID));
		
		// When
		RelatedPerson result = translator.toFhirResource(relationship);
		
		// Then
		assertThat(result.hasPeriod(), is(false));
	}
	
	@Test
	public void toFhirResource_whenEndDateSet_periodEndIsSet() {
		// Given
		Patient patientB = buildPatient(PERSON_B_UUID);
		Person personA = buildPerson(PERSON_A_UUID);
		Relationship relationship = buildRelationshipBothPatients(personA, patientB);
		Date endDate = new Date();
		relationship.setEndDate(endDate);
		
		when(patientService.getPatient(patientB.getPersonId())).thenReturn(patientB);
		when(patientReferenceTranslator.toFhirResource(patientB)).thenReturn(buildPatientReference(PERSON_B_UUID));
		
		// When
		RelatedPerson result = translator.toFhirResource(relationship);
		
		// Then
		assertThat(result.hasPeriod(), is(true));
		assertThat(result.getPeriod().getEnd(), equalTo(endDate));
	}
	
	@Test
	public void toFhirResource_whenStartDateSet_periodStartIsSet() {
		// Given
		Patient patientB = buildPatient(PERSON_B_UUID);
		Person personA = buildPerson(PERSON_A_UUID);
		Relationship relationship = buildRelationshipBothPatients(personA, patientB);
		Date startDate = new Date();
		relationship.setStartDate(startDate);
		relationship.setEndDate(null);
		
		when(patientService.getPatient(patientB.getPersonId())).thenReturn(patientB);
		when(patientReferenceTranslator.toFhirResource(patientB)).thenReturn(buildPatientReference(PERSON_B_UUID));
		
		// When
		RelatedPerson result = translator.toFhirResource(relationship);
		
		// Then
		assertThat(result.hasPeriod(), is(true));
		assertThat(result.getPeriod().getStart(), equalTo(startDate));
	}
	
	// ===============================
	// toOpenmrsType(RelatedPerson) TESTS
	// ===============================
	
	@Test
	public void toOpenmrsType_parsesPatientReferenceIntoPersonB() {
		// Given
		RelatedPerson relatedPerson = buildFhirRelatedPerson();
		Person personB = buildPerson(PERSON_B_UUID);
		
		when(personService.getPersonByUuid(PERSON_B_UUID)).thenReturn(personB);
		
		// When
		Relationship result = translator.toOpenmrsType(relatedPerson);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.getPersonB(), equalTo(personB));
	}
	
	@Test
	public void toOpenmrsType_parsesExtensionIntoPersonA() {
		// Given
		RelatedPerson relatedPerson = buildFhirRelatedPerson();
		Person personB = buildPerson(PERSON_B_UUID);
		Person personA = buildPerson(PERSON_A_UUID);
		
		// Add related patient extension for personA
		Reference personARef = new Reference("Patient/" + PERSON_A_UUID);
		relatedPerson.addExtension(new Extension(RELATED_PATIENT_EXT_URL, personARef));
		
		when(personService.getPersonByUuid(PERSON_B_UUID)).thenReturn(personB);
		when(personService.getPersonByUuid(PERSON_A_UUID)).thenReturn(personA);
		
		// When
		Relationship result = translator.toOpenmrsType(relatedPerson);
		
		// Then
		assertThat(result.getPersonA(), equalTo(personA));
	}
	
	@Test
	public void toOpenmrsType_parsesRelationshipCodingIntoType() {
		// Given
		RelatedPerson relatedPerson = buildFhirRelatedPerson();
		Person personB = buildPerson(PERSON_B_UUID);
		RelationshipType relType = buildRelationshipType();
		
		when(personService.getPersonByUuid(PERSON_B_UUID)).thenReturn(personB);
		when(personService.getRelationshipTypeByUuid(RELATIONSHIP_TYPE_UUID)).thenReturn(relType);
		
		// When
		Relationship result = translator.toOpenmrsType(relatedPerson);
		
		// Then
		assertThat(result.getRelationshipType(), equalTo(relType));
	}
	
	@Test
	public void toOpenmrsType_whenPeriodPresent_setsDates() {
		// Given
		RelatedPerson relatedPerson = buildFhirRelatedPerson();
		Person personB = buildPerson(PERSON_B_UUID);
		Date startDate = new Date(System.currentTimeMillis() - 10000);
		Date endDate = new Date();
		Period period = new Period();
		period.setStart(startDate);
		period.setEnd(endDate);
		relatedPerson.setPeriod(period);
		
		when(personService.getPersonByUuid(PERSON_B_UUID)).thenReturn(personB);
		
		// When
		Relationship result = translator.toOpenmrsType(relatedPerson);
		
		// Then
		assertThat(result.getStartDate(), equalTo(startDate));
		assertThat(result.getEndDate(), equalTo(endDate));
	}
	
	// ===============================
	// toOpenmrsType(Relationship, RelatedPerson) TESTS
	// ===============================
	
	@Test
	public void toOpenmrsType_existingRelationship_updatesPeriodDates() {
		// Given
		Patient patientA = buildPatient(PERSON_A_UUID);
		Patient patientB = buildPatient(PERSON_B_UUID);
		Relationship existing = buildRelationshipBothPatients(patientA, patientB);
		RelatedPerson relatedPerson = buildFhirRelatedPerson();
		Date startDate = new Date(System.currentTimeMillis() - 5000);
		Date endDate = new Date();
		Period period = new Period();
		period.setStart(startDate);
		period.setEnd(endDate);
		relatedPerson.setPeriod(period);
		
		// When
		Relationship result = translator.toOpenmrsType(existing, relatedPerson);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.getStartDate(), equalTo(startDate));
		assertThat(result.getEndDate(), equalTo(endDate));
	}
	
	@Test
	public void toOpenmrsType_existingRelationship_updatesRelationshipType() {
		// Given
		Patient patientA = buildPatient(PERSON_A_UUID);
		Patient patientB = buildPatient(PERSON_B_UUID);
		Relationship existing = buildRelationshipBothPatients(patientA, patientB);
		RelatedPerson relatedPerson = buildFhirRelatedPerson();
		RelationshipType newType = buildRelationshipType();
		
		when(personService.getRelationshipTypeByUuid(RELATIONSHIP_TYPE_UUID)).thenReturn(newType);
		
		// When
		Relationship result = translator.toOpenmrsType(existing, relatedPerson);
		
		// Then
		assertThat(result.getRelationshipType(), equalTo(newType));
	}
	
	@Test
	public void toOpenmrsType_existingRelationship_returnsSameInstance() {
		// Given
		Patient patientA = buildPatient(PERSON_A_UUID);
		Patient patientB = buildPatient(PERSON_B_UUID);
		Relationship existing = buildRelationshipBothPatients(patientA, patientB);
		RelatedPerson relatedPerson = buildFhirRelatedPerson();
		
		// When
		Relationship result = translator.toOpenmrsType(existing, relatedPerson);
		
		// Then — must return the same existing object (updated in place)
		assertThat(result, sameInstance(existing));
	}
	
	// ===============================
	// TEST DATA BUILDERS
	// ===============================
	
	/**
	 * Builds a Relationship with explicit personA and personB. Use Patient objects where
	 * getIsPatient() must return true, plain Person objects where it should return false.
	 */
	private Relationship buildRelationshipBothPatients(Person personA, Person personB) {
		RelationshipType type = buildRelationshipType();
		Relationship relationship = new Relationship();
		relationship.setUuid(RELATIONSHIP_UUID);
		relationship.setPersonA(personA);
		relationship.setPersonB(personB);
		relationship.setRelationshipType(type);
		return relationship;
	}
	
	private Person buildPerson(String uuid) {
		Person person = new Person();
		person.setUuid(uuid);
		person.setId(Math.abs(uuid.hashCode()) % 10000 + 1);
		return person;
	}
	
	private Patient buildPatient(String uuid) {
		Patient patient = new Patient();
		patient.setUuid(uuid);
		patient.setId(Math.abs(uuid.hashCode()) % 10000 + 1);
		return patient;
	}
	
	private RelationshipType buildRelationshipType() {
		RelationshipType type = new RelationshipType();
		type.setUuid(RELATIONSHIP_TYPE_UUID);
		type.setaIsToB(A_IS_TO_B);
		type.setbIsToA(B_IS_TO_A);
		return type;
	}
	
	private Reference buildPatientReference(String patientUuid) {
		Reference ref = new Reference();
		ref.setType("Patient");
		ref.setReference("Patient/" + patientUuid);
		return ref;
	}
	
	/**
	 * Builds a minimal valid FHIR RelatedPerson with patient reference pointing to personB and a
	 * relationship coding for the relationship type system.
	 */
	private RelatedPerson buildFhirRelatedPerson() {
		RelatedPerson relatedPerson = new RelatedPerson();
		relatedPerson.setId(RELATIONSHIP_UUID);
		
		// Patient reference pointing to personB (the focal patient)
		Reference patientRef = new Reference("Patient/" + PERSON_B_UUID);
		patientRef.setType("Patient");
		relatedPerson.setPatient(patientRef);
		
		// Relationship coding with the relationship type system and uuid
		CodeableConcept relationship = new CodeableConcept();
		Coding coding = new Coding();
		coding.setSystem(RELATIONSHIP_TYPE_SYSTEM);
		coding.setCode(RELATIONSHIP_TYPE_UUID);
		coding.setDisplay(A_IS_TO_B);
		relationship.addCoding(coding);
		relatedPerson.addRelationship(relationship);
		
		return relatedPerson;
	}
}
