/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. Bahmni amd OpenMRS are also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright 2025 (C) Thoughtworks Inc.
 */

package org.bahmni.module.fhir2AddlExtension.api.providers;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2AddlExtension.api.domain.ConsultationBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.openmrs.module.fhir2.api.FhirEncounterService;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.providers.util.FhirProviderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
        //TODO validate whether bundle type is transaction (later)

        //Rule check: to be defined in IG
        // For all entries must contain resources, and have request elements
        //resource references: can have server side references e.g. Patient/ABC12345
        List<Bundle.BundleEntryComponent> emptyResouceEntries = bundle.getEntry()
                .stream().filter(entry -> !(entry.hasResource() && entry.hasRequest()))
                .collect(Collectors.toList());
        if (!emptyResouceEntries.isEmpty()) {
            throw new InvalidRequestException("Bundle entry must have resource && request defined");
        }

        List<Resource> encounters = bundle.getEntry()
                .stream().filter(entry -> entry.hasResource() && entry.getResource().getResourceType().equals(ResourceType.Encounter))
                .map(entry -> entry.getResource())
                .collect(Collectors.toList());
        if (encounters.isEmpty()) {
            throw new InvalidRequestException("Bundle must accompany an encounter resource");
        }

        //we expect additional visit resource to come in as well
        //order the encounters and create them - visit first
        //TODO not to be hardcoded. Must decide whether PUT or POST request and delegate to either create or update
        MethodOutcome encounterCreationOutcome = FhirProviderUtils.buildCreate(encounterService.create((Encounter) encounters.get(0)));
        //TODO code here
        //run through the rest of the entries in the bundle. process them in the order of dependencies. e.g obs can be part of another obs or other resources
        //delegate processing to resource specific OpenMRS Fhir Services such as observationFhirResourceProcessor, conditionFhirResourceProcessor etc


        /**
         * Consideration:
         * 1. We can restrict adding any resource def or operations on the patient resource.
         * 2. We inject respective openmrs fhirresource providers and delegate to them, however
         * there may be a way, we can get hold of all the resource providers, through
         * Spring applciation context .. e.g. ctx.getBeansOfType(IResourceProvider.class) and using either reflection
         * or the wayRestfulServer.handleRequest(..) method does, by determining registered resoruce providers handler method
         * through determineResourceMethod(). this approach will save us a lot of boiler plate code
         */

        //the following code is temporary
        MethodOutcome methodOutcome = new MethodOutcome();
        methodOutcome.setCreated(true);
        Resource newBundle = new ConsultationBundle();
        if (newBundle != null) {
            methodOutcome.setId(newBundle.getIdElement());
            methodOutcome.setResource(newBundle);
        }
        return methodOutcome;
    }
}
