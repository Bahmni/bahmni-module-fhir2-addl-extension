/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. Bahmni amd OpenMRS are also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright 2025 (C) Thoughtworks Inc.
 */

package org.bahmni.module.fhir2addlextension.api.providers;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2addlextension.api.domain.ConsultationBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.openmrs.module.fhir2.api.FhirConditionService;
import org.openmrs.module.fhir2.api.FhirEncounterService;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.providers.util.FhirProviderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component("consultationBundleFhirR4ResourceProvider")
@R4Provider
public class ConsultationBundleFhirR4ResourceProvider implements IResourceProvider {
	
	private FhirEncounterService encounterService;
	
	@Autowired
	public ConsultationBundleFhirR4ResourceProvider(FhirEncounterService encounterService) {
		this.encounterService = encounterService;
	}
	
	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return ConsultationBundle.class;
	}
	
	@Create
    public MethodOutcome createBundle(@ResourceParam ConsultationBundle bundle) {
		/**
		 * We want to handle the whole request as a transaction.
		 * - TODO: Define FHIR IG for the bundle.
		 * - TODO: validate bundle type = transaction
		 * - ensure all entries contain resources and request element
		 * - resource references: can have server side references e.g. Patient/ABC12345
		 */

		/**
		 * TODO: The entire processing must be atomic. We can't use @trandsactional annotation
		 * on the method to define transaction bounday in the provider, as spring creates a proxy around it,
		 * and the HAPI FHIR servlet reflection method invocation will fail.
		 * One way to solve is - create another service where the processing can be done.
		 */

        //For all entries must have resources and request elements
        List<Bundle.BundleEntryComponent> emptyResouceEntries = bundle.getEntry()
                .stream().filter(entry -> !(entry.hasResource() && entry.hasRequest()))
                .collect(Collectors.toList());
        if (!emptyResouceEntries.isEmpty()) {
            throw new InvalidRequestException("Bundle entry must have resource & request defined");
        }

        List<Bundle.BundleEntryComponent> encounterEntries = bundle.getEntry()
                .stream().filter(entry -> entry.hasResource() && entry.getResource().getResourceType().equals(ResourceType.Encounter))
                .collect(Collectors.toList());
        if (encounterEntries.isEmpty()) {
            throw new InvalidRequestException("Bundle must accompany an encounter resource");
        }

        Bundle responseBundle = new ConsultationBundle();
        //we expect additional visit resource (new visit to be created) to come in as well
        //therefore we must process the FHIR Encounters in order - to OMRS visit first, then OMRS encounter
        responseBundle.addEntry(processEncounter(encounterEntries.get(0)));
        //run through the rest of the entries in the bundle. process them in the order of dependencies. e.g obs can be part of another obs or other resources
        //delegate processing to resource specific OpenMRS Fhir Services such as observationFhirResourceProcessor, conditionFhirResourceProcessor etc


        /**
         * Consideration:
         * 1. We can restrict adding any resource def or operations on the patient resource.
         * 2. We inject respective openmrs fhirResource providers and delegate to them, however
         * there may be a way, we can get hold of all the resource providers, through
         * Spring application context .. e.g. ctx.getBeansOfType(IResourceProvider.class) and using either reflection
         * or the wayRestfulServer.handleRequest(..) method does, by determining registered resource providers handler method
         * through determineResourceMethod(). e.g binding.getMethod().invoke(binding.getProvider(), theMethodParams)
         * this approach will save us a lot of boilerplate code
         */

        MethodOutcome methodOutcome = new MethodOutcome();
        methodOutcome.setCreated(true);
        methodOutcome.setResource(responseBundle);
        return methodOutcome;
    }
	
	private Bundle.BundleEntryComponent processEncounter(Bundle.BundleEntryComponent bundleEntryComponent) {
		//TODO not to be hardcoded. Must decide whether PUT or POST request and delegate to either create or update
		MethodOutcome encounterCreationOutcome = FhirProviderUtils.buildCreate(encounterService
		        .create((Encounter) bundleEntryComponent.getResource()));
		Bundle.BundleEntryComponent encounterEntry = new Bundle.BundleEntryComponent();
		encounterEntry.setResource((Resource) encounterCreationOutcome.getResource());
		Bundle.BundleEntryResponseComponent response = new Bundle.BundleEntryResponseComponent();
		if (bundleEntryComponent.getRequest().getMethod().equals(Bundle.HTTPVerb.POST)) {
			response.setStatus("201");
		} else {
			response.setStatus("200");
		}
		encounterEntry.setResponse(response);
		return encounterEntry;
		
	}
}
