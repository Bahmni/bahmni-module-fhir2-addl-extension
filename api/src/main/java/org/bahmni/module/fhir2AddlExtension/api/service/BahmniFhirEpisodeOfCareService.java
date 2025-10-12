package org.bahmni.module.fhir2AddlExtension.api.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import org.openmrs.module.fhir2.api.FhirEpisodeOfCareService;

public interface BahmniFhirEpisodeOfCareService extends FhirEpisodeOfCareService {
	
	IBundleProvider episodesForPatient(ReferenceAndListParam patientReference);
}
