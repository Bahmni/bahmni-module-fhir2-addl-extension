package org.bahmni.module.fhir2addlextension.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import org.openmrs.api.APIAuthenticationException;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

@Interceptor
@Component("fhirAuthExceptionHandlingInterceptor")
public class FhirAuthExceptionHandlingInterceptor {
	
	@Hook(Pointcut.SERVER_PRE_PROCESS_OUTGOING_EXCEPTION)
	public BaseServerResponseException handleException(RequestDetails requestDetails, Throwable exception,
	        HttpServletRequest request) throws ServletException {
		Throwable cause = exception;
		while (cause != null) {
			if (cause instanceof APIAuthenticationException) {
				return new ForbiddenOperationException(cause.getMessage());
			}
			cause = cause.getCause();
		}
		return null;
	}
}
