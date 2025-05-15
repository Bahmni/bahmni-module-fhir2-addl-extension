package org.bahmni.module.fhir2AddlExtension.api.providers;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.*;
import org.apache.commons.collections.CollectionUtils;
import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirServiceRequestService;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.providers.r4.ServiceRequestFhirResourceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
@R4Provider
public class BahmniServiceRequestFhirR4ResourceProvider extends ServiceRequestFhirResourceProvider {
	
	@Autowired
	private BahmniFhirServiceRequestService serviceRequestService;
	
	@Search
	public IBundleProvider searchForServiceRequests(
	        @OptionalParam(name = ServiceRequest.SP_PATIENT, chainWhitelist = { "", Patient.SP_IDENTIFIER, Patient.SP_GIVEN,
	                Patient.SP_FAMILY, Patient.SP_NAME }, targetTypes = Patient.class) ReferenceAndListParam patientReference,
	        @OptionalParam(name = ServiceRequest.SP_SUBJECT, chainWhitelist = { "", Patient.SP_IDENTIFIER, Patient.SP_GIVEN,
	                Patient.SP_FAMILY, Patient.SP_NAME }, targetTypes = Patient.class) ReferenceAndListParam subjectReference,
	        @OptionalParam(name = ServiceRequest.SP_CODE) TokenAndListParam code,
	        @OptionalParam(name = ServiceRequest.SP_ENCOUNTER, chainWhitelist = { "" }, targetTypes = Encounter.class) ReferenceAndListParam encounterReference,
	        @OptionalParam(name = ServiceRequest.SP_REQUESTER, chainWhitelist = { "", Practitioner.SP_IDENTIFIER,
	                Practitioner.SP_GIVEN, Practitioner.SP_FAMILY, Practitioner.SP_NAME }, targetTypes = Practitioner.class) ReferenceAndListParam participantReference,
	        @OptionalParam(name = ServiceRequest.SP_OCCURRENCE) DateRangeParam occurrence,
	        @OptionalParam(name = ServiceRequest.SP_RES_ID) TokenAndListParam uuid,
	        @OptionalParam(name = ServiceRequest.SP_CATEGORY) ReferenceAndListParam categoryReference,
	        @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated, @IncludeParam(allow = {
	                "ServiceRequest:" + ServiceRequest.SP_PATIENT, "ServiceRequest:" + ServiceRequest.SP_REQUESTER,
	                "ServiceRequest:" + ServiceRequest.SP_ENCOUNTER }) HashSet<Include> includes) {
		if (patientReference == null) {
			patientReference = subjectReference;
		}
		
		if (CollectionUtils.isEmpty(includes)) {
			includes = null;
		}
		
		return serviceRequestService.searchForServiceRequestsWithCategory(patientReference, code, encounterReference,
		    participantReference, categoryReference, occurrence, uuid, lastUpdated, includes);
	}
	
	@Search
	public IBundleProvider searchForServiceRequestsByNumberOfVisits(
	        @RequiredParam(name = ServiceRequest.SP_PATIENT, chainWhitelist = { "", Patient.SP_IDENTIFIER, Patient.SP_GIVEN,
	                Patient.SP_FAMILY, Patient.SP_NAME }, targetTypes = Patient.class) ReferenceParam patientReference,
	        @RequiredParam(name = BahmniFhirConstants.SP_NUMBER_OF_VISITS) NumberParam numberOfVisits,
	        @OptionalParam(name = ServiceRequest.SP_CATEGORY) ReferenceAndListParam categoryReference,
	        @IncludeParam(allow = { "ServiceRequest:" + ServiceRequest.SP_PATIENT,
	                "ServiceRequest:" + ServiceRequest.SP_REQUESTER, "ServiceRequest:" + ServiceRequest.SP_ENCOUNTER }) HashSet<Include> includes) {
		
		if (CollectionUtils.isEmpty(includes)) {
			includes = null;
		}
		
		return serviceRequestService.searchForServiceRequestsByNumberOfVisits(patientReference, numberOfVisits,
		    categoryReference, includes);
	}
}
