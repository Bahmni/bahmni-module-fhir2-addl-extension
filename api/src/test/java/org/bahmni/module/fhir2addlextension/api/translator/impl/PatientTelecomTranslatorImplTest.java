package org.bahmni.module.fhir2addlextension.api.translator.impl;

import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.hl7.fhir.r4.model.ContactPoint;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner.Silent;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.PersonService;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(Silent.class)
public class PatientTelecomTranslatorImplTest {
	
	@Mock
	private PersonService personService;
	
	@Mock
	private AdministrationService administrationService;
	
	private PatientTelecomTranslatorImpl translator;
	
	private PersonAttributeType phoneType;
	
	private PersonAttributeType emailType;
	
	@Before
	public void setup() {
		translator = new PatientTelecomTranslatorImpl(personService, administrationService);
		
		phoneType = new PersonAttributeType();
		phoneType.setUuid("phone-uuid");
		phoneType.setName("phoneNumber");
		phoneType.setFormat("java.lang.String");
		
		emailType = new PersonAttributeType();
		emailType.setUuid("email-uuid");
		emailType.setName("email");
		emailType.setFormat("java.lang.String");
	}
	
	// --- AC1: Phone only in telecom on read ---
	
	@Test
	public void getContactPoints_shouldReturnPhoneContactPointForPhoneAttribute() {
		when(administrationService.getGlobalProperty(BahmniFhirConstants.GP_PATIENT_CONTACT_TELECOM_MAP)).thenReturn(
		    "phoneNumber:phone");
		
		org.openmrs.Patient patient = new org.openmrs.Patient();
		patient.addAttribute(new PersonAttribute(phoneType, "+919876543210"));
		
		List<ContactPoint> result = translator.getContactPoints(patient);
		
		assertEquals(1, result.size());
		assertEquals(ContactPoint.ContactPointSystem.PHONE, result.get(0).getSystem());
		assertEquals("+919876543210", result.get(0).getValue());
	}
	
	// --- AC2: Email only in telecom on read ---
	
	@Test
	public void getContactPoints_shouldReturnEmailContactPointForEmailAttribute() {
		when(administrationService.getGlobalProperty(BahmniFhirConstants.GP_PATIENT_CONTACT_TELECOM_MAP)).thenReturn(
		    "email:email");
		
		org.openmrs.Patient patient = new org.openmrs.Patient();
		patient.addAttribute(new PersonAttribute(emailType, "patient@example.com"));
		
		List<ContactPoint> result = translator.getContactPoints(patient);
		
		assertEquals(1, result.size());
		assertEquals(ContactPoint.ContactPointSystem.EMAIL, result.get(0).getSystem());
		assertEquals("patient@example.com", result.get(0).getValue());
	}
	
	// --- AC3: Multiple contacts in telecom on read ---
	
	@Test
	public void getContactPoints_shouldReturnBothPhoneAndEmailContactPoints() {
		when(administrationService.getGlobalProperty(BahmniFhirConstants.GP_PATIENT_CONTACT_TELECOM_MAP))
		        .thenReturn("phoneNumber:phone;email:email");

		org.openmrs.Patient patient = new org.openmrs.Patient();
		patient.addAttribute(new PersonAttribute(phoneType, "+919876543210"));
		patient.addAttribute(new PersonAttribute(emailType, "patient@example.com"));

		List<ContactPoint> result = translator.getContactPoints(patient);

		assertEquals(2, result.size());
		boolean hasPhone = result.stream().anyMatch(cp -> cp.getSystem() == ContactPoint.ContactPointSystem.PHONE
		        && "+919876543210".equals(cp.getValue()));
		boolean hasEmail = result.stream().anyMatch(cp -> cp.getSystem() == ContactPoint.ContactPointSystem.EMAIL
		        && "patient@example.com".equals(cp.getValue()));
		assertTrue("Should have phone contact point", hasPhone);
		assertTrue("Should have email contact point", hasEmail);
	}
	
	// --- AC4: No telecom when no contacts / empty mapping ---
	
	@Test
	public void getContactPoints_shouldReturnEmptyListWhenMappingIsEmpty() {
		when(administrationService.getGlobalProperty(BahmniFhirConstants.GP_PATIENT_CONTACT_TELECOM_MAP)).thenReturn("");
		
		org.openmrs.Patient patient = new org.openmrs.Patient();
		patient.addAttribute(new PersonAttribute(phoneType, "+919876543210"));
		
		List<ContactPoint> result = translator.getContactPoints(patient);
		
		assertTrue("Should return empty list when no mapping configured", result.isEmpty());
	}
	
