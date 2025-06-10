package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.hl7.fhir.r4.model.ValueSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.api.ConceptService;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirValueSetServiceImplTest {
	
	private static final String PARENT_CONCEPT_UUID = "parent-concept-uuid";
	
	private static final String CHILD_CONCEPT_UUID = "child-concept-uuid";
	
	private static final String PARENT_CONCEPT_NAME = "Parent Concept";
	
	private static final String CHILD_CONCEPT_NAME = "Child Concept";
	
	@Mock
	private ConceptService conceptService;
	
	@Mock
	private ConceptClass conceptClass;
	
	private BahmniFhirValueSetServiceImpl valueSetService;
	
	private ValueSet baseValueSet;
	
	@Before
	public void setup() {
		valueSetService = new BahmniFhirValueSetServiceImpl();
		valueSetService.setConceptService(conceptService);
		
		// Setup base ValueSet
		baseValueSet = new ValueSet();
		baseValueSet.setId(PARENT_CONCEPT_UUID);
		baseValueSet.setUrl("http://example.org/ValueSet/test");
		baseValueSet.setName("TestValueSet");
	}
	
	@Test
	public void shouldCreateHierarchicalExpansion() {
		// Given
		Concept parentConcept = org.mockito.Mockito.mock(Concept.class);
		Concept childConcept = org.mockito.Mockito.mock(Concept.class);
		
		// Setup only the methods that will actually be called
		when(parentConcept.getUuid()).thenReturn(PARENT_CONCEPT_UUID);
		when(parentConcept.getSetMembers()).thenReturn(Arrays.asList(childConcept));
		
		when(childConcept.getUuid()).thenReturn(CHILD_CONCEPT_UUID);
		when(childConcept.getDisplayString()).thenReturn(CHILD_CONCEPT_NAME);
		when(childConcept.isRetired()).thenReturn(false);
		when(childConcept.getConceptClass()).thenReturn(conceptClass);
		when(childConcept.getSetMembers()).thenReturn(Collections.emptyList());
		
		when(conceptClass.isRetired()).thenReturn(false);
		when(conceptService.getConceptByUuid(PARENT_CONCEPT_UUID)).thenReturn(parentConcept);
		
		// Mock the inherited get method from base service
		BahmniFhirValueSetServiceImpl spyService = new BahmniFhirValueSetServiceImpl() {
			
			@Override
			public ValueSet get(String uuid) {
				return baseValueSet;
			}
		};
		spyService.setConceptService(conceptService);
		
		// When
		ValueSet result = spyService.expandedValueSet(PARENT_CONCEPT_UUID);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.hasExpansion(), is(true));
		
		ValueSet.ValueSetExpansionComponent expansion = result.getExpansion();
		assertThat(expansion.getContains(), hasSize(1));
		
		// Verify only child concept is in expansion (parent concept excluded)
		ValueSet.ValueSetExpansionContainsComponent childComponent = expansion.getContains().get(0);
		assertThat(childComponent.getCode(), equalTo(CHILD_CONCEPT_UUID));
		assertThat(childComponent.getDisplay(), equalTo(CHILD_CONCEPT_NAME));
		assertThat(childComponent.getContains(), hasSize(0));
	}
	
	@Test(expected = RuntimeException.class)
	public void shouldThrowExceptionWhenValueSetNotFound() {
		// Given - Mock service that returns null for get() method
		BahmniFhirValueSetServiceImpl spyService = new BahmniFhirValueSetServiceImpl() {
			
			@Override
			public ValueSet get(String uuid) {
				return null;
			}
		};
		spyService.setConceptService(conceptService);
		
		// When/Then - Should throw RuntimeException
		spyService.expandedValueSet(PARENT_CONCEPT_UUID);
	}
	
	@Test(expected = RuntimeException.class)
	public void shouldThrowExceptionWhenConceptNotFound() {
		// Given - ConceptService returns null for the concept
		when(conceptService.getConceptByUuid(PARENT_CONCEPT_UUID)).thenReturn(null);
		
		BahmniFhirValueSetServiceImpl spyService = new BahmniFhirValueSetServiceImpl() {
			
			@Override
			public ValueSet get(String uuid) {
				return baseValueSet;
			}
		};
		spyService.setConceptService(conceptService);
		
		// When/Then - Should throw RuntimeException
		spyService.expandedValueSet(PARENT_CONCEPT_UUID);
	}
	
	@Test
	public void shouldIncludeSetMembersInHierarchicalExpansion() {
		// Given
		String setMemberUuid = "set-member-uuid";
		String setMemberName = "Set Member Concept";
		
		Concept parentConcept = org.mockito.Mockito.mock(Concept.class);
		Concept setMemberConcept = org.mockito.Mockito.mock(Concept.class);
		
		when(parentConcept.getUuid()).thenReturn(PARENT_CONCEPT_UUID);
		when(parentConcept.getSetMembers()).thenReturn(Arrays.asList(setMemberConcept));
		
		when(setMemberConcept.getUuid()).thenReturn(setMemberUuid);
		when(setMemberConcept.getDisplayString()).thenReturn(setMemberName);
		when(setMemberConcept.getSetMembers()).thenReturn(Collections.emptyList());
		when(setMemberConcept.isRetired()).thenReturn(false);
		when(setMemberConcept.getConceptClass()).thenReturn(conceptClass);
		
		when(conceptClass.isRetired()).thenReturn(false);
		when(conceptService.getConceptByUuid(PARENT_CONCEPT_UUID)).thenReturn(parentConcept);
		
		BahmniFhirValueSetServiceImpl spyService = new BahmniFhirValueSetServiceImpl() {
			
			@Override
			public ValueSet get(String uuid) {
				return baseValueSet;
			}
		};
		spyService.setConceptService(conceptService);
		
		// When
		ValueSet result = spyService.expandedValueSet(PARENT_CONCEPT_UUID);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.hasExpansion(), is(true));
		
		ValueSet.ValueSetExpansionComponent expansion = result.getExpansion();
		assertThat(expansion.getContains(), hasSize(1));
		
		ValueSet.ValueSetExpansionContainsComponent setMemberComponent = expansion.getContains().get(0);
		assertThat(setMemberComponent.getCode(), equalTo(setMemberUuid));
		assertThat(setMemberComponent.getDisplay(), equalTo(setMemberName));
		assertThat(setMemberComponent.getContains(), hasSize(0));
	}
	
	@Test
	public void shouldProcessSetMembersOnly() {
		// Given - concept with only setMembers
		String setMemberUuid = "set-member-uuid";
		String setMemberName = "Set Member Concept";
		
		Concept parentConcept = org.mockito.Mockito.mock(Concept.class);
		Concept setMemberConcept = org.mockito.Mockito.mock(Concept.class);
		
		when(parentConcept.getUuid()).thenReturn(PARENT_CONCEPT_UUID);
		when(parentConcept.getSetMembers()).thenReturn(Arrays.asList(setMemberConcept));
		
		when(setMemberConcept.getUuid()).thenReturn(setMemberUuid);
		when(setMemberConcept.getDisplayString()).thenReturn(setMemberName);
		when(setMemberConcept.getSetMembers()).thenReturn(Collections.emptyList());
		when(setMemberConcept.isRetired()).thenReturn(false);
		when(setMemberConcept.getConceptClass()).thenReturn(conceptClass);
		
		when(conceptClass.isRetired()).thenReturn(false);
		when(conceptService.getConceptByUuid(PARENT_CONCEPT_UUID)).thenReturn(parentConcept);
		
		BahmniFhirValueSetServiceImpl spyService = new BahmniFhirValueSetServiceImpl() {
			
			@Override
			public ValueSet get(String uuid) {
				return baseValueSet;
			}
		};
		spyService.setConceptService(conceptService);
		
		// When
		ValueSet result = spyService.expandedValueSet(PARENT_CONCEPT_UUID);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.hasExpansion(), is(true));
		
		ValueSet.ValueSetExpansionComponent expansion = result.getExpansion();
		assertThat(expansion.getContains(), hasSize(1));
		
		ValueSet.ValueSetExpansionContainsComponent setMemberComponent = expansion.getContains().get(0);
		assertThat(setMemberComponent.getCode(), equalTo(setMemberUuid));
		assertThat(setMemberComponent.getDisplay(), equalTo(setMemberName));
		assertThat(setMemberComponent.getContains(), hasSize(0));
	}
	
	@Test
	public void shouldSetInactiveForRetiredConcepts() {
		// Given - retired concept
		Concept parentConcept = org.mockito.Mockito.mock(Concept.class);
		Concept retiredConcept = org.mockito.Mockito.mock(Concept.class);
		
		when(parentConcept.getUuid()).thenReturn(PARENT_CONCEPT_UUID);
		when(parentConcept.getSetMembers()).thenReturn(Arrays.asList(retiredConcept));
		
		when(retiredConcept.getUuid()).thenReturn("retired-concept-uuid");
		when(retiredConcept.getDisplayString()).thenReturn("Retired Concept");
		when(retiredConcept.isRetired()).thenReturn(true);
		when(retiredConcept.getSetMembers()).thenReturn(Collections.emptyList());
		
		when(conceptService.getConceptByUuid(PARENT_CONCEPT_UUID)).thenReturn(parentConcept);
		
		BahmniFhirValueSetServiceImpl spyService = new BahmniFhirValueSetServiceImpl() {
			@Override
			public ValueSet get(String uuid) {
				return baseValueSet;
			}
		};
		spyService.setConceptService(conceptService);
		
		// When
		ValueSet result = spyService.expandedValueSet(PARENT_CONCEPT_UUID);
		
		// Then
		ValueSet.ValueSetExpansionComponent expansion = result.getExpansion();
		
		ValueSet.ValueSetExpansionContainsComponent retiredComponent = expansion.getContains().stream()
		    .filter(c -> c.getCode().equals("retired-concept-uuid"))
		    .findFirst()
		    .orElse(null);
		
		assertThat(retiredComponent, notNullValue());
		assertThat("Retired concept should have inactive=true", retiredComponent.getInactive(), equalTo(true));
	}
	
	@Test
	public void shouldSetInactiveForConceptsWithRetiredConceptClass() {
		// Given - concept with retired ConceptClass
		ConceptClass retiredConceptClass = org.mockito.Mockito.mock(ConceptClass.class);
		when(retiredConceptClass.isRetired()).thenReturn(true);
		
		Concept parentConcept = org.mockito.Mockito.mock(Concept.class);
		Concept conceptWithRetiredClass = org.mockito.Mockito.mock(Concept.class);
		
		when(parentConcept.getUuid()).thenReturn(PARENT_CONCEPT_UUID);
		when(parentConcept.getSetMembers()).thenReturn(Arrays.asList(conceptWithRetiredClass));
		
		when(conceptWithRetiredClass.getUuid()).thenReturn("concept-retired-class-uuid");
		when(conceptWithRetiredClass.getDisplayString()).thenReturn("Concept with Retired Class");
		when(conceptWithRetiredClass.isRetired()).thenReturn(false);
		when(conceptWithRetiredClass.getConceptClass()).thenReturn(retiredConceptClass);
		when(conceptWithRetiredClass.getSetMembers()).thenReturn(Collections.emptyList());
		
		when(conceptService.getConceptByUuid(PARENT_CONCEPT_UUID)).thenReturn(parentConcept);
		
		BahmniFhirValueSetServiceImpl spyService = new BahmniFhirValueSetServiceImpl() {
			@Override
			public ValueSet get(String uuid) {
				return baseValueSet;
			}
		};
		spyService.setConceptService(conceptService);
		
		// When
		ValueSet result = spyService.expandedValueSet(PARENT_CONCEPT_UUID);
		
		// Then
		ValueSet.ValueSetExpansionComponent expansion = result.getExpansion();
		
		ValueSet.ValueSetExpansionContainsComponent conceptComponent = expansion.getContains().stream()
		    .filter(c -> c.getCode().equals("concept-retired-class-uuid"))
		    .findFirst()
		    .orElse(null);
		
		assertThat(conceptComponent, notNullValue());
		assertThat("Concept with retired class should have inactive=true", conceptComponent.getInactive(), equalTo(true));
	}
	
	@Test
	public void shouldSetInactiveForConceptsWithNullConceptClass() {
		// Given - concept with null ConceptClass
		Concept parentConcept = org.mockito.Mockito.mock(Concept.class);
		Concept conceptWithNullClass = org.mockito.Mockito.mock(Concept.class);
		
		when(parentConcept.getUuid()).thenReturn(PARENT_CONCEPT_UUID);
		when(parentConcept.getSetMembers()).thenReturn(Arrays.asList(conceptWithNullClass));
		
		when(conceptWithNullClass.getUuid()).thenReturn("concept-null-class-uuid");
		when(conceptWithNullClass.getDisplayString()).thenReturn("Concept with Null Class");
		when(conceptWithNullClass.isRetired()).thenReturn(false);
		when(conceptWithNullClass.getConceptClass()).thenReturn(null);
		when(conceptWithNullClass.getSetMembers()).thenReturn(Collections.emptyList());
		
		when(conceptService.getConceptByUuid(PARENT_CONCEPT_UUID)).thenReturn(parentConcept);
		
		BahmniFhirValueSetServiceImpl spyService = new BahmniFhirValueSetServiceImpl() {
			@Override
			public ValueSet get(String uuid) {
				return baseValueSet;
			}
		};
		spyService.setConceptService(conceptService);
		
		// When
		ValueSet result = spyService.expandedValueSet(PARENT_CONCEPT_UUID);
		
		// Then
		ValueSet.ValueSetExpansionComponent expansion = result.getExpansion();
		
		ValueSet.ValueSetExpansionContainsComponent conceptComponent = expansion.getContains().stream()
		    .filter(c -> c.getCode().equals("concept-null-class-uuid"))
		    .findFirst()
		    .orElse(null);
		
		assertThat(conceptComponent, notNullValue());
		assertThat("Concept with null class should have inactive=true", conceptComponent.getInactive(), equalTo(true));
	}
	
	@Test
	public void shouldNotSetInactiveForActiveConcepts() {
		// Given - active concept setup
		Concept parentConcept = org.mockito.Mockito.mock(Concept.class);
		Concept childConcept = org.mockito.Mockito.mock(Concept.class);
		
		when(parentConcept.getUuid()).thenReturn(PARENT_CONCEPT_UUID);
		when(parentConcept.getSetMembers()).thenReturn(Arrays.asList(childConcept));
		
		when(childConcept.getUuid()).thenReturn(CHILD_CONCEPT_UUID);
		when(childConcept.getDisplayString()).thenReturn(CHILD_CONCEPT_NAME);
		when(childConcept.isRetired()).thenReturn(false);
		when(childConcept.getConceptClass()).thenReturn(conceptClass);
		when(childConcept.getSetMembers()).thenReturn(Collections.emptyList());
		
		when(conceptClass.isRetired()).thenReturn(false);
		when(conceptService.getConceptByUuid(PARENT_CONCEPT_UUID)).thenReturn(parentConcept);
		
		BahmniFhirValueSetServiceImpl spyService = new BahmniFhirValueSetServiceImpl() {
			@Override
			public ValueSet get(String uuid) {
				return baseValueSet;
			}
		};
		spyService.setConceptService(conceptService);
		
		// When
		ValueSet result = spyService.expandedValueSet(PARENT_CONCEPT_UUID);
		
		// Then
		ValueSet.ValueSetExpansionComponent expansion = result.getExpansion();
		
		ValueSet.ValueSetExpansionContainsComponent childComponent = expansion.getContains().stream()
		    .filter(c -> c.getCode().equals(CHILD_CONCEPT_UUID))
		    .findFirst()
		    .orElse(null);
		
		assertThat(childComponent, notNullValue());
		Boolean inactiveValue = childComponent.getInactive();
		assertThat("Active concept should not have inactive=true", inactiveValue == null || inactiveValue == false, is(true));
	}
}
