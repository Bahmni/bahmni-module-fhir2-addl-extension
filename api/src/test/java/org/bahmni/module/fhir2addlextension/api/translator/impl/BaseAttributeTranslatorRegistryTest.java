package org.bahmni.module.fhir2addlextension.api.translator.impl;

import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReferenceAttribute;
import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReferenceAttributeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class BaseAttributeTranslatorRegistryTest {
	
	private static final String DEFAULT_EXT_URL_PREFIX = "http://fhir.bahmni.org/ext/default/attribute#";
	
	private static final String CUSTOM_EXT_URL_PREFIX = "http://fhir.bahmni.org/ext/custom/attribute#";
	
	private TestAttributeTranslatorRegistry registry;
	
	private TestAttributeTranslator defaultTranslator;
	
	private TestAttributeTranslator customTranslator;
	
	@Before
	public void setUp() {
		FhirDocumentReferenceAttributeType defaultAttrType = createAttributeType("Default Attribute");
		defaultTranslator = new TestAttributeTranslator(DEFAULT_EXT_URL_PREFIX, Collections.singletonList(defaultAttrType));
		
		FhirDocumentReferenceAttributeType customAttrType = createAttributeType("Custom Attribute");
		customTranslator = new TestAttributeTranslator(CUSTOM_EXT_URL_PREFIX, Collections.singletonList(customAttrType));
		
		registry = new TestAttributeTranslatorRegistry(defaultTranslator);
	}
	
	@Test
	public void shouldReturnFalseForHasAttributeTranslatorWhenNoMatchingExtension() {
		Extension extension = new Extension("http://unknown.url/attribute#test", new StringType("value"));
		
		assertFalse(registry.hasAttributeTranslator(extension));
	}
	
	@Test
	public void shouldReturnTrueForHasAttributeTranslatorWithDefaultTranslator() {
		Extension extension = new Extension(DEFAULT_EXT_URL_PREFIX + "default-attribute", new StringType("value"));
		
		assertTrue(registry.hasAttributeTranslator(extension));
	}
	
	@Test
	public void shouldReturnTrueForHasAttributeTranslatorWithRegisteredTranslator() {
		registry.registerAttributeTranslator(customTranslator);
		Extension extension = new Extension(CUSTOM_EXT_URL_PREFIX + "custom-attribute", new StringType("value"));
		
		assertTrue(registry.hasAttributeTranslator(extension));
	}
	
	@Test
	public void shouldGetDefaultTranslatorByExtensionUrl() {
		String extUrl = DEFAULT_EXT_URL_PREFIX + "default-attribute";
		
		Optional<TestAttributeTranslator> result = registry.getAttributeTranslator(extUrl);
		
		assertTrue(result.isPresent());
		assertEquals(defaultTranslator, result.get());
	}
	
	@Test
	public void shouldGetRegisteredTranslatorByExtensionUrl() {
		registry.registerAttributeTranslator(customTranslator);
		String extUrl = CUSTOM_EXT_URL_PREFIX + "custom-attribute";
		
		Optional<TestAttributeTranslator> result = registry.getAttributeTranslator(extUrl);
		
		assertTrue(result.isPresent());
		assertEquals(customTranslator, result.get());
	}
	
	@Test
	public void shouldReturnEmptyWhenNoTranslatorMatchesExtensionUrl() {
		String extUrl = "http://unknown.url/attribute#test";
		
		Optional<TestAttributeTranslator> result = registry.getAttributeTranslator(extUrl);
		
		assertFalse(result.isPresent());
	}
	
	@Test
	public void shouldPreferRegisteredTranslatorOverDefaultForMatchingUrl() {
		TestAttributeTranslator overlappingTranslator = new TestAttributeTranslator(DEFAULT_EXT_URL_PREFIX,
		        Collections.singletonList(createAttributeType("Overlapping Attribute")));
		registry.registerAttributeTranslator(overlappingTranslator);
		
		String extUrl = DEFAULT_EXT_URL_PREFIX + "overlapping-attribute";
		
		Optional<TestAttributeTranslator> result = registry.getAttributeTranslator(extUrl);
		
		assertTrue(result.isPresent());
		assertEquals(overlappingTranslator, result.get());
	}
	
	@Test
	public void shouldGetTranslatorByAttribute() {
		FhirDocumentReferenceAttribute attribute = new FhirDocumentReferenceAttribute();
		
		Optional<TestAttributeTranslator> result = registry.getAttributeTranslator(attribute);
		
		assertTrue(result.isPresent());
		assertEquals(defaultTranslator, result.get());
	}
	
	@Test
	public void shouldGetRegisteredTranslatorByAttributeWhenSupported() {
		TestAttributeTranslator selectiveTranslator = new SelectiveAttributeTranslator(CUSTOM_EXT_URL_PREFIX,
		        Collections.singletonList(createAttributeType("Custom Attribute")));
		registry.registerAttributeTranslator(selectiveTranslator);
		
		FhirDocumentReferenceAttribute attribute = new FhirDocumentReferenceAttribute();
		FhirDocumentReferenceAttributeType attrType = createAttributeType("Custom Attribute");
		attribute.setAttributeType(attrType);
		
		Optional<TestAttributeTranslator> result = registry.getAttributeTranslator(attribute);
		
		assertTrue(result.isPresent());
	}
	
	@Test
	public void shouldRegisterMultipleTranslators() {
		TestAttributeTranslator translator1 = new TestAttributeTranslator("http://url1.com/attr#",
		        Collections.singletonList(createAttributeType("Attr1")));
		TestAttributeTranslator translator2 = new TestAttributeTranslator("http://url2.com/attr#",
		        Collections.singletonList(createAttributeType("Attr2")));
		
		registry.registerAttributeTranslator(translator1);
		registry.registerAttributeTranslator(translator2);
		
		assertTrue(registry.getAttributeTranslator("http://url1.com/attr#attr1").isPresent());
		assertTrue(registry.getAttributeTranslator("http://url2.com/attr#attr2").isPresent());
	}
	
	@Test
	public void shouldFallbackToDefaultTranslatorWhenNoRegisteredTranslatorMatches() {
		registry.registerAttributeTranslator(customTranslator);
		String extUrl = DEFAULT_EXT_URL_PREFIX + "default-attribute";
		
		Optional<TestAttributeTranslator> result = registry.getAttributeTranslator(extUrl);
		
		assertTrue(result.isPresent());
		assertEquals(defaultTranslator, result.get());
	}
	
	private FhirDocumentReferenceAttributeType createAttributeType(String name) {
		FhirDocumentReferenceAttributeType attributeType = new FhirDocumentReferenceAttributeType();
		attributeType.setName(name);
		attributeType.setDatatypeClassname("org.openmrs.customdatatype.datatype.FreeTextDatatype");
		attributeType.setMinOccurs(0);
		attributeType.setMaxOccurs(1);
		return attributeType;
	}
	
	private static class TestAttributeTranslatorRegistry extends BaseAttributeTranslatorRegistry<FhirDocumentReferenceAttribute, FhirDocumentReferenceAttributeType, TestAttributeTranslator> {
		
		public TestAttributeTranslatorRegistry(TestAttributeTranslator defaultAttributeTranslator) {
			super(defaultAttributeTranslator);
		}
	}
	
	private static class TestAttributeTranslator extends BaseAttributeTranslator<FhirDocumentReferenceAttribute, FhirDocumentReferenceAttributeType> {
		
		private final String extensionUrlPrefix;
		
		private final List<FhirDocumentReferenceAttributeType> attributeTypes;
		
		public TestAttributeTranslator(String extensionUrlPrefix, List<FhirDocumentReferenceAttributeType> attributeTypes) {
			this.extensionUrlPrefix = extensionUrlPrefix;
			this.attributeTypes = attributeTypes;
		}
		
		@Override
		protected String getExtensionUrlPrefix() {
			return extensionUrlPrefix;
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
	}
	
	private static class SelectiveAttributeTranslator extends TestAttributeTranslator {
		
		public SelectiveAttributeTranslator(String extensionUrlPrefix,
		    List<FhirDocumentReferenceAttributeType> attributeTypes) {
			super(extensionUrlPrefix, attributeTypes);
		}
		
		@Override
		public boolean supports(FhirDocumentReferenceAttribute attribute) {
			if (attribute.getAttributeType() == null) {
				return false;
			}
			return attribute.getAttributeType().getName() != null
			        && attribute.getAttributeType().getName().contains("Custom");
		}
	}
}
