package org.bahmni.module.fhir2addlextension.api.translator.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;

import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2addlextension.api.service.BahmniPatientPhotoService;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Base64BinaryType;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BahmniPatientTranslatorPhotoTest {

	@Mock
	private BahmniPatientPhotoService photoService;

	private BahmniPatientTranslatorImpl translator;

	@Before
	public void setup() {
		translator = new BahmniPatientTranslatorImpl();
		translator.setPhotoService(photoService);
	}

	// --- addPhotoUrl ---

	@Test
	public void addPhotoUrl_shouldNotAddPhotoWhenUuidIsNull() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		openmrsPatient.setUuid(null);
		Patient fhirPatient = new Patient();

		translator.addPhotoUrl(fhirPatient, openmrsPatient);

		assertTrue(fhirPatient.getPhoto().isEmpty());
	}

	@Test
	public void addPhotoUrl_shouldAddPhotoWhenImageExistsOnDisk() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		openmrsPatient.setUuid("patient-uuid-123");
		when(photoService.hasPhoto(openmrsPatient)).thenReturn(true);

		Patient fhirPatient = new Patient();
		translator.addPhotoUrl(fhirPatient, openmrsPatient);

		assertEquals(1, fhirPatient.getPhoto().size());
		Attachment photo = fhirPatient.getPhoto().get(0);
		assertEquals(BahmniFhirConstants.PATIENT_PHOTO_CONTENT_TYPE, photo.getContentType());
		assertEquals(String.format(BahmniFhirConstants.PATIENT_PHOTO_URL_TEMPLATE, "patient-uuid-123"), photo.getUrl());
	}

	@Test
	public void addPhotoUrl_shouldNotAddPhotoWhenImageDoesNotExist() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		openmrsPatient.setUuid("patient-uuid-123");
		when(photoService.hasPhoto(openmrsPatient)).thenReturn(false);

		Patient fhirPatient = new Patient();
		translator.addPhotoUrl(fhirPatient, openmrsPatient);

		assertTrue(fhirPatient.getPhoto().isEmpty());
	}

	// --- processPhoto ---

	@Test
	public void processPhoto_shouldNotProcessWhenPhotoListIsNull() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		Patient fhirPatient = new Patient();
		fhirPatient.setPhoto(null);

		translator.processPhoto(openmrsPatient, fhirPatient);

		verifyNoInteractions(photoService);
	}

	@Test
	public void processPhoto_shouldNotSaveWhenBase64DataIsEmpty() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();

		Attachment photo = new Attachment();
		photo.setDataElement(new Base64BinaryType(""));

		Patient fhirPatient = new Patient();
		fhirPatient.setPhoto(Collections.singletonList(photo));

		translator.processPhoto(openmrsPatient, fhirPatient);

		verifyNoInteractions(photoService);
	}

	@Test
	public void processPhoto_shouldNotSaveWhenUuidIsNull() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		openmrsPatient.setUuid(null);

		Attachment photo = new Attachment();
		photo.setDataElement(new Base64BinaryType("aW1hZ2VkYXRh"));

		Patient fhirPatient = new Patient();
		fhirPatient.setPhoto(Collections.singletonList(photo));

		translator.processPhoto(openmrsPatient, fhirPatient);

		verify(photoService, never()).savePhoto(any(), anyString());
	}

	@Test
	public void processPhoto_shouldCallSaveWhenBase64DataPresent() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		openmrsPatient.setUuid("patient-uuid-123");

		Attachment photo = new Attachment();
		photo.setDataElement(new Base64BinaryType("aW1hZ2VkYXRh"));

		Patient fhirPatient = new Patient();
		fhirPatient.setPhoto(Collections.singletonList(photo));

		translator.processPhoto(openmrsPatient, fhirPatient);

		verify(photoService).savePhoto(openmrsPatient, "aW1hZ2VkYXRh");
	}

	@Test
	public void processPhoto_shouldDeletePhotoWhenEmptyList() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();

		Patient fhirPatient = new Patient();
		fhirPatient.setPhoto(new ArrayList<>());

		translator.processPhoto(openmrsPatient, fhirPatient);

		verify(photoService).deletePhoto(openmrsPatient);
	}
}
