package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
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
	
	private Concept parentConcept;
	
	private Concept childConcept;
	
	private ValueSet baseValueSet;
	
	@Before
	public void setup() {
		valueSetService = new BahmniFhirValueSetServiceImpl();
		valueSetService.setConceptService(conceptService);
		
		// Setup mock concepts using Mockito to avoid OpenMRS validation issues
		parentConcept = org.mockito.Mockito.mock(Concept.class);
		when(parentConcept.getUuid()).thenReturn(PARENT_CONCEPT_UUID);
		when(parentConcept.getDisplayString()).thenReturn(PARENT_CONCEPT_NAME);
		when(parentConcept.isRetired()).thenReturn(false);
		when(parentConcept.getConceptClass()).thenReturn(conceptClass);
		when(conceptClass.isRetired()).thenReturn(false);
		
		childConcept = org.mockito.Mockito.mock(Concept.class);
		when(childConcept.getUuid()).thenReturn(CHILD_CONCEPT_UUID);
		when(childConcept.getDisplayString()).thenReturn(CHILD_CONCEPT_NAME);
		when(childConcept.isRetired()).thenReturn(false);
		when(childConcept.getConceptClass()).thenReturn(conceptClass);
		when(childConcept.getSetMembers()).thenReturn(Collections.emptyList());
		
		// Setup parent-child relationship using setMembers (not answers)
		when(parentConcept.getSetMembers()).thenReturn(Arrays.asList(childConcept));
		
		// Setup base ValueSet
		baseValueSet = new ValueSet();
		baseValueSet.setId(PARENT_CONCEPT_UUID);
		baseValueSet.setUrl("http://example.org/ValueSet/test");
		baseValueSet.setName("TestValueSet");
	}
	
	@Test
	public void shouldCreateHierarchicalExpansionWhenRequested() {
		// Given
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
		ValueSet result = spyService.expandedValueSet(PARENT_CONCEPT_UUID, true, null, null, null);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.hasExpansion(), is(true));
		
		ValueSet.ValueSetExpansionComponent expansion = result.getExpansion();
		assertThat(expansion.getContains(), hasSize(1));
		
		// Verify hierarchical structure
		ValueSet.ValueSetExpansionContainsComponent parentComponent = expansion.getContains().get(0);
		assertThat(parentComponent.getCode(), equalTo(PARENT_CONCEPT_UUID));
		assertThat(parentComponent.getDisplay(), equalTo(PARENT_CONCEPT_NAME));
		assertThat(parentComponent.getContains(), hasSize(1));
		
		ValueSet.ValueSetExpansionContainsComponent childComponent = parentComponent.getContains().get(0);
		assertThat(childComponent.getCode(), equalTo(CHILD_CONCEPT_UUID));
		assertThat(childComponent.getDisplay(), equalTo(CHILD_CONCEPT_NAME));
	}
	
	@Test
	public void shouldCreateFlatExpansionWhenNotHierarchical() {
		// Given
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
		ValueSet result = spyService.expandedValueSet(PARENT_CONCEPT_UUID, false, null, null, null);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.hasExpansion(), is(true));
		
		ValueSet.ValueSetExpansionComponent expansion = result.getExpansion();
		assertThat(expansion.getContains(), hasSize(2)); // Parent + child in flat structure
		
		// Verify flat structure (no nested contains)
		for (ValueSet.ValueSetExpansionContainsComponent component : expansion.getContains()) {
			assertThat(component.getContains(), hasSize(0));
		}
		
		// Verify both concepts are present
		assertThat(expansion.getContains().get(0).getCode(), equalTo(PARENT_CONCEPT_UUID));
		assertThat(expansion.getContains().get(1).getCode(), equalTo(CHILD_CONCEPT_UUID));
	}
	
	@Test
	public void shouldApplyFilterCorrectly() {
		// Given
		when(conceptService.getConceptByUuid(PARENT_CONCEPT_UUID)).thenReturn(parentConcept);
		
		// Mock the inherited get method from base service
		BahmniFhirValueSetServiceImpl spyService = new BahmniFhirValueSetServiceImpl() {
			
			@Override
			public ValueSet get(String uuid) {
				return baseValueSet;
			}
		};
		spyService.setConceptService(conceptService);
		
		// When - filter for "Child" which should match only the child concept
		ValueSet result = spyService.expandedValueSet(PARENT_CONCEPT_UUID, false, "Child", null, null);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.hasExpansion(), is(true));
		
		ValueSet.ValueSetExpansionComponent expansion = result.getExpansion();
		assertThat(expansion.getContains(), hasSize(1));
		assertThat(expansion.getContains().get(0).getCode(), equalTo(CHILD_CONCEPT_UUID));
	}
	
	@Test
	public void shouldApplyCountLimitCorrectly() {
		// Given
		when(conceptService.getConceptByUuid(PARENT_CONCEPT_UUID)).thenReturn(parentConcept);
		
		// Mock the inherited get method from base service
		BahmniFhirValueSetServiceImpl spyService = new BahmniFhirValueSetServiceImpl() {
			
			@Override
			public ValueSet get(String uuid) {
				return baseValueSet;
			}
		};
		spyService.setConceptService(conceptService);
		
		// When - limit to 1 concept
		ValueSet result = spyService.expandedValueSet(PARENT_CONCEPT_UUID, false, null, 1, null);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.hasExpansion(), is(true));
		
		ValueSet.ValueSetExpansionComponent expansion = result.getExpansion();
		assertThat(expansion.getContains(), hasSize(1));
	}
	
	@Test(expected = RuntimeException.class)
	public void shouldThrowExceptionWhenValueSetNotFound() {
		// Given
		// Mock the inherited get method to return null
		BahmniFhirValueSetServiceImpl spyService = new BahmniFhirValueSetServiceImpl() {
			
			@Override
			public ValueSet get(String uuid) {
				return null;
			}
		};
		spyService.setConceptService(conceptService);
		
		// When/Then
		spyService.expandedValueSet(PARENT_CONCEPT_UUID, true, null, null, null);
	}
	
	@Test(expected = RuntimeException.class)
	public void shouldThrowExceptionWhenConceptNotFound() {
		// Given
		when(conceptService.getConceptByUuid(PARENT_CONCEPT_UUID)).thenReturn(null);
		
		// Mock the inherited get method
		BahmniFhirValueSetServiceImpl spyService = new BahmniFhirValueSetServiceImpl() {
			
			@Override
			public ValueSet get(String uuid) {
				return baseValueSet;
			}
		};
		spyService.setConceptService(conceptService);
		
		// When/Then
		spyService.expandedValueSet(PARENT_CONCEPT_UUID, true, null, null, null);
	}
	
	// ===============================
	// SET MEMBERS TESTS (Updated for setMembers-only implementation)
	// ===============================
	
	@Test
	public void shouldIncludeSetMembersInHierarchicalExpansion() {
		// Given
		String setMemberUuid = "set-member-uuid";
		String setMemberName = "Set Member Concept";
		
		Concept setMemberConcept = org.mockito.Mockito.mock(Concept.class);
		when(setMemberConcept.getUuid()).thenReturn(setMemberUuid);
		when(setMemberConcept.getDisplayString()).thenReturn(setMemberName);
		when(setMemberConcept.getSetMembers()).thenReturn(Collections.emptyList()); // No nested members
		when(setMemberConcept.isRetired()).thenReturn(false);
		when(setMemberConcept.getConceptClass()).thenReturn(conceptClass);
		
		// Setup parent concept with setMembers (replace the child from @Before setup)
		when(parentConcept.getSetMembers()).thenReturn(Arrays.asList(setMemberConcept));
		when(conceptService.getConceptByUuid(PARENT_CONCEPT_UUID)).thenReturn(parentConcept);
		
		BahmniFhirValueSetServiceImpl spyService = new BahmniFhirValueSetServiceImpl() {
			
			@Override
			public ValueSet get(String uuid) {
				return baseValueSet;
			}
		};
		spyService.setConceptService(conceptService);
		
		// When
		ValueSet result = spyService.expandedValueSet(PARENT_CONCEPT_UUID, true, null, null, null);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.hasExpansion(), is(true));
		
		ValueSet.ValueSetExpansionComponent expansion = result.getExpansion();
		assertThat(expansion.getContains(), hasSize(1));
		
		// Verify hierarchical structure includes setMembers
		ValueSet.ValueSetExpansionContainsComponent parentComponent = expansion.getContains().get(0);
		assertThat(parentComponent.getCode(), equalTo(PARENT_CONCEPT_UUID));
		
		// Should have only setMember (implementation only processes setMembers, not answers)
		assertThat(parentComponent.getContains(), hasSize(1)); // Only setMember
		
		// Find the setMember in the contains list
		ValueSet.ValueSetExpansionContainsComponent setMemberComponent = parentComponent.getContains().get(0);
		assertThat(setMemberComponent.getCode(), equalTo(setMemberUuid));
		assertThat(setMemberComponent.getDisplay(), equalTo(setMemberName));
	}
	
	@Test
	public void shouldIncludeSetMembersInFlatExpansion() {
		// Given
		String setMemberUuid = "set-member-uuid";
		String setMemberName = "Set Member Concept";
		
		Concept setMemberConcept = org.mockito.Mockito.mock(Concept.class);
		when(setMemberConcept.getUuid()).thenReturn(setMemberUuid);
		when(setMemberConcept.getDisplayString()).thenReturn(setMemberName);
		when(setMemberConcept.getSetMembers()).thenReturn(Collections.emptyList()); // No nested members
		when(setMemberConcept.isRetired()).thenReturn(false);
		when(setMemberConcept.getConceptClass()).thenReturn(conceptClass);
		
		// Setup parent concept with setMembers (replace the child from @Before setup)
		when(parentConcept.getSetMembers()).thenReturn(Arrays.asList(setMemberConcept));
		when(conceptService.getConceptByUuid(PARENT_CONCEPT_UUID)).thenReturn(parentConcept);
		
		BahmniFhirValueSetServiceImpl spyService = new BahmniFhirValueSetServiceImpl() {
			@Override
			public ValueSet get(String uuid) {
				return baseValueSet;
			}
		};
		spyService.setConceptService(conceptService);
		
		// When
		ValueSet result = spyService.expandedValueSet(PARENT_CONCEPT_UUID, false, null, null, null);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.hasExpansion(), is(true));
		
		ValueSet.ValueSetExpansionComponent expansion = result.getExpansion();
		// Should have parent + setMember = 2 total (implementation only processes setMembers)
		assertThat(expansion.getContains(), hasSize(2));
		
		// Verify all concepts are at flat level (no nesting)
		for (ValueSet.ValueSetExpansionContainsComponent component : expansion.getContains()) {
			assertThat(component.getContains(), hasSize(0));
		}
		
		// Find the setMember in the flat list
		boolean setMemberFound = expansion.getContains().stream()
		    .anyMatch(c -> c.getCode().equals(setMemberUuid) && c.getDisplay().equals(setMemberName));
		assertThat("SetMember should be included in flat expansion", setMemberFound, is(true));
	}
	
	@Test
	public void shouldProcessSetMembersOnly() {
		// Given - concept with only setMembers
		String setMemberUuid = "set-member-uuid";
		String setMemberName = "Set Member Concept";
		
		Concept setMemberConcept = org.mockito.Mockito.mock(Concept.class);
		when(setMemberConcept.getUuid()).thenReturn(setMemberUuid);
		when(setMemberConcept.getDisplayString()).thenReturn(setMemberName);
		when(setMemberConcept.getSetMembers()).thenReturn(Collections.emptyList());
		when(setMemberConcept.isRetired()).thenReturn(false);
		when(setMemberConcept.getConceptClass()).thenReturn(conceptClass);
		
		// Parent concept with only setMembers
		when(parentConcept.getSetMembers()).thenReturn(Arrays.asList(setMemberConcept));
		when(conceptService.getConceptByUuid(PARENT_CONCEPT_UUID)).thenReturn(parentConcept);
		
		BahmniFhirValueSetServiceImpl spyService = new BahmniFhirValueSetServiceImpl() {
			
			@Override
			public ValueSet get(String uuid) {
				return baseValueSet;
			}
		};
		spyService.setConceptService(conceptService);
		
		// When
		ValueSet result = spyService.expandedValueSet(PARENT_CONCEPT_UUID, true, null, null, null);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.hasExpansion(), is(true));
		
		ValueSet.ValueSetExpansionComponent expansion = result.getExpansion();
		assertThat(expansion.getContains(), hasSize(1));
		
		// Verify parent has only setMember
		ValueSet.ValueSetExpansionContainsComponent parentComponent = expansion.getContains().get(0);
		assertThat(parentComponent.getContains(), hasSize(1)); // Only setMember
		
		ValueSet.ValueSetExpansionContainsComponent setMemberComponent = parentComponent.getContains().get(0);
		assertThat(setMemberComponent.getCode(), equalTo(setMemberUuid));
		assertThat(setMemberComponent.getDisplay(), equalTo(setMemberName));
	}
	
	// ===============================
	// INACTIVE FIELD TESTS (New functionality)
	// ===============================
	
	@Test
	public void shouldSetInactiveForRetiredConcepts() {
		// Given - retired concept
		Concept retiredConcept = org.mockito.Mockito.mock(Concept.class);
		when(retiredConcept.getUuid()).thenReturn("retired-concept-uuid");
		when(retiredConcept.getDisplayString()).thenReturn("Retired Concept");
		when(retiredConcept.isRetired()).thenReturn(true); // Retired concept
		when(retiredConcept.getConceptClass()).thenReturn(conceptClass);
		when(retiredConcept.getSetMembers()).thenReturn(Collections.emptyList());
		
		when(parentConcept.getSetMembers()).thenReturn(Arrays.asList(retiredConcept));
		when(conceptService.getConceptByUuid(PARENT_CONCEPT_UUID)).thenReturn(parentConcept);
		
		BahmniFhirValueSetServiceImpl spyService = new BahmniFhirValueSetServiceImpl() {
			@Override
			public ValueSet get(String uuid) {
				return baseValueSet;
			}
		};
		spyService.setConceptService(conceptService);
		
		// When
		ValueSet result = spyService.expandedValueSet(PARENT_CONCEPT_UUID, false, null, null, null);
		
		// Then
		ValueSet.ValueSetExpansionComponent expansion = result.getExpansion();
		
		// Find the retired concept in the expansion
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
		
		Concept conceptWithRetiredClass = org.mockito.Mockito.mock(Concept.class);
		when(conceptWithRetiredClass.getUuid()).thenReturn("concept-retired-class-uuid");
		when(conceptWithRetiredClass.getDisplayString()).thenReturn("Concept with Retired Class");
		when(conceptWithRetiredClass.isRetired()).thenReturn(false); // Concept itself not retired
		when(conceptWithRetiredClass.getConceptClass()).thenReturn(retiredConceptClass); // But class is retired
		when(conceptWithRetiredClass.getSetMembers()).thenReturn(Collections.emptyList());
		
		when(parentConcept.getSetMembers()).thenReturn(Arrays.asList(conceptWithRetiredClass));
		when(conceptService.getConceptByUuid(PARENT_CONCEPT_UUID)).thenReturn(parentConcept);
		
		BahmniFhirValueSetServiceImpl spyService = new BahmniFhirValueSetServiceImpl() {
			@Override
			public ValueSet get(String uuid) {
				return baseValueSet;
			}
		};
		spyService.setConceptService(conceptService);
		
		// When
		ValueSet result = spyService.expandedValueSet(PARENT_CONCEPT_UUID, false, null, null, null);
		
		// Then
		ValueSet.ValueSetExpansionComponent expansion = result.getExpansion();
		
		// Find the concept with retired class in the expansion
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
		Concept conceptWithNullClass = org.mockito.Mockito.mock(Concept.class);
		when(conceptWithNullClass.getUuid()).thenReturn("concept-null-class-uuid");
		when(conceptWithNullClass.getDisplayString()).thenReturn("Concept with Null Class");
		when(conceptWithNullClass.isRetired()).thenReturn(false); // Concept itself not retired
		when(conceptWithNullClass.getConceptClass()).thenReturn(null); // Null class
		when(conceptWithNullClass.getSetMembers()).thenReturn(Collections.emptyList());
		
		when(parentConcept.getSetMembers()).thenReturn(Arrays.asList(conceptWithNullClass));
		when(conceptService.getConceptByUuid(PARENT_CONCEPT_UUID)).thenReturn(parentConcept);
		
		BahmniFhirValueSetServiceImpl spyService = new BahmniFhirValueSetServiceImpl() {
			@Override
			public ValueSet get(String uuid) {
				return baseValueSet;
			}
		};
		spyService.setConceptService(conceptService);
		
		// When
		ValueSet result = spyService.expandedValueSet(PARENT_CONCEPT_UUID, false, null, null, null);
		
		// Then
		ValueSet.ValueSetExpansionComponent expansion = result.getExpansion();
		
		// Find the concept with null class in the expansion
		ValueSet.ValueSetExpansionContainsComponent conceptComponent = expansion.getContains().stream()
		    .filter(c -> c.getCode().equals("concept-null-class-uuid"))
		    .findFirst()
		    .orElse(null);
		
		assertThat(conceptComponent, notNullValue());
		assertThat("Concept with null class should have inactive=true", conceptComponent.getInactive(), equalTo(true));
	}
	
	@Test
	public void shouldNotSetInactiveForActiveConcepts() {
		// Given - active concept (this is the default setup in @Before)
		when(conceptService.getConceptByUuid(PARENT_CONCEPT_UUID)).thenReturn(parentConcept);
		
		BahmniFhirValueSetServiceImpl spyService = new BahmniFhirValueSetServiceImpl() {
			@Override
			public ValueSet get(String uuid) {
				return baseValueSet;
			}
		};
		spyService.setConceptService(conceptService);
		
		// When
		ValueSet result = spyService.expandedValueSet(PARENT_CONCEPT_UUID, false, null, null, null);
		
		// Then
		ValueSet.ValueSetExpansionComponent expansion = result.getExpansion();
		
		// Find the parent concept in the expansion
		ValueSet.ValueSetExpansionContainsComponent parentComponent = expansion.getContains().stream()
		    .filter(c -> c.getCode().equals(PARENT_CONCEPT_UUID))
		    .findFirst()
		    .orElse(null);
		
		assertThat(parentComponent, notNullValue());
		// Active concepts should either not have the inactive field set (null) or have it set to false
		// HAPI FHIR may initialize boolean fields to false by default
		Boolean inactiveValue = parentComponent.getInactive();
		assertThat("Active concept should not have inactive=true", inactiveValue == null || inactiveValue == false, is(true));
	}
}
