package org.bahmni.module.fhir2AddlExtension.api;

import org.openmrs.module.fhir2.api.translators.impl.DiagnosticReportTranslatorImpl;

import java.lang.reflect.Field;

public class TestUtils {
	
	public static void setPropertyOnObject(DiagnosticReportTranslatorImpl openmrsTranslator, String attributeName,
	        Object value) throws NoSuchFieldException, IllegalAccessException {
		//TBD: unfortunately, the setters are package private
		Class<?> clazz = openmrsTranslator.getClass();
		Field field = clazz.getDeclaredField(attributeName);
		field.setAccessible(true);
		field.set(openmrsTranslator, value);
	}
}
