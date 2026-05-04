package org.bahmni.module.fhir2addlextension.api.utils;

import org.openmrs.Location;

import javax.validation.constraints.NotNull;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

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
	 * Compares two dates up to ChronoUnit granularity.
	 * @param d1 1st date to compare
	 * @param d2 2nd date to compare
	 * @param unit granularity to compare, default is MINUTES
	 * @return comparison result
	 * 0 if they are in the same granularity
	 * < 0 if d1 is before d2
	 * > 0 if d1 is after d2
	 */
	public static int compareDates(@NotNull Date d1, @NotNull Date d2, ChronoUnit unit) {
		ChronoUnit granularity = unit != null ? unit : ChronoUnit.MINUTES;
		ZonedDateTime dt1 = d1.toInstant().atZone(ZoneId.systemDefault()).truncatedTo(granularity);
		ZonedDateTime dt2 = d2.toInstant().atZone(ZoneId.systemDefault()).truncatedTo(granularity);
		return dt1.compareTo(dt2);
	}
}
