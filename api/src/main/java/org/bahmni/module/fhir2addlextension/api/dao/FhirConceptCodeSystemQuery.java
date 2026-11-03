package org.bahmni.module.fhir2addlextension.api.dao;

import java.util.List;

/**
 * Interface providing utility for checking whether a concept reference code list is effectively
 * empty (system-only search). System-only searches (e.g. {@code ?code=http://system|}) are handled
 * natively by {@code FhirSearchQueryHelper.handleQueryForSystem} in fhir2 v4.3.0+.
 */
public interface FhirConceptCodeSystemQuery {

	/**
	 * Checks if the concept reference codes list is effectively empty. A codes list is considered
	 * empty if it is null, has no elements, or contains only a single empty string.
	 *
	 * @param codes The list of concept reference codes
	 * @return true if codes is empty, false otherwise
	 */
	default boolean isConceptReferenceCodeEmpty(List<String> codes) {
		if (codes == null || codes.isEmpty()) {
			return true;
		}
		return codes.size() == 1 && codes.get(0).isEmpty();
	}
}
