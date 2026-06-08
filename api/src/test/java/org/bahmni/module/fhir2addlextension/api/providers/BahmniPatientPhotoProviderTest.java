package org.bahmni.module.fhir2addlextension.api.providers;

import org.bahmni.module.fhir2addlextension.api.PrivilegeConstants;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class BahmniPatientPhotoProviderTest {

	@Mock
	private UserContext userContext;

	@Mock
	private HttpServletResponse response;

	private BahmniPatientPhotoProvider provider;

	@Before
	public void setUp() {
		PowerMockito.mockStatic(Context.class);
		PowerMockito.when(Context.getUserContext()).thenReturn(userContext);

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
}
