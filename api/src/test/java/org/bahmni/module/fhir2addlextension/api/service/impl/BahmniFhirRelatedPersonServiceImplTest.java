package org.bahmni.module.fhir2addlextension.api.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirRelatedPersonDao;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniRelatedPersonSearchParams;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirRelatedPersonService;
import org.bahmni.module.fhir2addlextension.api.translator.BahmniRelatedPersonTranslator;
import org.hl7.fhir.r4.model.RelatedPerson;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Relationship;
import org.openmrs.RelationshipType;
import org.openmrs.module.fhir2.api.search.param.RelatedPersonSearchParams;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirRelatedPersonServiceImplTest {
	
	// Test Data Constants
	private static final String PATIENT_UUID = "patient-uuid-1234-5678-9012";
	
	private static final String RELATIONSHIP_UUID = "rel-uuid-1234-5678-9012-3456";
	
	@Mock
	private BahmniFhirRelatedPersonDao dao;
	
	@Mock
	private BahmniRelatedPersonTranslator translator;
	
	private BahmniFhirRelatedPersonService relatedPersonService;
	
	@Before
	public void setup() {
		relatedPersonService = new BahmniFhirRelatedPersonServiceImpl(dao, translator);
	}
	
	// ===============================
	// searchByPatient TESTS
	// ===============================
	
	@Test
	public void searchByPatient_withValidPatientReference_returnsIBundleProvider() {
		// Given
		ReferenceAndListParam patientReference = buildPatientReference(PATIENT_UUID);
		BahmniRelatedPersonSearchParams searchParams = BahmniRelatedPersonSearchParams.builder()
		        .patientReference(patientReference).build();
		
		Relationship relationship = buildRelationship();
		RelatedPerson relatedPerson = buildRelatedPerson();
		
		when(dao.getSearchResults(any(SearchParameterMap.class))).thenReturn(Collections.singletonList(relationship));
		when(translator.toFhirResource(relationship, PATIENT_UUID)).thenReturn(relatedPerson);
		
		// When
		IBundleProvider result = relatedPersonService.searchByPatient(searchParams);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.getAllResources(), hasSize(1));
		assertThat(result.getAllResources().get(0), equalTo(relatedPerson));
	}
	
	@Test
	public void searchByPatient_withMultipleRelationships_returnsAllTranslated() {
		// Given
		ReferenceAndListParam patientReference = buildPatientReference(PATIENT_UUID);
		BahmniRelatedPersonSearchParams searchParams = BahmniRelatedPersonSearchParams.builder()
		        .patientReference(patientReference).build();
		
		Relationship rel1 = buildRelationship();
		Relationship rel2 = buildRelationship();
		RelatedPerson rp1 = buildRelatedPerson();
		RelatedPerson rp2 = buildRelatedPerson();
		
		List<Relationship> relationships = java.util.Arrays.asList(rel1, rel2);
		when(dao.getSearchResults(any(SearchParameterMap.class))).thenReturn(relationships);
		when(translator.toFhirResource(rel1, PATIENT_UUID)).thenReturn(rp1);
		when(translator.toFhirResource(rel2, PATIENT_UUID)).thenReturn(rp2);
		
		// When
		IBundleProvider result = relatedPersonService.searchByPatient(searchParams);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.getAllResources(), hasSize(2));
	}
	
	@Test(expected = InvalidRequestException.class)
	public void searchByPatient_withNullPatientReference_throwsInvalidRequestException() {
		// Given — no patient reference at all
		BahmniRelatedPersonSearchParams searchParams = BahmniRelatedPersonSearchParams.builder().patientReference(null)
		        .build();
		
		// When/Then
		relatedPersonService.searchByPatient(searchParams);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void searchByPatient_withEmptyPatientReferenceValue_throwsInvalidRequestException() {
		// Given — patient param present but value is blank
		ReferenceAndListParam emptyReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue("")));
		BahmniRelatedPersonSearchParams searchParams = BahmniRelatedPersonSearchParams.builder()
		        .patientReference(emptyReference).build();
		
		// When/Then
		relatedPersonService.searchByPatient(searchParams);
	}
	
	@Test
	public void searchByPatient_withNoResults_returnsEmptyBundleProvider() {
		// Given
		ReferenceAndListParam patientReference = buildPatientReference(PATIENT_UUID);
		BahmniRelatedPersonSearchParams searchParams = BahmniRelatedPersonSearchParams.builder()
		        .patientReference(patientReference).build();
		
		when(dao.getSearchResults(any(SearchParameterMap.class))).thenReturn(Collections.emptyList());
		
		// When
		IBundleProvider result = relatedPersonService.searchByPatient(searchParams);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.getAllResources(), hasSize(0));
	}
	
	// ===============================
	// searchForRelatedPeople TESTS
	// ===============================
	
	@Test(expected = NotImplementedOperationException.class)
	public void searchForRelatedPeople_alwaysThrowsNotImplementedOperationException() {
		// Given
		RelatedPersonSearchParams searchParams = new RelatedPersonSearchParams();
		
		// When/Then
		relatedPersonService.searchForRelatedPeople(searchParams);
	}
	
	// ===============================
	// TEST DATA BUILDERS
	// ===============================
	
	private ReferenceAndListParam buildPatientReference(String patientUuid) {
		return new ReferenceAndListParam()
		        .addAnd(new ReferenceOrListParam().add(new ReferenceParam().setValue(patientUuid)));
	}
	
	private Relationship buildRelationship() {
		RelationshipType type = new RelationshipType();
		type.setUuid(UUID.randomUUID().toString());
		type.setaIsToB("Parent");
		type.setbIsToA("Child");
		
		Relationship relationship = new Relationship();
		relationship.setUuid(RELATIONSHIP_UUID);
		relationship.setRelationshipType(type);
		return relationship;
	}
	
	private RelatedPerson buildRelatedPerson() {
		RelatedPerson relatedPerson = new RelatedPerson();
		relatedPerson.setId(UUID.randomUUID().toString());
		return relatedPerson;
	}
}
