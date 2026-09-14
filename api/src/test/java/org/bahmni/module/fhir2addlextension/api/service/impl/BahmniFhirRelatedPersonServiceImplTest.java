package org.bahmni.module.fhir2addlextension.api.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
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
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import java.util.Collections;
import java.util.UUID;
import org.mockito.ArgumentCaptor;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirRelatedPersonServiceImplTest {
	
	// Test Data Constants
	private static final String PATIENT_UUID = "patient-uuid-1234-5678-9012";
	
	private static final String RELATIONSHIP_UUID = "rel-uuid-1234-5678-9012-3456";
	
	@Mock
	private BahmniFhirRelatedPersonDao dao;
	
	@Mock
	private BahmniRelatedPersonTranslator translator;
	
	@Mock
	private SearchQueryInclude<RelatedPerson> searchQueryInclude;
	
	@Mock
	private SearchQuery<Relationship, RelatedPerson, BahmniFhirRelatedPersonDao, BahmniRelatedPersonTranslator, SearchQueryInclude<RelatedPerson>> searchQuery;
	
	private BahmniFhirRelatedPersonService relatedPersonService;
	
	@Before
	public void setup() {
		relatedPersonService = new BahmniFhirRelatedPersonServiceImpl(dao, translator, searchQueryInclude, searchQuery);
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
		
		RelatedPerson relatedPerson = buildRelatedPerson();
		IBundleProvider expectedBundle = new SimpleBundleProvider(Collections.singletonList(relatedPerson));
		
		when(searchQuery.getQueryResults(any(SearchParameterMap.class), any(), any(), any())).thenReturn(expectedBundle);
		
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
		
		RelatedPerson rp1 = buildRelatedPerson();
		RelatedPerson rp2 = buildRelatedPerson();
		IBundleProvider expectedBundle = new SimpleBundleProvider(java.util.Arrays.asList(rp1, rp2));
		
		when(searchQuery.getQueryResults(any(SearchParameterMap.class), any(), any(), any())).thenReturn(expectedBundle);
		
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
		
		when(searchQuery.getQueryResults(any(SearchParameterMap.class), any(), any(), any())).thenReturn(
		    new SimpleBundleProvider());
		
		// When
		IBundleProvider result = relatedPersonService.searchByPatient(searchParams);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.getAllResources(), hasSize(0));
	}
	
	// ===============================
	// PerspectiveAwareTranslatorWrapper TESTS
	// ===============================
	
	@Test
	public void searchByPatient_perspectiveWrapper_routesToFhirResourceWithPatientUuid() {
		ReferenceAndListParam patientReference = buildPatientReference(PATIENT_UUID);
		BahmniRelatedPersonSearchParams searchParams = BahmniRelatedPersonSearchParams.builder()
		        .patientReference(patientReference).build();
		
		ArgumentCaptor<BahmniRelatedPersonTranslator> wrapperCaptor = ArgumentCaptor
		        .forClass(BahmniRelatedPersonTranslator.class);
		when(searchQuery.getQueryResults(any(SearchParameterMap.class), any(), wrapperCaptor.capture(), any())).thenReturn(
		    new SimpleBundleProvider());
		
		relatedPersonService.searchByPatient(searchParams);
		
		BahmniRelatedPersonTranslator wrapper = wrapperCaptor.getValue();
		Relationship rel = buildRelationship();
		RelatedPerson expected = buildRelatedPerson();
		when(translator.toFhirResource(rel, PATIENT_UUID)).thenReturn(expected);
		
		RelatedPerson result = wrapper.toFhirResource(rel);
		assertThat(result, equalTo(expected));
		verify(translator).toFhirResource(rel, PATIENT_UUID);
	}
	
	@Test
	public void searchByPatient_perspectiveWrapper_delegatesAllMethods() {
		ReferenceAndListParam patientReference = buildPatientReference(PATIENT_UUID);
		BahmniRelatedPersonSearchParams searchParams = BahmniRelatedPersonSearchParams.builder()
		        .patientReference(patientReference).build();
		
		ArgumentCaptor<BahmniRelatedPersonTranslator> wrapperCaptor = ArgumentCaptor
		        .forClass(BahmniRelatedPersonTranslator.class);
		when(searchQuery.getQueryResults(any(SearchParameterMap.class), any(), wrapperCaptor.capture(), any())).thenReturn(
		    new SimpleBundleProvider());
		
		relatedPersonService.searchByPatient(searchParams);
		
		BahmniRelatedPersonTranslator wrapper = wrapperCaptor.getValue();
		Relationship rel = buildRelationship();
		RelatedPerson rp = buildRelatedPerson();
		
		wrapper.toFhirResource(rel, "other-uuid");
		verify(translator).toFhirResource(rel, "other-uuid");
		
		wrapper.toOpenmrsType(rp);
		verify(translator).toOpenmrsType(rp);
		
		wrapper.toOpenmrsType(rel, rp);
		verify(translator).toOpenmrsType(rel, rp);
	}
	
	// ===============================
	// update / delete TESTS
	// ===============================
	
	@Test
	public void update_updatesRelationshipAndReturnsFhirResource() {
		Relationship existing = buildRelationship();
		RelatedPerson updatedRp = buildRelatedPerson();
		Relationship saved = buildRelationship();
		RelatedPerson expected = buildRelatedPerson();
		
		when(dao.get(RELATIONSHIP_UUID)).thenReturn(existing);
		when(translator.toOpenmrsType(existing, updatedRp)).thenReturn(saved);
		when(dao.createOrUpdate(saved)).thenReturn(saved);
		when(translator.toFhirResource(saved, null)).thenReturn(expected);
		
		RelatedPerson result = relatedPersonService.update(RELATIONSHIP_UUID, updatedRp);
		
		assertThat(result, equalTo(expected));
	}
	
	@Test(expected = ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException.class)
	public void update_throwsWhenNotFound() {
		when(dao.get(RELATIONSHIP_UUID)).thenReturn(null);
		relatedPersonService.update(RELATIONSHIP_UUID, buildRelatedPerson());
	}
	
	@Test
	public void delete_callsDaoDelete() {
		relatedPersonService.delete(RELATIONSHIP_UUID);
		verify(dao).delete(RELATIONSHIP_UUID);
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
