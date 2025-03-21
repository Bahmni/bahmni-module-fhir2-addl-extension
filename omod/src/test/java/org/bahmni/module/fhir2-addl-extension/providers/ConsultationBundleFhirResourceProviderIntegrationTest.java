/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. Bahmni amd OpenMRS are also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright 2025 (C) Thoughtworks Inc.
 */

package org.bahmni.module.fhir2addlextension.providers;

import lombok.AccessLevel;
import lombok.Getter;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.Ignore;
import org.junit.Test;
import org.bahmni.module.fhir2addlextension.api.providers.ConsultationBundleFhirR4ResourceProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.openmrs.module.fhir2.api.util.GeneralUtils.inputStreamToString;

@Ignore("Need to work with OMRS for the improvement on the BaseFHIRR4IntegrationTest class. Right now the code is copied and modified locally")
public class ConsultationBundleFhirResourceProviderIntegrationTest extends BahmniBaseFhirR4IntegrationTest<ConsultationBundleFhirR4ResourceProvider, Bundle> {
	
	private static final String BUNDLE_JSON_CREATE_ENCOUNTER_PATH = "consultation_bundle_create_encounter.json";
	
	@Autowired
	@Getter(AccessLevel.PUBLIC)
	private ConsultationBundleFhirR4ResourceProvider resourceProvider;
	
	@Test
    public void shouldCreateEncounterFromBundleAsJson() throws Exception {
        String jsonEncounter;
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(BUNDLE_JSON_CREATE_ENCOUNTER_PATH)) {
            Objects.requireNonNull(is);
            jsonEncounter = inputStreamToString(is, UTF_8);
        }

//        MockHttpServletResponse response = post("/Encounter").accept(FhirMediaTypes.JSON).jsonContent(jsonEncounter).go();
//
//        assertThat(response, isCreated());
//        assertThat(response.getContentType(), is((FhirMediaTypes.JSON.toString())));
//        assertThat(response.getContentAsString(), notNullValue());
//
//        Encounter encounter = readResponse(response);
//
//        assertThat(encounter, notNullValue());
//        assertThat(encounter.getIdElement().getIdPart(), notNullValue());
//        assertThat(encounter.getStatus().getDisplay(), notNullValue());
//        assertThat(encounter.getMeta().getTag().get(0).getSystem(), equalTo(FhirConstants.OPENMRS_FHIR_EXT_ENCOUNTER_TAG));
//        assertThat(encounter.getType().get(0).getCoding().get(0).getSystem(),
//                equalTo(FhirConstants.ENCOUNTER_TYPE_SYSTEM_URI));
//        assertThat(encounter.getSubject().getType(), equalTo("Patient"));
//        assertThat(encounter.getParticipant().get(0).getIndividual().getType(), equalTo("Practitioner"));
//        assertThat(encounter.getPeriod().getStart(), notNullValue());
//        assertThat(encounter.getLocation().get(0).getLocation().getDisplay(), equalTo("Unknown Location"));
//
//        response = get("/Encounter/" + encounter.getIdElement().getIdPart()).accept(FhirMediaTypes.JSON).go();
//
//        assertThat(response, isOk());
//
//        Encounter newEncounter = readResponse(response);
//
//        assertThat(newEncounter.getId(), equalTo(encounter.getId()));
    }
}
