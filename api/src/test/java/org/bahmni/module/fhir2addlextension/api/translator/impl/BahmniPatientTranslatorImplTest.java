package org.bahmni.module.fhir2addlextension.api.translator.impl;

import org.bahmni.module.fhir2addlextension.api.context.AppContext;
import org.bahmni.module.fhir2addlextension.api.model.TelecomAttributeTypeMapping;
import org.hl7.fhir.r4.model.ContactPoint;
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
import org.openmrs.api.PersonService;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.FhirGlobalPropertyService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
	
	@Mock
	private PersonService personService;
	
	@Mock
	private FhirGlobalPropertyService globalPropertyService;
	
	@Mock
	private AppContext appContext;
	
	private BahmniPatientTranslatorImpl translator;
	
	private PersonAttributeType phoneType;
	
	private PersonAttributeType emailType;
	
	private Map<String, PersonAttributeType> slugToTypeMap;
	
	@Before
	public void setup() {
		translator = new BahmniPatientTranslatorImpl();
		translator.setPersonAttributeTranslator(personAttributeTranslator);
		translator.setPersonService(personService);
		translator.setGlobalPropertyService(globalPropertyService);
		translator.setAppContext(appContext);
		
		phoneType = new PersonAttributeType();
		phoneType.setUuid("phone-uuid");
		phoneType.setName("phoneNumber");
		phoneType.setFormat("java.lang.String");
		
		emailType = new PersonAttributeType();
		emailType.setUuid("email-uuid");
		emailType.setName("email");
		emailType.setFormat("java.lang.String");
		
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
		assertEquals("+919876543210",
		    ((StringType) fhirPatient.getExtensionByUrl(PREFIX + "phonenumber").getValue()).getValue());
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
	
	@Test
	public void addPersonAttributeExtensions_shouldSkipAttributesAlreadyInTelecom() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		PersonAttribute phoneAttribute = new PersonAttribute(phoneType, "+919876543210");
		phoneAttribute.setUuid("phone-attr-uuid");
		openmrsPatient.addAttribute(phoneAttribute);
		
		Patient fhirPatient = new Patient();
		ContactPoint existing = new ContactPoint();
		existing.setId("phone-attr-uuid");
		existing.setSystem(ContactPoint.ContactPointSystem.PHONE);
		fhirPatient.addTelecom(existing);
		
		translator.addPersonAttributeExtensions(fhirPatient, openmrsPatient);
		
		assertTrue(fhirPatient.getExtension().isEmpty());
	}
	
	// --- addAdditionalContactPoints ---
	
	@Test
	public void addAdditionalContactPoints_shouldAddContactPointForAttributeMappedToContactPoint() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		PersonAttribute emailAttribute = new PersonAttribute(emailType, "jean.claude@example.com");
		emailAttribute.setUuid("email-attr-uuid");
		openmrsPatient.addAttribute(emailAttribute);
		
		when(appContext.getTelecomAttributeTypeMappings()).thenReturn(
		    Collections.singletonList(new TelecomAttributeTypeMapping(emailType.getUuid(),
		            ContactPoint.ContactPointSystem.EMAIL, null, null)));
		
		Patient fhirPatient = new Patient();
		translator.addAdditionalContactPoints(fhirPatient, openmrsPatient);
		
		assertEquals(1, fhirPatient.getTelecom().size());
		assertEquals(ContactPoint.ContactPointSystem.EMAIL, fhirPatient.getTelecomFirstRep().getSystem());
		assertEquals("jean.claude@example.com", fhirPatient.getTelecomFirstRep().getValue());
	}
	
	@Test
	public void addAdditionalContactPoints_shouldNotDuplicateAttributeAlreadyInTelecom() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		PersonAttribute phoneAttribute = new PersonAttribute(phoneType, "+919876543210");
		phoneAttribute.setUuid("phone-attr-uuid");
		openmrsPatient.addAttribute(phoneAttribute);
		
		when(appContext.getTelecomAttributeTypeMappings()).thenReturn(Collections.emptyList());
		
		Patient fhirPatient = new Patient();
		ContactPoint existing = new ContactPoint();
		existing.setId("phone-attr-uuid");
		existing.setSystem(ContactPoint.ContactPointSystem.PHONE);
		existing.setValue("+919876543210");
		fhirPatient.addTelecom(existing);
		
		translator.addAdditionalContactPoints(fhirPatient, openmrsPatient);
		
		assertEquals(1, fhirPatient.getTelecom().size());
	}
	
	@Test
	public void addAdditionalContactPoints_shouldSkipAttributesWithNoContactPointMapping() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		openmrsPatient.addAttribute(new PersonAttribute(phoneType, "some non-contact attribute"));
		
		when(appContext.getTelecomAttributeTypeMappings()).thenReturn(Collections.emptyList());
		
		Patient fhirPatient = new Patient();
		translator.addAdditionalContactPoints(fhirPatient, openmrsPatient);
		
		assertTrue(fhirPatient.getTelecom().isEmpty());
	}
	
	// --- resolvePersonAttributeTypeForContactPoint / processContactPoints ---
	
	@Test
	public void resolvePersonAttributeTypeForContactPoint_shouldResolveBySystem() {
		ContactPoint contactPoint = new ContactPoint();
		contactPoint.setSystem(ContactPoint.ContactPointSystem.EMAIL);
		
		when(appContext.getTelecomAttributeTypeMappings()).thenReturn(
		    Collections.singletonList(new TelecomAttributeTypeMapping(emailType.getUuid(),
		            ContactPoint.ContactPointSystem.EMAIL, null, null)));
		when(personService.getPersonAttributeTypeByUuid(emailType.getUuid())).thenReturn(emailType);
		
		PersonAttributeType result = translator.resolvePersonAttributeTypeForContactPoint(contactPoint);
		
		assertEquals(emailType, result);
	}
	
	@Test
	public void resolvePersonAttributeTypeForContactPoint_shouldFallBackToConfiguredTypeWhenNoSystem() {
		ContactPoint contactPoint = new ContactPoint();
		
		when(globalPropertyService.getGlobalProperty(FhirConstants.PERSON_CONTACT_POINT_ATTRIBUTE_TYPE)).thenReturn(
		    "phone-uuid");
		when(personService.getPersonAttributeTypeByUuid("phone-uuid")).thenReturn(phoneType);
		
		PersonAttributeType result = translator.resolvePersonAttributeTypeForContactPoint(contactPoint);
		
		assertEquals(phoneType, result);
	}
	
	@Test
	public void resolvePersonAttributeTypeForContactPoint_shouldFallBackToConfiguredTypeWhenSystemUnmapped() {
		ContactPoint contactPoint = new ContactPoint();
		contactPoint.setSystem(ContactPoint.ContactPointSystem.FAX);
		
		when(appContext.getTelecomAttributeTypeMappings()).thenReturn(Collections.emptyList());
		when(globalPropertyService.getGlobalProperty(FhirConstants.PERSON_CONTACT_POINT_ATTRIBUTE_TYPE)).thenReturn(
		    "phone-uuid");
		when(personService.getPersonAttributeTypeByUuid("phone-uuid")).thenReturn(phoneType);
		
		PersonAttributeType result = translator.resolvePersonAttributeTypeForContactPoint(contactPoint);
		
		assertEquals(phoneType, result);
	}
	
	@Test
	public void processContactPoints_shouldWriteEachContactPointToItsOwnAttributeType() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();

		ContactPoint phoneContactPoint = new ContactPoint();
		phoneContactPoint.setSystem(ContactPoint.ContactPointSystem.PHONE);
		phoneContactPoint.setValue("+919876543210");

		ContactPoint emailContactPoint = new ContactPoint();
		emailContactPoint.setSystem(ContactPoint.ContactPointSystem.EMAIL);
		emailContactPoint.setValue("jean.claude@example.com");

		when(appContext.getTelecomAttributeTypeMappings()).thenReturn(Arrays.asList(
		    new TelecomAttributeTypeMapping(phoneType.getUuid(), ContactPoint.ContactPointSystem.PHONE, null, null),
		    new TelecomAttributeTypeMapping(emailType.getUuid(), ContactPoint.ContactPointSystem.EMAIL, null, null)));
		when(personService.getPersonAttributeTypeByUuid(phoneType.getUuid())).thenReturn(phoneType);
		when(personService.getPersonAttributeTypeByUuid(emailType.getUuid())).thenReturn(emailType);

		translator.processContactPoints(openmrsPatient, Arrays.asList(phoneContactPoint, emailContactPoint));

		List<PersonAttribute> attributes = new java.util.ArrayList<>(openmrsPatient.getActiveAttributes());
		assertEquals(2, attributes.size());
		assertTrue(attributes.stream()
		        .anyMatch(a -> a.getAttributeType().equals(phoneType) && "+919876543210".equals(a.getValue())));
		assertTrue(attributes.stream()
		        .anyMatch(a -> a.getAttributeType().equals(emailType) && "jean.claude@example.com".equals(a.getValue())));
	}
	
	@Test
	public void processContactPoints_shouldSkipContactPointWhenAttributeTypeCannotBeResolved() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		
		ContactPoint contactPoint = new ContactPoint();
		contactPoint.setSystem(ContactPoint.ContactPointSystem.FAX);
		contactPoint.setValue("some fax number");
		
		when(appContext.getTelecomAttributeTypeMappings()).thenReturn(Collections.emptyList());
		when(globalPropertyService.getGlobalProperty(FhirConstants.PERSON_CONTACT_POINT_ATTRIBUTE_TYPE)).thenReturn(null);
		
		translator.processContactPoints(openmrsPatient, Collections.singletonList(contactPoint));
		
		assertTrue(openmrsPatient.getActiveAttributes().isEmpty());
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
		fhirPatient.getBirthDateElement().addExtension(BahmniPatientTranslatorImpl.BIRTH_TIME_EXT_URL,
		    new DateTimeType(expectedTime));
		
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
