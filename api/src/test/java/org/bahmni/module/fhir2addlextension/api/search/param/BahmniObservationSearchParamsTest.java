package org.bahmni.module.fhir2addlextension.api.search.param;

import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.junit.Test;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BahmniObservationSearchParamsTest {
	
	private static final String PATIENT_UUID = "patient-uuid-123";
	
	private static final String SERVICE_REQUEST_UUID = "service-request-uuid-456";
	
	@Test
	public void hasPatientReference_shouldReturnFalseWhenNull() {
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(null, null, null, null);
		assertFalse(searchParams.hasPatientReference());
	}
	
	@Test
	public void hasPatientReference_shouldReturnFalseWhenEmpty() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(patientRef, null, null, null);
		assertFalse(searchParams.hasPatientReference());
	}
	
	@Test
	public void hasPatientReference_shouldReturnFalseWhenEmptyValue() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam().setValue(""));
		patientRef.addAnd(orListParam);
		
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(patientRef, null, null, null);
		assertFalse(searchParams.hasPatientReference());
	}
	
	@Test
	public void hasPatientReference_shouldReturnFalseWhenNullValue() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam().setValue(null));
		patientRef.addAnd(orListParam);
		
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(patientRef, null, null, null);
		assertFalse(searchParams.hasPatientReference());
	}
	
	@Test
	public void hasPatientReference_shouldReturnTrueWhenValidValue() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam("Patient", PATIENT_UUID));
		patientRef.addAnd(orListParam);
		
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(patientRef, null, null, null);
		assertTrue(searchParams.hasPatientReference());
	}
	
	@Test
	public void hasPatientReference_shouldReturnTrueWithMultipleValues() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam("Patient", "patient-uuid-1"));
		orListParam.add(new ReferenceParam("Patient", "patient-uuid-2"));
		patientRef.addAnd(orListParam);
		
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(patientRef, null, null, null);
		assertTrue(searchParams.hasPatientReference());
	}
	
	@Test
	public void hasBasedOnReference_shouldReturnFalseWhenNull() {
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(null, null, null, null);
		assertFalse(searchParams.hasBasedOnReference());
	}
	
	@Test
	public void hasBasedOnReference_shouldReturnFalseWhenEmpty() {
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam();
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(null, basedOnRef, null, null);
		assertFalse(searchParams.hasBasedOnReference());
	}
	
	@Test
	public void hasBasedOnReference_shouldReturnFalseWhenEmptyValue() {
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam().setValue(""));
		basedOnRef.addAnd(orListParam);
		
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(null, basedOnRef, null, null);
		assertFalse(searchParams.hasBasedOnReference());
	}
	
	@Test
	public void hasBasedOnReference_shouldReturnFalseWhenNullValue() {
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam().setValue(null));
		basedOnRef.addAnd(orListParam);
		
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(null, basedOnRef, null, null);
		assertFalse(searchParams.hasBasedOnReference());
	}
	
	@Test
	public void hasBasedOnReference_shouldReturnTrueWhenValidValue() {
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam(SERVICE_REQUEST_UUID));
		basedOnRef.addAnd(orListParam);
		
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(null, basedOnRef, null, null);
		assertTrue(searchParams.hasBasedOnReference());
	}
	
	@Test
	public void hasBasedOnReference_shouldReturnTrueWithMultipleValues() {
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam("order-uuid-1"));
		orListParam.add(new ReferenceParam("order-uuid-2"));
		basedOnRef.addAnd(orListParam);
		
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(null, basedOnRef, null, null);
		assertTrue(searchParams.hasBasedOnReference());
	}
	
	@Test
	public void toSearchParameterMap_shouldIncludePatientReferenceWhenPresent() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam("Patient", PATIENT_UUID));
		patientRef.addAnd(orListParam);
		
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(patientRef, null, null, null);
		SearchParameterMap paramMap = searchParams.toSearchParameterMap();
		
		assertNotNull(paramMap);
		assertTrue(paramMap.getParameters().stream()
		        .anyMatch(p -> p.getKey().equals(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER)));
	}
	
	@Test
	public void toSearchParameterMap_shouldIncludeBasedOnReferenceWhenPresent() {
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam(SERVICE_REQUEST_UUID));
		basedOnRef.addAnd(orListParam);
		
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(null, basedOnRef, null, null);
		SearchParameterMap paramMap = searchParams.toSearchParameterMap();
		
		assertNotNull(paramMap);
		assertTrue(paramMap.getParameters().stream()
		        .anyMatch(p -> p.getKey().equals(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER)));
	}
	
	@Test
	public void toSearchParameterMap_shouldNotIncludePatientReferenceWhenNull() {
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(null, null, null, null);
		SearchParameterMap paramMap = searchParams.toSearchParameterMap();
		
		assertNotNull(paramMap);
		assertFalse(paramMap.getParameters().stream()
		        .anyMatch(p -> p.getKey().equals(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER)));
	}
	
	@Test
	public void toSearchParameterMap_shouldNotIncludeBasedOnReferenceWhenNull() {
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(null, null, null, null);
		SearchParameterMap paramMap = searchParams.toSearchParameterMap();
		
		assertNotNull(paramMap);
		assertFalse(paramMap.getParameters().stream()
		        .anyMatch(p -> p.getKey().equals(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER)));
	}
	
	@Test
	public void toSearchParameterMap_shouldIncludeAllParametersWhenProvided() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam patientOrList = new ReferenceOrListParam();
		patientOrList.add(new ReferenceParam("Patient", PATIENT_UUID));
		patientRef.addAnd(patientOrList);
		
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam();
		ReferenceOrListParam basedOnOrList = new ReferenceOrListParam();
		basedOnOrList.add(new ReferenceParam(SERVICE_REQUEST_UUID));
		basedOnRef.addAnd(basedOnOrList);
		
		DateRangeParam lastUpdated = new DateRangeParam("2026-01-01", "2026-12-31");
		SortSpec sort = new SortSpec("_lastUpdated");
		
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(patientRef, basedOnRef, lastUpdated,
		    sort);
		SearchParameterMap paramMap = searchParams.toSearchParameterMap();
		
		assertNotNull(paramMap);
		assertTrue(paramMap.getParameters().stream()
		        .anyMatch(p -> p.getKey().equals(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER)));
		assertTrue(paramMap.getParameters().stream()
		        .anyMatch(p -> p.getKey().equals(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER)));
	}
	
	@Test
	public void toSearchParameterMap_shouldWorkWithEmptyParameters() {
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(null, null, null, null);
		SearchParameterMap paramMap = searchParams.toSearchParameterMap();
		
		assertNotNull(paramMap);
	}
	
	@Test
	public void hasPatientReference_shouldReturnFalseWhenValuesAsQueryTokensIsEmpty() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		patientRef.addAnd(orListParam);
		
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(patientRef, null, null, null);
		assertFalse(searchParams.hasPatientReference());
	}
	
	@Test
	public void hasBasedOnReference_shouldReturnFalseWhenValuesAsQueryTokensIsEmpty() {
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		basedOnRef.addAnd(orListParam);
		
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(null, basedOnRef, null, null);
		assertFalse(searchParams.hasBasedOnReference());
	}
	
	@Test
	public void shouldAccessPatientReferenceField() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		patientRef.addAnd(new ReferenceOrListParam().add(new ReferenceParam("Patient", PATIENT_UUID)));
		
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(patientRef, null, null, null);
		
		assertNotNull(searchParams.getPatientReference());
		assertTrue(searchParams.hasPatientReference());
	}
	
	@Test
	public void shouldAccessBasedOnReferenceField() {
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam();
		basedOnRef.addAnd(new ReferenceOrListParam().add(new ReferenceParam(SERVICE_REQUEST_UUID)));
		
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(null, basedOnRef, null, null);
		
		assertNotNull(searchParams.getBasedOnReference());
		assertTrue(searchParams.hasBasedOnReference());
	}
}
