package org.bahmni.module.fhir2addlextension.api;

import org.checkerframework.checker.nullness.qual.NonNull;

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

	public static @NonNull Date minusDays(Date currentDate, int days) {
		Instant instant = currentDate.toInstant();
		Instant resultInstant = instant.minus(days, ChronoUnit.DAYS);
		Date dateOfActivation = Date.from(resultInstant);
		return dateOfActivation;
	}

	public static @NonNull Date plusDays(Date currentDate, int days) {
		Instant instant = currentDate.toInstant();
		Instant resultInstant = instant.plus(days, ChronoUnit.DAYS);
		Date dateOfActivation = Date.from(resultInstant);
		return dateOfActivation;
	}
}
