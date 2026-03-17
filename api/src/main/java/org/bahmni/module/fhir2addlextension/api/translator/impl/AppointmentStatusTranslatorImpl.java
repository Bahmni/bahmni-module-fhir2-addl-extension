package org.bahmni.module.fhir2addlextension.api.translator.impl;

import org.bahmni.module.fhir2addlextension.api.translator.AppointmentStatusTranslator;
import org.hl7.fhir.r4.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentStatus;
import org.springframework.stereotype.Component;

@Component
public class AppointmentStatusTranslatorImpl implements AppointmentStatusTranslator {
	
	@Override
	public Appointment.AppointmentStatus toFhirResource(AppointmentStatus bahmniStatus) {
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
			case WaitList:
				return Appointment.AppointmentStatus.WAITLIST;
			default:
				return Appointment.AppointmentStatus.PENDING;
		}
	}
	
	@Override
	public AppointmentStatus toOpenmrsType(Appointment.AppointmentStatus fhirStatus) {
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
			case WAITLIST:
				return AppointmentStatus.WaitList;
			default:
				return AppointmentStatus.Requested;
		}
	}
}
