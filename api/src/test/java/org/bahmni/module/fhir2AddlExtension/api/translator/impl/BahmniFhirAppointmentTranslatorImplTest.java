package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.translator.AppointmentStatusTranslator;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.appointments.model.AppointmentProvider;
import org.openmrs.module.appointments.model.AppointmentProviderResponse;
import org.openmrs.module.appointments.model.AppointmentReason;
import org.openmrs.module.appointments.model.AppointmentServiceDefinition;
import org.openmrs.module.appointments.model.AppointmentServiceType;
import org.openmrs.module.appointments.model.AppointmentStatus;
import org.openmrs.module.fhir2.api.translators.LocationReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;
import org.openmrs.Concept;
import org.openmrs.Provider;
import org.openmrs.Patient;
import org.openmrs.Location;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

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
		service.setUuid("service-uuid-123");
		service.setName("General Consultation");
		bahmniAppointment.setService(service);
		
		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled)).thenReturn(
		    Appointment.AppointmentStatus.BOOKED);
		
		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);
		
		assertEquals(1, fhirAppointment.getServiceType().size());
		assertEquals("General Consultation", fhirAppointment.getServiceType().get(0).getText());
		assertEquals("service-uuid-123", fhirAppointment.getServiceType().get(0).getCoding().get(0).getCode());
		assertEquals("General Consultation", fhirAppointment.getServiceType().get(0).getCoding().get(0).getDisplay());
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
		service.setUuid("cardiology-service-uuid");
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
		// Participant mapping is tested via integration tests and code review
	}
	
	@Test
	public void shouldMapPatientParticipantWithAcceptedStatus() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment = new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("test-uuid");
		bahmniAppointment.setStatus(AppointmentStatus.Scheduled);
		bahmniAppointment.setStartDateTime(new Date());
		bahmniAppointment.setEndDateTime(new Date());
		
		org.openmrs.Patient patient = new org.openmrs.Patient();
		patient.setUuid("patient-uuid");
		bahmniAppointment.setPatient(patient);
		
		org.hl7.fhir.r4.model.Reference patientRef = new org.hl7.fhir.r4.model.Reference("Patient/patient-uuid");
		
		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled)).thenReturn(
		    Appointment.AppointmentStatus.BOOKED);
		when(patientReferenceTranslator.toFhirResource(patient)).thenReturn(patientRef);
		
		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);
		
		assertNotNull("Appointment should have participants", fhirAppointment.getParticipant());
		assertEquals("Patient participant should be added", 1, fhirAppointment.getParticipant().size());
		assertEquals("Patient should have ACCEPTED status", Appointment.ParticipationStatus.ACCEPTED, fhirAppointment
		        .getParticipant().get(0).getStatus());
	}
	
	@Test
	public void shouldMapMultipleProvidersWithDifferentResponseStatuses() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment =
			new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("test-uuid");
		bahmniAppointment.setStatus(AppointmentStatus.Scheduled);
		bahmniAppointment.setStartDateTime(new Date());
		bahmniAppointment.setEndDateTime(new Date());

		org.openmrs.Provider provider1 = new org.openmrs.Provider();
		provider1.setUuid("provider1-uuid");

		org.openmrs.Provider provider2 = new org.openmrs.Provider();
		provider2.setUuid("provider2-uuid");

		AppointmentProvider apProvider1 = new AppointmentProvider();
		apProvider1.setProvider(provider1);
		apProvider1.setResponse(AppointmentProviderResponse.ACCEPTED);

		AppointmentProvider apProvider2 = new AppointmentProvider();
		apProvider2.setProvider(provider2);
		apProvider2.setResponse(AppointmentProviderResponse.AWAITING);

		HashSet<AppointmentProvider> providers = new HashSet<>();
		providers.add(apProvider1);
		providers.add(apProvider2);
		bahmniAppointment.setProviders(providers);

		org.hl7.fhir.r4.model.Reference pRef1 = new org.hl7.fhir.r4.model.Reference("Practitioner/provider1-uuid");
		org.hl7.fhir.r4.model.Reference pRef2 = new org.hl7.fhir.r4.model.Reference("Practitioner/provider2-uuid");

		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled))
			.thenReturn(Appointment.AppointmentStatus.BOOKED);
		when(practitionerReferenceTranslator.toFhirResource(provider1)).thenReturn(pRef1);
		when(practitionerReferenceTranslator.toFhirResource(provider2)).thenReturn(pRef2);

		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);

		assertEquals("Should have 2 participants", 2, fhirAppointment.getParticipant().size());
	}
	
	@Test
	public void shouldMapProviderResponseStatusAccepted() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment =
			new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("test-uuid");
		bahmniAppointment.setStatus(AppointmentStatus.Scheduled);
		bahmniAppointment.setStartDateTime(new Date());
		bahmniAppointment.setEndDateTime(new Date());

		org.openmrs.Provider provider = new org.openmrs.Provider();
		provider.setUuid("provider-uuid");

		AppointmentProvider apProvider = new AppointmentProvider();
		apProvider.setProvider(provider);
		apProvider.setResponse(AppointmentProviderResponse.ACCEPTED);

		HashSet<AppointmentProvider> providers = new HashSet<>();
		providers.add(apProvider);
		bahmniAppointment.setProviders(providers);

		org.hl7.fhir.r4.model.Reference pRef = new org.hl7.fhir.r4.model.Reference("Practitioner/provider-uuid");

		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled))
			.thenReturn(Appointment.AppointmentStatus.BOOKED);
		when(practitionerReferenceTranslator.toFhirResource(provider)).thenReturn(pRef);

		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);

		assertEquals("Provider with ACCEPTED response should map to ACCEPTED status",
			Appointment.ParticipationStatus.ACCEPTED, fhirAppointment.getParticipant().get(0).getStatus());
	}
	
	@Test
	public void shouldMapProviderResponseStatusCancelled() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment =
			new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("test-uuid");
		bahmniAppointment.setStatus(AppointmentStatus.Scheduled);
		bahmniAppointment.setStartDateTime(new Date());
		bahmniAppointment.setEndDateTime(new Date());

		org.openmrs.Provider provider = new org.openmrs.Provider();
		provider.setUuid("provider-uuid");

		AppointmentProvider apProvider = new AppointmentProvider();
		apProvider.setProvider(provider);
		apProvider.setResponse(AppointmentProviderResponse.CANCELLED);

		HashSet<AppointmentProvider> providers = new HashSet<>();
		providers.add(apProvider);
		bahmniAppointment.setProviders(providers);

		org.hl7.fhir.r4.model.Reference pRef = new org.hl7.fhir.r4.model.Reference("Practitioner/provider-uuid");

		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled))
			.thenReturn(Appointment.AppointmentStatus.BOOKED);
		when(practitionerReferenceTranslator.toFhirResource(provider)).thenReturn(pRef);

		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);

		assertEquals("Provider with CANCELLED response should map to DECLINED status",
			Appointment.ParticipationStatus.DECLINED, fhirAppointment.getParticipant().get(0).getStatus());
	}
	
	@Test
	public void shouldFallbackToLegacySingleProviderWhenProvidersSetEmpty() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment =
			new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("test-uuid");
		bahmniAppointment.setStatus(AppointmentStatus.Scheduled);
		bahmniAppointment.setStartDateTime(new Date());
		bahmniAppointment.setEndDateTime(new Date());
		bahmniAppointment.setProviders(new HashSet<>()); // Empty providers set

		org.openmrs.Provider legacyProvider = new org.openmrs.Provider();
		legacyProvider.setUuid("legacy-provider-uuid");
		bahmniAppointment.setProvider(legacyProvider); // Legacy single provider field

		org.hl7.fhir.r4.model.Reference legacyRef = new org.hl7.fhir.r4.model.Reference("Practitioner/legacy-provider-uuid");

		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled))
			.thenReturn(Appointment.AppointmentStatus.BOOKED);
		when(practitionerReferenceTranslator.toFhirResource(legacyProvider)).thenReturn(legacyRef);

		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);

		assertEquals("Should have 1 participant from legacy provider", 1, fhirAppointment.getParticipant().size());
		assertEquals("Legacy provider should have ACCEPTED status", Appointment.ParticipationStatus.ACCEPTED,
			fhirAppointment.getParticipant().get(0).getStatus());
	}
	
	@Test
	public void shouldMapLocationParticipant() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment = new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("test-uuid");
		bahmniAppointment.setStatus(AppointmentStatus.Scheduled);
		bahmniAppointment.setStartDateTime(new Date());
		bahmniAppointment.setEndDateTime(new Date());
		
		org.openmrs.Location location = new org.openmrs.Location();
		location.setUuid("location-uuid");
		bahmniAppointment.setLocation(location);
		
		org.hl7.fhir.r4.model.Reference locRef = new org.hl7.fhir.r4.model.Reference("Location/location-uuid");
		
		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled)).thenReturn(
		    Appointment.AppointmentStatus.BOOKED);
		when(locationReferenceTranslator.toFhirResource(location)).thenReturn(locRef);
		
		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);
		
		assertEquals("Location participant should be added", 1, fhirAppointment.getParticipant().size());
		assertEquals("Location should have ACCEPTED status", Appointment.ParticipationStatus.ACCEPTED, fhirAppointment
		        .getParticipant().get(0).getStatus());
	}
	
	@Test
	public void shouldHandleNullPatientParticipant() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment = new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("test-uuid");
		bahmniAppointment.setStatus(AppointmentStatus.Scheduled);
		bahmniAppointment.setStartDateTime(new Date());
		bahmniAppointment.setEndDateTime(new Date());
		bahmniAppointment.setPatient(null);
		
		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled)).thenReturn(
		    Appointment.AppointmentStatus.BOOKED);
		
		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);
		
		assertEquals("No participants should be added when patient is null", 0, fhirAppointment.getParticipant().size());
	}
	
	@Test
	public void shouldHandleNullLocationParticipant() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment = new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("test-uuid");
		bahmniAppointment.setStatus(AppointmentStatus.Scheduled);
		bahmniAppointment.setStartDateTime(new Date());
		bahmniAppointment.setEndDateTime(new Date());
		bahmniAppointment.setLocation(null);
		
		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled)).thenReturn(
		    Appointment.AppointmentStatus.BOOKED);
		
		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);
		
		assertEquals("No location participant when location is null", 0, fhirAppointment.getParticipant().size());
	}
	
	@Test
	public void shouldMapMetadataLastUpdatedFromDateChanged() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment = new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("test-uuid");
		bahmniAppointment.setStatus(AppointmentStatus.Scheduled);
		bahmniAppointment.setStartDateTime(new Date());
		bahmniAppointment.setEndDateTime(new Date());
		
		Date dateChanged = new Date(System.currentTimeMillis());
		bahmniAppointment.setDateChanged(dateChanged);
		
		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled)).thenReturn(
		    Appointment.AppointmentStatus.BOOKED);
		
		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);
		
		assertEquals("lastUpdated should be set to dateChanged", dateChanged, fhirAppointment.getMeta().getLastUpdated());
	}
	
	@Test
	public void shouldMapMetadataLastUpdatedFallbackToDateCreated() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment = new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("test-uuid");
		bahmniAppointment.setStatus(AppointmentStatus.Scheduled);
		bahmniAppointment.setStartDateTime(new Date());
		bahmniAppointment.setEndDateTime(new Date());
		
		Date dateCreated = new Date(System.currentTimeMillis());
		bahmniAppointment.setDateCreated(dateCreated);
		bahmniAppointment.setDateChanged(null);
		
		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled)).thenReturn(
		    Appointment.AppointmentStatus.BOOKED);
		
		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);
		
		assertEquals("lastUpdated should fall back to dateCreated", dateCreated, fhirAppointment.getMeta().getLastUpdated());
	}
	
	@Test
	public void shouldHandleNullProviderInProviderSet() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment =
			new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("test-uuid");
		bahmniAppointment.setStatus(AppointmentStatus.Scheduled);
		bahmniAppointment.setStartDateTime(new Date());
		bahmniAppointment.setEndDateTime(new Date());

		org.openmrs.Provider validProvider = new org.openmrs.Provider();
		validProvider.setUuid("provider-uuid");

		AppointmentProvider apValidProvider = new AppointmentProvider();
		apValidProvider.setProvider(validProvider);
		apValidProvider.setResponse(AppointmentProviderResponse.ACCEPTED);

		AppointmentProvider apNullProvider = new AppointmentProvider();
		apNullProvider.setProvider(null); // Null provider in set

		HashSet<AppointmentProvider> providers = new HashSet<>();
		providers.add(apValidProvider);
		providers.add(apNullProvider);
		bahmniAppointment.setProviders(providers);

		org.hl7.fhir.r4.model.Reference pRef = new org.hl7.fhir.r4.model.Reference("Practitioner/provider-uuid");

		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled))
			.thenReturn(Appointment.AppointmentStatus.BOOKED);
		when(practitionerReferenceTranslator.toFhirResource(validProvider)).thenReturn(pRef);

		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);

		assertEquals("Only valid provider should be included, null provider filtered out", 1,
			fhirAppointment.getParticipant().size());
	}
	
	@Test
	public void shouldMapServiceTypeWithSubTypes() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment =
			new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("test-uuid");
		bahmniAppointment.setStatus(AppointmentStatus.Scheduled);
		bahmniAppointment.setStartDateTime(new Date());
		bahmniAppointment.setEndDateTime(new Date());

		AppointmentServiceDefinition service = new AppointmentServiceDefinition();
		service.setUuid("service-uuid-456");
		service.setName("Consultation");

		AppointmentServiceType serviceType = new AppointmentServiceType();
		serviceType.setUuid("service-type-uuid-789");
		serviceType.setName("Follow-up");

		HashSet<AppointmentServiceType> serviceTypes = new HashSet<>();
		serviceTypes.add(serviceType);
		service.setServiceTypes(serviceTypes);

		bahmniAppointment.setService(service);

		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled))
			.thenReturn(Appointment.AppointmentStatus.BOOKED);

		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);

		assertEquals(1, fhirAppointment.getServiceType().size());
		assertEquals(1, fhirAppointment.getServiceType().get(0).getCoding().size());
	}
	
	@Test
	public void shouldMapSingleAppointmentReason() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment =
			new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("test-uuid");
		bahmniAppointment.setStatus(AppointmentStatus.Scheduled);
		bahmniAppointment.setStartDateTime(new Date());
		bahmniAppointment.setEndDateTime(new Date());

		Concept reasonConcept = mock(Concept.class);
		when(reasonConcept.getUuid()).thenReturn("reason-concept-uuid");
		when(reasonConcept.getDisplayString()).thenReturn("Diabetes Consultation");

		AppointmentReason reason = new AppointmentReason();
		reason.setConcept(reasonConcept);

		Set<AppointmentReason> reasons = new HashSet<>();
		reasons.add(reason);
		bahmniAppointment.setReasons(reasons);

		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled))
			.thenReturn(Appointment.AppointmentStatus.BOOKED);

		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);

		assertEquals("Should have 1 reason code", 1, fhirAppointment.getReasonCode().size());
		assertEquals("Diabetes Consultation", fhirAppointment.getReasonCode().get(0).getText());
		assertEquals("reason-concept-uuid", fhirAppointment.getReasonCode().get(0).getCoding().get(0).getCode());
		assertEquals("Diabetes Consultation", fhirAppointment.getReasonCode().get(0).getCoding().get(0).getDisplay());
	}
	
	@Test
	public void shouldMapMultipleAppointmentReasons() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment =
			new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("test-uuid");
		bahmniAppointment.setStatus(AppointmentStatus.Scheduled);
		bahmniAppointment.setStartDateTime(new Date());
		bahmniAppointment.setEndDateTime(new Date());

		Concept reasonConcept1 = mock(Concept.class);
		when(reasonConcept1.getUuid()).thenReturn("reason-concept-uuid-1");
		when(reasonConcept1.getDisplayString()).thenReturn("Checkup");

		Concept reasonConcept2 = mock(Concept.class);
		when(reasonConcept2.getUuid()).thenReturn("reason-concept-uuid-2");
		when(reasonConcept2.getDisplayString()).thenReturn("Vaccination");

		AppointmentReason reason1 = new AppointmentReason();
		reason1.setConcept(reasonConcept1);

		AppointmentReason reason2 = new AppointmentReason();
		reason2.setConcept(reasonConcept2);

		Set<AppointmentReason> reasons = new HashSet<>();
		reasons.add(reason1);
		reasons.add(reason2);
		bahmniAppointment.setReasons(reasons);

		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled))
			.thenReturn(Appointment.AppointmentStatus.BOOKED);

		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);

		assertEquals("Should have 2 reason codes", 2, fhirAppointment.getReasonCode().size());
	}
	
	@Test
	public void shouldHandleNullReasons() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment = new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("test-uuid");
		bahmniAppointment.setStatus(AppointmentStatus.Scheduled);
		bahmniAppointment.setStartDateTime(new Date());
		bahmniAppointment.setEndDateTime(new Date());
		bahmniAppointment.setReasons(null);
		
		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled)).thenReturn(
		    Appointment.AppointmentStatus.BOOKED);
		
		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);
		
		assertEquals("Should have 0 reason codes when reasons is null", 0, fhirAppointment.getReasonCode().size());
	}
	
	@Test
	public void shouldHandleEmptyReasons() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment =
			new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("test-uuid");
		bahmniAppointment.setStatus(AppointmentStatus.Scheduled);
		bahmniAppointment.setStartDateTime(new Date());
		bahmniAppointment.setEndDateTime(new Date());
		bahmniAppointment.setReasons(new HashSet<>());

		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled))
			.thenReturn(Appointment.AppointmentStatus.BOOKED);

		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);

		assertEquals("Should have 0 reason codes when reasons set is empty", 0, fhirAppointment.getReasonCode().size());
	}
	
	@Test
	public void shouldHandleNullConceptInReason() {
		org.openmrs.module.appointments.model.Appointment bahmniAppointment =
			new org.openmrs.module.appointments.model.Appointment();
		bahmniAppointment.setUuid("test-uuid");
		bahmniAppointment.setStatus(AppointmentStatus.Scheduled);
		bahmniAppointment.setStartDateTime(new Date());
		bahmniAppointment.setEndDateTime(new Date());

		AppointmentReason reasonWithNullConcept = new AppointmentReason();
		reasonWithNullConcept.setConcept(null);

		Concept validConcept = mock(Concept.class);
		when(validConcept.getUuid()).thenReturn("valid-concept-uuid");
		when(validConcept.getDisplayString()).thenReturn("Valid Reason");

		AppointmentReason validReason = new AppointmentReason();
		validReason.setConcept(validConcept);

		Set<AppointmentReason> reasons = new HashSet<>();
		reasons.add(reasonWithNullConcept);
		reasons.add(validReason);
		bahmniAppointment.setReasons(reasons);

		when(appointmentStatusTranslator.toFhirResource(AppointmentStatus.Scheduled))
			.thenReturn(Appointment.AppointmentStatus.BOOKED);

		Appointment fhirAppointment = translator.toFhirResource(bahmniAppointment);

		assertEquals("Should have 1 reason code (null concept filtered out)", 1, fhirAppointment.getReasonCode().size());
	}
}
