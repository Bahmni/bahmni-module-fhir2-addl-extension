package org.bahmni.module.fhir2AddlExtension.api.utils;

import org.openmrs.Location;

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
}
