package org.bahmni.module.fhir2addlextension.api.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniEpisodeOfCareSearchParams;
import org.openmrs.module.fhir2.api.FhirEpisodeOfCareService;

public interface BahmniFhirEpisodeOfCareService extends FhirEpisodeOfCareService {
	
	IBundleProvider episodesForPatient(BahmniEpisodeOfCareSearchParams searchParams);
}
