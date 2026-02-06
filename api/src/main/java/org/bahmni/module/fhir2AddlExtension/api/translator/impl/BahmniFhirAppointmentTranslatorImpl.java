package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.translator.AppointmentStatusTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniFhirAppointmentTranslator;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent;
import org.hl7.fhir.r4.model.Appointment.ParticipationStatus;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.openmrs.module.fhir2.api.translators.LocationReferenceTranslator;
import org.openmrs.Provider;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;
import org.openmrs.module.appointments.model.AppointmentProvider;
import org.openmrs.module.appointments.model.AppointmentProviderResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.Validate.notNull;

@Component
public class BahmniFhirAppointmentTranslatorImpl implements BahmniFhirAppointmentTranslator {
	
	@Autowired
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Autowired
	@Qualifier("practitionerReferenceTranslatorProviderImpl")
	private PractitionerReferenceTranslator<Provider> practitionerReferenceTranslator;
	
	@Autowired
	private LocationReferenceTranslator locationReferenceTranslator;
	
	@Autowired
	private AppointmentStatusTranslator appointmentStatusTranslator;
	
	@Override
	public Appointment toFhirResource(@Nonnull org.openmrs.module.appointments.model.Appointment bahmniAppointment) {
		notNull(bahmniAppointment, "The Bahmni Appointment object should not be null");

		Appointment fhirAppointment = new Appointment();

		// Basic fields
		fhirAppointment.setId(bahmniAppointment.getUuid());
		fhirAppointment.setStatus(appointmentStatusTranslator.toFhirResource(bahmniAppointment.getStatus()));
		fhirAppointment.setStart(bahmniAppointment.getStartDateTime());
		fhirAppointment.setEnd(bahmniAppointment.getEndDateTime());

		// Appointment identifier/number
		Optional.ofNullable(bahmniAppointment.getAppointmentNumber()).ifPresent(appointmentNumber -> {
			fhirAppointment.addIdentifier()
				.setSystem("urn:system:bahmni:appointments")
				.setValue(appointmentNumber);
		});

		// Comments mapped to comment field (FHIR R4 Appointment - for additional comments/notes)
		Optional.ofNullable(bahmniAppointment.getComments()).ifPresent(fhirAppointment::setComment);

		// Service Type mapping
		Optional.ofNullable(bahmniAppointment.getService()).ifPresent(service -> {
			CodeableConcept serviceType = new CodeableConcept();
			// Add the service name as text display
			serviceType.setText(service.getName());
			// Add coding with custom system for Bahmni services
			Coding serviceCoding = new Coding()
				.setSystem("urn:system:bahmni:appointment-services")
				.setCode(service.getName())
				.setDisplay(service.getName());
			serviceType.addCoding(serviceCoding);
			fhirAppointment.addServiceType(serviceType);

			// If service has a specific service type, add it as additional coding
			Optional.ofNullable(service.getServiceTypes()).ifPresent(serviceTypes -> {
				if (!serviceTypes.isEmpty()) {
					serviceTypes.stream().findFirst().ifPresent(serviceTypeObj -> {
						Coding serviceTypeCoding = new Coding()
							.setSystem("urn:system:bahmni:appointment-service-types")
							.setCode(serviceTypeObj.getName())
							.setDisplay(serviceTypeObj.getName());
						serviceType.addCoding(serviceTypeCoding);
					});
				}
			});
		});

		// Participants
		List<AppointmentParticipantComponent> participants = new ArrayList<>();

		// Patient participant
		Optional.ofNullable(bahmniAppointment.getPatient()).ifPresent(patient -> {
			AppointmentParticipantComponent patientParticipant = new AppointmentParticipantComponent();
			patientParticipant.setActor(patientReferenceTranslator.toFhirResource(patient));
			patientParticipant.setStatus(ParticipationStatus.ACCEPTED);
			participants.add(patientParticipant);
		});

		// Provider/Practitioner participants - handle multiple providers (current approach)
		Optional.ofNullable(bahmniAppointment.getProviders())
			.filter(providers -> !providers.isEmpty())
			.ifPresent(appointmentProviders -> {
				appointmentProviders.stream()
					.filter(ap -> ap != null && ap.getProvider() != null)
					.forEach(appointmentProvider -> {
						AppointmentParticipantComponent practitionerParticipant = new AppointmentParticipantComponent();
						practitionerParticipant.setActor(
							practitionerReferenceTranslator.toFhirResource(appointmentProvider.getProvider())
						);
						practitionerParticipant.setStatus(
							mapProviderResponseToParticipationStatus(appointmentProvider.getResponse())
						);
						participants.add(practitionerParticipant);
					});
			});

		// Fallback to single provider (legacy) if no providers were added
		if (participants.stream().noneMatch(p -> p.getActor() != null && "Practitioner".equals(p.getActor().getType()))) {
			Optional.ofNullable(bahmniAppointment.getProvider()).ifPresent(provider -> {
				AppointmentParticipantComponent practitionerParticipant = new AppointmentParticipantComponent();
				practitionerParticipant.setActor(practitionerReferenceTranslator.toFhirResource(provider));
				practitionerParticipant.setStatus(ParticipationStatus.ACCEPTED);
				participants.add(practitionerParticipant);
			});
		}

		// Location participant
		Optional.ofNullable(bahmniAppointment.getLocation()).ifPresent(location -> {
			AppointmentParticipantComponent locationParticipant = new AppointmentParticipantComponent();
			locationParticipant.setActor(locationReferenceTranslator.toFhirResource(location));
			locationParticipant.setStatus(ParticipationStatus.ACCEPTED);
			participants.add(locationParticipant);
		});

		fhirAppointment.setParticipant(participants);

		// Metadata - handle safely without relying on Auditable interface
		Date lastUpdated = bahmniAppointment.getDateChanged() != null
			? bahmniAppointment.getDateChanged()
			: bahmniAppointment.getDateCreated();
		if (lastUpdated != null) {
			fhirAppointment.getMeta().setLastUpdated(lastUpdated);
		}

		return fhirAppointment;
	}
	
	@Override
	public org.openmrs.module.appointments.model.Appointment toOpenmrsType(@Nonnull Appointment fhirAppointment) {
		throw new UnsupportedOperationException("Appointment resource is read-only via FHIR API");
	}
	
	private ParticipationStatus mapProviderResponseToParticipationStatus(AppointmentProviderResponse response) {
		if (response == null) {
			return ParticipationStatus.NEEDSACTION;
		}
		switch (response) {
			case ACCEPTED:
				return ParticipationStatus.ACCEPTED;
			case CANCELLED:
			case REJECTED:
				return ParticipationStatus.DECLINED;
			case AWAITING:
			default:
				return ParticipationStatus.NEEDSACTION;
		}
	}
}
