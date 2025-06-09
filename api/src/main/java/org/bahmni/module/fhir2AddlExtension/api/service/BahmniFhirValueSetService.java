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
	 * Expands a ValueSet by ID with support for hierarchical output
	 * 
	 * @param valueSetId the ID of the ValueSet to expand
	 * @param includeHierarchy whether to include hierarchical relationships in the expansion
	 * @param filter optional filter to apply to the expansion
	 * @param count optional limit on the number of concepts to return
	 * @param offset optional offset for pagination
	 * @return expanded ValueSet with concepts and optionally hierarchical structure
	 */
	ValueSet expandedValueSet(@Nonnull String valueSetId, @Nullable Boolean includeHierarchy, @Nullable String filter,
	        @Nullable Integer count, @Nullable Integer offset);
	
}
