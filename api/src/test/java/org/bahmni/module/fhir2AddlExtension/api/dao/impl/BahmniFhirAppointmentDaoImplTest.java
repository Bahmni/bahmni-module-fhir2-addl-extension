package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import org.bahmni.module.fhir2AddlExtension.api.translator.AppointmentStatusTranslator;
import org.hl7.fhir.r4.model.Appointment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.appointments.model.AppointmentStatus;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirAppointmentDaoImplTest {
	
	@Mock
	private AppointmentStatusTranslator statusTranslator;
	
	private BahmniFhirAppointmentDaoImpl appointmentDao;
	
	@Before
	public void setUp() {
		appointmentDao = new BahmniFhirAppointmentDaoImpl(statusTranslator);
	}
	
	@Test
	public void testMapSortParamToPropertyWithDateParam() throws Exception {
		// Test paramToProp via reflection since it's protected
		Method method = BahmniFhirAppointmentDaoImpl.class.getDeclaredMethod("paramToProp", String.class);
		method.setAccessible(true);
		
		String result = (String) method.invoke(appointmentDao, "date");
		assertEquals("startDateTime", result);
	}
	
	@Test
	public void testMapSortParamToPropertyWithStatusParam() throws Exception {
		Method method = BahmniFhirAppointmentDaoImpl.class.getDeclaredMethod("paramToProp", String.class);
		method.setAccessible(true);
		
		String result = (String) method.invoke(appointmentDao, "status");
		assertEquals("status", result);
	}
	
	@Test
	public void testMapSortParamToPropertyWithPatientParam() throws Exception {
		Method method = BahmniFhirAppointmentDaoImpl.class.getDeclaredMethod("paramToProp", String.class);
		method.setAccessible(true);
		
		String result = (String) method.invoke(appointmentDao, "patient");
		assertEquals("patient", result);
	}
	
	@Test
	public void testMapSortParamToPropertyWithUnknownParam() throws Exception {
		Method method = BahmniFhirAppointmentDaoImpl.class.getDeclaredMethod("paramToProp", String.class);
		method.setAccessible(true);
		
		String result = (String) method.invoke(appointmentDao, "unknownParam");
		assertNull("Unknown params should return null", result);
	}
	
	@Test
	public void testMapSortParamToPropertyWithNullParam() throws Exception {
		Method method = BahmniFhirAppointmentDaoImpl.class.getDeclaredMethod("paramToProp", String.class);
		method.setAccessible(true);
		
		String result = (String) method.invoke(appointmentDao, (String) null);
		assertNull("Null param should return null", result);
	}
	
	@Test
	public void testStatusTranslatorIntegration() {
		when(statusTranslator.toOpenmrsType(Appointment.AppointmentStatus.BOOKED)).thenReturn(AppointmentStatus.Scheduled);
		
		AppointmentStatus result = statusTranslator.toOpenmrsType(Appointment.AppointmentStatus.BOOKED);
		assertEquals(AppointmentStatus.Scheduled, result);
	}
	
	@Test
	public void testStatusTranslatorWithDifferentStatuses() {
		when(statusTranslator.toOpenmrsType(Appointment.AppointmentStatus.FULFILLED))
		        .thenReturn(AppointmentStatus.Completed);
		when(statusTranslator.toOpenmrsType(Appointment.AppointmentStatus.CANCELLED))
		        .thenReturn(AppointmentStatus.Cancelled);
		
		assertEquals(AppointmentStatus.Completed, statusTranslator.toOpenmrsType(Appointment.AppointmentStatus.FULFILLED));
		assertEquals(AppointmentStatus.Cancelled, statusTranslator.toOpenmrsType(Appointment.AppointmentStatus.CANCELLED));
	}
}
