package org.bahmni.module.fhir2addlextension.api.providers;

import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniAppointmentSearchParams;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirAppointmentService;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.IdType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ca.uhn.fhir.rest.api.server.IBundleProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniAppointmentFhirR4ResourceProviderTest {
	
	@Mock
	private BahmniFhirAppointmentService appointmentService;
	
	@Mock
	private IBundleProvider bundleProvider;
	
	@Captor
	private ArgumentCaptor<BahmniAppointmentSearchParams> searchParamsCaptor;
	
	private BahmniAppointmentFhirR4ResourceProvider resourceProvider;
	
	@Before
	public void setUp() {
		resourceProvider = new BahmniAppointmentFhirR4ResourceProvider(appointmentService);
	}
	
	@Test
	public void testGetResourceType() {
		assertEquals(Appointment.class, resourceProvider.getResourceType());
	}
	
	@Test
	public void testGetAppointmentByUuidWithValidId() {
		String appointmentId = "test-uuid-123";
		Appointment expectedAppointment = new Appointment();
		expectedAppointment.setId(appointmentId);
		
		when(appointmentService.get(appointmentId)).thenReturn(expectedAppointment);
		
		Appointment result = resourceProvider.getAppointmentByUuid(new IdType(appointmentId));
		
		assertNotNull(result);
		assertEquals(appointmentId, result.getId());
		verify(appointmentService).get(appointmentId);
	}
	
	@Test
	public void testGetAppointmentByUuidWithInvalidId() {
		String appointmentId = "invalid-uuid";

		when(appointmentService.get(appointmentId)).thenReturn(null);

		assertThrows(ResourceNotFoundException.class, () -> {
			resourceProvider.getAppointmentByUuid(new IdType(appointmentId));
		});
	}
	
	@Test
	public void testSearchAppointmentsWithPatientReference() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam();
		TokenAndListParam status = null;
		DateRangeParam date = null;
		TokenAndListParam id = null;
		DateRangeParam lastUpdated = null;
		SortSpec sort = null;
		
		when(appointmentService.searchAppointments(any(BahmniAppointmentSearchParams.class))).thenReturn(bundleProvider);
		
		IBundleProvider result = resourceProvider.searchAppointments(patientReference, status, date, id, lastUpdated, sort);
		
		assertNotNull(result);
		assertEquals(bundleProvider, result);
		verify(appointmentService).searchAppointments(searchParamsCaptor.capture());
		
		BahmniAppointmentSearchParams capturedParams = searchParamsCaptor.getValue();
		assertNotNull(capturedParams);
	}
	
	@Test
	public void testSearchAppointmentsWithStatus() {
		ReferenceAndListParam patientReference = null;
		TokenAndListParam status = new TokenAndListParam();
		DateRangeParam date = null;
		TokenAndListParam id = null;
		DateRangeParam lastUpdated = null;
		SortSpec sort = null;
		
		when(appointmentService.searchAppointments(any(BahmniAppointmentSearchParams.class))).thenReturn(bundleProvider);
		
		IBundleProvider result = resourceProvider.searchAppointments(patientReference, status, date, id, lastUpdated, sort);
		
		assertNotNull(result);
		verify(appointmentService).searchAppointments(searchParamsCaptor.capture());
		
		BahmniAppointmentSearchParams capturedParams = searchParamsCaptor.getValue();
		assertNotNull(capturedParams);
	}
	
	@Test
	public void testSearchAppointmentsWithDateRange() {
		ReferenceAndListParam patientReference = null;
		TokenAndListParam status = null;
		DateRangeParam date = new DateRangeParam();
		TokenAndListParam id = null;
		DateRangeParam lastUpdated = null;
		SortSpec sort = null;
		
		when(appointmentService.searchAppointments(any(BahmniAppointmentSearchParams.class))).thenReturn(bundleProvider);
		
		IBundleProvider result = resourceProvider.searchAppointments(patientReference, status, date, id, lastUpdated, sort);
		
		assertNotNull(result);
		verify(appointmentService).searchAppointments(searchParamsCaptor.capture());
		
		BahmniAppointmentSearchParams capturedParams = searchParamsCaptor.getValue();
		assertNotNull(capturedParams);
	}
	
	@Test
	public void testSearchAppointmentsWithAllParameters() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam();
		TokenAndListParam status = new TokenAndListParam();
		DateRangeParam date = new DateRangeParam();
		TokenAndListParam id = new TokenAndListParam();
		DateRangeParam lastUpdated = new DateRangeParam();
		SortSpec sort = new SortSpec("date");
		
		when(appointmentService.searchAppointments(any(BahmniAppointmentSearchParams.class))).thenReturn(bundleProvider);
		
		IBundleProvider result = resourceProvider.searchAppointments(patientReference, status, date, id, lastUpdated, sort);
		
		assertNotNull(result);
		verify(appointmentService).searchAppointments(searchParamsCaptor.capture());
		
		BahmniAppointmentSearchParams capturedParams = searchParamsCaptor.getValue();
		assertNotNull(capturedParams);
	}
}
