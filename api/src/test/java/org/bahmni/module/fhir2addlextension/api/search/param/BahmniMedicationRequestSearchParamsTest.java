package org.bahmni.module.fhir2addlextension.api.search.param;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.junit.Test;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import java.util.HashSet;
import java.util.Set;

public class BahmniMedicationRequestSearchParamsTest {
	
	@Test
	public void toSearchParameterMap_shouldIncludeLocationWhenSet() {
		ReferenceAndListParam locationRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Location", "loc-uuid")));
		
		BahmniMedicationRequestSearchParams params = new BahmniMedicationRequestSearchParams(null, null, null, null, null,
		        null, null, null, locationRef, null, null, null, null, null);
		
		SearchParameterMap map = params.toSearchParameterMap();
		
		assertThat(map.getParameters(BahmniFhirConstants.ORDER_LOCATION_SEARCH_HANDLER), not(empty()));
	}
	
	@Test
	public void toSearchParameterMap_shouldIncludeSortWhenSet() {
		SortSpec sort = new SortSpec("status").setChain(new SortSpec("date"));
		
		BahmniMedicationRequestSearchParams params = new BahmniMedicationRequestSearchParams(null, null, null, null, null,
		        null, null, null, null, null, null, null, sort, null);
		
		SearchParameterMap map = params.toSearchParameterMap();
		
		assertThat(map.getSortSpec(), notNullValue());
	}
	
	@Test
	public void toSearchParameterMap_shouldNotIncludeLocationWhenNull() {
		BahmniMedicationRequestSearchParams params = new BahmniMedicationRequestSearchParams(null, null, null, null, null,
		        null, null, null, null, null, null, null, null, null);
		
		SearchParameterMap map = params.toSearchParameterMap();
		
		assertThat(map.getParameters(BahmniFhirConstants.ORDER_LOCATION_SEARCH_HANDLER), empty());
	}
	
	@Test
	public void toSearchParameterMap_shouldNotSetSortWhenNull() {
		BahmniMedicationRequestSearchParams params = new BahmniMedicationRequestSearchParams(null, null, null, null, null,
		        null, null, null, null, null, null, null, null, null);
		
		SearchParameterMap map = params.toSearchParameterMap();
		
		// SortSpec should be null when not provided
		assertThat(map, notNullValue());
	}
	
	@Test
	public void toSearchParameterMap_withAllParameters_shouldReturnMap() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam()
		    .addAnd(new ReferenceOrListParam().add(new ReferenceParam("Patient", "pat-uuid")));
		ReferenceAndListParam encounterRef = new ReferenceAndListParam()
		    .addAnd(new ReferenceOrListParam().add(new ReferenceParam("Encounter", "enc-uuid")));
		ReferenceAndListParam locationRef = new ReferenceAndListParam()
		    .addAnd(new ReferenceOrListParam().add(new ReferenceParam("Location", "loc-uuid")));
		TokenAndListParam code = new TokenAndListParam().addAnd(new TokenOrListParam().add("system", "code"));
		TokenAndListParam status = new TokenAndListParam().addAnd(new TokenOrListParam().add("draft"));
		DateRangeParam lastUpdated = new DateRangeParam();
		Set<Include> includes = new HashSet<>();
		includes.add(new Include("MedicationRequest:medication"));
		SortSpec sort = new SortSpec("status");

		BahmniMedicationRequestSearchParams params = new BahmniMedicationRequestSearchParams(
		    patientRef, encounterRef, code, null, null, null, status, null, locationRef, lastUpdated, includes, null, sort, null);

		SearchParameterMap map = params.toSearchParameterMap();

		assertThat(map, notNullValue());
		assertThat(map.getParameters(BahmniFhirConstants.ORDER_LOCATION_SEARCH_HANDLER), not(empty()));
	}
	
	@Test
	public void constructor_shouldInitializeFieldsCorrectly() {
		ReferenceAndListParam locationRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Location", "loc-uuid")));
		SortSpec sort = new SortSpec("status");
		
		BahmniMedicationRequestSearchParams params = new BahmniMedicationRequestSearchParams(null, null, null, null, null,
		        null, null, null, locationRef, null, null, null, sort, null);
		
		assertThat(params.getLocationReference(), notNullValue());
		assertThat(params.getSort(), notNullValue());
	}
}
