package org.bahmni.module.fhir2addlextension.api.translator.impl;

import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniPatientTranslatorImplTest {

	private static final String PREFIX = "http://fhir.bahmni.org/ext/patient/";

	@Mock
	private PersonAttributeExtensionTranslator personAttributeTranslator;

	private BahmniPatientTranslatorImpl translator;

	private PersonAttributeType phoneType;

	@Before
	public void setup() {
		translator = new BahmniPatientTranslatorImpl();
		translator.setPersonAttributeTranslator(personAttributeTranslator);

		phoneType = new PersonAttributeType();
		phoneType.setUuid("phone-uuid");
		phoneType.setName("phoneNumber");
		phoneType.setFormat("java.lang.String");
	}

	@Test
	public void toOpenmrsType_shouldUpsertAttributeWithValue() {
		org.openmrs.Patient existingPatient = new org.openmrs.Patient();
		existingPatient.setUuid("patient-uuid");

		Patient fhirPatient = new Patient();
		fhirPatient.setId("patient-uuid");
		fhirPatient.addExtension(new Extension(PREFIX + "phonenumber", new StringType("+919876543210")));

		when(personAttributeTranslator.resolveType(PREFIX + "phonenumber")).thenReturn(phoneType);

		org.openmrs.Patient result = translator.toOpenmrsType(existingPatient, fhirPatient);

		boolean found = false;
		for (PersonAttribute attr : result.getAttributes()) {
			if ("phoneNumber".equals(attr.getAttributeType().getName())) {
				assertEquals("+919876543210", attr.getValue());
				found = true;
			}
		}
		assertTrue("Phone attribute should be added", found);
	}

	@Test
	public void toOpenmrsType_shouldUpdateExistingAttributeValue() {
		org.openmrs.Patient existingPatient = new org.openmrs.Patient();
		existingPatient.setUuid("patient-uuid");
		PersonAttribute existingAttr = new PersonAttribute(phoneType, "+91-OLD");
		existingPatient.addAttribute(existingAttr);

		Patient fhirPatient = new Patient();
		fhirPatient.setId("patient-uuid");
		fhirPatient.addExtension(new Extension(PREFIX + "phonenumber", new StringType("+91-NEW")));

		when(personAttributeTranslator.resolveType(PREFIX + "phonenumber")).thenReturn(phoneType);

		org.openmrs.Patient result = translator.toOpenmrsType(existingPatient, fhirPatient);

		assertTrue("Old attribute should be voided", existingAttr.getVoided());
		boolean foundNew = false;
		for (PersonAttribute attr : result.getAttributes()) {
			if (!attr.getVoided() && "phoneNumber".equals(attr.getAttributeType().getName())) {
				assertEquals("+91-NEW", attr.getValue());
				foundNew = true;
			}
		}
		assertTrue("New attribute value should exist", foundNew);
	}

	@Test
	public void toOpenmrsType_shouldVoidAttributeWithoutValue() {
		org.openmrs.Patient existingPatient = new org.openmrs.Patient();
		existingPatient.setUuid("patient-uuid");
		PersonAttribute existingAttr = new PersonAttribute(phoneType, "+919876543210");
		existingPatient.addAttribute(existingAttr);

		Patient fhirPatient = new Patient();
		fhirPatient.setId("patient-uuid");
		fhirPatient.addExtension(new Extension(PREFIX + "phonenumber"));

		when(personAttributeTranslator.resolveType(PREFIX + "phonenumber")).thenReturn(phoneType);

		org.openmrs.Patient result = translator.toOpenmrsType(existingPatient, fhirPatient);

		for (PersonAttribute attr : result.getAttributes()) {
			if ("phoneNumber".equals(attr.getAttributeType().getName())) {
				assertTrue("Existing attribute should be voided", attr.getVoided());
			}
		}
	}

	@Test
	public void toOpenmrsType_shouldSkipUnknownExtensions() {
		org.openmrs.Patient existingPatient = new org.openmrs.Patient();
		existingPatient.setUuid("patient-uuid");

		Patient fhirPatient = new Patient();
		fhirPatient.setId("patient-uuid");
		fhirPatient.addExtension(new Extension(PREFIX + "unknown", new StringType("value")));

		when(personAttributeTranslator.resolveType(PREFIX + "unknown")).thenReturn(null);

		org.openmrs.Patient result = translator.toOpenmrsType(existingPatient, fhirPatient);

		assertTrue(result.getActiveAttributes().isEmpty());
	}

	@Test
	public void toOpenmrsType_shouldIgnoreNonPatientExtensions() {
		org.openmrs.Patient existingPatient = new org.openmrs.Patient();
		existingPatient.setUuid("patient-uuid");

		Patient fhirPatient = new Patient();
		fhirPatient.setId("patient-uuid");
		fhirPatient.addExtension(new Extension("http://fhir.bahmni.org/ext/service-request/something", new StringType("value")));

		org.openmrs.Patient result = translator.toOpenmrsType(existingPatient, fhirPatient);

		assertTrue(result.getActiveAttributes().isEmpty());
	}

	@Test
	public void toOpenmrsType_shouldVoidExistingAddressesWhenNewAddressProvided() {
		org.openmrs.Patient existingPatient = new org.openmrs.Patient();
		existingPatient.setUuid("patient-uuid");
		PersonAddress existingAddr = new PersonAddress();
		existingAddr.setCityVillage("OldCity");
		existingPatient.addAddress(existingAddr);

		Patient fhirPatient = new Patient();
		fhirPatient.setId("patient-uuid");
		fhirPatient.addAddress().setCity("NewCity").setUse(org.hl7.fhir.r4.model.Address.AddressUse.HOME);

		org.openmrs.Patient result = translator.toOpenmrsType(existingPatient, fhirPatient);

		assertTrue("Old address should be voided", existingAddr.getVoided());
	}
}
