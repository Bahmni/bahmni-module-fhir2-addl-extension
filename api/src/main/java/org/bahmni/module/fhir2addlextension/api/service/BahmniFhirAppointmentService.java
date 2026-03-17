package org.bahmni.module.fhir2addlextension.api.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniAppointmentSearchParams;
import org.hl7.fhir.r4.model.Appointment;
import org.openmrs.module.fhir2.api.FhirService;

public interface BahmniFhirAppointmentService extends FhirService<Appointment> {
	
	IBundleProvider searchAppointments(BahmniAppointmentSearchParams searchParams);
}
