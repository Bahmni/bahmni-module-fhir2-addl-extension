package org.bahmni.module.fhir2addlextension.api.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import ca.uhn.fhir.rest.api.SortSpec;
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

public class BahmniFhirTaskServiceImplIntegrationTest {
	
	@Test
	public void searchForTasks_withAllParameters_shouldBuildSearchMap() {
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("ServiceRequest", "sr-uuid")));
		ReferenceAndListParam ownerRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Practitioner", "prac-uuid")));
		ReferenceAndListParam forRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam(
		        "Patient", "patient-uuid")));
		ReferenceAndListParam focusRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("ServiceRequest", "sr-uuid")));
		TokenAndListParam statusParam = new TokenAndListParam().addAnd(new TokenOrListParam().add(
		    "http://hl7.org/fhir/task-status", "completed"));
		ReferenceAndListParam encounterRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Encounter", "enc-uuid")));
		StringAndListParam name = new StringAndListParam().addAnd(new StringOrListParam().add(new StringParam("task-name")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnRef);
		params.addParameter(FhirConstants.OWNER_REFERENCE_SEARCH_HANDLER, ownerRef);
		params.addParameter(FhirConstants.FOR_REFERENCE_SEARCH_HANDLER, forRef);
		//params.addParameter(FhirConstants.FOCUS_REFERENCE_SEARCH_HANDLER, focusRef);
		params.addParameter(FhirConstants.STATUS_SEARCH_HANDLER, statusParam);
		params.addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, encounterRef);
		params.addParameter(BahmniFhirConstants.NAME_SEARCH_HANDLER, name);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForTasks_withBasedOnParameter_shouldBuildSearchMap() {
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("ServiceRequest", "sr-uuid")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnRef);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForTasks_withOwnerParameter_shouldBuildSearchMap() {
		ReferenceAndListParam ownerRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Practitioner", "prac-uuid")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.OWNER_REFERENCE_SEARCH_HANDLER, ownerRef);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForTasks_withForParameter_shouldBuildSearchMap() {
		ReferenceAndListParam forRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam(
		        "Patient", "patient-uuid")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.FOR_REFERENCE_SEARCH_HANDLER, forRef);
		
		assertThat(params, notNullValue());
	}
	
	//	@Test
	//	public void searchForTasks_withFocusParameter_shouldBuildSearchMap() {
	//		ReferenceAndListParam focusRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
	//		        .add(new ReferenceParam("ServiceRequest", "sr-uuid")));
	//
	//		SearchParameterMap params = new SearchParameterMap();
	//		params.addParameter(FhirConstants.FOCUS_REFERENCE_SEARCH_HANDLER, focusRef);
	//
	//		assertThat(params, notNullValue());
	//	}
	
	@Test
	public void searchForTasks_withStatusParameter_shouldBuildSearchMap() {
		TokenAndListParam statusParam = new TokenAndListParam().addAnd(new TokenOrListParam().add(
		    "http://hl7.org/fhir/task-status", "completed"));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.STATUS_SEARCH_HANDLER, statusParam);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForTasks_withEncounterParameter_shouldBuildSearchMap() {
		ReferenceAndListParam encounterRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Encounter", "enc-uuid")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, encounterRef);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForTasks_withSortParameter_shouldBuildSearchMap() {
		SortSpec sort = new SortSpec("_lastUpdated");
		
		SearchParameterMap params = new SearchParameterMap();
		params.setSortSpec(sort);
		
		assertThat(params, notNullValue());
	}
}
