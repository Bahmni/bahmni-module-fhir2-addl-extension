package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2AddlExtension.api.translator.AppointmentStatusTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniFhirAppointmentTranslator;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent;
import org.hl7.fhir.r4.model.Appointment.ParticipationStatus;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.LocationReferenceTranslator;
import org.openmrs.Provider;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;
import org.openmrs.module.appointments.model.AppointmentProvider;
import org.openmrs.module.appointments.model.AppointmentProviderResponse;
import org.openmrs.module.appointments.model.AppointmentReason;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.Validate.notNull;
import static org.openmrs.module.fhir2.api.translators.impl.FhirTranslatorUtils.getLastUpdated;

@Component
public class BahmniFhirAppointmentTranslatorImpl implements BahmniFhirAppointmentTranslator {
	
	private final PatientReferenceTranslator patientReferenceTranslator;
	
	private final PractitionerReferenceTranslator<Provider> practitionerReferenceTranslator;
	
	private final LocationReferenceTranslator locationReferenceTranslator;
	
	private final AppointmentStatusTranslator appointmentStatusTranslator;
	
	private final ConceptTranslator conceptTranslator;
	
	@Autowired
	public BahmniFhirAppointmentTranslatorImpl(PatientReferenceTranslator patientReferenceTranslator,
	    PractitionerReferenceTranslator<Provider> practitionerReferenceTranslator,
	    LocationReferenceTranslator locationReferenceTranslator, AppointmentStatusTranslator appointmentStatusTranslator,
	    ConceptTranslator conceptTranslator) {
		this.patientReferenceTranslator = patientReferenceTranslator;
		this.practitionerReferenceTranslator = practitionerReferenceTranslator;
		this.locationReferenceTranslator = locationReferenceTranslator;
		this.appointmentStatusTranslator = appointmentStatusTranslator;
		this.conceptTranslator = conceptTranslator;
	}
	
	@Override
	public Appointment toFhirResource(@Nonnull org.openmrs.module.appointments.model.Appointment bahmniAppointment) {
		notNull(bahmniAppointment, "The Bahmni Appointment object should not be null");

		Appointment fhirAppointment = new Appointment();

		fhirAppointment.setId(bahmniAppointment.getUuid());
		fhirAppointment.setStatus(appointmentStatusTranslator.toFhirResource(bahmniAppointment.getStatus()));
		fhirAppointment.setStart(bahmniAppointment.getStartDateTime());
		fhirAppointment.setEnd(bahmniAppointment.getEndDateTime());

		Optional.ofNullable(bahmniAppointment.getAppointmentNumber()).ifPresent(appointmentNumber -> {
			fhirAppointment.addIdentifier()
				.setSystem(BahmniFhirConstants.BAHMNI_APPOINTMENT_SYSTEM)
				.setValue(appointmentNumber);
		});

		Optional.ofNullable(bahmniAppointment.getComments()).ifPresent(fhirAppointment::setComment);

		Optional.ofNullable(bahmniAppointment.getService()).ifPresent(service -> {
			CodeableConcept serviceType = new CodeableConcept();
			serviceType.setText(service.getName());
			Coding serviceCoding = new Coding()
				.setSystem(BahmniFhirConstants.BAHMNI_APPOINTMENT_SERVICE_SYSTEM)
				.setCode(service.getUuid())
				.setDisplay(service.getName());
			serviceType.addCoding(serviceCoding);
			fhirAppointment.addServiceType(serviceType);
		});

		Optional.ofNullable(bahmniAppointment.getReasons())
			.filter(reasons -> !reasons.isEmpty())
			.ifPresent(reasons -> {
				reasons.stream()
					.filter(reason -> reason != null && reason.getConcept() != null)
					.forEach(reason -> {
						fhirAppointment.addReasonCode(conceptTranslator.toFhirResource(reason.getConcept()));
					});
			});

		fhirAppointment.setParticipant(mapParticipants(bahmniAppointment));

		fhirAppointment.getMeta().setLastUpdated(getLastUpdated(bahmniAppointment));

		return fhirAppointment;
	}
	
	@Override
	public org.openmrs.module.appointments.model.Appointment toOpenmrsType(@Nonnull Appointment fhirAppointment) {
		throw new UnsupportedOperationException("Appointment resource is read-only via FHIR API");
	}
	
	private List<AppointmentParticipantComponent> mapParticipants(org.openmrs.module.appointments.model.Appointment bahmniAppointment) {
		List<AppointmentParticipantComponent> participants = new ArrayList<>();

		Optional.ofNullable(bahmniAppointment.getPatient()).ifPresent(patient -> {
			AppointmentParticipantComponent patientParticipant = new AppointmentParticipantComponent();
			patientParticipant.setActor(patientReferenceTranslator.toFhirResource(patient));
			patientParticipant.setStatus(ParticipationStatus.ACCEPTED);
			participants.add(patientParticipant);
		});

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

		if (participants.stream().noneMatch(p -> p.getActor() != null
                && p.getActor().getType() != null
                && "Practitioner".equals(p.getActor().getType()))) {
            Optional.ofNullable(bahmniAppointment.getProvider()).ifPresent(provider -> {
                AppointmentParticipantComponent practitionerParticipant = new AppointmentParticipantComponent();
                practitionerParticipant.setActor(practitionerReferenceTranslator.toFhirResource(provider));
                practitionerParticipant.setStatus(ParticipationStatus.ACCEPTED);
                participants.add(practitionerParticipant);
            });
        }

		Optional.ofNullable(bahmniAppointment.getLocation()).ifPresent(location -> {
			AppointmentParticipantComponent locationParticipant = new AppointmentParticipantComponent();
			locationParticipant.setActor(locationReferenceTranslator.toFhirResource(location));
			locationParticipant.setStatus(ParticipationStatus.ACCEPTED);
			participants.add(locationParticipant);
		});

		return participants;
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
