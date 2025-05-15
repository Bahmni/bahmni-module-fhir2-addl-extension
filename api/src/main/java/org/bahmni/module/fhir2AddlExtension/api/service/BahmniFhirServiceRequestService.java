package org.bahmni.module.fhir2AddlExtension.api.service;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import org.openmrs.module.fhir2.api.FhirServiceRequestService;

import java.util.HashSet;

public interface BahmniFhirServiceRequestService extends FhirServiceRequestService {
	
	IBundleProvider searchForServiceRequestsWithCategory(ReferenceAndListParam patientReference, TokenAndListParam code,
	        ReferenceAndListParam encounterReference, ReferenceAndListParam participantReference,
	        ReferenceAndListParam category, DateRangeParam occurrence, TokenAndListParam uuid, DateRangeParam lastUpdated,
	        HashSet<Include> includes);
}
