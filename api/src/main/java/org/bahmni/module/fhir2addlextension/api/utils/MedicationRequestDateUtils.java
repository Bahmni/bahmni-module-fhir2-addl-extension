package org.bahmni.module.fhir2addlextension.api.utils;

import java.util.Calendar;
import java.util.Date;

public class MedicationRequestDateUtils {
	
	private MedicationRequestDateUtils() {
	}
	
	public static boolean isFutureDate(Date date, Date now) {
		return date.after(now) && !isSameDay(date, now);
	}
	
	public static boolean isSameDay(Date date1, Date date2) {
		Calendar cal1 = Calendar.getInstance();
		cal1.setTime(date1);
		Calendar cal2 = Calendar.getInstance();
		cal2.setTime(date2);
		return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
		        && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
	}
}
