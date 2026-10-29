package org.bahmni.module.fhir2addlextension.api.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.junit.Test;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

public class BahmniFhirTaskServiceImplNewCodeTest {
	
	@Test
	public void searchForTasks_withBasedOnParameter_shouldBuildMap() {
		ReferenceAndListParam basedOn = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("ServiceRequest", "sr-1")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOn);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForTasks_withOwnerParameter_shouldBuildMap() {
		ReferenceAndListParam owner = new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam(
		        "Practitioner", "prac-1")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.OWNER_REFERENCE_SEARCH_HANDLER, owner);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForTasks_withForParameter_shouldBuildMap() {
		ReferenceAndListParam forRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam(
		        "Patient", "patient-1")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.FOR_REFERENCE_SEARCH_HANDLER, forRef);
		
		assertThat(params, notNullValue());
	}
	
	//	@Test
	//	public void searchForTasks_withFocusParameter_shouldBuildMap() {
	//		ReferenceAndListParam focus = new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam(
	//		        "ServiceRequest", "sr-1")));
	//
	//		SearchParameterMap params = new SearchParameterMap();
	//		params.addParameter(FhirConstants.FOCUS_REFERENCE_SEARCH_HANDLER, focus);
	//
	//		assertThat(params, notNullValue());
	//	}
	
	@Test
	public void searchForTasks_withStatusParameter_shouldBuildMap() {
		TokenAndListParam status = new TokenAndListParam().addAnd(new TokenOrListParam().add(
		    "http://hl7.org/fhir/task-status", "completed"));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.STATUS_SEARCH_HANDLER, status);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForTasks_withNameParameter_shouldBuildMap() {
		StringAndListParam name = new StringAndListParam().addAnd(new StringOrListParam().add(new StringParam("task-name")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(BahmniFhirConstants.NAME_SEARCH_HANDLER, name);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForTasks_withEncounterParameter_shouldBuildMap() {
		ReferenceAndListParam encounter = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Encounter", "enc-1")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, encounter);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForTasks_withAllParameters_shouldBuildMap() {
		ReferenceAndListParam basedOn = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("ServiceRequest", "sr-1")));
		ReferenceAndListParam owner = new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam(
		        "Practitioner", "prac-1")));
		ReferenceAndListParam forRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam(
		        "Patient", "patient-1")));
		TokenAndListParam status = new TokenAndListParam().addAnd(new TokenOrListParam().add(
		    "http://hl7.org/fhir/task-status", "completed"));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOn);
		params.addParameter(FhirConstants.OWNER_REFERENCE_SEARCH_HANDLER, owner);
		params.addParameter(FhirConstants.FOR_REFERENCE_SEARCH_HANDLER, forRef);
		params.addParameter(FhirConstants.STATUS_SEARCH_HANDLER, status);
		
		assertThat(params, notNullValue());
	}
	
	//	@Test
	//	public void searchForTasks_withMultiFocusParameter_shouldHandleMultiValue() {
	//		ReferenceAndListParam focus = new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(
	//		    new ReferenceParam("Observation", "obs-1")).add(new ReferenceParam("Observation", "obs-2")));
	//
	//		SearchParameterMap params = new SearchParameterMap();
	//		params.addParameter(FhirConstants.FOCUS_REFERENCE_SEARCH_HANDLER, focus);
	//
	//		assertThat(params, notNullValue());
	//	}
	
	@Test
	public void searchForTasks_withCombinedReferencesAndStatus_shouldBuildMap() {
		ReferenceAndListParam basedOn = new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(
		    new ReferenceParam("ServiceRequest", "sr-mix-1")).add(new ReferenceParam("ServiceRequest", "sr-mix-2")));
		TokenAndListParam status = new TokenAndListParam().addAnd(new TokenOrListParam().add(
		    "http://hl7.org/fhir/task-status", "in-progress"));
		ReferenceAndListParam encounter = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Encounter", "enc-123")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOn);
		params.addParameter(FhirConstants.STATUS_SEARCH_HANDLER, status);
		params.addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, encounter);
		
		assertThat(params, notNullValue());
	}
}
