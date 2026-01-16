package org.bahmni.module.fhir2AddlExtension.api.utils;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.openmrs.Location;
import org.openmrs.module.fhir2.model.FhirConceptSource;

import java.util.List;

import static org.hibernate.criterion.Projections.property;
import static org.hibernate.criterion.Restrictions.eq;
import static org.hibernate.criterion.Subqueries.propertyEq;

public class ModuleUtils {
	
	public static final String VISIT_LOCATION_TAG = "Visit Location";
	
	/**
	 * Cleans a string by replacing all spaces with a single underscore and stripping all other
	 * special characters.
	 * 
	 * @param originalString The input string to clean.
	 * @return The cleaned string, containing only letters, digits, and underscores.
	 */
	public static String toSlugCase(String originalString) {
		if (originalString == null) {
			return null;
		}
		return originalString.replaceAll("\\s+", "-").replaceAll("[^a-zA-Z0-9-]", "").replaceAll("-{2,}", "-").toLowerCase();
	}
	
	public static Location getVisitLocation(Location aLocation) {
		if (aLocation == null) {
			return null;
		}
		if (aLocation.getTags() != null) {
			//check if the location is marked as visit location
			for (org.openmrs.LocationTag tag : aLocation.getTags()) {
				if (tag.getName().equals(VISIT_LOCATION_TAG)) {
					return aLocation;
				}
			}
		}
		//check if the location is root of the location hierarchy
		if (aLocation.getParentLocation() == null) {
			return aLocation;
		}
		//check location parent
		return getVisitLocation(aLocation.getParentLocation());
	}
	
	/**
	 * Generates a Criterion to filter by concept source when codes are empty. This is used for FHIR
	 * token search where only the system is provided without a code.
	 * 
	 * @param system The FHIR concept source URL
	 * @param conceptReferenceTermAlias The alias for the concept reference term in the query
	 * @return A Criterion that filters by concept source
	 */
	public static Criterion generateSystemQueryForEmptyCodes(String system, String conceptReferenceTermAlias) {
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
	public static boolean isConceptReferenceCodeEmpty(List<String> codes) {
		if (codes == null || codes.isEmpty()) {
			return true;
		}
		return codes.size() == 1 && codes.get(0).isEmpty();
	}
}
