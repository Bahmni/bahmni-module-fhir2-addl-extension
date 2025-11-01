package org.bahmni.module.fhir2AddlExtension.api.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
	
}
