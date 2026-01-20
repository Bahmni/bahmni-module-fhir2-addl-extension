package org.bahmni.module.fhir2AddlExtension.api.service;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.*;
import org.openmrs.module.fhir2.api.FhirServiceRequestService;

import java.util.HashSet;

public interface BahmniFhirServiceRequestService extends FhirServiceRequestService {
	
	IBundleProvider searchForServiceRequestsWithCategory(ReferenceAndListParam patientReference, TokenAndListParam code,
	        ReferenceAndListParam encounterReference, ReferenceAndListParam participantReference,
	        ReferenceAndListParam category, DateRangeParam occurrence, TokenAndListParam uuid, DateRangeParam lastUpdated,
	        HashSet<Include> includes, HashSet<Include> revIncludes);
	
	IBundleProvider searchForServiceRequestsByNumberOfVisits(ReferenceParam patientReference, NumberParam numberOfVisits,
	        ReferenceAndListParam category, SortSpec sort, HashSet<Include> includes, HashSet<Include> revIncludes);
}
