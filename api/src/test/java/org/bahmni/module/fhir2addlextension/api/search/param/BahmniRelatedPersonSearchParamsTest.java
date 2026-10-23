package org.bahmni.module.fhir2addlextension.api.search.param;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.junit.Test;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class BahmniRelatedPersonSearchParamsTest {
	
	private static final String PATIENT_UUID = "patient-uuid-1234-5678-9012";
	
	private ReferenceAndListParam buildPatientReference(String uuid) {
		return new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam().setValue(uuid)));
	}
	
	// ===============================
	// hasPatientReference TESTS
	// ===============================
	
	@Test
	public void hasPatientReference_returnsFalseWhenNull() {
		BahmniRelatedPersonSearchParams params = BahmniRelatedPersonSearchParams.builder().patientReference(null).build();
		
		assertThat(params.hasPatientReference(), equalTo(false));
	}
	
	@Test
	public void hasPatientReference_returnsFalseWhenValueIsEmpty() {
		ReferenceAndListParam emptyRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue("")));
		BahmniRelatedPersonSearchParams params = BahmniRelatedPersonSearchParams.builder().patientReference(emptyRef)
		        .build();
		
		assertThat(params.hasPatientReference(), equalTo(false));
	}
	
	@Test
	public void hasPatientReference_returnsTrueWhenValidUuid() {
		BahmniRelatedPersonSearchParams params = BahmniRelatedPersonSearchParams.builder()
		        .patientReference(buildPatientReference(PATIENT_UUID)).build();
		
		assertThat(params.hasPatientReference(), equalTo(true));
	}
	
	@Test
	public void hasPatientReference_returnsTrueWhenPrefixedReference() {
		BahmniRelatedPersonSearchParams params = BahmniRelatedPersonSearchParams.builder()
		        .patientReference(buildPatientReference("Patient/" + PATIENT_UUID)).build();
		
		assertThat(params.hasPatientReference(), equalTo(true));
	}
	
	// ===============================
	// extractPatientUuid TESTS
	// ===============================
	
	@Test
	public void extractPatientUuid_returnsNullWhenReferenceIsNull() {
		BahmniRelatedPersonSearchParams params = BahmniRelatedPersonSearchParams.builder().patientReference(null).build();
		
		assertThat(params.extractPatientUuid(), nullValue());
	}
	
	@Test
	public void extractPatientUuid_returnsUuidFromReference() {
		BahmniRelatedPersonSearchParams params = BahmniRelatedPersonSearchParams.builder()
		        .patientReference(buildPatientReference(PATIENT_UUID)).build();
		
		assertThat(params.extractPatientUuid(), equalTo(PATIENT_UUID));
	}
	
	// ===============================
	// toSearchParameterMap TESTS
	// ===============================
	
	@Test
	public void toSearchParameterMap_returnsValidMap() {
		BahmniRelatedPersonSearchParams params = BahmniRelatedPersonSearchParams.builder()
		        .patientReference(buildPatientReference(PATIENT_UUID)).build();
		
		SearchParameterMap map = params.toSearchParameterMap();
		
		assertThat(map, notNullValue());
	}
	
	// ===============================
	// equals / hashCode (Lombok @Data)
	// ===============================
	
	@Test
	public void equals_returnsTrueForIdenticalInstances() {
		BahmniRelatedPersonSearchParams p1 = new BahmniRelatedPersonSearchParams();
		BahmniRelatedPersonSearchParams p2 = new BahmniRelatedPersonSearchParams();
		
		assertThat(p1.equals(p2), equalTo(true));
	}
	
	@Test
	public void hashCode_isConsistentForEqualInstances() {
		BahmniRelatedPersonSearchParams p1 = new BahmniRelatedPersonSearchParams();
		BahmniRelatedPersonSearchParams p2 = new BahmniRelatedPersonSearchParams();
		
		assertThat(p1.hashCode(), equalTo(p2.hashCode()));
	}
}
