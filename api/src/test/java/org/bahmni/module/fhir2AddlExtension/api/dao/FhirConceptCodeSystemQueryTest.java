package org.bahmni.module.fhir2AddlExtension.api.dao;

import org.hibernate.criterion.Criterion;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FhirConceptCodeSystemQueryTest {
	
	private FhirConceptCodeSystemQuery codeSystemQuery;
	
	@Before
	public void setUp() {
		codeSystemQuery = new FhirConceptCodeSystemQuery() {};
	}
	
	@Test
	public void isConceptReferenceCodeEmpty_shouldReturnTrueWhenCodesIsNull() {
		assertTrue(codeSystemQuery.isConceptReferenceCodeEmpty(null));
	}
	
	@Test
	public void isConceptReferenceCodeEmpty_shouldReturnTrueWhenCodesIsEmptyList() {
		assertTrue(codeSystemQuery.isConceptReferenceCodeEmpty(Collections.emptyList()));
	}
	
	@Test
	public void isConceptReferenceCodeEmpty_shouldReturnTrueWhenCodesHasSingleEmptyString() {
		assertTrue(codeSystemQuery.isConceptReferenceCodeEmpty(Collections.singletonList("")));
	}
	
	@Test
	public void isConceptReferenceCodeEmpty_shouldReturnFalseWhenCodesHasNonEmptyValue() {
		assertFalse(codeSystemQuery.isConceptReferenceCodeEmpty(Collections.singletonList("code1")));
	}
	
	@Test
	public void isConceptReferenceCodeEmpty_shouldReturnFalseWhenCodesHasMultipleValues() {
		assertFalse(codeSystemQuery.isConceptReferenceCodeEmpty(Arrays.asList("code1", "code2")));
	}
	
	@Test
	public void generateSystemQueryForEmptyCodes_shouldReturnPropertySubqueryExpression() {
		String system = "http://example.com/system";
		String alias = "crt";
		
		Criterion result = codeSystemQuery.generateSystemQueryForEmptyCodes(system, alias);
		
		assertNotNull(result);
		assertTrue(result instanceof org.hibernate.criterion.PropertySubqueryExpression);
	}
}
