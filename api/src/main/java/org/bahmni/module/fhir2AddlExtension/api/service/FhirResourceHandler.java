package org.bahmni.module.fhir2AddlExtension.api.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

import java.util.Optional;

public interface FhirResourceHandler {
	
	Optional<IResourceProvider> getResourceProvider(Class clazz);
	
	Optional<MethodOutcome> invokeResourceProvider(Bundle.HTTPVerb httpVerb, Resource resource);
	
	Optional<MethodOutcome> invokeResourceProvider(Bundle.HTTPVerb httpVerb, Resource resource,
	        IResourceProvider resourceProvider);
}
