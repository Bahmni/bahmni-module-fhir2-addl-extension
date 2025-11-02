package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.bahmni.module.fhir2AddlExtension.api.dao.DocumentReferenceAttributeTypeDao;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReferenceAttribute;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.bahmni.module.fhir2AddlExtension.api.TestDataFactory.exampleAttrTypeExternalOrganization;
import static org.bahmni.module.fhir2AddlExtension.api.TestDataFactory.exampleAttrTypeIsSelfSubmitted;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultDocumentReferenceAttributeTranslatorImplTest {
	
	private DefaultDocumentReferenceAttributeTranslatorImpl attributeTranslator;
	
	@Mock
	private DocumentReferenceAttributeTypeDao attributeTypeDao;
	
	@Before
	public void setUp() {
		attributeTranslator = new DefaultDocumentReferenceAttributeTranslatorImpl(attributeTypeDao);
		when(attributeTypeDao.getAttributeTypes(false)).thenReturn(
		    Arrays.asList(exampleAttrTypeExternalOrganization(), exampleAttrTypeIsSelfSubmitted()));
	}
	
	@Test
	public void shouldSupportAttributeExtensionUrl() {
		Assert.assertTrue(attributeTranslator.getAttributeType(
		    "http://fhir.bahmni.org/ext/document-reference/attribute#external-organization").isPresent());
		Assert.assertFalse(attributeTranslator.getAttributeType(
		    "http://fhir.bahmni.org/ext/document-reference/some-attribute#external-organization").isPresent());
	}
	
	@Test
	public void shouldConvertExtensionToOpenmrsType() {
		String extnUrl = "http://fhir.bahmni.org/ext/document-reference/attribute#external-organization";
		Extension extOrgExtn = new Extension(extnUrl, new StringType("Good Health Clinic"));
		List<FhirDocumentReferenceAttribute> fhirDocumentReferenceAttributes = attributeTranslator.toOpenmrsType(extnUrl,
		    Collections.singletonList(extOrgExtn));
		Assert.assertEquals(1, fhirDocumentReferenceAttributes.size());
		Assert.assertEquals("Good Health Clinic", fhirDocumentReferenceAttributes.get(0).getValueReference());
	}
	
	@Test(expected = UnprocessableEntityException.class)
	public void shouldThrowErrorForMoreThanAllowedAttributeNumber() {
		String extnUrl = "http://fhir.bahmni.org/ext/document-reference/attribute#external-organization";
		Extension extOrgExtn1 = new Extension(extnUrl, new StringType("Good Health Clinic"));
		Extension extOrgExtn2 = new Extension(extnUrl, new StringType("Good Samaritan Clinic"));
		attributeTranslator.toOpenmrsType(extnUrl, Arrays.asList(extOrgExtn1, extOrgExtn2));
	}
	
	@Ignore("Ignored because Openmrs Attribute uses  CustomDatatypeUtil.getDatatype() to load datatypeclassname, and in turn loads DataTypeService. "
	        + "Other than to use Powermock to mock static CustomDatatypeUtil.getDatatype(), only writing an integration test will work. ")
	@Test
	public void shouldConvertOpenmrsTypeToExtension() {
		FhirDocumentReferenceAttribute attribute = new FhirDocumentReferenceAttribute();
		attribute.setAttributeType(exampleAttrTypeExternalOrganization());
		attribute.setValueReferenceInternal("Good Health Clinic");
		Extension extension = attributeTranslator.toFhirResource(attribute);
		Assert.assertEquals("http://fhir.bahmni.org/ext/document-reference/attribute#external-organization",
		    extension.getUrl());
		Assert.assertEquals("Good Health Clinic", extension.getValue().toString());
	}
}
