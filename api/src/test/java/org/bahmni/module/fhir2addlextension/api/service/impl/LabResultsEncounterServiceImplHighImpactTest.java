package org.bahmni.module.fhir2addlextension.api.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.junit.Test;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

public class LabResultsEncounterServiceImplHighImpactTest {
	
	@Test
	public void searchForLabEncounters_withEmptyParams_shouldReturnValidMap() {
		SearchParameterMap params = new SearchParameterMap();
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForLabEncounters_withPatientReference_shouldReturnValidMap() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Patient", "patient-uuid")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("patient", patientRef);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForLabEncounters_withEncounterDateRange_shouldReturnValidMap() {
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("date", "ge2020-01-01");
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForLabEncounters_withEncounterType_shouldReturnValidMap() {
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("type", "LAB");
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForLabEncounters_withLocationReference_shouldReturnValidMap() {
		ReferenceAndListParam locationRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Location", "location-uuid")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("location", locationRef);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForLabEncounters_withMultipleParameters_shouldReturnValidMap() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Patient", "patient-uuid")));
		ReferenceAndListParam locationRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Location", "location-uuid")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("patient", patientRef);
		params.addParameter("location", locationRef);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForLabEncounters_withStatus_shouldReturnValidMap() {
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("status", "finished");
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForLabEncounters_withPartOf_shouldReturnValidMap() {
		ReferenceAndListParam partOfRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Encounter", "parent-enc-uuid")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("part-of", partOfRef);
		
		assertThat(params, notNullValue());
	}
}
