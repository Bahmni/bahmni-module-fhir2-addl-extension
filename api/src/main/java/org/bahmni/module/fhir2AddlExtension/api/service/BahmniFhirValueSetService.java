package org.bahmni.module.fhir2AddlExtension.api.service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.hl7.fhir.r4.model.ValueSet;
import org.openmrs.module.fhir2.api.FhirValueSetService;

/**
 * Extended FHIR ValueSet service interface for Bahmni-specific functionality including hierarchical
 * expansion
 */
public interface BahmniFhirValueSetService extends FhirValueSetService {
	
	/**
	 * Expands a ValueSet by ID with hierarchical output
	 * 
	 * @param valueSetId the ID of the ValueSet to expand
	 * @return expanded ValueSet with concepts in hierarchical structure
	 */
	ValueSet expandedValueSet(@Nonnull String valueSetId);
	
}
