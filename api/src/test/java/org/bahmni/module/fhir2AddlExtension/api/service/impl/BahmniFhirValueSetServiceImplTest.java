package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptSearchResult;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir2.api.translators.ValueSetTranslator;
import org.openmrs.util.LocaleUtility;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.*", "org.apache.*", "org.slf4j.*"})
@PrepareForTest({LocaleUtility.class})
public class BahmniFhirValueSetServiceImplTest {
	
	private static final String PARENT_CONCEPT_UUID = "parent-concept-uuid";
	
	private static final String CHILD_CONCEPT_UUID = "child-concept-uuid";
	
	private static final String CHILD_CONCEPT_NAME = "Child Concept";
	
	private static final Set<Locale> MOCK_LOCALES = new LinkedHashSet<>(Arrays.asList(Locale.ENGLISH));
	
	@Mock
	private ConceptService conceptService;
	
	@Mock
	private ConceptClass conceptClass;
	
	@Mock
	private ValueSetTranslator valueSetTranslator;
	
	private BahmniFhirValueSetServiceImpl valueSetService;
	
	private ValueSet baseValueSet;
	
	@Before
	public void setup() {
		// Mock the static LocaleUtility method
		mockStatic(LocaleUtility.class);
		when(LocaleUtility.getLocalesInOrder()).thenReturn(MOCK_LOCALES);
		
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
	
	@Test
	public void filterAndExpandValueSet_shouldReturnExpandedValueSetForSingleConcept() {
		// Given
		String conceptName = "Test Concept";
		Concept filteredConcept = org.mockito.Mockito.mock(Concept.class);
		Concept childConcept = org.mockito.Mockito.mock(Concept.class);
		ConceptSearchResult searchResult = org.mockito.Mockito.mock(ConceptSearchResult.class);
		
		when(searchResult.getConcept()).thenReturn(filteredConcept);
		when(
		    conceptService.getConcepts(eq(conceptName), anyList(), eq(false), isNull(), isNull(), isNull(), isNull(),
		        isNull(), eq(0), isNull())).thenReturn(Arrays.asList(searchResult));
		
		when(filteredConcept.getUuid()).thenReturn("filtered-concept-uuid");
		when(filteredConcept.getSetMembers()).thenReturn(Arrays.asList(childConcept));
		
		when(childConcept.getUuid()).thenReturn("child-concept-uuid");
		when(childConcept.getDisplayString()).thenReturn("Child Concept");
		when(childConcept.isRetired()).thenReturn(false);
		when(childConcept.getConceptClass()).thenReturn(conceptClass);
		when(childConcept.getSetMembers()).thenReturn(Collections.emptyList());
		
		when(conceptClass.isRetired()).thenReturn(false);
		
		ValueSet translatedValueSet = new ValueSet();
		translatedValueSet.setId("filtered-concept-uuid");
		translatedValueSet.setName(conceptName);
		
		// Create a spy service with mocked translator
		BahmniFhirValueSetServiceImpl spyService = new BahmniFhirValueSetServiceImpl() {
			
			@Override
			protected ValueSetTranslator getTranslator() {
				return valueSetTranslator;
			}
		};
		spyService.setConceptService(conceptService);
		
		when(valueSetTranslator.toFhirResource(filteredConcept)).thenReturn(translatedValueSet);
		
		// When
		ValueSet result = spyService.filterAndExpandValueSet(conceptName);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.hasExpansion(), is(true));
		
		ValueSet.ValueSetExpansionComponent expansion = result.getExpansion();
		assertThat(expansion.getContains(), hasSize(1));
		
		// Verify only child concept is in expansion (filtered concept excluded)
		ValueSet.ValueSetExpansionContainsComponent childComponent = expansion.getContains().get(0);
		assertThat(childComponent.getCode(), equalTo("child-concept-uuid"));
		assertThat(childComponent.getDisplay(), equalTo("Child Concept"));
		assertThat(childComponent.getContains(), hasSize(0));
	}
	
