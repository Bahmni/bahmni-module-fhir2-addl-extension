package org.bahmni.module.fhir2AddlExtension.api.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.bahmni.module.fhir2AddlExtension.api.search.param.BahmniConditionSearchParams;
import org.openmrs.module.fhir2.api.FhirConditionService;

public interface BahmniFhirConditionService extends FhirConditionService {
	
	IBundleProvider searchConditions(BahmniConditionSearchParams searchParams);
	
}
