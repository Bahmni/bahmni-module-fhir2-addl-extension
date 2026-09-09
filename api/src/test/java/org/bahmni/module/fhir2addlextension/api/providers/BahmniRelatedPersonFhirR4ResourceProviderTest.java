package org.bahmni.module.fhir2addlextension.api.providers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniRelatedPersonSearchParams;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirRelatedPersonService;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedPerson;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class BahmniRelatedPersonFhirR4ResourceProviderTest {
	
	// Test Data Constants
	private static final String RELATED_PERSON_UUID = "related-person-uuid-1234-5678";
	
	private static final String PATIENT_UUID = "patient-uuid-1234-5678-9012";
	
	private static final String RELATIONSHIP_TYPE_UUID = "rel-type-uuid-1234-5678-9012";
	
	private static final String RELATIONSHIP_TYPE_SYSTEM = "http://fhir.bahmni.org/RelationshipType";
	
	@Mock
	private BahmniFhirRelatedPersonService relatedPersonService;
	
	private BahmniRelatedPersonFhirR4ResourceProvider provider;
	
	@Before
	public void setup() {
		provider = new BahmniRelatedPersonFhirR4ResourceProvider(relatedPersonService);
	}
	
	// ===============================
	// getResourceType TEST
	// ===============================
	
	@Test
	public void getResourceType_returnsRelatedPersonClass() {
		assertThat(provider.getResourceType(), equalTo(RelatedPerson.class));
	}
	
	// ===============================
	// getRelatedPersonById TESTS
	// ===============================
	
	@Test
	public void getRelatedPersonById_whenFound_returnsRelatedPerson() {
		// Given
		IdType id = new IdType(RELATED_PERSON_UUID);
		RelatedPerson expectedRelatedPerson = buildRelatedPerson();
		
		when(relatedPersonService.get(RELATED_PERSON_UUID)).thenReturn(expectedRelatedPerson);
		
		// When
		RelatedPerson result = provider.getRelatedPersonById(id);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.getId(), equalTo(RELATED_PERSON_UUID));
	}
	
	@Test(expected = ResourceNotFoundException.class)
	public void getRelatedPersonById_whenNotFound_throwsResourceNotFoundException() {
		// Given
		IdType id = new IdType(RELATED_PERSON_UUID);
		when(relatedPersonService.get(RELATED_PERSON_UUID)).thenReturn(null);
		
		// When/Then
		provider.getRelatedPersonById(id);
	}
	
	// ===============================
	// createRelatedPerson TESTS
	// ===============================
	
	@Test
	public void createRelatedPerson_withValidRelatedPerson_returnsMethodOutcomeWithCreatedResource() {
		// Given
		RelatedPerson relatedPerson = buildRelatedPersonWithPatientAndRelationship();
		RelatedPerson createdRelatedPerson = buildRelatedPerson();
		
		when(relatedPersonService.create(relatedPerson)).thenReturn(createdRelatedPerson);
		
		// When
		MethodOutcome result = provider.createRelatedPerson(relatedPerson);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.getResource(), notNullValue());
		assertThat(result.getResource().getIdElement().getIdPart(), equalTo(RELATED_PERSON_UUID));
	}
	
	@Test(expected = InvalidRequestException.class)
	public void createRelatedPerson_missingPatientReference_throwsInvalidRequestException() {
		// Given — RelatedPerson with relationship but no patient reference
		RelatedPerson relatedPerson = new RelatedPerson();
		relatedPerson.addRelationship(buildRelationshipCodeableConcept());
		
		// When/Then
		provider.createRelatedPerson(relatedPerson);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void createRelatedPerson_missingRelationship_throwsInvalidRequestException() {
		// Given — RelatedPerson with patient reference but no relationship
		RelatedPerson relatedPerson = new RelatedPerson();
		relatedPerson.setPatient(new Reference("Patient/" + PATIENT_UUID));
		
		// When/Then
		provider.createRelatedPerson(relatedPerson);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void createRelatedPerson_missingBothPatientAndRelationship_throwsInvalidRequestException() {
		// Given — completely empty RelatedPerson
		RelatedPerson relatedPerson = new RelatedPerson();
		
		// When/Then
		provider.createRelatedPerson(relatedPerson);
	}
	
	// ===============================
	// deleteRelatedPerson TESTS
	// ===============================
	
	@Test
	public void deleteRelatedPerson_callsServiceDelete() {
		// Given
		IdType id = new IdType(RELATED_PERSON_UUID);
		
		// When
		provider.deleteRelatedPerson(id);
		
		// Then
		verify(relatedPersonService).delete(RELATED_PERSON_UUID);
	}
	
	// ===============================
	// searchRelatedPersons TESTS
	// ===============================
	
	@Test
	public void searchRelatedPersons_withPatientParam_returnsIBundleProvider() {
		// Given
		ReferenceAndListParam patientParam = buildPatientReference(PATIENT_UUID);
		IBundleProvider expectedBundle = new SimpleBundleProvider(Collections.singletonList(buildRelatedPerson()));
		
		when(relatedPersonService.searchByPatient(any(BahmniRelatedPersonSearchParams.class))).thenReturn(expectedBundle);
		
		// When
		IBundleProvider result = provider.searchRelatedPersons(patientParam, null, null);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.getAllResources(), hasSize(1));
	}
	
	@Test
	public void searchRelatedPersons_withNullPatientParam_delegatesToService() {
		// Given — null patient param is passed through to the service (validation is service's job)
		IBundleProvider expectedBundle = new SimpleBundleProvider(Collections.emptyList());
		
		when(relatedPersonService.searchByPatient(any(BahmniRelatedPersonSearchParams.class))).thenReturn(expectedBundle);
		
		// When
		IBundleProvider result = provider.searchRelatedPersons(null, null, null);
		
		// Then
		assertThat(result, notNullValue());
	}
	
	@Test
	public void searchRelatedPersons_returnsEmptyBundle_whenServiceReturnsNoResults() {
		// Given
		ReferenceAndListParam patientParam = buildPatientReference(PATIENT_UUID);
		IBundleProvider emptyBundle = new SimpleBundleProvider(Collections.emptyList());
		
		when(relatedPersonService.searchByPatient(any(BahmniRelatedPersonSearchParams.class))).thenReturn(emptyBundle);
		
		// When
		IBundleProvider result = provider.searchRelatedPersons(patientParam, null, null);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.getAllResources(), hasSize(0));
	}
	
	// ===============================
	// TEST DATA BUILDERS
	// ===============================
	
	private RelatedPerson buildRelatedPerson() {
		RelatedPerson relatedPerson = new RelatedPerson();
		relatedPerson.setId(RELATED_PERSON_UUID);
		return relatedPerson;
	}
	
	private RelatedPerson buildRelatedPersonWithPatientAndRelationship() {
		RelatedPerson relatedPerson = new RelatedPerson();
		relatedPerson.setId(RELATED_PERSON_UUID);
		relatedPerson.setPatient(new Reference("Patient/" + PATIENT_UUID));
		relatedPerson.addRelationship(buildRelationshipCodeableConcept());
		return relatedPerson;
	}
	
	private CodeableConcept buildRelationshipCodeableConcept() {
		CodeableConcept relationship = new CodeableConcept();
		Coding coding = new Coding();
		coding.setSystem(RELATIONSHIP_TYPE_SYSTEM);
		coding.setCode(RELATIONSHIP_TYPE_UUID);
		coding.setDisplay("Parent");
		relationship.addCoding(coding);
		return relationship;
	}
	
	private ReferenceAndListParam buildPatientReference(String patientUuid) {
		return new ReferenceAndListParam()
		        .addAnd(new ReferenceOrListParam().add(new ReferenceParam().setValue(patientUuid)));
	}
}
