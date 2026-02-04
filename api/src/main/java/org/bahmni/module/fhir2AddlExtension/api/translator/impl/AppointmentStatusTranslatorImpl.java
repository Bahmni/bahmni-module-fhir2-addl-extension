package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.translator.AppointmentStatusTranslator;
import org.hl7.fhir.r4.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentStatus;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component
public class AppointmentStatusTranslatorImpl implements AppointmentStatusTranslator {
	
	@Override
	public Appointment.AppointmentStatus toFhirResource(@Nonnull AppointmentStatus bahmniStatus) {
		if (bahmniStatus == null) {
			return Appointment.AppointmentStatus.PENDING;
		}
		
		switch (bahmniStatus) {
			case Scheduled:
				return Appointment.AppointmentStatus.BOOKED;
			case Completed:
				return Appointment.AppointmentStatus.FULFILLED;
			case Cancelled:
				return Appointment.AppointmentStatus.CANCELLED;
			case Missed:
				return Appointment.AppointmentStatus.NOSHOW;
			case CheckedIn:
				return Appointment.AppointmentStatus.CHECKEDIN;
			case Requested:
			default:
				return Appointment.AppointmentStatus.PENDING;
		}
	}
	
	@Override
	public AppointmentStatus toOpenmrsType(@Nonnull Appointment.AppointmentStatus fhirStatus) {
		if (fhirStatus == null) {
			return AppointmentStatus.Requested;
		}
		
		switch (fhirStatus) {
			case BOOKED:
				return AppointmentStatus.Scheduled;
			case FULFILLED:
				return AppointmentStatus.Completed;
			case CANCELLED:
				return AppointmentStatus.Cancelled;
			case NOSHOW:
				return AppointmentStatus.Missed;
			case CHECKEDIN:
				return AppointmentStatus.CheckedIn;
			case PENDING:
			default:
				return AppointmentStatus.Requested;
		}
	}
}
