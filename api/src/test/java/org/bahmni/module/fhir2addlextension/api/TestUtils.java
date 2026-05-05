package org.bahmni.module.fhir2addlextension.api;

import javax.validation.constraints.NotNull;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class TestUtils {
	
	public static void setPropertyOnObject(Object target, String attributeName, Object value) throws NoSuchFieldException,
	        IllegalAccessException {
		//TBD: unfortunately, the setters are package private
		Class<?> clazz = target.getClass();
		Field field = clazz.getDeclaredField(attributeName);
		field.setAccessible(true);
		field.set(target, value);
	}
	
	public static @NotNull
	Date minusDays(@NotNull Date currentDate, int days) {
		Instant instant = currentDate.toInstant();
		Instant resultInstant = instant.minus(days, ChronoUnit.DAYS);
		return Date.from(resultInstant);
	}
	
	public static @NotNull
	Date plusDays(@NotNull Date currentDate, int days) {
		Instant instant = currentDate.toInstant();
		Instant resultInstant = instant.plus(days, ChronoUnit.DAYS);
		return Date.from(resultInstant);
	}
}
