package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.dao.DocumentReferenceAttributeTypeDao;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReferenceAttribute;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReferenceAttributeType;
import org.bahmni.module.fhir2AddlExtension.api.translator.DocumentReferenceAttributeTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.DocumentReferenceExtensionTranslator;
import org.hl7.fhir.r4.model.Extension;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.bahmni.module.fhir2AddlExtension.api.TestDataFactory.exampleAttrTypeExternalOrganization;
import static org.bahmni.module.fhir2AddlExtension.api.TestDataFactory.exampleAttrTypeIsSelfSubmitted;

public class DocumentReferenceExtensionTranslatorImplTest {
	
	private DocumentReferenceExtensionTranslator extensionTranslator;
	
	private DefaultDocumentReferenceAttributeTranslatorImpl defaultAttributeTranslator;
	
	private List<FhirDocumentReferenceAttributeType> supportedAttributes = Arrays.asList(
	    exampleAttrTypeExternalOrganization(), exampleAttrTypeIsSelfSubmitted());
	
	@Before
    public void setUp() {
        DocumentReferenceAttributeTypeDao attributeTypeDao = includeRetired -> supportedAttributes;
        defaultAttributeTranslator = new DefaultDocumentReferenceAttributeTranslatorImpl(attributeTypeDao);
        extensionTranslator = new DocumentReferenceExtensionTranslatorImpl(defaultAttributeTranslator);
    }
	
	@Test
	public void shouldReturnDefaultAttributeTranslatorForExtensionUrl() {
		Optional<DocumentReferenceAttributeTranslator> attributeTranslator = extensionTranslator
		        .getAttributeTranslator("http://fhir.bahmni.org/ext/document-reference/attribute#external-organization");
		Assert.assertTrue(attributeTranslator.isPresent());
		Assert.assertTrue(attributeTranslator.get() instanceof DefaultDocumentReferenceAttributeTranslatorImpl);
	}
	
	@Test
	public void shouldNotReturnAttributeTranslatorForUnknownExtensionUrl() {
		Optional<DocumentReferenceAttributeTranslator> attributeTranslator = extensionTranslator
		        .getAttributeTranslator("http://fhir.bahmni.org/ext/document-reference/something#external-organization");
		Assert.assertFalse(attributeTranslator.isPresent());
	}
	
	@Test
	public void shouldReturnRegisteredTranslatorForExternalOrganizationAttribute() {
		extensionTranslator.registerAttributeTranslator(proxyTranslator("External Organization"));
		Optional<DocumentReferenceAttributeTranslator> attributeTranslator = extensionTranslator
		        .getAttributeTranslator("http://fhir.bahmni.org/ext/document-reference/attribute#external-organization");
		Assert.assertTrue(attributeTranslator.isPresent());
		Assert.assertFalse(attributeTranslator.get() instanceof DefaultDocumentReferenceAttributeTranslatorImpl);
	}
	
	private DocumentReferenceAttributeTranslator proxyTranslator(String attrName) {
		return new DocumentReferenceAttributeTranslator() {
			
			@Override
			public boolean supports(FhirDocumentReferenceAttribute attribute) {
				return attribute.equals(supportedAttributes.get(0));
			}
			
			@Override
			public List<FhirDocumentReferenceAttribute> toOpenmrsType(String extUrl, List<Extension> extensions) {
				return Collections.emptyList();
			}
			
			@Override
			public Extension toFhirResource(FhirDocumentReferenceAttribute attribute) {
				return null;
			}
			
			@Override
			public Optional<FhirDocumentReferenceAttributeType> getAttributeType(String extUrl) {
				FhirDocumentReferenceAttributeType attributeType = new FhirDocumentReferenceAttributeType();
				attributeType.setDatatypeClassname("org.openmrs.customdatatype.datatype.FreeTextDatatype");
				attributeType.setName(attrName);
				attributeType.setMaxOccurs(1);
				return Optional.of(attributeType);
			}
		};
	}
}
