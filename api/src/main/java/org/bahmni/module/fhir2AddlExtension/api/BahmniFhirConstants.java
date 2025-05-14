package org.bahmni.module.fhir2AddlExtension.api;

import static org.openmrs.module.fhir2.FhirConstants.OPENMRS_CODE_SYSTEM_PREFIX;

public final class BahmniFhirConstants {
	
	private BahmniFhirConstants() {
	}
	
	public static final String BAHMNI_CODE_SYSTEM_PREFIX = "http://fhir.bahmni.org/code-system";
	
	public static final String ORDER_TYPE_SYSTEM_URI = BAHMNI_CODE_SYSTEM_PREFIX + "/order-type";
}
