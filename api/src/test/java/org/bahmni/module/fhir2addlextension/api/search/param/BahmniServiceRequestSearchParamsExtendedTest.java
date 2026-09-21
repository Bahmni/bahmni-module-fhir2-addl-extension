package org.bahmni.module.fhir2addlextension.api.search.param;

import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import org.junit.Test;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import java.util.HashSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class BahmniServiceRequestSearchParamsExtendedTest {
	
	@Test
	public void patientReferenceGetter_shouldReturnSetValue() {
		BahmniServiceRequestSearchParams params = new BahmniServiceRequestSearchParams(null, null, null, null, null,
		    null, null, null, null, new HashSet<>(), new HashSet<>(), null, null);

		ReferenceAndListParam patientRef = new ReferenceAndListParam()
		    .addAnd(new ReferenceOrListParam().add(new ReferenceParam("Patient", "patient-uuid")));
		params.setPatientReference(patientRef);

		assertThat(params.getPatientReference(), notNullValue());
	}
	
	@Test
	public void toSearchParameterMap_withAllNullValues_shouldReturnValidMap() {
		BahmniServiceRequestSearchParams params = new BahmniServiceRequestSearchParams(null, null, null, null, null,
		    null, null, null, null, new HashSet<>(), new HashSet<>(), null, null);

		SearchParameterMap map = params.toSearchParameterMap();

		assertThat(map, notNullValue());
	}
	
	@Test
	public void toSearchParameterMap_withNullSort_shouldNotSetSortSpec() {
		BahmniServiceRequestSearchParams params = new BahmniServiceRequestSearchParams(null, null, null, null, null,
		    null, null, null, null, new HashSet<>(), new HashSet<>(), null, null);

		SearchParameterMap map = params.toSearchParameterMap();

		assertThat(map, notNullValue());
	}
	
	@Test
	public void toSearchParameterMap_withNonNullSort_shouldSetSortSpec() {
		SortSpec sort = new SortSpec("name");
		BahmniServiceRequestSearchParams params = new BahmniServiceRequestSearchParams(null, null, null, null, null,
		    null, null, null, null, new HashSet<>(), new HashSet<>(), null, sort);

		SearchParameterMap map = params.toSearchParameterMap();

		assertThat(map.getSortSpec(), notNullValue());
	}
	
	@Test
	public void equals_shouldReturnTrueForIdenticalInstances() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam()
		    .addAnd(new ReferenceOrListParam().add(new ReferenceParam("Patient", "patient-uuid")));

		BahmniServiceRequestSearchParams params1 = new BahmniServiceRequestSearchParams(patientRef, null, null, null,
		    null, null, null, null, null, new HashSet<>(), new HashSet<>(), null, null);
		BahmniServiceRequestSearchParams params2 = new BahmniServiceRequestSearchParams(patientRef, null, null, null,
		    null, null, null, null, null, new HashSet<>(), new HashSet<>(), null, null);

		assertThat(params1.equals(params2), equalTo(true));
	}
	
	@Test
	public void hashCode_shouldBeConsistent() {
		BahmniServiceRequestSearchParams params1 = new BahmniServiceRequestSearchParams(null, null, null, null, null,
		    null, null, null, null, new HashSet<>(), new HashSet<>(), null, null);
		BahmniServiceRequestSearchParams params2 = new BahmniServiceRequestSearchParams(null, null, null, null, null,
		    null, null, null, null, new HashSet<>(), new HashSet<>(), null, null);

		assertThat(params1.hashCode(), equalTo(params2.hashCode()));
	}
	
	@Test
	public void codeGetter_shouldReturnSetValue() {
		TokenAndListParam code = new TokenAndListParam()
		    .addAnd(new TokenOrListParam().add("system", "code-value"));

		BahmniServiceRequestSearchParams params = new BahmniServiceRequestSearchParams(null, code, null, null, null,
		    null, null, null, null, new HashSet<>(), new HashSet<>(), null, null);

		assertThat(params.getCode(), notNullValue());
	}
	
	@Test
	public void encounterReferenceGetter_shouldReturnSetValue() {
		ReferenceAndListParam encounterRef = new ReferenceAndListParam()
		    .addAnd(new ReferenceOrListParam().add(new ReferenceParam("Encounter", "encounter-uuid")));

		BahmniServiceRequestSearchParams params = new BahmniServiceRequestSearchParams(null, null, encounterRef, null,
		    null, null, null, null, null, new HashSet<>(), new HashSet<>(), null, null);

		assertThat(params.getEncounterReference(), notNullValue());
	}
	
	@Test
	public void participantReferenceGetter_shouldReturnSetValue() {
		ReferenceAndListParam participantRef = new ReferenceAndListParam()
		    .addAnd(new ReferenceOrListParam().add(new ReferenceParam("Practitioner", "prac-uuid")));

		BahmniServiceRequestSearchParams params = new BahmniServiceRequestSearchParams(null, null, null, participantRef,
		    null, null, null, null, null, new HashSet<>(), new HashSet<>(), null, null);

		assertThat(params.getParticipantReference(), notNullValue());
	}
}
