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
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.bahmni.module.fhir2AddlExtension.api.domain.ConsultationBundle;
import org.bahmni.module.fhir2AddlExtension.api.service.ConsultationBundleService;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.openmrs.module.fhir2.api.FhirEncounterService;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.providers.util.FhirProviderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component("consultationBundleFhirR4ResourceProvider")
@R4Provider
public class ConsultationBundleFhirR4ResourceProvider implements IResourceProvider {
	
	private ConsultationBundleService consultationBundleService;
	
	@Autowired
	public ConsultationBundleFhirR4ResourceProvider(FhirEncounterService encounterService,
	    ConsultationBundleService consultationBundleService) {
		this.consultationBundleService = consultationBundleService;
	}
	
	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return ConsultationBundle.class;
	}
	
	@Create
	public MethodOutcome createConsultation(@ResourceParam ConsultationBundle bundle) {
		Bundle responseBundle = consultationBundleService.create(bundle);
		return FhirProviderUtils.buildCreate(responseBundle);
	}
	
	@Read
	public ConsultationBundle getConsultationByUuid(@IdParam @Nonnull IdType encounterUuid) {
		return null;
	}
}
