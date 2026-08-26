package org.bahmni.module.fhir2addlextension.api.service;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.*;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniServiceRequestSearchParams;
import org.openmrs.module.fhir2.api.FhirServiceRequestService;

import java.util.HashSet;

public interface BahmniFhirServiceRequestService extends FhirServiceRequestService {
	
	IBundleProvider searchForServiceRequestsWithCategory(BahmniServiceRequestSearchParams searchParams);
	
	IBundleProvider searchForServiceRequestsByNumberOfVisits(ReferenceParam patientReference, NumberParam numberOfVisits,
	        ReferenceAndListParam category, SortSpec sort, HashSet<Include> includes, HashSet<Include> revIncludes);
}