	@Test(expected = InvalidRequestException.class)
	public void filterAndExpandValueSet_shouldThrowExceptionWhenNoConceptFound() {
		// Given
		String conceptName = "Non-existent Concept";
		when(
		    conceptService.getConcepts(eq(conceptName), anyList(), eq(false), isNull(), isNull(), isNull(), isNull(),
		        isNull(), eq(0), isNull())).thenReturn(Collections.emptyList());
		
		// When/Then - Should throw InvalidRequestException
		valueSetService.filterAndExpandValueSet(conceptName);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void filterAndExpandValueSet_shouldThrowExceptionWhenMultipleConceptsFound() {
		// Given
		String conceptName = "Ambiguous Concept";
		Concept concept1 = org.mockito.Mockito.mock(Concept.class);
		Concept concept2 = org.mockito.Mockito.mock(Concept.class);
		ConceptSearchResult searchResult1 = org.mockito.Mockito.mock(ConceptSearchResult.class);
		ConceptSearchResult searchResult2 = org.mockito.Mockito.mock(ConceptSearchResult.class);
		
		when(searchResult1.getConcept()).thenReturn(concept1);
		when(searchResult2.getConcept()).thenReturn(concept2);
		when(
		    conceptService.getConcepts(eq(conceptName), anyList(), eq(false), isNull(), isNull(), isNull(), isNull(),
		        isNull(), eq(0), isNull())).thenReturn(Arrays.asList(searchResult1, searchResult2));
		
		// When/Then - Should throw InvalidRequestException
		valueSetService.filterAndExpandValueSet(conceptName);
	}
	
	@Test
	public void shouldAddConceptClassExtensionToExpandedConcepts() {
		// Given
		String conceptClassName = "Diagnosis";
		Concept parentConcept = org.mockito.Mockito.mock(Concept.class);
		Concept childConcept = org.mockito.Mockito.mock(Concept.class);
		ConceptClass childConceptClass = org.mockito.Mockito.mock(ConceptClass.class);
		
		when(parentConcept.getUuid()).thenReturn(PARENT_CONCEPT_UUID);
		when(parentConcept.getSetMembers()).thenReturn(Arrays.asList(childConcept));
		
		when(childConcept.getUuid()).thenReturn(CHILD_CONCEPT_UUID);
		when(childConcept.getDisplayString()).thenReturn(CHILD_CONCEPT_NAME);
		when(childConcept.isRetired()).thenReturn(false);
		when(childConcept.getConceptClass()).thenReturn(childConceptClass);
		when(childConcept.getSetMembers()).thenReturn(Collections.emptyList());
		
		when(childConceptClass.isRetired()).thenReturn(false);
		when(childConceptClass.getName()).thenReturn(conceptClassName);
		
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
		ValueSet.ValueSetExpansionContainsComponent childComponent = expansion.getContains().get(0);
		
		assertThat(childComponent.hasExtension(), is(true));
		assertThat(childComponent.getExtension(), hasSize(1));
		
		org.hl7.fhir.r4.model.Extension conceptClassExtension = childComponent.getExtension().get(0);
		assertThat(conceptClassExtension.getUrl(),
			equalTo(BahmniFhirConstants.VALUESET_CONCEPT_CLASS_EXTENSION_URL));
		assertThat(conceptClassExtension.getValue().toString(), equalTo(conceptClassName));
	}
	
	@Test
	public void shouldNotAddConceptClassExtensionWhenConceptClassIsNull() {
		// Given
		Concept parentConcept = org.mockito.Mockito.mock(Concept.class);
		Concept childConcept = org.mockito.Mockito.mock(Concept.class);
		
		when(parentConcept.getUuid()).thenReturn(PARENT_CONCEPT_UUID);
		when(parentConcept.getSetMembers()).thenReturn(Arrays.asList(childConcept));
		
		when(childConcept.getUuid()).thenReturn(CHILD_CONCEPT_UUID);
		when(childConcept.getDisplayString()).thenReturn(CHILD_CONCEPT_NAME);
		when(childConcept.isRetired()).thenReturn(false);
		when(childConcept.getConceptClass()).thenReturn(null);
		when(childConcept.getSetMembers()).thenReturn(Collections.emptyList());
		
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
		ValueSet.ValueSetExpansionContainsComponent childComponent = expansion.getContains().get(0);
		
		assertThat(childComponent.hasExtension(), is(false));
		assertThat(childComponent.getExtension(), hasSize(0));
	}
	
	@Test
	public void shouldAddConceptClassExtensionInFilterAndExpandValueSet() {
		// Given
		String conceptName = "Test Concept";
		String conceptClassName = "Test";
		Concept filteredConcept = org.mockito.Mockito.mock(Concept.class);
		Concept childConcept = org.mockito.Mockito.mock(Concept.class);
		ConceptClass childConceptClass = org.mockito.Mockito.mock(ConceptClass.class);
		ConceptSearchResult searchResult = org.mockito.Mockito.mock(ConceptSearchResult.class);
		
		when(searchResult.getConcept()).thenReturn(filteredConcept);
		when(
		    conceptService.getConcepts(eq(conceptName), anyList(), eq(false), isNull(), isNull(), isNull(), isNull(),
		        isNull(), eq(0), isNull())).thenReturn(Arrays.asList(searchResult));
		
		when(filteredConcept.getUuid()).thenReturn("filtered-concept-uuid");
		when(filteredConcept.getSetMembers()).thenReturn(Arrays.asList(childConcept));
		
		when(childConcept.getUuid()).thenReturn("child-concept-uuid");
		when(childConcept.getDisplayString()).thenReturn("Child Concept");
		when(childConcept.isRetired()).thenReturn(false);
		when(childConcept.getConceptClass()).thenReturn(childConceptClass);
		when(childConcept.getSetMembers()).thenReturn(Collections.emptyList());
		
		when(childConceptClass.isRetired()).thenReturn(false);
		when(childConceptClass.getName()).thenReturn(conceptClassName);
		
		ValueSet translatedValueSet = new ValueSet();
		translatedValueSet.setId("filtered-concept-uuid");
		translatedValueSet.setName(conceptName);
		
		BahmniFhirValueSetServiceImpl spyService = new BahmniFhirValueSetServiceImpl() {
			@Override
			protected ValueSetTranslator getTranslator() {
				return valueSetTranslator;
			}
		};
		spyService.setConceptService(conceptService);
		
		when(valueSetTranslator.toFhirResource(filteredConcept)).thenReturn(translatedValueSet);
		
		// When
		ValueSet result = spyService.filterAndExpandValueSet(conceptName);
		
		// Then
		ValueSet.ValueSetExpansionComponent expansion = result.getExpansion();
		ValueSet.ValueSetExpansionContainsComponent childComponent = expansion.getContains().get(0);
		
		assertThat(childComponent.hasExtension(), is(true));
		assertThat(childComponent.getExtension(), hasSize(1));
		
		org.hl7.fhir.r4.model.Extension conceptClassExtension = childComponent.getExtension().get(0);
		assertThat(conceptClassExtension.getUrl(),
			equalTo(BahmniFhirConstants.VALUESET_CONCEPT_CLASS_EXTENSION_URL));
		assertThat(conceptClassExtension.getValue().toString(), equalTo(conceptClassName));
	}
	
	@Test
	public void filterAndExpandValueSet_shouldHandleConceptWithNoSetMembers() {
		// Given
		String conceptName = "Leaf Concept";
		Concept leafConcept = org.mockito.Mockito.mock(Concept.class);
		ConceptSearchResult searchResult = org.mockito.Mockito.mock(ConceptSearchResult.class);
		
		when(searchResult.getConcept()).thenReturn(leafConcept);
		when(
		    conceptService.getConcepts(eq(conceptName), eq(Arrays.asList(Locale.ENGLISH)), eq(false), isNull(), isNull(), isNull(), isNull(),
		        isNull(), eq(0), isNull())).thenReturn(Arrays.asList(searchResult));
		
		when(leafConcept.getUuid()).thenReturn("leaf-concept-uuid");
		
		ValueSet translatedValueSet = new ValueSet();
		translatedValueSet.setId("leaf-concept-uuid");
		translatedValueSet.setName(conceptName);
		
		// Create a spy service with mocked translator
		BahmniFhirValueSetServiceImpl spyService = new BahmniFhirValueSetServiceImpl() {
			
			@Override
			protected ValueSetTranslator getTranslator() {
				return valueSetTranslator;
			}
		};
		spyService.setConceptService(conceptService);
		
		when(valueSetTranslator.toFhirResource(leafConcept)).thenReturn(translatedValueSet);
		
		// When
		ValueSet result = spyService.filterAndExpandValueSet(conceptName);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.hasExpansion(), is(true));
		
		ValueSet.ValueSetExpansionComponent expansion = result.getExpansion();
		assertThat(expansion.getContains(), hasSize(0)); // No children
		assertThat(expansion.getTotal(), equalTo(0));
	}
	
