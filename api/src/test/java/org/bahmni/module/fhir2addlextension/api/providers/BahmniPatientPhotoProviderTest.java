package org.bahmni.module.fhir2addlextension.api.providers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.File;
import java.io.FileOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.bahmni.module.fhir2addlextension.api.PrivilegeConstants;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.Person;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.*", "org.apache.*", "org.slf4j.*" })
@PrepareForTest(Context.class)
public class BahmniPatientPhotoProviderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Mock
	private UserContext userContext;

	@Mock
	private HttpServletResponse response;

	@Mock
	private ServletOutputStream outputStream;

	@Mock
	private PersonService personService;

	private BahmniPatientPhotoProvider provider;

	@Before
	public void setUp() throws Exception {
		mockStatic(Context.class);
		when(Context.getUserContext()).thenReturn(userContext);
		when(Context.getPersonService()).thenReturn(personService);
		when(response.getOutputStream()).thenReturn(outputStream);

		provider = new BahmniPatientPhotoProvider();
	}

	@Test
	public void getResourceType_shouldReturnPatient() {
		assertEquals(Patient.class, provider.getResourceType());
	}

	@Test
	public void getPhoto_shouldReturnForbiddenWhenLacksPrivilege() throws Exception {
		when(userContext.hasPrivilege(PrivilegeConstants.GET_PATIENT_PHOTO)).thenReturn(false);

		provider.getPhoto(new IdType("patient-uuid"), response);

		verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
	}

	@Test
	public void getPhoto_shouldReturnNotFoundWhenPersonNotFound() throws Exception {
		when(userContext.hasPrivilege(PrivilegeConstants.GET_PATIENT_PHOTO)).thenReturn(true);
		when(personService.getPersonByUuid("nonexistent-uuid")).thenReturn(null);
		mockEmrImageService(new PersonImageStub(null));

		provider.getPhoto(new IdType("nonexistent-uuid"), response);

		verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
	}

	@Test
	public void getPhoto_shouldReturnNotFoundWhenImageFileDoesNotExist() throws Exception {
		when(userContext.hasPrivilege(PrivilegeConstants.GET_PATIENT_PHOTO)).thenReturn(true);
		when(personService.getPersonByUuid("patient-uuid")).thenReturn(new Person());
		mockEmrImageService(new PersonImageStub(new File("/nonexistent/path.jpeg")));

		provider.getPhoto(new IdType("patient-uuid"), response);

		verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
	}

	@Test
	public void getPhoto_shouldReturnImageWhenFileExists() throws Exception {
		when(userContext.hasPrivilege(PrivilegeConstants.GET_PATIENT_PHOTO)).thenReturn(true);
		when(personService.getPersonByUuid("patient-uuid")).thenReturn(new Person());

		File imageFile = tempFolder.newFile("test.jpeg");
		try (FileOutputStream fos = new FileOutputStream(imageFile)) {
			fos.write(new byte[] { 1, 2, 3 });
		}
		mockEmrImageService(new PersonImageStub(imageFile));

		provider.getPhoto(new IdType("patient-uuid"), response);

		verify(response).setContentType("image/jpeg");
		verify(response).setStatus(HttpServletResponse.SC_OK);
	}

	@Test
	public void getPhoto_shouldReturn500WhenExceptionOccurs() throws Exception {
		when(userContext.hasPrivilege(PrivilegeConstants.GET_PATIENT_PHOTO)).thenReturn(true);
		when(Context.loadClass("org.openmrs.module.emrapi.person.image.EmrPersonImageService"))
				.thenThrow(new ClassNotFoundException("not found"));

		provider.getPhoto(new IdType("patient-uuid"), response);

		verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}

	@SuppressWarnings("unchecked")
	private void mockEmrImageService(PersonImageStub personImage) throws Exception {
		EmrImageServiceStub serviceStub = new EmrImageServiceStub(personImage);
		when(Context.loadClass("org.openmrs.module.emrapi.person.image.EmrPersonImageService"))
				.thenReturn((Class) EmrImageServiceStub.class);
		when(Context.getRegisteredComponent("emrPersonImageService", EmrImageServiceStub.class))
				.thenReturn(serviceStub);
	}

	/** Stub to simulate emrapi PersonImage — provides getSavedImage() for reflection calls */
	public static class PersonImageStub {

		private final File savedImage;

		public PersonImageStub(File savedImage) {
			this.savedImage = savedImage;
		}

		public File getSavedImage() {
			return savedImage;
		}
	}

	/** Stub to simulate EmrPersonImageService — provides getCurrentPersonImage() for reflection calls */
	public static class EmrImageServiceStub {

		private final PersonImageStub personImage;

		public EmrImageServiceStub(PersonImageStub personImage) {
			this.personImage = personImage;
		}

		public PersonImageStub getCurrentPersonImage(Person person) {
			return personImage;
		}
	}
}
