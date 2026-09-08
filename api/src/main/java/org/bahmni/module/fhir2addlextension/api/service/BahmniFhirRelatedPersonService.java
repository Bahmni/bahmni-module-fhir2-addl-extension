package org.bahmni.module.fhir2addlextension.api.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniRelatedPersonSearchParams;
import org.openmrs.module.fhir2.api.FhirRelatedPersonService;

public interface BahmniFhirRelatedPersonService extends FhirRelatedPersonService {
	
	IBundleProvider searchByPatient(BahmniRelatedPersonSearchParams searchParams);
}
