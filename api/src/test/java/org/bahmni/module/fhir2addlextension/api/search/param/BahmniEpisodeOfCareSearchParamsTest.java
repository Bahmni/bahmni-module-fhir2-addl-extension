package org.bahmni.module.fhir2addlextension.api.search.param;

import org.junit.Test;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;

public class BahmniEpisodeOfCareSearchParamsTest {
	
	@Test
	public void toSearchParameterMap_shouldReturnValidMap() {
		BahmniEpisodeOfCareSearchParams params = new BahmniEpisodeOfCareSearchParams();
		SearchParameterMap map = params.toSearchParameterMap();
		
		assertThat(map, notNullValue());
	}
	
	@Test
	public void equals_shouldReturnTrueForIdenticalInstances() {
		BahmniEpisodeOfCareSearchParams params1 = new BahmniEpisodeOfCareSearchParams();
		BahmniEpisodeOfCareSearchParams params2 = new BahmniEpisodeOfCareSearchParams();
		
		assertThat(params1.equals(params2), equalTo(true));
	}
	
	@Test
	public void hashCode_shouldBeConsistent() {
		BahmniEpisodeOfCareSearchParams params1 = new BahmniEpisodeOfCareSearchParams();
		BahmniEpisodeOfCareSearchParams params2 = new BahmniEpisodeOfCareSearchParams();
		
		assertThat(params1.hashCode(), equalTo(params2.hashCode()));
	}
}
