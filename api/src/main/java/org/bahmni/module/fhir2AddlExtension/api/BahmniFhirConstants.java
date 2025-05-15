package org.bahmni.module.fhir2AddlExtension.api;

public final class BahmniFhirConstants {
	
	private BahmniFhirConstants() {
	}
	
	public static final String FHIR_NAMESPACE = "http://fhir.bahmni.org";
	
	public static final String BAHMNI_CODE_SYSTEM_PREFIX = FHIR_NAMESPACE + "/code-system";
	
	public static final String ORDER_TYPE_SYSTEM_URI = BAHMNI_CODE_SYSTEM_PREFIX + "/order-type";
	
	public static final String SP_NUMBER_OF_VISITS = "numberOfVisits";
	
	public static final String LAB_TEST_CONCEPT_CLASS = "LabTest";
	
	public static final String TEST_CONCEPT_CLASS = "Test";
	
	public static final String LABSET_CONCEPT_CLASS = "LabSet";
	
	public static final String LAB_ORDER_CONCEPT_TYPE_EXTENSION_URL = FHIR_NAMESPACE + "/lab-order-concept-type-extension";
}
