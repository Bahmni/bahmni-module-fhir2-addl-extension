package org.bahmni.module.fhir2AddlExtension.api.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;

public interface BahmniFhirEpisodeOfCareEncounterService {
	
	IBundleProvider encountersForEpisodes(ReferenceAndListParam patientReference);
}