	@Test
	public void getContactPoints_shouldReturnEmptyListWhenMappingIsNull() {
		when(administrationService.getGlobalProperty(BahmniFhirConstants.GP_PATIENT_CONTACT_TELECOM_MAP)).thenReturn(null);
		
		org.openmrs.Patient patient = new org.openmrs.Patient();
		patient.addAttribute(new PersonAttribute(phoneType, "+919876543210"));
		
		List<ContactPoint> result = translator.getContactPoints(patient);
		
		assertTrue("Should return empty list when mapping is null", result.isEmpty());
	}
	
	@Test
	public void getContactPoints_shouldReturnEmptyListWhenPatientHasNoMatchingAttributes() {
		when(administrationService.getGlobalProperty(BahmniFhirConstants.GP_PATIENT_CONTACT_TELECOM_MAP)).thenReturn(
		    "phoneNumber:phone");
		
		org.openmrs.Patient patient = new org.openmrs.Patient();
		// No attributes added
		
		List<ContactPoint> result = translator.getContactPoints(patient);
		
		assertTrue("Should return empty list when no matching attributes", result.isEmpty());
	}
	
	@Test
	public void getContactPoints_shouldSkipAttributesWithNullOrEmptyValue() {
		when(administrationService.getGlobalProperty(BahmniFhirConstants.GP_PATIENT_CONTACT_TELECOM_MAP)).thenReturn(
		    "phoneNumber:phone");
		
		org.openmrs.Patient patient = new org.openmrs.Patient();
		patient.addAttribute(new PersonAttribute(phoneType, null));
		patient.addAttribute(new PersonAttribute(phoneType, ""));
		
		List<ContactPoint> result = translator.getContactPoints(patient);
		
		assertTrue("Should return empty list when attribute value is null or empty", result.isEmpty());
	}
	
	// --- AC5: Telecom write creates phone attribute ---
	
	@Test
	public void updateAttributes_shouldCreatePhoneAttributeFromContactPoint() {
		when(administrationService.getGlobalProperty(BahmniFhirConstants.GP_PATIENT_CONTACT_TELECOM_MAP)).thenReturn(
		    "phoneNumber:phone");
		when(personService.getPersonAttributeTypeByName("phoneNumber")).thenReturn(phoneType);
		
		org.openmrs.Patient patient = new org.openmrs.Patient();
		ContactPoint cp = new ContactPoint();
		cp.setSystem(ContactPoint.ContactPointSystem.PHONE);
		cp.setValue("+919876543210");
		
		translator.updateAttributes(patient, java.util.Collections.singletonList(cp));
		
		boolean found = false;
		for (PersonAttribute attr : patient.getAttributes()) {
			if (!attr.getVoided() && "phoneNumber".equals(attr.getAttributeType().getName())
			        && "+919876543210".equals(attr.getValue())) {
				found = true;
			}
		}
		assertTrue("Phone attribute should be created", found);
	}
	
	// --- AC6: Telecom write creates email attribute ---
	
	@Test
	public void updateAttributes_shouldCreateEmailAttributeFromContactPoint() {
		when(administrationService.getGlobalProperty(BahmniFhirConstants.GP_PATIENT_CONTACT_TELECOM_MAP)).thenReturn(
		    "email:email");
		when(personService.getPersonAttributeTypeByName("email")).thenReturn(emailType);
		
		org.openmrs.Patient patient = new org.openmrs.Patient();
		ContactPoint cp = new ContactPoint();
		cp.setSystem(ContactPoint.ContactPointSystem.EMAIL);
		cp.setValue("patient@example.com");
		
		translator.updateAttributes(patient, java.util.Collections.singletonList(cp));
		
		boolean found = false;
		for (PersonAttribute attr : patient.getAttributes()) {
			if (!attr.getVoided() && "email".equals(attr.getAttributeType().getName())
			        && "patient@example.com".equals(attr.getValue())) {
				found = true;
			}
		}
		assertTrue("Email attribute should be created", found);
	}
	
