package org.bahmni.module.fhir2addlextension.api.translator.impl;

import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
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

import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
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

	// --- addPersonAttributeExtensions ---

	@Test
	public void addPersonAttributeExtensions_shouldAddExtensionsForActiveAttributes() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		PersonAttribute attr = new PersonAttribute(phoneType, "+919876543210");
		openmrsPatient.addAttribute(attr);

		Extension mockExt = new Extension(PREFIX + "phonenumber", new StringType("+919876543210"));
		when(personAttributeTranslator.toFhirResource(any(PersonAttribute.class))).thenReturn(mockExt);

		Patient fhirPatient = new Patient();
		translator.addPersonAttributeExtensions(fhirPatient, openmrsPatient);

		assertEquals(1, fhirPatient.getExtension().size());
		assertEquals("+919876543210", ((StringType) fhirPatient.getExtensionByUrl(PREFIX + "phonenumber").getValue()).getValue());
	}

	@Test
	public void addPersonAttributeExtensions_shouldSkipNullExtensions() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		openmrsPatient.addAttribute(new PersonAttribute(phoneType, null));

		when(personAttributeTranslator.toFhirResource(any(PersonAttribute.class))).thenReturn(null);

		Patient fhirPatient = new Patient();
		translator.addPersonAttributeExtensions(fhirPatient, openmrsPatient);

		assertTrue(fhirPatient.getExtension().isEmpty());
	}

	// --- addBirthTimeExtension ---

	@Test
	public void addBirthTimeExtension_shouldAddExtensionWhenBirthTimeExists() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		Date birthtime = new Date();
		openmrsPatient.setBirthtime(birthtime);

		Patient fhirPatient = new Patient();
		fhirPatient.setBirthDateElement(new DateType("1990-01-15"));

		translator.addBirthTimeExtension(fhirPatient, openmrsPatient);

		Extension ext = fhirPatient.getBirthDateElement().getExtensionByUrl(BahmniPatientTranslatorImpl.BIRTH_TIME_EXT_URL);
		assertNotNull(ext);
		assertEquals(birthtime, ((DateTimeType) ext.getValue()).getValue());
	}

	@Test
	public void addBirthTimeExtension_shouldNotAddWhenBirthTimeIsNull() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();

		Patient fhirPatient = new Patient();
		fhirPatient.setBirthDateElement(new DateType("1990-01-15"));

		translator.addBirthTimeExtension(fhirPatient, openmrsPatient);

		assertNull(fhirPatient.getBirthDateElement().getExtensionByUrl(BahmniPatientTranslatorImpl.BIRTH_TIME_EXT_URL));
	}

	// --- readBirthTime ---

	@Test
	public void readBirthTime_shouldSetBirthTimeFromExtension() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		Date expectedTime = new Date();

		Patient fhirPatient = new Patient();
		fhirPatient.setBirthDateElement(new DateType("1990-01-15"));
		fhirPatient.getBirthDateElement().addExtension(BahmniPatientTranslatorImpl.BIRTH_TIME_EXT_URL, new DateTimeType(expectedTime));

		translator.readBirthTime(openmrsPatient, fhirPatient);

		assertNotNull(openmrsPatient.getBirthtime());
		assertEquals(expectedTime, openmrsPatient.getBirthtime());
	}

	@Test
	public void readBirthTime_shouldNotSetWhenNoExtension() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();

		Patient fhirPatient = new Patient();
		fhirPatient.setBirthDateElement(new DateType("1990-01-15"));

		translator.readBirthTime(openmrsPatient, fhirPatient);

		assertNull(openmrsPatient.getBirthtime());
	}

	@Test
	public void readBirthTime_shouldNotSetWhenNoBirthDateElement() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		Patient fhirPatient = new Patient();

		translator.readBirthTime(openmrsPatient, fhirPatient);

		assertNull(openmrsPatient.getBirthtime());
	}

	// --- processPersonAttributeExtensions ---

	@Test
	public void processPersonAttributeExtensions_shouldUpsertAttribute() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();

		Patient fhirPatient = new Patient();
		fhirPatient.addExtension(new Extension(PREFIX + "phonenumber", new StringType("+919876543210")));

		when(personAttributeTranslator.resolveType(PREFIX + "phonenumber")).thenReturn(phoneType);

		translator.processPersonAttributeExtensions(openmrsPatient, fhirPatient);

		boolean found = false;
		for (PersonAttribute attr : openmrsPatient.getAttributes()) {
			if ("phoneNumber".equals(attr.getAttributeType().getName())) {
				assertEquals("+919876543210", attr.getValue());
				found = true;
			}
		}
		assertTrue("Attribute should be added", found);
	}

	@Test
	public void processPersonAttributeExtensions_shouldUpdateExistingValue() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		PersonAttribute existingAttr = new PersonAttribute(phoneType, "+91-OLD");
		openmrsPatient.addAttribute(existingAttr);

		Patient fhirPatient = new Patient();
		fhirPatient.addExtension(new Extension(PREFIX + "phonenumber", new StringType("+91-NEW")));

		when(personAttributeTranslator.resolveType(PREFIX + "phonenumber")).thenReturn(phoneType);

		translator.processPersonAttributeExtensions(openmrsPatient, fhirPatient);

		assertTrue("Old attribute should be voided", existingAttr.getVoided());
		boolean foundNew = false;
		for (PersonAttribute attr : openmrsPatient.getAttributes()) {
			if (!attr.getVoided() && "phoneNumber".equals(attr.getAttributeType().getName())) {
				assertEquals("+91-NEW", attr.getValue());
				foundNew = true;
			}
		}
		assertTrue("New value should exist", foundNew);
	}

	@Test
	public void processPersonAttributeExtensions_shouldVoidWithoutValue() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		PersonAttribute existingAttr = new PersonAttribute(phoneType, "+919876543210");
		openmrsPatient.addAttribute(existingAttr);

		Patient fhirPatient = new Patient();
		fhirPatient.addExtension(new Extension(PREFIX + "phonenumber"));

		when(personAttributeTranslator.resolveType(PREFIX + "phonenumber")).thenReturn(phoneType);

		translator.processPersonAttributeExtensions(openmrsPatient, fhirPatient);

		assertTrue("Attribute should be voided", existingAttr.getVoided());
	}

	@Test
	public void processPersonAttributeExtensions_shouldSkipUnknownExtensions() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();

		Patient fhirPatient = new Patient();
		fhirPatient.addExtension(new Extension(PREFIX + "unknown", new StringType("value")));

		when(personAttributeTranslator.resolveType(PREFIX + "unknown")).thenReturn(null);

		translator.processPersonAttributeExtensions(openmrsPatient, fhirPatient);

		assertTrue(openmrsPatient.getActiveAttributes().isEmpty());
	}

	@Test
	public void processPersonAttributeExtensions_shouldIgnoreNonPatientExtensions() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();

		Patient fhirPatient = new Patient();
		fhirPatient.addExtension(new Extension("http://fhir.bahmni.org/ext/service-request/x", new StringType("v")));

		translator.processPersonAttributeExtensions(openmrsPatient, fhirPatient);

		assertTrue(openmrsPatient.getActiveAttributes().isEmpty());
	}

	// --- voidExistingAddresses ---

	@Test
	public void voidExistingAddresses_shouldVoidWhenNewAddressProvided() {
		org.openmrs.Patient existingPatient = new org.openmrs.Patient();
		PersonAddress existingAddr = new PersonAddress();
		existingAddr.setCityVillage("OldCity");
		existingPatient.addAddress(existingAddr);

		Patient fhirPatient = new Patient();
		fhirPatient.addAddress().setCity("NewCity");

		translator.voidExistingAddresses(existingPatient, fhirPatient);

		assertTrue("Old address should be voided", existingAddr.getVoided());
		assertEquals("Replaced via FHIR update", existingAddr.getVoidReason());
	}

	@Test
	public void voidExistingAddresses_shouldNotVoidWhenNoNewAddress() {
		org.openmrs.Patient existingPatient = new org.openmrs.Patient();
		PersonAddress existingAddr = new PersonAddress();
		existingAddr.setCityVillage("OldCity");
		existingPatient.addAddress(existingAddr);

		Patient fhirPatient = new Patient();

		translator.voidExistingAddresses(existingPatient, fhirPatient);

		assertFalse("Address should not be voided", existingAddr.getVoided());
	}
}
