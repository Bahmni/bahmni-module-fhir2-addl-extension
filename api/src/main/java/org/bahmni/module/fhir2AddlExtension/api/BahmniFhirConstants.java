package org.bahmni.module.fhir2AddlExtension.api;

import org.hl7.fhir.r4.model.ResourceType;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

	public static final Set<ResourceType> CONSULTATION_BUNDLE_SUPPORTED_RESOURCES = Collections.unmodifiableSet(
			new HashSet<>(Arrays.asList(
					ResourceType.Encounter,
					ResourceType.AllergyIntolerance,
					ResourceType.Condition
			))
	);
}
