package org.bahmni.module.fhir2addlextension.api.translator.impl;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReferenceAttribute;
import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReferenceAttributeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class BaseAttributeTranslatorTest {
	
	private static final String TEST_EXT_URL_PREFIX = "http://fhir.bahmni.org/ext/test/attribute#";
	
	private TestAttributeTranslator attributeTranslator;
	
	private List<FhirDocumentReferenceAttributeType> attributeTypes;
	
	@Before
	public void setUp() {
		FhirDocumentReferenceAttributeType stringAttrType = createAttributeType("Test String Attribute",
		    "org.openmrs.customdatatype.datatype.FreeTextDatatype", 1);
		FhirDocumentReferenceAttributeType booleanAttrType = createAttributeType("Test Boolean Attribute",
		    "org.openmrs.customdatatype.datatype.BooleanDatatype", 1);
		FhirDocumentReferenceAttributeType multiValueAttrType = createAttributeType("Multi Value Attribute",
		    "org.openmrs.customdatatype.datatype.FreeTextDatatype", 3);
		
		attributeTypes = Arrays.asList(stringAttrType, booleanAttrType, multiValueAttrType);
		attributeTranslator = new TestAttributeTranslator(attributeTypes);
	}
	
	@Test
	public void shouldSupportAnyAttribute() {
		FhirDocumentReferenceAttribute attribute = new FhirDocumentReferenceAttribute();
		assertTrue(attributeTranslator.supports(attribute));
	}
	
	@Test
	public void shouldSupportValidExtensionUrl() {
		assertTrue(attributeTranslator.supportsUrl(TEST_EXT_URL_PREFIX + "test-string-attribute"));
	}
	
	@Test
	public void shouldNotSupportInvalidExtensionUrl() {
		assertFalse(attributeTranslator.supportsUrl("http://invalid.url/attribute#test"));
		assertFalse(attributeTranslator.supportsUrl(null));
	}
	
	@Test
	public void shouldGetAttributeTypeForValidExtensionUrl() {
		String extUrl = TEST_EXT_URL_PREFIX + "test-string-attribute";
		
		Optional<FhirDocumentReferenceAttributeType> result = attributeTranslator.getAttributeType(extUrl);
		
		assertTrue(result.isPresent());
		assertEquals("Test String Attribute", result.get().getName());
	}
	
	@Test
	public void shouldReturnEmptyForInvalidAttributeName() {
		String extUrl = TEST_EXT_URL_PREFIX + "non-existent-attribute";
		
		Optional<FhirDocumentReferenceAttributeType> result = attributeTranslator.getAttributeType(extUrl);
		
		assertFalse(result.isPresent());
	}
	
	@Test
	public void shouldReturnEmptyForEmptyAttributeName() {
		String extUrl = TEST_EXT_URL_PREFIX;
		
		Optional<FhirDocumentReferenceAttributeType> result = attributeTranslator.getAttributeType(extUrl);
		
		assertFalse(result.isPresent());
	}
	
	@Test
	public void shouldReturnEmptyForUnsupportedUrl() {
		String extUrl = "http://unsupported.url/attribute#test";
		
		Optional<FhirDocumentReferenceAttributeType> result = attributeTranslator.getAttributeType(extUrl);
		
		assertFalse(result.isPresent());
	}
	
	@Test
	public void shouldConvertExtensionToOpenmrsType() {
		String extUrl = TEST_EXT_URL_PREFIX + "test-string-attribute";
		Extension extension = new Extension(extUrl, new StringType("Test Value"));
		
		List<FhirDocumentReferenceAttribute> result = attributeTranslator.toOpenmrsType(extUrl,
		    Collections.singletonList(extension));
		
		assertEquals(1, result.size());
		assertEquals("Test Value", result.get(0).getValueReference());
		assertNotNull(result.get(0).getAttributeType());
	}
	
	@Test
	public void shouldReturnEmptyListForUnknownAttributeType() {
		String extUrl = TEST_EXT_URL_PREFIX + "unknown-attribute";
		Extension extension = new Extension(extUrl, new StringType("Test Value"));
		
		List<FhirDocumentReferenceAttribute> result = attributeTranslator.toOpenmrsType(extUrl,
		    Collections.singletonList(extension));
		
		assertTrue(result.isEmpty());
	}
	
	@Test
	public void shouldFilterExtensionsWithNullValues() {
		String extUrl = TEST_EXT_URL_PREFIX + "test-string-attribute";
		Extension extWithValue = new Extension(extUrl, new StringType("Test Value"));
		Extension extWithoutValue = new Extension(extUrl);
		
		List<FhirDocumentReferenceAttribute> result = attributeTranslator.toOpenmrsType(extUrl,
		    Arrays.asList(extWithValue, extWithoutValue));
		
		assertEquals(1, result.size());
	}
	
	@Test
	public void shouldConvertMultipleExtensionsWithinLimit() {
		String extUrl = TEST_EXT_URL_PREFIX + "multi-value-attribute";
		Extension ext1 = new Extension(extUrl, new StringType("Value 1"));
		Extension ext2 = new Extension(extUrl, new StringType("Value 2"));
		Extension ext3 = new Extension(extUrl, new StringType("Value 3"));
		
		List<FhirDocumentReferenceAttribute> result = attributeTranslator.toOpenmrsType(extUrl,
		    Arrays.asList(ext1, ext2, ext3));
		
		assertEquals(3, result.size());
	}
	
	@Test(expected = UnprocessableEntityException.class)
	public void shouldThrowErrorWhenExceedingMaxOccurs() {
		String extUrl = TEST_EXT_URL_PREFIX + "test-string-attribute";
		Extension ext1 = new Extension(extUrl, new StringType("Value 1"));
		Extension ext2 = new Extension(extUrl, new StringType("Value 2"));
		
		attributeTranslator.toOpenmrsType(extUrl, Arrays.asList(ext1, ext2));
	}
	
	private FhirDocumentReferenceAttributeType createAttributeType(String name, String datatypeClassname, int maxOccurs) {
		FhirDocumentReferenceAttributeType attributeType = new FhirDocumentReferenceAttributeType();
		attributeType.setName(name);
		attributeType.setDatatypeClassname(datatypeClassname);
		attributeType.setMinOccurs(0);
		attributeType.setMaxOccurs(maxOccurs);
		return attributeType;
	}
	
	private static class TestAttributeTranslator extends BaseAttributeTranslator<FhirDocumentReferenceAttribute, FhirDocumentReferenceAttributeType> {
		
		private final List<FhirDocumentReferenceAttributeType> attributeTypes;
		
		public TestAttributeTranslator(List<FhirDocumentReferenceAttributeType> attributeTypes) {
			this.attributeTypes = attributeTypes;
		}
		
		@Override
		protected String getExtensionUrlPrefix() {
			return TEST_EXT_URL_PREFIX;
		}
		
		@Override
		protected ResourceType getResourceType() {
			return ResourceType.DocumentReference;
		}
		
		@Override
		protected List<FhirDocumentReferenceAttributeType> getActiveAttributeTypes() {
			return attributeTypes;
		}
		
		@Override
		protected FhirDocumentReferenceAttribute createAttribute() {
			return new FhirDocumentReferenceAttribute();
		}
		
		public boolean supportsUrl(String extUrl) {
			return supports(extUrl);
		}
	}
}
