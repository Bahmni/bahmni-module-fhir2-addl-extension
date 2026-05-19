package org.bahmni.module.fhir2addlextension.api.translator.impl;

import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniPatientTranslatorImplTest {
	
	private static final String PREFIX = "http://fhir.bahmni.org/ext/patient/";
	
	@Mock
	private PersonAttributeExtensionTranslator personAttributeTranslator;
	
	@InjectMocks
	private BahmniPatientTranslatorImpl translator;
	
	private PersonAttributeType phoneType;
	
	@Before
	public void setup() {
		phoneType = new PersonAttributeType();
		phoneType.setUuid("phone-uuid");
		phoneType.setName("phoneNumber");
		phoneType.setFormat("java.lang.String");
	}
	
	@Test
	public void toFhirResource_shouldAddPersonAttributeExtensions() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		openmrsPatient.setUuid("patient-uuid");
		openmrsPatient.setGender("M");
		
		PersonAttribute attr = new PersonAttribute(phoneType, "+919876543210");
		openmrsPatient.addAttribute(attr);
		
		Extension mockExt = new Extension(PREFIX + "phonenumber", new StringType("+919876543210"));
		when(personAttributeTranslator.toFhirResource(any(PersonAttribute.class))).thenReturn(mockExt);
		
		Patient fhirPatient = translator.toFhirResource(openmrsPatient);
		
		assertFalse(fhirPatient.getExtension().isEmpty());
		Extension ext = fhirPatient.getExtensionByUrl(PREFIX + "phonenumber");
		assertNotNull(ext);
		assertEquals("+919876543210", ((StringType) ext.getValue()).getValue());
	}
	
	@Test
	public void toFhirResource_shouldSkipNullExtensions() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		openmrsPatient.setUuid("patient-uuid");
		openmrsPatient.setGender("M");
		
		PersonAttribute attr = new PersonAttribute(phoneType, null);
		openmrsPatient.addAttribute(attr);
		
		when(personAttributeTranslator.toFhirResource(any(PersonAttribute.class))).thenReturn(null);
		
		Patient fhirPatient = translator.toFhirResource(openmrsPatient);
		
		assertTrue(fhirPatient.getExtensionsByUrl(PREFIX + "phonenumber").isEmpty());
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
		fhirPatient.addExtension(new Extension("http://fhir.bahmni.org/ext/service-request/something", new StringType(
		        "value")));
		
		org.openmrs.Patient result = translator.toOpenmrsType(existingPatient, fhirPatient);
		
		assertTrue(result.getActiveAttributes().isEmpty());
	}
}
