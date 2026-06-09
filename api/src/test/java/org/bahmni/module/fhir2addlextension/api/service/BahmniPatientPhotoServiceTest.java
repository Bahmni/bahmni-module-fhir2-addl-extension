package org.bahmni.module.fhir2addlextension.api.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.File;

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
public class BahmniPatientPhotoServiceTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private BahmniPatientPhotoService photoService;

	@Before
	public void setup() throws Exception {
		mockStatic(Context.class);
		photoService = new BahmniPatientPhotoService();
	}

	// --- hasPhoto ---

	@Test
	public void hasPhoto_shouldReturnTrueWhenFileExists() throws Exception {
		org.openmrs.Patient patient = new org.openmrs.Patient();
		File imageFile = tempFolder.newFile("photo.jpeg");
		mockEmrImageService(new PersonImageStub(imageFile));

		assertTrue(photoService.hasPhoto(patient));
	}

	@Test
	public void hasPhoto_shouldReturnFalseWhenFileDoesNotExist() throws Exception {
		org.openmrs.Patient patient = new org.openmrs.Patient();
		mockEmrImageService(new PersonImageStub(new File("/nonexistent/path.jpeg")));

		assertFalse(photoService.hasPhoto(patient));
	}

	@Test
	public void hasPhoto_shouldReturnFalseWhenExceptionOccurs() throws Exception {
		org.openmrs.Patient patient = new org.openmrs.Patient();
		when(Context.loadClass("org.openmrs.module.emrapi.person.image.EmrPersonImageService"))
				.thenThrow(new ClassNotFoundException("not found"));

		assertFalse(photoService.hasPhoto(patient));
	}

	// --- savePhoto ---

	@Test
	public void savePhoto_shouldCallSavePersonImage() throws Exception {
		org.openmrs.Patient patient = new org.openmrs.Patient();
		patient.setUuid("test-uuid");

		SaveCapturingServiceStub serviceStub = new SaveCapturingServiceStub();
		mockSaveService(serviceStub);

		photoService.savePhoto(patient, "aW1hZ2VkYXRh");

		assertTrue("savePersonImage should have been called", serviceStub.saveCalled);
	}

	@Test
	public void savePhoto_shouldNotThrowWhenExceptionOccurs() throws Exception {
		org.openmrs.Patient patient = new org.openmrs.Patient();
		when(Context.loadClass("org.openmrs.module.emrapi.person.image.EmrPersonImageService"))
				.thenThrow(new ClassNotFoundException("not found"));

		photoService.savePhoto(patient, "aW1hZ2VkYXRh");
		// no exception — error logged
	}

	// --- deletePhoto ---

	@Test
	public void deletePhoto_shouldDeleteFileWhenExists() throws Exception {
		org.openmrs.Patient patient = new org.openmrs.Patient();
		patient.setUuid("test-uuid");

		File imageFile = tempFolder.newFile("to-delete.jpeg");
		assertTrue(imageFile.exists());
		mockEmrImageService(new PersonImageStub(imageFile));

		photoService.deletePhoto(patient);

		assertFalse("File should be deleted", imageFile.exists());
	}

	@Test
	public void deletePhoto_shouldNotFailWhenFileDoesNotExist() throws Exception {
		org.openmrs.Patient patient = new org.openmrs.Patient();
		mockEmrImageService(new PersonImageStub(new File("/nonexistent/path.jpeg")));

		photoService.deletePhoto(patient);
		// no exception
	}

	@Test
	public void deletePhoto_shouldNotThrowWhenExceptionOccurs() throws Exception {
		org.openmrs.Patient patient = new org.openmrs.Patient();
		when(Context.loadClass("org.openmrs.module.emrapi.person.image.EmrPersonImageService"))
				.thenThrow(new ClassNotFoundException("not found"));

		photoService.deletePhoto(patient);
		// no exception — error logged
	}

	// --- getImageFile ---

	@Test
	public void getImageFile_shouldReturnFileFromService() throws Exception {
		org.openmrs.Patient patient = new org.openmrs.Patient();
		File expected = tempFolder.newFile("image.jpeg");
		mockEmrImageService(new PersonImageStub(expected));

		File result = photoService.getImageFile(patient);

		assertEquals(expected, result);
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
	private void mockSaveService(SaveCapturingServiceStub serviceStub) throws Exception {
		when(Context.loadClass("org.openmrs.module.emrapi.person.image.EmrPersonImageService"))
				.thenReturn((Class) SaveCapturingServiceStub.class);
		when(Context.getRegisteredComponent("emrPersonImageService", SaveCapturingServiceStub.class))
				.thenReturn(serviceStub);
		when(Context.loadClass("org.openmrs.module.emrapi.person.image.PersonImage"))
				.thenReturn((Class) SaveCapturingServiceStub.PersonImageStub.class);
	}

	public static class PersonImageStub {

		private final File savedImage;

		public PersonImageStub(File savedImage) {
			this.savedImage = savedImage;
		}

		public File getSavedImage() {
			return savedImage;
		}
	}

	public static class EmrImageServiceStub {

		private final PersonImageStub personImage;

		public EmrImageServiceStub(PersonImageStub personImage) {
			this.personImage = personImage;
		}

		public PersonImageStub getCurrentPersonImage(Person person) {
			return personImage;
		}
	}

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
