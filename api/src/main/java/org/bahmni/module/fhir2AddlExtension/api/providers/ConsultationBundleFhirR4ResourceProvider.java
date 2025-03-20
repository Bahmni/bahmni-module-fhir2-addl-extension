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
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component("consultationBundleFhirR4ResourceProvider")
@R4Provider
public class ConsultationBundleFhirR4ResourceProvider implements IResourceProvider {
	
	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return ConsultationBundle.class;
	}
	
	@Create
    public MethodOutcome createBundle(@ResourceParam ConsultationBundle bundle) {
        List<Resource> encounters = bundle.getEntry()
                .stream().filter(entry -> entry.hasResource() && entry.getResource().getResourceType().equals(ResourceType.Encounter))
                .map(entry -> entry.getResource())
                .collect(Collectors.toList());
        if (encounters.isEmpty()) {
            throw new InvalidRequestException("Bundle must accompany an encounter resource");
        }


        System.out.println("received bundle id: " + bundle.getId());
        System.out.println("payload:" + bundle);
        MethodOutcome methodOutcome = new MethodOutcome();
        methodOutcome.setCreated(true);
        Resource newBundle = new ConsultationBundle();
        if (newBundle != null) {
            methodOutcome.setId(newBundle.getIdElement());
            methodOutcome.setResource(newBundle);
        }
        return methodOutcome;
    }
	
	private Bundle createBundleResource(List<Resource> encounters) {
		Bundle bundle = new Bundle();
		bundle.setId("ABC");
		return bundle;
	}
}
