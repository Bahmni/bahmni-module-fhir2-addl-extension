package org.bahmni.module.fhir2AddlExtension.api.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.hl7.fhir.r4.model.Condition;
import org.openmrs.module.fhir2.api.FhirService;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

public interface FhirEncounterDiagnosisService extends FhirService<Condition> {
	
	IBundleProvider searchForDiagnosis(SearchParameterMap searchParameterMap);
}
