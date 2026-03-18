package org.bahmni.module.fhir2addlextension.api.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.openmrs.module.fhir2.api.FhirObservationService;
import org.openmrs.module.fhir2.api.search.param.ObservationSearchParams;

public interface BahmniFhirObservationService extends FhirObservationService {
	
	IBundleProvider searchForObservations(ObservationSearchParams searchParams);
}
