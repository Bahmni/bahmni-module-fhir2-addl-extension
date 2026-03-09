package org.bahmni.module.fhir2AddlExtension.api;

import java.lang.reflect.Field;

public class TestUtils {
	
	public static void setPropertyOnObject(Object target, String attributeName, Object value) throws NoSuchFieldException,
	        IllegalAccessException {
		//TBD: unfortunately, the setters are package private
		Class<?> clazz = target.getClass();
		Field field = clazz.getDeclaredField(attributeName);
		field.setAccessible(true);
		field.set(target, value);
	}
}
