package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.translator.AppointmentStatusTranslator;
import org.hl7.fhir.r4.model.Appointment;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.appointments.model.AppointmentStatus;

import static org.junit.Assert.assertEquals;

public class AppointmentStatusTranslatorImplTest {
	
	private AppointmentStatusTranslator appointmentStatusTranslator;
	
	@Before
	public void setUp() {
		appointmentStatusTranslator = new AppointmentStatusTranslatorImpl();
	}
	
	@Test
	public void shouldTranslateScheduledToBooked() {
		Appointment.AppointmentStatus result = appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled);
		assertEquals(Appointment.AppointmentStatus.BOOKED, result);
	}
	
	@Test
	public void shouldTranslateCompletedToFulfilled() {
		Appointment.AppointmentStatus result = appointmentStatusTranslator.toFhirResource(AppointmentStatus.Completed);
		assertEquals(Appointment.AppointmentStatus.FULFILLED, result);
	}
	
	@Test
	public void shouldTranslateCancelledToCancelled() {
		Appointment.AppointmentStatus result = appointmentStatusTranslator.toFhirResource(AppointmentStatus.Cancelled);
		assertEquals(Appointment.AppointmentStatus.CANCELLED, result);
	}
	
	@Test
	public void shouldTranslateMissedToNoshow() {
		Appointment.AppointmentStatus result = appointmentStatusTranslator.toFhirResource(AppointmentStatus.Missed);
		assertEquals(Appointment.AppointmentStatus.NOSHOW, result);
	}
	
	@Test
	public void shouldTranslateCheckedInToCheckedin() {
		Appointment.AppointmentStatus result = appointmentStatusTranslator.toFhirResource(AppointmentStatus.CheckedIn);
		assertEquals(Appointment.AppointmentStatus.CHECKEDIN, result);
	}
	
	@Test
	public void shouldTranslateRequestedToPending() {
		Appointment.AppointmentStatus result = appointmentStatusTranslator.toFhirResource(AppointmentStatus.Requested);
		assertEquals(Appointment.AppointmentStatus.PENDING, result);
	}
	
	@Test
	public void shouldReturnPendingForNullStatus() {
		Appointment.AppointmentStatus result = appointmentStatusTranslator.toFhirResource(null);
		assertEquals(Appointment.AppointmentStatus.PENDING, result);
	}
	
	// Reverse mapping tests
	@Test
	public void shouldTranslateFhirBookedToScheduled() {
		AppointmentStatus result = appointmentStatusTranslator.toOpenmrsType(Appointment.AppointmentStatus.BOOKED);
		assertEquals(AppointmentStatus.Scheduled, result);
	}
	
	@Test
	public void shouldTranslateFhirFulfilledToCompleted() {
		AppointmentStatus result = appointmentStatusTranslator.toOpenmrsType(Appointment.AppointmentStatus.FULFILLED);
		assertEquals(AppointmentStatus.Completed, result);
	}
	
	@Test
	public void shouldTranslateFhirCancelledToCancelled() {
		AppointmentStatus result = appointmentStatusTranslator.toOpenmrsType(Appointment.AppointmentStatus.CANCELLED);
		assertEquals(AppointmentStatus.Cancelled, result);
	}
	
	@Test
	public void shouldTranslateFhirNoshowToMissed() {
		AppointmentStatus result = appointmentStatusTranslator.toOpenmrsType(Appointment.AppointmentStatus.NOSHOW);
		assertEquals(AppointmentStatus.Missed, result);
	}
	
	@Test
	public void shouldReturnRequestedForNullFhirStatus() {
		AppointmentStatus result = appointmentStatusTranslator.toOpenmrsType(null);
		assertEquals(AppointmentStatus.Requested, result);
	}
}