	@Test
	public void updateAttributes_shouldVoidExistingAttributeBeforeCreatingNew() {
		when(administrationService.getGlobalProperty(BahmniFhirConstants.GP_PATIENT_CONTACT_TELECOM_MAP)).thenReturn(
		    "phoneNumber:phone");
		when(personService.getPersonAttributeTypeByName("phoneNumber")).thenReturn(phoneType);
		
		org.openmrs.Patient patient = new org.openmrs.Patient();
		PersonAttribute existing = new PersonAttribute(phoneType, "+91OLD");
		patient.addAttribute(existing);
		
		ContactPoint cp = new ContactPoint();
		cp.setSystem(ContactPoint.ContactPointSystem.PHONE);
		cp.setValue("+91NEW");
		
		translator.updateAttributes(patient, java.util.Collections.singletonList(cp));
		
		assertTrue("Old attribute should be voided", existing.getVoided());
		assertEquals("Updated via FHIR", existing.getVoidReason());
		
		boolean newFound = false;
		for (PersonAttribute attr : patient.getAttributes()) {
			if (!attr.getVoided() && "+91NEW".equals(attr.getValue())) {
				newFound = true;
			}
		}
		assertTrue("New attribute should be created", newFound);
	}
	
	@Test
	public void updateAttributes_shouldDoNothingWhenTelecomListIsNull() {
		when(administrationService.getGlobalProperty(BahmniFhirConstants.GP_PATIENT_CONTACT_TELECOM_MAP)).thenReturn(
		    "phoneNumber:phone");
		
		org.openmrs.Patient patient = new org.openmrs.Patient();
		patient.addAttribute(new PersonAttribute(phoneType, "+919876543210"));
		
		translator.updateAttributes(patient, null);
		
		// No attributes should be voided
		for (PersonAttribute attr : patient.getAttributes()) {
			assertFalse("Attribute should not be voided", attr.getVoided());
		}
	}
	
	@Test
	public void updateAttributes_shouldSkipContactPointWithUnknownSystem() {
		when(administrationService.getGlobalProperty(BahmniFhirConstants.GP_PATIENT_CONTACT_TELECOM_MAP)).thenReturn(
		    "phoneNumber:phone");
		
		org.openmrs.Patient patient = new org.openmrs.Patient();
		
		ContactPoint cp = new ContactPoint();
		cp.setSystem(ContactPoint.ContactPointSystem.EMAIL); // not in mapping
		cp.setValue("patient@example.com");
		
		translator.updateAttributes(patient, java.util.Collections.singletonList(cp));
		
		assertTrue("No attributes should be added for unmapped system", patient.getAttributes().isEmpty());
	}
	
	@Test
	public void updateAttributes_shouldWarnAndSkipWhenAttributeTypeNotFound() {
		when(administrationService.getGlobalProperty(BahmniFhirConstants.GP_PATIENT_CONTACT_TELECOM_MAP)).thenReturn(
		    "phoneNumber:phone");
		when(personService.getPersonAttributeTypeByName("phoneNumber")).thenReturn(null);
		
		org.openmrs.Patient patient = new org.openmrs.Patient();
		
		ContactPoint cp = new ContactPoint();
		cp.setSystem(ContactPoint.ContactPointSystem.PHONE);
		cp.setValue("+919876543210");
		
		// Should not throw; attribute not found is a warning
		translator.updateAttributes(patient, java.util.Collections.singletonList(cp));
		
		assertTrue("No attributes should be added when type not found", patient.getAttributes().isEmpty());
	}
	
	// --- AC8: Configurable attribute type mapping ---
	
	@Test
	public void getContactPoints_shouldSupportCustomAttributeToSystemMapping() {
		when(administrationService.getGlobalProperty(BahmniFhirConstants.GP_PATIENT_CONTACT_TELECOM_MAP)).thenReturn(
		    "mobilePhone:sms");
		
		PersonAttributeType mobileType = new PersonAttributeType();
		mobileType.setUuid("mobile-uuid");
		mobileType.setName("mobilePhone");
		mobileType.setFormat("java.lang.String");
		
		org.openmrs.Patient patient = new org.openmrs.Patient();
		patient.addAttribute(new PersonAttribute(mobileType, "+919876543210"));
		
		List<ContactPoint> result = translator.getContactPoints(patient);
		
		assertEquals(1, result.size());
		assertEquals(ContactPoint.ContactPointSystem.SMS, result.get(0).getSystem());
		assertEquals("+919876543210", result.get(0).getValue());
	}
	
