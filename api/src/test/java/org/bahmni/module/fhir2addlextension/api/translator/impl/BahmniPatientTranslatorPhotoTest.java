package org.bahmni.module.fhir2addlextension.api.translator.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Base64BinaryType;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.openmrs.Person;
import org.openmrs.api.context.Context;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.*", "org.apache.*", "org.slf4j.*" })
@PrepareForTest(Context.class)
public class BahmniPatientTranslatorPhotoTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private BahmniPatientTranslatorImpl translator;

	@Before
	public void setup() {
		mockStatic(Context.class);
		translator = new BahmniPatientTranslatorImpl();
	}

	// --- addPhotoUrl ---

	@Test
	public void addPhotoUrl_shouldNotAddPhotoWhenUuidIsNull() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		Patient fhirPatient = new Patient();

		translator.addPhotoUrl(fhirPatient, openmrsPatient);

		assertTrue(fhirPatient.getPhoto().isEmpty());
	}

	@Test
	public void addPhotoUrl_shouldAddPhotoWhenImageExistsOnDisk() throws Exception {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		openmrsPatient.setUuid("patient-uuid-123");

		File imageFile = tempFolder.newFile("patient-uuid-123.jpeg");
		mockEmrImageService(new PersonImageStub(imageFile));

		Patient fhirPatient = new Patient();
		translator.addPhotoUrl(fhirPatient, openmrsPatient);

		assertEquals(1, fhirPatient.getPhoto().size());
		Attachment photo = fhirPatient.getPhoto().get(0);
		assertEquals(BahmniFhirConstants.PATIENT_PHOTO_CONTENT_TYPE, photo.getContentType());
		assertEquals(String.format(BahmniFhirConstants.PATIENT_PHOTO_URL_TEMPLATE, "patient-uuid-123"), photo.getUrl());
	}

	@Test
	public void addPhotoUrl_shouldNotAddPhotoWhenImageDoesNotExist() throws Exception {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		openmrsPatient.setUuid("patient-uuid-123");
		mockEmrImageService(new PersonImageStub(new File("/nonexistent/path.jpeg")));

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
		// no exception — null safely handled
	}

	@Test
	public void processPhoto_shouldNotSaveWhenBase64DataIsEmpty() {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();

		Attachment photo = new Attachment();
		photo.setContentType("image/jpeg");
		photo.setDataElement(new Base64BinaryType(""));

		Patient fhirPatient = new Patient();
		fhirPatient.setPhoto(Collections.singletonList(photo));

		translator.processPhoto(openmrsPatient, fhirPatient);
		// no exception — empty data safely handled
	}

	@Test
	public void processPhoto_shouldCallSaveWhenBase64DataPresent() throws Exception {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		openmrsPatient.setUuid("patient-uuid-123");

		SaveCapturingServiceStub serviceStub = new SaveCapturingServiceStub();
		mockSaveCapturingService(serviceStub);

		Attachment photo = new Attachment();
		photo.setContentType("image/jpeg");
		photo.setDataElement(new Base64BinaryType("aW1hZ2VkYXRh"));

		Patient fhirPatient = new Patient();
		fhirPatient.setPhoto(Collections.singletonList(photo));

		translator.processPhoto(openmrsPatient, fhirPatient);

		assertTrue("savePersonImage should have been called", serviceStub.saveCalled);
	}

	// --- deletePhoto ---

	@Test
	public void deletePhoto_shouldDeleteFileWhenExists() throws Exception {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		openmrsPatient.setUuid("patient-uuid-123");

		File imageFile = tempFolder.newFile("to-delete.jpeg");
		assertTrue(imageFile.exists());
		mockEmrImageService(new PersonImageStub(imageFile));

		translator.deletePhoto(openmrsPatient);

		assertFalse("Image file should be deleted", imageFile.exists());
	}

	@Test
	public void deletePhoto_shouldNotFailWhenFileDoesNotExist() throws Exception {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		openmrsPatient.setUuid("patient-uuid-123");
		mockEmrImageService(new PersonImageStub(new File("/nonexistent/path.jpeg")));

		translator.deletePhoto(openmrsPatient);
		// no exception thrown
	}

	@Test
	public void processPhoto_shouldDeletePhotoWhenEmptyList() throws Exception {
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		openmrsPatient.setUuid("patient-uuid-123");

		File imageFile = tempFolder.newFile("to-delete-via-process.jpeg");
		assertTrue(imageFile.exists());
		mockEmrImageService(new PersonImageStub(imageFile));

		Patient fhirPatient = new Patient();
		fhirPatient.setPhoto(new ArrayList<>());

		translator.processPhoto(openmrsPatient, fhirPatient);

		assertFalse("Image file should be deleted on empty photo list", imageFile.exists());
	}

	// --- helpers ---

	@SuppressWarnings("unchecked")
	private void mockEmrImageService(PersonImageStub personImage) throws Exception {
		EmrImageServiceStub serviceStub = new EmrImageServiceStub(personImage);
		when(Context.loadClass("org.openmrs.module.emrapi.person.image.EmrPersonImageService"))
				.thenReturn((Class) EmrImageServiceStub.class);
		when(Context.getRegisteredComponent("emrPersonImageService", EmrImageServiceStub.class))
				.thenReturn(serviceStub);
	}

	@SuppressWarnings("unchecked")
	private void mockSaveCapturingService(SaveCapturingServiceStub serviceStub) throws Exception {
		when(Context.loadClass("org.openmrs.module.emrapi.person.image.EmrPersonImageService"))
				.thenReturn((Class) SaveCapturingServiceStub.class);
		when(Context.getRegisteredComponent("emrPersonImageService", SaveCapturingServiceStub.class))
				.thenReturn(serviceStub);
		when(Context.loadClass("org.openmrs.module.emrapi.person.image.PersonImage"))
				.thenReturn((Class) SaveCapturingServiceStub.PersonImageStub.class);
	}

	/** Stub for getCurrentPersonImage reflection */
	public static class PersonImageStub {

		private final File savedImage;

		public PersonImageStub(File savedImage) {
			this.savedImage = savedImage;
		}

		public File getSavedImage() {
			return savedImage;
		}
	}

	/** Stub for EmrPersonImageService reflection */
	public static class EmrImageServiceStub {

		private final PersonImageStub personImage;

		public EmrImageServiceStub(PersonImageStub personImage) {
			this.personImage = personImage;
		}

		public PersonImageStub getCurrentPersonImage(Person person) {
			return personImage;
		}
	}

	/** Stub that captures savePersonImage calls */
	public static class SaveCapturingServiceStub {

		boolean saveCalled = false;

		public PersonImageStub savePersonImage(PersonImageStub personImage) {
			saveCalled = true;
			return personImage;
		}

		public static class PersonImageStub {

			private Person person;

			private String base64EncodedImage;

			public PersonImageStub() {
			}

			public void setPerson(Person person) {
				this.person = person;
			}

			public void setBase64EncodedImage(String base64EncodedImage) {
				this.base64EncodedImage = base64EncodedImage;
			}
		}
	}
}
