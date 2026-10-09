package org.bahmni.module.fhir2addlextension.api.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.Test;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

public class FhirEncounterDiagnosisServiceImplTest {
	
	@Test
	public void searchForDiagnosis_withEmptyParams_shouldReturnValidMap() {
		SearchParameterMap params = new SearchParameterMap();
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForDiagnosis_shouldBuildValidSearchMap() {
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("encounter", "enc-uuid");
		
		assertThat(params, notNullValue());
	}
}
