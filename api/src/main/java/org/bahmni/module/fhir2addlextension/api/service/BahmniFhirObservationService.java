package org.bahmni.module.fhir2addlextension.api.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniObservationSearchParams;
import org.hl7.fhir.r4.model.Bundle;
import org.openmrs.module.fhir2.api.FhirObservationService;

public interface BahmniFhirObservationService extends FhirObservationService {
	
	Bundle fetchAllByEncounter(BahmniObservationSearchParams searchParams);
	
	IBundleProvider searchObservations(BahmniObservationSearchParams searchParams);
}
