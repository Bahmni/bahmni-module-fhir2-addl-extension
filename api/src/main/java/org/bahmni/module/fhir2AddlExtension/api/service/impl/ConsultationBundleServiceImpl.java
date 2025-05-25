/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. Bahmni amd OpenMRS are also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright 2025 (C) Thoughtworks Inc.
 */

package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2AddlExtension.api.domain.ConsultationBundle;
import org.bahmni.module.fhir2AddlExtension.api.helper.ConsultationBundleEntriesHelper;
import org.bahmni.module.fhir2AddlExtension.api.service.ConsultationBundleService;
import org.bahmni.module.fhir2AddlExtension.api.service.FhirResourceHandler;
import org.bahmni.module.fhir2AddlExtension.api.validators.ConsultationBundleValidator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Transactional
@Slf4j
public class ConsultationBundleServiceImpl implements ConsultationBundleService {
	
	final private FhirResourceHandler resourceHandler;
	
	@Autowired
	private ConsultationBundleValidator consultationBundleValidator;
	
	@Autowired
	public ConsultationBundleServiceImpl(FhirResourceHandler resourceHandler) {
		this.resourceHandler = resourceHandler;
	}
	
	@Override
	public Bundle create(Bundle bundle) {
		/*
		  We want to handle the whole request as a transaction.
		  - TODO: Define FHIR IG for the bundle.
		  - ensure all entries contain resources and request element
		  - resource references: can have server side references e.g. Patient/ABC12345
		 */
		consultationBundleValidator.validateBundleType(bundle);
		consultationBundleValidator.validateBundleEntries(bundle);

		List<Bundle.BundleEntryComponent> bundleEntryComponents = bundle.getEntry();
		
		List<Bundle.BundleEntryComponent> orderedEntries = ConsultationBundleEntriesHelper.orderEntriesByReference(bundleEntryComponents);

		Map<String, Bundle.BundleEntryComponent> processedResourceEntryMap = new HashMap<>();
		for (Bundle.BundleEntryComponent orderedEntry : orderedEntries) {
			try {
				Bundle.BundleEntryComponent referenceResolvedEntry = ConsultationBundleEntriesHelper.resolveReferences(orderedEntry, processedResourceEntryMap);
				Optional<Bundle.BundleEntryComponent> bundleEntryComponent = createOrUpdateResource(referenceResolvedEntry);
				if (bundleEntryComponent.isPresent()) {
					processedResourceEntryMap.put(orderedEntry.getFullUrl(), bundleEntryComponent.get());
				} else {
					throw new InvalidRequestException(String.format("Could not process resource [%s]",
					    referenceResolvedEntry.getFullUrl()));
				}
			}
			catch (UndeclaredThrowableException e) {
				String errorMessage = String.format("Error occurred while processing bundle entry [%s]",
				    orderedEntry.getFullUrl());
				log.error(errorMessage, e);
				throw new InvalidRequestException(String.format("%s. %s", errorMessage, e.getUndeclaredThrowable()
				        .getCause().getMessage()));
				
			}
			catch (Exception e) {
				String errorMessage = String.format("Error occurred while processing bundle entry [%s]",
				    orderedEntry.getFullUrl());
				log.error(errorMessage, e);
				throw new InvalidRequestException(String.format("%s. %s", errorMessage, e.getMessage()));
			}
		}


		Bundle responseBundle = new ConsultationBundle();
		for(Bundle.BundleEntryComponent entry: bundleEntryComponents) {
			responseBundle.addEntry(processedResourceEntryMap.get(entry.getFullUrl()));
		}
		return responseBundle;
	}
	
	/**
	 * Consideration: 1. We can restrict adding any resource def or operations on the patient
	 * resource. 2. We inject respective openmrs fhirResource providers and delegate to them,
	 * however there may be a way, we can get hold of all the resource providers, through Spring
	 * application context .. e.g. ctx.getBeansOfType(IResourceProvider.class) and using either
	 * reflection or the wayRestfulServer.handleRequest(..) method does, by determining registered
	 * resource providers handler method through determineResourceMethod(). e.g
	 * binding.getMethod().invoke(binding.getProvider(), theMethodParams) this approach will save us
	 * a lot of boilerplate code
	 * 
	 * @return Optional<Bundle.BundleEntryComponent>
	 */
	private Optional<Bundle.BundleEntryComponent> createOrUpdateResource(Bundle.BundleEntryComponent bundleEntryComponent) {
		Bundle.HTTPVerb httpVerb = bundleEntryComponent.getRequest().getMethod();
		Resource resource = bundleEntryComponent.getResource();
		Optional<IResourceProvider> resourceProvider = resourceHandler.getResourceProvider(resource.getClass());
		if (!resourceProvider.isPresent()) {
			log.warn("Could not find resource provider for: " + resource.getClass().getName());
			return Optional.empty();
		}
		Optional<MethodOutcome> result = resourceHandler.invokeResourceProvider(httpVerb, resource);
		if (result.isPresent()) {
			Bundle.BundleEntryComponent bundleEntry = new Bundle.BundleEntryComponent();
			bundleEntry.setResource((Resource) result.get().getResource());
			Bundle.BundleEntryResponseComponent responseEntry = new Bundle.BundleEntryResponseComponent();
			//TODO - use a map of Bundle httpVerb
			if (httpVerb.equals(Bundle.HTTPVerb.POST)) {
				responseEntry.setStatus("201");
			} else {
				responseEntry.setStatus("200");
			}
			bundleEntry.setResponse(responseEntry);
			return Optional.of(bundleEntry);
		}
		log.warn(String
		        .format(
		            "Resource [%s] has an associated resource provider, but invocation of resource provider didn't respond with a method outcome",
		            resource.getClass().getName()));
		return Optional.empty();
	}
}
