package org.bahmni.module.fhir2AddlExtension.api.utils;

public class ModuleUtils {
	
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
}
