package org.bahmni.module.fhir2addlextension.api.search.param;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.StringParam;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.junit.Test;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

public class BahmniTaskSearchParamsTest {
	
	@Test
	public void toSearchParameterMap_shouldIncludeEncounterReferenceWhenSet() {
		ReferenceAndListParam encounterRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Encounter", "encounter-uuid-123")));
		
		BahmniTaskSearchParams params = new BahmniTaskSearchParams();
		params.setEncounterReference(encounterRef);
		
		SearchParameterMap map = params.toSearchParameterMap();
		
		assertThat(map.getParameters(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER), not(empty()));
	}
	
	@Test
	public void toSearchParameterMap_shouldIncludeNameWhenSet() {
		StringAndListParam name = new StringAndListParam().addAnd(new StringOrListParam().add(new StringParam(
		        "PatientHistory")));
		
		BahmniTaskSearchParams params = new BahmniTaskSearchParams();
		params.setName(name);
		
		SearchParameterMap map = params.toSearchParameterMap();
		
		assertThat(map.getParameters(BahmniFhirConstants.NAME_SEARCH_HANDLER), not(empty()));
	}
	
	@Test
	public void toSearchParameterMap_shouldNotIncludeEncounterHandlerWhenNotSet() {
		BahmniTaskSearchParams params = new BahmniTaskSearchParams();
		
		SearchParameterMap map = params.toSearchParameterMap();
		
		assertThat(map.getParameters(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER), empty());
	}
	
	@Test
	public void toSearchParameterMap_shouldNotIncludeNameHandlerWhenNotSet() {
		BahmniTaskSearchParams params = new BahmniTaskSearchParams();
		
		SearchParameterMap map = params.toSearchParameterMap();
		
		assertThat(map.getParameters(BahmniFhirConstants.NAME_SEARCH_HANDLER), empty());
	}
	
	@Test
	public void toSearchParameterMap_shouldReturnNonNullMap() {
		BahmniTaskSearchParams params = new BahmniTaskSearchParams();
		
		SearchParameterMap map = params.toSearchParameterMap();
		
		assertThat(map, notNullValue());
	}
}
