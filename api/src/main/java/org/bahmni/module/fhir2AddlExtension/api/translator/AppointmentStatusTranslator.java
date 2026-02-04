package org.bahmni.module.fhir2AddlExtension.api.translator;

import org.hl7.fhir.r4.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentStatus;

import javax.annotation.Nonnull;

public interface AppointmentStatusTranslator {
	
	Appointment.AppointmentStatus toFhirResource(@Nonnull AppointmentStatus bahmniStatus);
	
	AppointmentStatus toOpenmrsType(@Nonnull Appointment.AppointmentStatus fhirStatus);
}
