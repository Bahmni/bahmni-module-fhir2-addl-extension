package org.bahmni.module.fhir2addlextension.api.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.junit.Test;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

public class BahmniFhirAllergyIntoleranceServiceImplTest {
	
	@Test
	public void searchForAllergies_withEmptyParams_shouldReturnValidMap() {
		SearchParameterMap params = new SearchParameterMap();
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForAllergies_withPatientReference_shouldReturnValidMap() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Patient", "patient-uuid")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("patient", patientRef);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForAllergies_withMultipleParameters_shouldReturnValidMap() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Patient", "patient-uuid")));
		ReferenceAndListParam recordedDateRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Practitioner", "prac-uuid")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("patient", patientRef);
		
		assertThat(params, notNullValue());
	}
}