	@Test
	public void filterAndExpandValueSet_shouldHandleNestedHierarchy() {
		// Given
		String conceptName = "Root Concept";
		Concept rootConcept = org.mockito.Mockito.mock(Concept.class);
		Concept level1Concept = org.mockito.Mockito.mock(Concept.class);
		Concept level2Concept = org.mockito.Mockito.mock(Concept.class);
		ConceptSearchResult searchResult = org.mockito.Mockito.mock(ConceptSearchResult.class);
		
		when(searchResult.getConcept()).thenReturn(rootConcept);
		when(
		    conceptService.getConcepts(eq(conceptName), eq(Arrays.asList(Locale.ENGLISH)), eq(false), isNull(), isNull(), isNull(), isNull(),
		        isNull(), eq(0), isNull())).thenReturn(Arrays.asList(searchResult));
		
		// Setup root concept
		when(rootConcept.getUuid()).thenReturn("root-uuid");
		when(rootConcept.getSetMembers()).thenReturn(Arrays.asList(level1Concept));
		
		// Setup level 1 concept
		when(level1Concept.getUuid()).thenReturn("level1-uuid");
		when(level1Concept.getDisplayString()).thenReturn("Level 1 Concept");
		when(level1Concept.isRetired()).thenReturn(false);
		when(level1Concept.getConceptClass()).thenReturn(conceptClass);
		when(level1Concept.getSetMembers()).thenReturn(Arrays.asList(level2Concept));
		
		// Setup level 2 concept
		when(level2Concept.getUuid()).thenReturn("level2-uuid");
		when(level2Concept.getDisplayString()).thenReturn("Level 2 Concept");
		when(level2Concept.isRetired()).thenReturn(false);
		when(level2Concept.getConceptClass()).thenReturn(conceptClass);
		when(level2Concept.getSetMembers()).thenReturn(Collections.emptyList());
		
		when(conceptClass.isRetired()).thenReturn(false);
		
		ValueSet translatedValueSet = new ValueSet();
		translatedValueSet.setId("root-uuid");
		translatedValueSet.setName(conceptName);
		
		// Create a spy service with mocked translator
		BahmniFhirValueSetServiceImpl spyService = new BahmniFhirValueSetServiceImpl() {
			
			@Override
			protected ValueSetTranslator getTranslator() {
				return valueSetTranslator;
			}
		};
		spyService.setConceptService(conceptService);
		
		when(valueSetTranslator.toFhirResource(rootConcept)).thenReturn(translatedValueSet);
		
		// When
		ValueSet result = spyService.filterAndExpandValueSet(conceptName);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.hasExpansion(), is(true));
		
