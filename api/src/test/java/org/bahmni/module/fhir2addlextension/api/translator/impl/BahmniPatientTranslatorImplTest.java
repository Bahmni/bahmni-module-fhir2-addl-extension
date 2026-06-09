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
import org.mockito.junit.MockitoJUnitRunner.Silent;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(Silent.class)
public class BahmniPatientTranslatorImplTest {

	private static final String PREFIX = "http://fhir.bahmni.org/ext/patient/";

	@Mock
	private org.bahmni.module.fhir2addlextension.api.translator.PersonAttributeExtensionTranslator personAttributeTranslator;

	private BahmniPatientTranslatorImpl translator;

	private PersonAttributeType phoneType;

	private Map<String, PersonAttributeType> slugToTypeMap;

	@Before
	public void setup() {
		translator = new BahmniPatientTranslatorImpl();
		translator.setPersonAttributeTranslator(personAttributeTranslator);

		phoneType = new PersonAttributeType();
		phoneType.setUuid("phone-uuid");
		phoneType.setName("phoneNumber");
		phoneType.setFormat("java.lang.String");

		slugToTypeMap = Collections.singletonMap("phonenumber", phoneType);
		when(personAttributeTranslator.buildSlugToTypeMap()).thenReturn(slugToTypeMap);
	}

	// --- addPersonAttributeExtensions ---

	@Test
	public void addPersonAttributeExtensions_shouldAddExtensionsForActiveAttributes() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		openmrsPatient.addAttribute(new PersonAttribute(phoneType, "+919876543210"));

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

	// --- addDateCreatedExtension ---

	@Test
	public void addDateCreatedExtension_shouldAddExtensionWhenDateExists() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		Date created = new Date();
		openmrsPatient.setDateCreated(created);

		Patient fhirPatient = new Patient();
		translator.addDateCreatedExtension(fhirPatient, openmrsPatient);

		Extension ext = fhirPatient.getExtensionByUrl(BahmniPatientTranslatorImpl.DATE_CREATED_EXT_URL);
		assertNotNull(ext);
		assertEquals(created, ((DateTimeType) ext.getValue()).getValue());
	}

	@Test
	public void addDateCreatedExtension_shouldNotAddWhenDateIsNull() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		Patient fhirPatient = new Patient();

		translator.addDateCreatedExtension(fhirPatient, openmrsPatient);

		assertNull(fhirPatient.getExtensionByUrl(BahmniPatientTranslatorImpl.DATE_CREATED_EXT_URL));
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
		translator.readBirthTime(openmrsPatient, new Patient());
		assertNull(openmrsPatient.getBirthtime());
	}

	// --- processPersonAttributeExtensions ---

	@Test
	public void processPersonAttributeExtensions_shouldUpsertAttribute() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		Patient fhirPatient = new Patient();
		fhirPatient.addExtension(new Extension(PREFIX + "phonenumber", new StringType("+91NEW")));

		when(personAttributeTranslator.resolveType(PREFIX + "phonenumber", slugToTypeMap)).thenReturn(phoneType);

		translator.processPersonAttributeExtensions(openmrsPatient, fhirPatient);

		boolean found = false;
		for (PersonAttribute attr : openmrsPatient.getAttributes()) {
			if ("phoneNumber".equals(attr.getAttributeType().getName())) {
				assertEquals("+91NEW", attr.getValue());
				found = true;
			}
		}
		assertTrue("Attribute should be added", found);
	}

	@Test
	public void processPersonAttributeExtensions_shouldUpdateExistingValue() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		PersonAttribute existingAttr = new PersonAttribute(phoneType, "+91OLD");
		openmrsPatient.addAttribute(existingAttr);

		Patient fhirPatient = new Patient();
		fhirPatient.addExtension(new Extension(PREFIX + "phonenumber", new StringType("+91NEW")));

		when(personAttributeTranslator.resolveType(PREFIX + "phonenumber", slugToTypeMap)).thenReturn(phoneType);

		translator.processPersonAttributeExtensions(openmrsPatient, fhirPatient);

		assertTrue("Old attribute should be voided", existingAttr.getVoided());
	}

	@Test
	public void processPersonAttributeExtensions_shouldVoidWithoutValue() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		PersonAttribute existingAttr = new PersonAttribute(phoneType, "+91OLD");
		openmrsPatient.addAttribute(existingAttr);

		Patient fhirPatient = new Patient();
		fhirPatient.addExtension(new Extension(PREFIX + "phonenumber"));

		when(personAttributeTranslator.resolveType(PREFIX + "phonenumber", slugToTypeMap)).thenReturn(phoneType);

		translator.processPersonAttributeExtensions(openmrsPatient, fhirPatient);

		assertTrue("Attribute should be voided", existingAttr.getVoided());
	}

	@Test
	public void processPersonAttributeExtensions_shouldSkipUnknownExtensions() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		Patient fhirPatient = new Patient();
		fhirPatient.addExtension(new Extension(PREFIX + "unknown", new StringType("val")));

		when(personAttributeTranslator.resolveType(PREFIX + "unknown", slugToTypeMap)).thenReturn(null);

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

	// --- setPreferredNameFlag ---

	@Test
	public void setPreferredNameFlag_shouldSetPreferredTrueOnFirstName() {
		org.openmrs.Patient patient = new org.openmrs.Patient();
		PersonName name = new PersonName("John", null, "Doe");
		patient.addName(name);

		translator.setPreferredNameFlag(patient);

		assertTrue(name.getPreferred());
	}

	@Test
	public void setPreferredNameFlag_shouldNotChangeIfAlreadyPreferred() {
		org.openmrs.Patient patient = new org.openmrs.Patient();
		PersonName name = new PersonName("John", null, "Doe");
		name.setPreferred(true);
		patient.addName(name);

		translator.setPreferredNameFlag(patient);

		assertTrue(name.getPreferred());
	}

	@Test
	public void setPreferredNameFlag_shouldHandlePatientWithNoNames() {
		org.openmrs.Patient patient = new org.openmrs.Patient();

		translator.setPreferredNameFlag(patient);

		assertNull(patient.getPersonName());
	}

	// --- voidExistingAddresses ---

	@Test
	public void voidExistingAddresses_shouldVoidWhenNewAddressProvided() {
		org.openmrs.Patient patient = new org.openmrs.Patient();
		PersonAddress addr = new PersonAddress();
		addr.setCityVillage("OldCity");
		patient.addAddress(addr);

		Patient fhirPatient = new Patient();
		fhirPatient.addAddress().setCity("NewCity");

		translator.voidExistingAddresses(patient, fhirPatient);

		assertTrue(addr.getVoided());
		assertEquals("Replaced via FHIR update", addr.getVoidReason());
	}

	@Test
	public void voidExistingAddresses_shouldNotVoidWhenNoNewAddress() {
		org.openmrs.Patient patient = new org.openmrs.Patient();
		PersonAddress addr = new PersonAddress();
		patient.addAddress(addr);

		translator.voidExistingAddresses(patient, new Patient());

		assertFalse(addr.getVoided());
	}

}
