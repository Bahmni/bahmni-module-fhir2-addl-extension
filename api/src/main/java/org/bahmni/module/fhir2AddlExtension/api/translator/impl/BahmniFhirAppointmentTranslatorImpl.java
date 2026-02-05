package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.translator.AppointmentStatusTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniFhirAppointmentTranslator;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent;
import org.hl7.fhir.r4.model.Appointment.ParticipationStatus;
import org.openmrs.module.fhir2.api.translators.LocationReferenceTranslator;
import org.openmrs.Provider;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;
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
		fhirAppointment.setDescription(bahmniAppointment.getComments());

		// Participants
		List<AppointmentParticipantComponent> participants = new ArrayList<>();

		// Patient participant
		Optional.ofNullable(bahmniAppointment.getPatient()).ifPresent(patient -> {
			AppointmentParticipantComponent patientParticipant = new AppointmentParticipantComponent();
			patientParticipant.setActor(patientReferenceTranslator.toFhirResource(patient));
			patientParticipant.setStatus(ParticipationStatus.ACCEPTED);
			participants.add(patientParticipant);
		});

		// Provider/Practitioner participant
		Optional.ofNullable(bahmniAppointment.getProvider()).ifPresent(provider -> {
			AppointmentParticipantComponent practitionerParticipant = new AppointmentParticipantComponent();
			practitionerParticipant.setActor(practitionerReferenceTranslator.toFhirResource(provider));
			practitionerParticipant.setStatus(ParticipationStatus.ACCEPTED);
			participants.add(practitionerParticipant);
		});

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
}
