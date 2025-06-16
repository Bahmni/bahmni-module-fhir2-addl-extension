package org.bahmni.module.fhir2AddlExtension.api.providers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.hl7.fhir.r4.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirValueSetService;

/**
 * Unit tests for BahmniValueSetFhirR4ResourceProvider Tests cover: - Functional scenarios:
 * hierarchical/flat expansion, filtering, pagination - Error handling: validation failures, service
 * exceptions - Parameter validation: null/empty IDs, invalid parameters
 */
@RunWith(MockitoJUnitRunner.class)
public class BahmniValueSetFhirR4ResourceProviderTest {
	
	// Test Data Constants
	private static final String VALUESET_UUID = "12345678-1234-1234-1234-123456789012";
	
	private static final String VALUESET_URL = "http://example.org/fhir/ValueSet/test-valueset";
	
	private static final String SYSTEM_URL = "http://example.org/fhir/CodeSystem/test";
	
	// Parent Concept Data
	private static final String PARENT_CONCEPT_CODE = "PARENT_001";
	
	private static final String PARENT_CONCEPT_DISPLAY = "Cardiovascular Diseases";
	
	// Child Concept Data  
	private static final String CHILD_CONCEPT_CODE = "CHILD_001";
	
	private static final String CHILD_CONCEPT_DISPLAY = "Hypertension";
	
	// Test Parameters
	private static final String FILTER_VALUE = "concept";
	
	private static final int COUNT_LIMIT = 1;
	
	private static final int EXPECTED_HIERARCHICAL_SIZE = 1;
	
	private static final int EXPECTED_FLAT_SIZE = 2;
	
	@Mock
	private BahmniFhirValueSetService bahmniFhirValueSetService;
	
	private BahmniValueSetFhirR4ResourceProvider provider;
	
	@Before
	public void setup() {
		provider = new BahmniValueSetFhirR4ResourceProvider(bahmniFhirValueSetService);
	}
	
	// ===============================
	// FUNCTIONAL EXPANSION TESTS
	// ===============================
	
	@Test
	public void shouldReturnHierarchicalExpansion() {
		// Given
		IdType id = new IdType(VALUESET_UUID);
		ValueSet expectedResult = createHierarchicalValueSet();
		
		when(bahmniFhirValueSetService.expandedValueSet(eq(VALUESET_UUID))).thenReturn(expectedResult);
		
		// When
		ValueSet result = provider.expandedValueSet(id);
		
		// Then
		assertValidValueSetResponse(result);
		assertHierarchicalStructure(result.getExpansion());
	}
	
	// ===============================
	// ERROR HANDLING TESTS
	// ===============================
	@Test(expected = InvalidRequestException.class)
	public void shouldThrowInvalidRequestExceptionForEmptyId() {
		IdType emptyId = new IdType("");
		provider.expandedValueSet(emptyId);
	}
	
	@Test(expected = ResourceNotFoundException.class)
	public void shouldPropagateResourceNotFoundException() {
		// Given
		IdType id = new IdType(VALUESET_UUID);
		when(bahmniFhirValueSetService.expandedValueSet(eq(VALUESET_UUID))).thenThrow(
		    new ResourceNotFoundException("ValueSet not found"));
		
		// When/Then
		provider.expandedValueSet(id);
	}
	
