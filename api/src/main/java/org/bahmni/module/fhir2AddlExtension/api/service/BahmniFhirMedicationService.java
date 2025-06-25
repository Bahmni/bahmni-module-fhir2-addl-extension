package org.bahmni.module.fhir2AddlExtension.api.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.StringParam;

public interface BahmniFhirMedicationService {
	
	IBundleProvider searchMedicationsByName(StringParam nameParam);
}
