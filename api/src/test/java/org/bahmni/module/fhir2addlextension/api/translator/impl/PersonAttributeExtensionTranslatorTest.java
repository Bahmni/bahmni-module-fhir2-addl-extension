package org.bahmni.module.fhir2addlextension.api.translator.impl;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.PersonService;

import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PersonAttributeExtensionTranslatorTest {

	private static final String PREFIX = "http://fhir.bahmni.org/ext/patient/";

	@Mock
	private PersonService personService;

	private PersonAttributeExtensionTranslator translator;

	private PersonAttributeType phoneType;

	private PersonAttributeType boolType;

	@Before
	public void setup() {
		translator = new PersonAttributeExtensionTranslator(personService);

		phoneType = new PersonAttributeType();
		phoneType.setUuid("phone-uuid");
		phoneType.setName("phoneNumber");
		phoneType.setFormat("java.lang.String");

		boolType = new PersonAttributeType();
		boolType.setUuid("bool-uuid");
		boolType.setName("isVIP");
		boolType.setFormat("org.openmrs.customdatatype.datatype.BooleanDatatype");

		when(personService.getAllPersonAttributeTypes(false)).thenReturn(Arrays.asList(phoneType, boolType));
	}

	@Test
	public void toFhirResource_shouldConvertStringAttribute() {
		PersonAttribute attr = new PersonAttribute(phoneType, "+919876543210");

		Extension ext = translator.toFhirResource(attr);

		assertEquals(PREFIX + "phonenumber", ext.getUrl());
		assertEquals("+919876543210", ((StringType) ext.getValue()).getValue());
	}

	@Test
	public void toFhirResource_shouldConvertBooleanAttribute() {
		PersonAttribute attr = new PersonAttribute(boolType, "true");

		Extension ext = translator.toFhirResource(attr);

		assertEquals(PREFIX + "isvip", ext.getUrl());
		assertTrue(((BooleanType) ext.getValue()).booleanValue());
	}

	@Test
	public void toFhirResource_shouldReturnNullForNullValue() {
		PersonAttribute attr = new PersonAttribute(phoneType, null);

		assertNull(translator.toFhirResource(attr));
	}

	@Test
	public void resolveType_shouldMatchBySlugName() {
		PersonAttributeType result = translator.resolveType(PREFIX + "phonenumber");

		assertNotNull(result);
		assertEquals("phone-uuid", result.getUuid());
	}

	@Test
	public void resolveType_shouldReturnNullForUnknownSlug() {
		assertNull(translator.resolveType(PREFIX + "unknown"));
	}

	@Test
	public void resolveType_shouldReturnNullForNonPatientUrl() {
		assertNull(translator.resolveType("http://fhir.bahmni.org/ext/service-request/something"));
	}

	@Test
	public void resolveType_shouldReturnNullForNullUrl() {
		assertNull(translator.resolveType(null));
	}
}
