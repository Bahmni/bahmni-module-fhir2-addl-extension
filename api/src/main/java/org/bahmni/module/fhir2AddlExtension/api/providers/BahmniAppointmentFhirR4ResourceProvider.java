package org.bahmni.module.fhir2AddlExtension.api.providers;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.bahmni.module.fhir2AddlExtension.api.search.param.BahmniAppointmentSearchParams;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirAppointmentService;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("bahmniAppointmentFhirR4ResourceProvider")
@R4Provider
public class BahmniAppointmentFhirR4ResourceProvider implements IResourceProvider {
	
	private final BahmniFhirAppointmentService appointmentService;
	
	@Autowired
	public BahmniAppointmentFhirR4ResourceProvider(BahmniFhirAppointmentService appointmentService) {
		this.appointmentService = appointmentService;
	}
	
	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return Appointment.class;
	}
	
	@Read
	public Appointment getAppointmentByUuid(@IdParam IdType id) {
		Appointment appointment = appointmentService.get(id.getIdPart());
		if (appointment == null) {
			throw new ResourceNotFoundException("Could not find Appointment with Id " + id.getIdPart());
		}
		return appointment;
	}
	
	@Search
	public IBundleProvider searchAppointments(
	        @OptionalParam(name = Appointment.SP_PATIENT, chainWhitelist = { "", Patient.SP_IDENTIFIER, Patient.SP_NAME,
	                Patient.SP_GIVEN, Patient.SP_FAMILY }, targetTypes = Patient.class) ReferenceAndListParam patientReference,
	        @OptionalParam(name = Appointment.SP_STATUS) TokenAndListParam status,
	        @OptionalParam(name = Appointment.SP_DATE) DateRangeParam date,
	        @OptionalParam(name = Appointment.SP_RES_ID) TokenAndListParam id,
	        @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated, @Sort SortSpec sort) {
		
		BahmniAppointmentSearchParams searchParams = new BahmniAppointmentSearchParams(patientReference, status, date, id,
		        lastUpdated, sort);
		return appointmentService.searchAppointments(searchParams);
	}
}
