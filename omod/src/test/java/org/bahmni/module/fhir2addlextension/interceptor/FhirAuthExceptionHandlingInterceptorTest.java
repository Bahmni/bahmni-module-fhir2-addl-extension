package org.bahmni.module.fhir2addlextension.interceptor;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.APIAuthenticationException;

import static org.junit.Assert.*;

public class FhirAuthExceptionHandlingInterceptorTest {
	
	private FhirAuthExceptionHandlingInterceptor interceptor;
	
	@Before
	public void setUp() {
		interceptor = new FhirAuthExceptionHandlingInterceptor();
	}
	
	@Test
	public void shouldConvertAPIAuthenticationExceptionToForbiddenOperationException() throws Exception {
		APIAuthenticationException authException = new APIAuthenticationException(
		        "User does not have required privilege: Get Diagnostic Report");
		
		BaseServerResponseException result = interceptor.handleException(null, authException, null);
		
		assertNotNull(result);
		assertTrue(result instanceof ForbiddenOperationException);
		assertEquals(403, result.getStatusCode());
		assertTrue(result.getMessage().contains("User does not have required privilege: Get Diagnostic Report"));
	}
	
	@Test
	public void shouldUnwrapNestedAPIAuthenticationExceptionFromCauseChain() throws Exception {
		APIAuthenticationException authException = new APIAuthenticationException("No privilege");
		RuntimeException wrapper = new RuntimeException("Wrapper", authException);
		
		BaseServerResponseException result = interceptor.handleException(null, wrapper, null);
		
		assertNotNull(result);
		assertTrue(result instanceof ForbiddenOperationException);
		assertEquals(403, result.getStatusCode());
		assertEquals("No privilege", result.getMessage());
	}
	
	@Test
	public void shouldReturnNullForNonAuthExceptions() throws Exception {
		NullPointerException npe = new NullPointerException("something broke");
		
		BaseServerResponseException result = interceptor.handleException(null, npe, null);
		
		assertNull(result);
	}
	
	@Test
	public void shouldReturnNullForExceptionWithNullCause() throws Exception {
		RuntimeException exception = new RuntimeException("no cause");
		
		BaseServerResponseException result = interceptor.handleException(null, exception, null);
		
		assertNull(result);
	}
	
	@Test
	public void shouldPreserveExceptionMessage() throws Exception {
		String message = "User must be authenticated";
		APIAuthenticationException authException = new APIAuthenticationException(message);
		
		BaseServerResponseException result = interceptor.handleException(null, authException, null);
		
		assertNotNull(result);
		assertTrue(result.getMessage().contains(message));
	}
	
	@Test
	public void shouldHandleDeeplyNestedCauseChain() throws Exception {
		APIAuthenticationException authException = new APIAuthenticationException("deep cause");
		Exception level1 = new Exception("level1", authException);
		Exception level2 = new Exception("level2", level1);
		RuntimeException level3 = new RuntimeException("level3", level2);
		
		BaseServerResponseException result = interceptor.handleException(null, level3, null);
		
		assertNotNull(result);
		assertTrue(result instanceof ForbiddenOperationException);
		assertEquals("deep cause", result.getMessage());
	}
}