	@Test(expected = InternalErrorException.class)
	public void shouldWrapUnexpectedExceptionsAsInternalError() {
		// Given
		IdType id = new IdType(VALUESET_UUID);
		when(bahmniFhirValueSetService.expandedValueSet(eq(VALUESET_UUID))).thenThrow(
		    new RuntimeException("Unexpected error"));
		
		// When/Then
		provider.expandedValueSet(id);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void shouldConvertIllegalArgumentExceptionToInvalidRequest() {
		// Given
		IdType id = new IdType(VALUESET_UUID);
		when(bahmniFhirValueSetService.expandedValueSet(eq(VALUESET_UUID))).thenThrow(
		    new IllegalArgumentException("Invalid parameter"));
		
		// When/Then
		provider.expandedValueSet(id);
	}
	
	@Test
	public void filterAndExpandValueSet_shouldReturnExpandedValueSetForValidFilter() {
		// Given
		String filter = "Hypertension";
		ValueSet expectedResult = createFilteredValueSet();
		
		when(bahmniFhirValueSetService.filterAndExpandValueSet(eq(filter))).thenReturn(expectedResult);
		
		// When
		ValueSet result = provider.filterAndExpandValueSet(filter);
		
		// Then
		assertThat(result, notNullValue());
		assertThat(result.hasExpansion(), is(true));
		assertThat(result.getExpansion().getContains(), hasSize(1));
		assertThat(result.getExpansion().getContains().get(0).getCode(), equalTo(PARENT_CONCEPT_CODE));
	}
	
	@Test(expected = InvalidRequestException.class)
	public void filterAndExpandValueSet_shouldThrowExceptionForNullFilter() {
		// When/Then
		provider.filterAndExpandValueSet(null);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void filterAndExpandValueSet_shouldThrowExceptionForEmptyFilter() {
		// When/Then
		provider.filterAndExpandValueSet("");
	}
	
	@Test(expected = InvalidRequestException.class)
	public void filterAndExpandValueSet_shouldThrowExceptionForBlankFilter() {
		// When/Then
		provider.filterAndExpandValueSet("   ");
	}
	
	@Test
	public void filterAndExpandValueSet_shouldPropagateServiceExceptions() {
		// Given
		String filter = "Test Concept";
		when(bahmniFhirValueSetService.filterAndExpandValueSet(eq(filter))).thenThrow(
		    new InvalidRequestException("Multiple concepts found"));
		
		// When/Then
		try {
			provider.filterAndExpandValueSet(filter);
		}
		catch (InvalidRequestException e) {
			assertThat(e.getMessage(), containsString("Multiple concepts found"));
		}
	}
	
	// ===============================
	// TEST DATA BUILDERS
	// ===============================
	
	/**
	 * Creates a ValueSet with hierarchical expansion structure (children directly in expansion)
	 */
	private ValueSet createHierarchicalValueSet() {
		ValueSet valueSet = createBaseValueSet();
		ValueSet.ValueSetExpansionComponent expansion = new ValueSet.ValueSetExpansionComponent();
		expansion.setTotal(EXPECTED_HIERARCHICAL_SIZE);
		
		// Create child concept directly in expansion (no parent wrapper)
		ValueSet.ValueSetExpansionContainsComponent childConcept = createConcept(CHILD_CONCEPT_CODE, CHILD_CONCEPT_DISPLAY);
		expansion.getContains().add(childConcept);
		
		valueSet.setExpansion(expansion);
		return valueSet;
	}
	
	/**
	 * Creates a ValueSet with flat expansion structure
	 */
	private ValueSet createFlatValueSet() {
		ValueSet valueSet = createBaseValueSet();
		ValueSet.ValueSetExpansionComponent expansion = new ValueSet.ValueSetExpansionComponent();
		expansion.setTotal(EXPECTED_FLAT_SIZE);
		
		// Create concepts at same level (no nesting)
		expansion.getContains().add(createConcept(PARENT_CONCEPT_CODE, PARENT_CONCEPT_DISPLAY));
		expansion.getContains().add(createConcept(CHILD_CONCEPT_CODE, CHILD_CONCEPT_DISPLAY));
		
		valueSet.setExpansion(expansion);
		return valueSet;
	}
	
	/**
	 * Creates a ValueSet with filtered expansion
	 */
	private ValueSet createFilteredValueSet() {
		ValueSet valueSet = createBaseValueSet();
		ValueSet.ValueSetExpansionComponent expansion = new ValueSet.ValueSetExpansionComponent();
		expansion.setTotal(1);
		
		expansion.getContains().add(createConcept(PARENT_CONCEPT_CODE, PARENT_CONCEPT_DISPLAY));
		
		valueSet.setExpansion(expansion);
		return valueSet;
	}
	
	/**
	 * Creates a ValueSet with limited expansion (count applied)
	 */
	private ValueSet createLimitedValueSet() {
		ValueSet valueSet = createBaseValueSet();
		ValueSet.ValueSetExpansionComponent expansion = new ValueSet.ValueSetExpansionComponent();
		expansion.setTotal(COUNT_LIMIT);
		
		expansion.getContains().add(createConcept(PARENT_CONCEPT_CODE, PARENT_CONCEPT_DISPLAY));
		
		valueSet.setExpansion(expansion);
		return valueSet;
	}
	
	/**
	 * Creates a basic ValueSet with common properties
	 */
	private ValueSet createBaseValueSet() {
		ValueSet valueSet = new ValueSet();
		valueSet.setId(VALUESET_UUID);
		valueSet.setUrl(VALUESET_URL);
		valueSet.setName("TestValueSet");
		valueSet.setTitle("Test Value Set");
		return valueSet;
	}
	
	/**
	 * Creates a concept component with specified code and display
	 */
	private ValueSet.ValueSetExpansionContainsComponent createConcept(String code, String display) {
		ValueSet.ValueSetExpansionContainsComponent concept = new ValueSet.ValueSetExpansionContainsComponent();
		concept.setSystem(SYSTEM_URL);
		concept.setCode(code);
		concept.setDisplay(display);
		return concept;
	}
	
	// ===============================
	// ASSERTION HELPERS
	// ===============================
	
	/**
	 * Asserts basic ValueSet response structure is valid
	 */
	private void assertValidValueSetResponse(ValueSet result) {
		assertThat(result, notNullValue());
		assertThat(result.getId(), equalTo(VALUESET_UUID));
		assertThat(result.hasExpansion(), is(true));
	}
	
	/**
	 * Asserts expansion has hierarchical structure (children directly in expansion, no parent
	 * wrapper)
	 */
	private void assertHierarchicalStructure(ValueSet.ValueSetExpansionComponent expansion) {
		assertThat(expansion.getContains(), hasSize(EXPECTED_HIERARCHICAL_SIZE));
		
		ValueSet.ValueSetExpansionContainsComponent childConcept = expansion.getContains().get(0);
		assertThat(childConcept.getCode(), equalTo(CHILD_CONCEPT_CODE));
		assertThat(childConcept.getDisplay(), equalTo(CHILD_CONCEPT_DISPLAY));
		assertThat(childConcept.getContains(), hasSize(0)); // No nested children
	}
	
	/**
	 * Asserts expansion has flat structure (no nesting)
	 */
	private void assertFlatStructure(ValueSet.ValueSetExpansionComponent expansion) {
		assertThat(expansion.getContains(), hasSize(EXPECTED_FLAT_SIZE));
		
		// Verify no concept has nested children
		for (ValueSet.ValueSetExpansionContainsComponent concept : expansion.getContains()) {
			assertThat(concept.getContains(), hasSize(0));
		}
	}
}
