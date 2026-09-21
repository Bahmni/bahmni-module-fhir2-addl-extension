package org.bahmni.module.fhir2addlextension.api.search.param;

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
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import java.util.HashSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

public class BahmniServiceRequestSearchParamsTest {
	
	@Test
	public void toSearchParameterMap_shouldIncludePatientReference() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam()
		    .addAnd(new ReferenceOrListParam().add(new ReferenceParam("Patient", "patient-uuid")));

		BahmniServiceRequestSearchParams params = new BahmniServiceRequestSearchParams(patientRef, null, null, null,
		    null, null, null, null, null, new HashSet<>(), new HashSet<>(), null, null);

		SearchParameterMap map = params.toSearchParameterMap();

		assertThat(map, notNullValue());
		assertThat(map.getParameters(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER), hasSize(1));
	}
	
	@Test
	public void toSearchParameterMap_shouldIncludeCode() {
		TokenAndListParam code = new TokenAndListParam()
		    .addAnd(new TokenOrListParam().add("system", "code-value"));

		BahmniServiceRequestSearchParams params = new BahmniServiceRequestSearchParams(null, code, null, null, null,
		    null, null, null, null, new HashSet<>(), new HashSet<>(), null, null);

		SearchParameterMap map = params.toSearchParameterMap();

		assertThat(map, notNullValue());
		assertThat(map.getParameters(FhirConstants.CODED_SEARCH_HANDLER), hasSize(1));
	}
	
	@Test
	public void toSearchParameterMap_shouldIncludeEncounterReference() {
		ReferenceAndListParam encounterRef = new ReferenceAndListParam()
		    .addAnd(new ReferenceOrListParam().add(new ReferenceParam("Encounter", "encounter-uuid")));

		BahmniServiceRequestSearchParams params = new BahmniServiceRequestSearchParams(null, null, encounterRef, null,
		    null, null, null, null, null, new HashSet<>(), new HashSet<>(), null, null);

		SearchParameterMap map = params.toSearchParameterMap();

		assertThat(map, notNullValue());
		assertThat(map.getParameters(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER), hasSize(1));
	}
	
	@Test
	public void toSearchParameterMap_shouldIncludeParticipantReference() {
		ReferenceAndListParam participantRef = new ReferenceAndListParam()
		    .addAnd(new ReferenceOrListParam().add(new ReferenceParam("Practitioner", "practitioner-uuid")));

		BahmniServiceRequestSearchParams params = new BahmniServiceRequestSearchParams(null, null, null,
		    participantRef, null, null, null, null, null, new HashSet<>(), new HashSet<>(), null, null);

		SearchParameterMap map = params.toSearchParameterMap();

		assertThat(map, notNullValue());
		assertThat(map.getParameters(FhirConstants.PARTICIPANT_REFERENCE_SEARCH_HANDLER), hasSize(1));
	}
	
	@Test
	public void toSearchParameterMap_shouldIncludeCategory() {
		ReferenceAndListParam category = new ReferenceAndListParam()
		    .addAnd(new ReferenceOrListParam().add(new ReferenceParam("category-uuid")));

		BahmniServiceRequestSearchParams params = new BahmniServiceRequestSearchParams(null, null, null, null, category,
		    null, null, null, null, new HashSet<>(), new HashSet<>(), null, null);

		SearchParameterMap map = params.toSearchParameterMap();

		assertThat(map, notNullValue());
		assertThat(map.getParameters(FhirConstants.CATEGORY_SEARCH_HANDLER), hasSize(1));
	}
	
	@Test
	public void toSearchParameterMap_shouldIncludeBasedOnReference() {
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam()
		    .addAnd(new ReferenceOrListParam().add(new ReferenceParam("ServiceRequest", "based-on-uuid")));

		BahmniServiceRequestSearchParams params = new BahmniServiceRequestSearchParams(null, null, null, null, null,
		    basedOnRef, null, null, null, new HashSet<>(), new HashSet<>(), null, null);

		SearchParameterMap map = params.toSearchParameterMap();

		assertThat(map, notNullValue());
		assertThat(map.getParameters(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER), hasSize(1));
	}
	
	@Test
	public void toSearchParameterMap_shouldIncludeLocationReference() {
		ReferenceAndListParam locationRef = new ReferenceAndListParam()
		    .addAnd(new ReferenceOrListParam().add(new ReferenceParam("Location", "location-uuid")));

		BahmniServiceRequestSearchParams params = new BahmniServiceRequestSearchParams(null, null, null, null, null,
		    null, null, null, null, new HashSet<>(), new HashSet<>(), locationRef, null);

		SearchParameterMap map = params.toSearchParameterMap();

		assertThat(map, notNullValue());
		assertThat(map.getParameters(BahmniFhirConstants.ORDER_LOCATION_SEARCH_HANDLER), hasSize(1));
	}
	
	@Test
	public void toSearchParameterMap_shouldIncludeOccurrenceDate() {
		DateRangeParam occurrence = new DateRangeParam();

		BahmniServiceRequestSearchParams params = new BahmniServiceRequestSearchParams(null, null, null, null, null,
		    null, occurrence, null, null, new HashSet<>(), new HashSet<>(), null, null);

		SearchParameterMap map = params.toSearchParameterMap();

		assertThat(map, notNullValue());
		assertThat(map.getParameters(FhirConstants.DATE_RANGE_SEARCH_HANDLER), hasSize(1));
	}
	
	@Test
	public void toSearchParameterMap_shouldIncludeSortSpec() {
		SortSpec sort = new SortSpec("name");

		BahmniServiceRequestSearchParams params = new BahmniServiceRequestSearchParams(null, null, null, null, null,
		    null, null, null, null, new HashSet<>(), new HashSet<>(), null, sort);

		SearchParameterMap map = params.toSearchParameterMap();

		assertThat(map, notNullValue());
		assertThat(map.getSortSpec(), notNullValue());
	}
	
	@Test
	public void toSearchParameterMap_shouldHandleAllParameters() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam()
		    .addAnd(new ReferenceOrListParam().add(new ReferenceParam("Patient", "patient-uuid")));
		TokenAndListParam code = new TokenAndListParam()
		    .addAnd(new TokenOrListParam().add("system", "code"));
		ReferenceAndListParam encounterRef = new ReferenceAndListParam()
		    .addAnd(new ReferenceOrListParam().add(new ReferenceParam("Encounter", "encounter-uuid")));
		ReferenceAndListParam participantRef = new ReferenceAndListParam()
		    .addAnd(new ReferenceOrListParam().add(new ReferenceParam("Practitioner", "prac-uuid")));
		ReferenceAndListParam category = new ReferenceAndListParam()
		    .addAnd(new ReferenceOrListParam().add(new ReferenceParam("category-uuid")));
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam()
		    .addAnd(new ReferenceOrListParam().add(new ReferenceParam("ServiceRequest", "based-on-uuid")));
		DateRangeParam occurrence = new DateRangeParam();
		ReferenceAndListParam locationRef = new ReferenceAndListParam()
		    .addAnd(new ReferenceOrListParam().add(new ReferenceParam("Location", "location-uuid")));
		SortSpec sort = new SortSpec("name");
		HashSet<Include> includes = new HashSet<>();
		includes.add(new Include("ServiceRequest:patient"));

		BahmniServiceRequestSearchParams params = new BahmniServiceRequestSearchParams(patientRef, code, encounterRef,
		    participantRef, category, basedOnRef, occurrence, null, null, includes, new HashSet<>(), locationRef,
		    sort);

		SearchParameterMap map = params.toSearchParameterMap();

		assertThat(map, notNullValue());
		assertThat(map.getSortSpec(), notNullValue());
	}
	
	@Test
	public void constructor_shouldInitializeWithDefaultValues() {
		BahmniServiceRequestSearchParams params = new BahmniServiceRequestSearchParams();
		
		SearchParameterMap map = params.toSearchParameterMap();
		
		assertThat(map, notNullValue());
	}
}
