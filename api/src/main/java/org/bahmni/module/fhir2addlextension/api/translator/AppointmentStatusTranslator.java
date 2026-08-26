package org.bahmni.module.fhir2addlextension.api.translator;

import org.hl7.fhir.r4.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentStatus;

public interface AppointmentStatusTranslator {
	
	Appointment.AppointmentStatus toFhirResource(AppointmentStatus bahmniStatus);
	
	AppointmentStatus toOpenmrsType(Appointment.AppointmentStatus fhirStatus);
}