	// --- AC9: Backward compatibility — existing attributes surface once mapping present ---
	
	@Test
	public void getContactPoints_shouldReturnExistingAttributesWithoutMigration() {
		// When a mapping is configured for an attribute type that already has data, it should be returned
		when(administrationService.getGlobalProperty(BahmniFhirConstants.GP_PATIENT_CONTACT_TELECOM_MAP)).thenReturn(
		    "phoneNumber:phone");
		
		org.openmrs.Patient patient = new org.openmrs.Patient();
		// Existing data from before feature was deployed
		patient.addAttribute(new PersonAttribute(phoneType, "+919876543210"));
		
		List<ContactPoint> result = translator.getContactPoints(patient);
		
		assertEquals("Existing contact attribute should appear in telecom once mapping is configured", 1, result.size());
		assertEquals(ContactPoint.ContactPointSystem.PHONE, result.get(0).getSystem());
	}
	
	// --- Malformed/invalid mapping entries skipped ---
	
	@Test
	public void parseMapping_shouldSkipMalformedEntries() {
		when(administrationService.getGlobalProperty(BahmniFhirConstants.GP_PATIENT_CONTACT_TELECOM_MAP)).thenReturn(
		    "phoneNumber:phone;badEntry;:email;phoneNumber:");
		
		org.openmrs.Patient patient = new org.openmrs.Patient();
		patient.addAttribute(new PersonAttribute(phoneType, "+919876543210"));
		
		List<ContactPoint> result = translator.getContactPoints(patient);
		
		// Only "phoneNumber:phone" is valid
		assertEquals(1, result.size());
		assertEquals(ContactPoint.ContactPointSystem.PHONE, result.get(0).getSystem());
	}
	
	@Test
	public void parseMapping_shouldSkipInvalidSystemCode() {
		when(administrationService.getGlobalProperty(BahmniFhirConstants.GP_PATIENT_CONTACT_TELECOM_MAP)).thenReturn(
		    "phoneNumber:notASystem;email:email");
		
		org.openmrs.Patient patient = new org.openmrs.Patient();
		patient.addAttribute(new PersonAttribute(emailType, "patient@example.com"));
		
		List<ContactPoint> result = translator.getContactPoints(patient);
		
		// Only "email:email" is valid
		assertEquals(1, result.size());
		assertEquals(ContactPoint.ContactPointSystem.EMAIL, result.get(0).getSystem());
	}
	
	@Test
	public void parseMapping_shouldTrimWhitespaceAroundNamesAndSystems() {
		when(administrationService.getGlobalProperty(BahmniFhirConstants.GP_PATIENT_CONTACT_TELECOM_MAP)).thenReturn(
		    "  phoneNumber : phone ;  email : email  ");
		
		org.openmrs.Patient patient = new org.openmrs.Patient();
		patient.addAttribute(new PersonAttribute(phoneType, "+919876543210"));
		
		List<ContactPoint> result = translator.getContactPoints(patient);
		
		assertEquals(1, result.size());
		assertEquals(ContactPoint.ContactPointSystem.PHONE, result.get(0).getSystem());
	}
	
	// --- getMappedAttributeTypeNames ---
	
	@Test
	public void getMappedAttributeTypeNames_shouldReturnSetOfConfiguredNames() {
		when(administrationService.getGlobalProperty(BahmniFhirConstants.GP_PATIENT_CONTACT_TELECOM_MAP)).thenReturn(
		    "phoneNumber:phone;email:email");
		
		Set<String> names = translator.getMappedAttributeTypeNames();
		
		assertTrue("Should contain phoneNumber", names.contains("phoneNumber"));
		assertTrue("Should contain email", names.contains("email"));
		assertEquals(2, names.size());
	}
	
	@Test
	public void getMappedAttributeTypeNames_shouldReturnEmptySetWhenNoMapping() {
		when(administrationService.getGlobalProperty(BahmniFhirConstants.GP_PATIENT_CONTACT_TELECOM_MAP)).thenReturn(null);
		
		Set<String> names = translator.getMappedAttributeTypeNames();
		
		assertTrue("Should return empty set when no mapping", names.isEmpty());
	}
}
