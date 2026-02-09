package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.translator.AppointmentStatusTranslator;
import org.hl7.fhir.r4.model.Appointment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.appointments.model.AppointmentProvider;
import org.openmrs.module.appointments.model.AppointmentProviderResponse;
import org.openmrs.module.appointments.model.AppointmentServiceDefinition;
import org.openmrs.module.appointments.model.AppointmentStatus;
import org.openmrs.module.fhir2.api.translators.LocationReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;
import org.openmrs.Provider;

import java.util.Date;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirAppointmentTranslatorImplTest {
	
	@Mock
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Mock
	private PractitionerReferenceTranslator<Provider> practitionerReferenceTranslator;
	
	@Mock
	private LocationReferenceTranslator locationReferenceTranslator;
	
	@Mock
	private AppointmentStatusTranslator appointmentStatusTranslator;
	
	private BahmniFhirAppointmentTranslatorImpl translator;
	
	@Before
	public void setUp() {
		translator = new BahmniFhirAppointmentTranslatorImpl(patientReferenceTranslator, practitionerReferenceTranslator,
		        locationReferenceTranslator, appointmentStatusTranslator);
	}
	
	@Test
	public void shouldMapBasicAppointmentFields() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment = new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("test-uuid-123");
		bahmniAppointment.setStatus(AppointmentStatus.Scheduled);
		bahmniAppointment.setStartDateTime(new Date(1000000));
		bahmniAppointment.setEndDateTime(new Date(2000000));
		bahmniAppointment.setComments("Patient has flu symptoms");
		bahmniAppointment.setAppointmentNumber("APT-2026-001");
		
		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled)).thenReturn(
		    Appointment.AppointmentStatus.BOOKED);
		
		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);
		
		assertEquals("test-uuid-123", fhirAppointment.getId());
		assertEquals(Appointment.AppointmentStatus.BOOKED, fhirAppointment.getStatus());
		assertEquals(new Date(1000000), fhirAppointment.getStart());
		assertEquals(new Date(2000000), fhirAppointment.getEnd());
		assertEquals("Patient has flu symptoms", fhirAppointment.getComment());
		assertEquals("APT-2026-001", fhirAppointment.getIdentifierFirstRep().getValue());
	}
	
	@Test
	public void shouldMapServiceType() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment = new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("test-uuid");
		bahmniAppointment.setStatus(AppointmentStatus.Scheduled);
		bahmniAppointment.setStartDateTime(new Date());
		bahmniAppointment.setEndDateTime(new Date());
		
		AppointmentServiceDefinition service = new AppointmentServiceDefinition();
		service.setName("General Consultation");
		bahmniAppointment.setService(service);
		
		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled)).thenReturn(
		    Appointment.AppointmentStatus.BOOKED);
		
		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);
		
		assertEquals(1, fhirAppointment.getServiceType().size());
		assertEquals("General Consultation", fhirAppointment.getServiceType().get(0).getText());
		assertEquals("General Consultation", fhirAppointment.getServiceType().get(0).getCoding().get(0).getCode());
	}
	
	@Test
	public void shouldHandleNullService() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment = new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("test-uuid");
		bahmniAppointment.setStatus(AppointmentStatus.Scheduled);
		bahmniAppointment.setStartDateTime(new Date());
		bahmniAppointment.setEndDateTime(new Date());
		bahmniAppointment.setService(null);
		
		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled)).thenReturn(
		    Appointment.AppointmentStatus.BOOKED);
		
		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);
		
		assertEquals(0, fhirAppointment.getServiceType().size());
	}
	
	@Test
	public void shouldHandleNullComments() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment = new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("test-uuid");
		bahmniAppointment.setStatus(AppointmentStatus.Scheduled);
		bahmniAppointment.setStartDateTime(new Date());
		bahmniAppointment.setEndDateTime(new Date());
		bahmniAppointment.setComments(null);
		
		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled)).thenReturn(
		    Appointment.AppointmentStatus.BOOKED);
		
		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);
		
		assertEquals(null, fhirAppointment.getComment());
	}
	
	@Test
	public void shouldHandleNullAppointmentNumber() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment = new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("test-uuid");
		bahmniAppointment.setStatus(AppointmentStatus.Scheduled);
		bahmniAppointment.setStartDateTime(new Date());
		bahmniAppointment.setEndDateTime(new Date());
		bahmniAppointment.setAppointmentNumber(null);
		
		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled)).thenReturn(
		    Appointment.AppointmentStatus.BOOKED);
		
		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);
		
		assertEquals(0, fhirAppointment.getIdentifier().size());
	}
	
	@Test
	public void shouldMapCompletedStatus() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment = new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("test-uuid");
		bahmniAppointment.setStatus(AppointmentStatus.Completed);
		bahmniAppointment.setStartDateTime(new Date());
		bahmniAppointment.setEndDateTime(new Date());
		
		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Completed)).thenReturn(
		    Appointment.AppointmentStatus.FULFILLED);
		
		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);
		
		assertEquals(Appointment.AppointmentStatus.FULFILLED, fhirAppointment.getStatus());
	}
	
	@Test
	public void shouldMapCancelledStatus() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment = new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("test-uuid");
		bahmniAppointment.setStatus(AppointmentStatus.Cancelled);
		bahmniAppointment.setStartDateTime(new Date());
		bahmniAppointment.setEndDateTime(new Date());
		
		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Cancelled)).thenReturn(
		    Appointment.AppointmentStatus.CANCELLED);
		
		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);
		
		assertEquals(Appointment.AppointmentStatus.CANCELLED, fhirAppointment.getStatus());
	}
	
	@Test
	public void shouldThrowExceptionOnToOpenmrsType() {
		try {
			translator.toOpenmrsType(new Appointment());
		}
		catch (UnsupportedOperationException e) {
			assertEquals("Appointment resource is read-only via FHIR API", e.getMessage());
		}
	}
	
	@Test
	public void shouldMapMultipleFieldsWithServiceAndNumber() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment = new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("APT-UUID-456");
		bahmniAppointment.setStatus(AppointmentStatus.Scheduled);
		bahmniAppointment.setStartDateTime(new Date(1609459200000L));
		bahmniAppointment.setEndDateTime(new Date(1609462800000L));
		bahmniAppointment.setComments("Follow-up consultation");
		bahmniAppointment.setAppointmentNumber("APT-2026-00456");
		
		AppointmentServiceDefinition service = new AppointmentServiceDefinition();
		service.setName("Cardiology");
		bahmniAppointment.setService(service);
		
		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled)).thenReturn(
		    Appointment.AppointmentStatus.BOOKED);
		
		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);
		
		assertNotNull("FHIR appointment should be created", fhirAppointment);
		assertEquals("APT-UUID-456", fhirAppointment.getId());
		assertEquals(Appointment.AppointmentStatus.BOOKED, fhirAppointment.getStatus());
		assertEquals("Follow-up consultation", fhirAppointment.getComment());
		assertEquals("APT-2026-00456", fhirAppointment.getIdentifierFirstRep().getValue());
		assertEquals("Cardiology", fhirAppointment.getServiceType().get(0).getText());
	}
	
	@Test
	public void shouldHandleEmptyProvidersSet() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment =
			new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("test-uuid");
		bahmniAppointment.setStatus(AppointmentStatus.Scheduled);
		bahmniAppointment.setStartDateTime(new Date());
		bahmniAppointment.setEndDateTime(new Date());
		bahmniAppointment.setProviders(new HashSet<>());

		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled))
			.thenReturn(Appointment.AppointmentStatus.BOOKED);

		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);

		assertNotNull("FHIR appointment should be created", fhirAppointment);
		assertEquals(0, fhirAppointment.getParticipant().stream()
			.filter(p -> p.getActor() != null && "Practitioner".equals(p.getActor().getType()))
			.count());
	}
}