		ValueSet.ValueSetExpansionComponent expansion = result.getExpansion();
		assertThat(expansion.getContains(), hasSize(1));
		
		// Check level 1
		ValueSet.ValueSetExpansionContainsComponent level1Component = expansion.getContains().get(0);
		assertThat(level1Component.getCode(), equalTo("level1-uuid"));
		assertThat(level1Component.getDisplay(), equalTo("Level 1 Concept"));
		assertThat(level1Component.getContains(), hasSize(1));
		
		// Check level 2
		ValueSet.ValueSetExpansionContainsComponent level2Component = level1Component.getContains().get(0);
		assertThat(level2Component.getCode(), equalTo("level2-uuid"));
		assertThat(level2Component.getDisplay(), equalTo("Level 2 Concept"));
		assertThat(level2Component.getContains(), hasSize(0));
	}
	
	@Test
	public void filterAndExpandValueSet_shouldHandleRetiredConceptsInFilteredResult() {
		// Given
		String conceptName = "Parent Concept";
		Concept parentConcept = org.mockito.Mockito.mock(Concept.class);
		Concept retiredChildConcept = org.mockito.Mockito.mock(Concept.class);
		ConceptSearchResult searchResult = org.mockito.Mockito.mock(ConceptSearchResult.class);
		
		when(searchResult.getConcept()).thenReturn(parentConcept);
		when(
		    conceptService.getConcepts(eq(conceptName), eq(Arrays.asList(Locale.ENGLISH)), eq(false), isNull(), isNull(), isNull(), isNull(),
		        isNull(), eq(0), isNull())).thenReturn(Arrays.asList(searchResult));
		
		when(parentConcept.getUuid()).thenReturn("parent-uuid");
		when(parentConcept.getSetMembers()).thenReturn(Arrays.asList(retiredChildConcept));
		
		when(retiredChildConcept.getUuid()).thenReturn("retired-child-uuid");
		when(retiredChildConcept.getDisplayString()).thenReturn("Retired Child");
		when(retiredChildConcept.isRetired()).thenReturn(true);
		when(retiredChildConcept.getSetMembers()).thenReturn(Collections.emptyList());
		
		ValueSet translatedValueSet = new ValueSet();
		translatedValueSet.setId("parent-uuid");
		translatedValueSet.setName(conceptName);
		
		// Create a spy service with mocked translator
		BahmniFhirValueSetServiceImpl spyService = new BahmniFhirValueSetServiceImpl() {
			
			@Override
			protected ValueSetTranslator getTranslator() {
				return valueSetTranslator;
			}
		};
		spyService.setConceptService(conceptService);
		
		when(valueSetTranslator.toFhirResource(parentConcept)).thenReturn(translatedValueSet);
		
		// When
		ValueSet result = spyService.filterAndExpandValueSet(conceptName);
		
		// Then
		ValueSet.ValueSetExpansionComponent expansion = result.getExpansion();
		ValueSet.ValueSetExpansionContainsComponent retiredComponent = expansion.getContains().get(0);
		
		assertThat(retiredComponent.getCode(), equalTo("retired-child-uuid"));
		assertThat(retiredComponent.getInactive(), equalTo(true));
	}
}
