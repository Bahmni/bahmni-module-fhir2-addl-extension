package org.bahmni.module.fhir2addlextension.api.providers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.File;
import java.io.FileOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.bahmni.module.fhir2addlextension.api.PrivilegeConstants;
import org.bahmni.module.fhir2addlextension.api.service.BahmniPatientPhotoService;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.api.PatientService;
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
	private PatientService patientService;

	@Mock
	private BahmniPatientPhotoService photoService;

	private BahmniPatientPhotoProvider provider;

	@Before
	public void setUp() throws Exception {
		mockStatic(Context.class);
		when(Context.getUserContext()).thenReturn(userContext);
		when(Context.getPatientService()).thenReturn(patientService);
		when(response.getOutputStream()).thenReturn(outputStream);

		provider = new BahmniPatientPhotoProvider();
		java.lang.reflect.Field field = BahmniPatientPhotoProvider.class.getDeclaredField("photoService");
		field.setAccessible(true);
		field.set(provider, photoService);
	}

	@Test
	public void getResourceType_shouldReturnPatient() {
		assertEquals(Patient.class, provider.getResourceType());
	}

	@Test(expected = ForbiddenOperationException.class)
	public void getPhoto_shouldThrowForbiddenWhenLacksPrivilege() throws Exception {
		when(userContext.hasPrivilege(PrivilegeConstants.GET_PATIENT_PHOTO)).thenReturn(false);

		provider.getPhoto(new IdType("patient-uuid"), response);
	}

	@Test(expected = ResourceNotFoundException.class)
	public void getPhoto_shouldThrowNotFoundWhenPatientNotFound() throws Exception {
		when(userContext.hasPrivilege(PrivilegeConstants.GET_PATIENT_PHOTO)).thenReturn(true);
		when(patientService.getPatientByUuid("nonexistent-uuid")).thenReturn(null);

		provider.getPhoto(new IdType("nonexistent-uuid"), response);
	}

	@Test(expected = ResourceNotFoundException.class)
	public void getPhoto_shouldThrowNotFoundWhenImageFileDoesNotExist() throws Exception {
		when(userContext.hasPrivilege(PrivilegeConstants.GET_PATIENT_PHOTO)).thenReturn(true);
		org.openmrs.Patient patient = new org.openmrs.Patient();
		when(patientService.getPatientByUuid("patient-uuid")).thenReturn(patient);
		when(photoService.getImageFile(patient)).thenReturn(new File("/nonexistent/path.jpeg"));

		provider.getPhoto(new IdType("patient-uuid"), response);
	}

	@Test
	public void getPhoto_shouldReturnImageWhenFileExists() throws Exception {
		when(userContext.hasPrivilege(PrivilegeConstants.GET_PATIENT_PHOTO)).thenReturn(true);
		org.openmrs.Patient patient = new org.openmrs.Patient();
		when(patientService.getPatientByUuid("patient-uuid")).thenReturn(patient);

		File imageFile = tempFolder.newFile("test.jpeg");
		try (FileOutputStream fos = new FileOutputStream(imageFile)) {
			fos.write(new byte[] { 1, 2, 3 });
		}
		when(photoService.getImageFile(patient)).thenReturn(imageFile);

		provider.getPhoto(new IdType("patient-uuid"), response);

		verify(response).setContentType("image/jpeg");
		verify(response).setStatus(HttpServletResponse.SC_OK);
	}

	@Test(expected = InternalErrorException.class)
	public void getPhoto_shouldThrowInternalErrorWhenExceptionOccurs() throws Exception {
		when(userContext.hasPrivilege(PrivilegeConstants.GET_PATIENT_PHOTO)).thenReturn(true);
		org.openmrs.Patient patient = new org.openmrs.Patient();
		when(patientService.getPatientByUuid("patient-uuid")).thenReturn(patient);
		when(photoService.getImageFile(patient)).thenThrow(new RuntimeException("service error"));

		provider.getPhoto(new IdType("patient-uuid"), response);
	}
}
