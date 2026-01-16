package org.bahmni.module.fhir2AddlExtension.api.utils;

import org.hibernate.criterion.Criterion;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ModuleUtilsTest {
	
	@Test
	public void shouldConvertAttributeNametoFhirExtensionName() {
		assertEquals("", ModuleUtils.toSlugCase(""));
		assertEquals("attribute1", ModuleUtils.toSlugCase("Attribute1"));
		assertEquals("case-number", ModuleUtils.toSlugCase("Case number"));
		assertEquals("case-1-2-number", ModuleUtils.toSlugCase("Case 1 & 2 number"));
		assertEquals("case-1-2-3-number", ModuleUtils.toSlugCase("Case 1 & 2 && 3 number"));
		//String attributeString = "http://fhir.bahmni.org/ext//document-reference/attribute#case-1-2-3-number";
	}
	
	@Test
	public void isConceptReferenceCodeEmpty_shouldReturnTrueWhenCodesIsNull() {
		assertTrue(ModuleUtils.isConceptReferenceCodeEmpty(null));
	}
	
	@Test
	public void isConceptReferenceCodeEmpty_shouldReturnTrueWhenCodesIsEmptyList() {
		assertTrue(ModuleUtils.isConceptReferenceCodeEmpty(Collections.emptyList()));
	}
	
	@Test
	public void isConceptReferenceCodeEmpty_shouldReturnTrueWhenCodesHasSingleEmptyString() {
		assertTrue(ModuleUtils.isConceptReferenceCodeEmpty(Collections.singletonList("")));
	}
	
	@Test
	public void isConceptReferenceCodeEmpty_shouldReturnFalseWhenCodesHasNonEmptyValue() {
		assertFalse(ModuleUtils.isConceptReferenceCodeEmpty(Collections.singletonList("code1")));
	}
	
	@Test
	public void isConceptReferenceCodeEmpty_shouldReturnFalseWhenCodesHasMultipleValues() {
		assertFalse(ModuleUtils.isConceptReferenceCodeEmpty(Arrays.asList("code1", "code2")));
	}
	
	@Test
	public void generateSystemQueryForEmptyCodes_shouldReturnPropertySubqueryExpression() {
		String system = "http://example.com/system";
		String alias = "crt";
		
		Criterion result = ModuleUtils.generateSystemQueryForEmptyCodes(system, alias);
		
		assertNotNull(result);
		assertTrue(result instanceof org.hibernate.criterion.PropertySubqueryExpression);
	}
}
