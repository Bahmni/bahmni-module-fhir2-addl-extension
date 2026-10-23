package org.bahmni.module.fhir2addlextension.interceptor;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.util.Map;

public class ApiExceptionHandlerTest {
	
	@Test
	public void shouldReturnOriginalMessageWhenMaskingIsDisabled() throws Exception {
		ApiExceptionHandler handler = new ApiExceptionHandler();
		setMaskingField(handler, false);
		
		ResponseEntity<Map<String, Object>> response = handler.handleException(new RuntimeException(
		        "Concept with name FHIR Export User Name not found"));
		
		Assert.assertEquals(500, response.getStatusCodeValue());
		Assert.assertEquals("Concept with name FHIR Export User Name not found", response.getBody().get("error"));
	}
	
	@Test
	public void shouldMaskMessageWhenMaskingIsEnabled() throws Exception {
		ApiExceptionHandler handler = new ApiExceptionHandler();
		setMaskingField(handler, true);
		
		ResponseEntity<Map<String, Object>> response = handler.handleException(new RuntimeException(
		        "Concept with name FHIR Export User Name not found"));
		
		Assert.assertEquals(500, response.getStatusCodeValue());
		Assert.assertEquals("Internal server error", response.getBody().get("error"));
	}
	
	private void setMaskingField(ApiExceptionHandler handler, boolean value) throws Exception {
		Field field = ApiExceptionHandler.class.getDeclaredField("maskExceptionMessage");
		field.setAccessible(true);
		field.setBoolean(handler, value);
	}
}
