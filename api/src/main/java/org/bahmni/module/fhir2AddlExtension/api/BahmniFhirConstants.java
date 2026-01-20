package org.bahmni.module.fhir2AddlExtension.api;

import org.hl7.fhir.r4.model.ResourceType;
import org.openmrs.module.fhir2.FhirConstants;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class BahmniFhirConstants {

    private BahmniFhirConstants() {
	}
	
	public static final String FHIR_NAMESPACE = "http://fhir.bahmni.org";
	public static final String EXTENSION_PREFIX = FHIR_NAMESPACE + "/ext";

	public static final String BAHMNI_CODE_SYSTEM_PREFIX = FHIR_NAMESPACE + "/code-system";
	
	public static final String ORDER_TYPE_SYSTEM_URI = BAHMNI_CODE_SYSTEM_PREFIX + "/order-type";
	
	public static final String SP_NUMBER_OF_VISITS = "numberOfVisits";
	
	public static final String LAB_TEST_CONCEPT_CLASS = "LabTest";
	
	public static final String TEST_CONCEPT_CLASS = "Test";
	
	public static final String LABSET_CONCEPT_CLASS = "LabSet";
	
	public static final String LAB_ORDER_CONCEPT_TYPE_EXTENSION_URL = EXTENSION_PREFIX + "/lab-order-concept-type";
	public static final String VALUESET_CONCEPT_CLASS_EXTENSION_URL = EXTENSION_PREFIX + "/ValueSet/concept-class";

	public static final Set<ResourceType> CONSULTATION_BUNDLE_SUPPORTED_RESOURCES = Collections.unmodifiableSet(
			new HashSet<>(Arrays.asList(
					ResourceType.Encounter,
					ResourceType.AllergyIntolerance,
					ResourceType.Condition,
					ResourceType.ServiceRequest,
					ResourceType.MedicationRequest,
					ResourceType.Observation
			))
	);
	public static final String HL7_CONDITION_CATEGORY_CODE_SYSTEM = FhirConstants.HL7_FHIR_CODE_SYSTEM_PREFIX + "/condition-category";
	public static final String HL7_CONDITION_CATEGORY_CONDITION_CODE = "problem-list-item";
	public static final String HL7_CONDITION_CATEGORY_DIAGNOSIS_CODE = "encounter-diagnosis";

	public static final String CONDITION_VERIFICATION_STATUS_SEARCH_HANDLER = "condition.verification.status.handler";
	public static final String FHIR_EXT_EPISODE_OF_CARE_REASON = EXTENSION_PREFIX + "/episode-of-care/reason";
	public static final String INCLUDE_EPISODE_OF_CARE_PARAM = "episode-of-care";
	public static final String EPISODE_OF_CARE_REFERENCE_SEARCH_PARAM = "episodeOfCare.reference.search.handler";
	public static final String FHIR_EXT_DOCUMENT_REFERENCE_ATTRIBUTE = EXTENSION_PREFIX + "/document-reference/attribute";
	public static final String FHIR_EXT_DOCUMENT_REFERENCE_BASED_ON = EXTENSION_PREFIX + "/document-reference/based-on-service-request";
	public static final String FHIR_EXT_IMAGING_STUDY_PERFORMER = EXTENSION_PREFIX + "/imaging-study/performer";
	public static final String FHIR_EXT_IMAGING_STUDY_COMPLETION_DATE = EXTENSION_PREFIX + "/imaging-study/completion-date";
	public static final String FHIR_EXT_OBSERVATION_FORM_NAMESPACE_PATH = EXTENSION_PREFIX + "/observation/form-namespace-path";
	public static final String FHIR_EXT_OBSERVATION_ATTACHMENT_VALUE = EXTENSION_PREFIX + "/observation/obs-value-attachment";
	public static final String INCLUDE_BASED_ON_PARAM = "basedon";
	public static final String IMAGING_STUDY = "ImagingStudy";
}
