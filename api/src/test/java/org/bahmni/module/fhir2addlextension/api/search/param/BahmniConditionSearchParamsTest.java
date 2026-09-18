package org.bahmni.module.fhir2addlextension.api.search.param;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import org.junit.Test;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public class BahmniConditionSearchParamsTest {
	
	@Test
	public void toSearchParameterMap_shouldReturnValidMap() {
		BahmniConditionSearchParams params = new BahmniConditionSearchParams();
		SearchParameterMap map = params.toSearchParameterMap();
		
		assertThat(map, notNullValue());
	}
	
	@Test
	public void equals_shouldReturnTrueForIdenticalInstances() {
		BahmniConditionSearchParams params1 = new BahmniConditionSearchParams();
		BahmniConditionSearchParams params2 = new BahmniConditionSearchParams();
		
		assertThat(params1.equals(params2), org.hamcrest.Matchers.equalTo(true));
	}
	
	@Test
	public void hashCode_shouldBeConsistent() {
		BahmniConditionSearchParams params1 = new BahmniConditionSearchParams();
		BahmniConditionSearchParams params2 = new BahmniConditionSearchParams();
		
		assertThat(params1.hashCode(), org.hamcrest.Matchers.equalTo(params2.hashCode()));
	}
}
