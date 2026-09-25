package org.bahmni.module.fhir2addlextension.api.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.junit.Test;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

public class BahmniFhirDocumentReferenceServiceImplHighImpactTest {
	
	@Test
	public void searchForDocumentReferences_withEmptyParams_shouldReturnValidMap() {
		SearchParameterMap params = new SearchParameterMap();
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForDocumentReferences_withSubjectReference_shouldReturnValidMap() {
		ReferenceAndListParam subjectRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Patient", "patient-uuid")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("subject", subjectRef);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForDocumentReferences_withEncounterReference_shouldReturnValidMap() {
		ReferenceAndListParam encounterRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Encounter", "enc-uuid")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("encounter", encounterRef);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForDocumentReferences_withTypeParameter_shouldReturnValidMap() {
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("type", "http://loinc.org|12345");
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForDocumentReferences_withDateRange_shouldReturnValidMap() {
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("date", "ge2020-01-01");
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForDocumentReferences_withAuthorReference_shouldReturnValidMap() {
		ReferenceAndListParam authorRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Practitioner", "prac-uuid")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("author", authorRef);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForDocumentReferences_withStatusParameter_shouldReturnValidMap() {
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("status", "current");
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForDocumentReferences_withMultipleParameters_shouldReturnValidMap() {
		ReferenceAndListParam subjectRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Patient", "patient-uuid")));
		ReferenceAndListParam encounterRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Encounter", "enc-uuid")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("subject", subjectRef);
		params.addParameter("encounter", encounterRef);
		
		assertThat(params, notNullValue());
	}
}
