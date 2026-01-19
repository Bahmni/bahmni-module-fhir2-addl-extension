package org.bahmni.module.fhir2AddlExtension.api.dao;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.openmrs.module.fhir2.model.FhirConceptSource;

import java.util.List;

import static org.hibernate.criterion.Projections.property;
import static org.hibernate.criterion.Restrictions.eq;
import static org.hibernate.criterion.Subqueries.propertyEq;

/**
 * Interface providing default implementation for generating system queries when filtering FHIR
 * resources by code system. This is used when only the system URL is provided without a specific
 * code.
 */
public interface FhirConceptCodeSystemQuery {

	/**
	 * Generates a Criterion to filter by concept source when codes are empty. This is used for FHIR
	 * token search where only the system is provided without a code.
	 *
	 * @param system The FHIR concept source URL
	 * @param conceptReferenceTermAlias The alias for the concept reference term in the query
	 * @return A Criterion that filters by concept source
	 */
	default Criterion generateSystemQueryForEmptyCodes(String system, String conceptReferenceTermAlias) {
		DetachedCriteria conceptSourceCriteria = DetachedCriteria.forClass(FhirConceptSource.class).add(eq("url", system))
		        .setProjection(property("conceptSource"));
		return propertyEq(String.format("%s.conceptSource", conceptReferenceTermAlias), conceptSourceCriteria);
	}

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
