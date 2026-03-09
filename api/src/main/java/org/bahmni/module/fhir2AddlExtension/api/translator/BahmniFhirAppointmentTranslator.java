package org.bahmni.module.fhir2AddlExtension.api.translator;

import org.hl7.fhir.r4.model.Appointment;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;

public interface BahmniFhirAppointmentTranslator extends OpenmrsFhirTranslator<org.openmrs.module.appointments.model.Appointment, Appointment> {
	
}
