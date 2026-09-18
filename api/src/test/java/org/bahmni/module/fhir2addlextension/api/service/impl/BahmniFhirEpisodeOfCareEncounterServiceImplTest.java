package org.bahmni.module.fhir2addlextension.api.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.junit.Test;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

public class BahmniFhirEpisodeOfCareEncounterServiceImplTest {
	
	@Test
	public void searchForEpisodeOfCareEncounters_withEmptyParams_shouldReturnValidMap() {
		SearchParameterMap params = new SearchParameterMap();
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForEpisodeOfCareEncounters_withEpisodeOfCareReference_shouldReturnValidMap() {
		ReferenceAndListParam episodeOfCareRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("EpisodeOfCare", "eoc-uuid")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("episodeOfCare", episodeOfCareRef);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void searchForEpisodeOfCareEncounters_withEncounterReference_shouldReturnValidMap() {
		ReferenceAndListParam encounterRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Encounter", "enc-uuid")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("encounter", encounterRef);
		
		assertThat(params, notNullValue());
	}
}
