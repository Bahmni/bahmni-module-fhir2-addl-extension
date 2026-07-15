package org.bahmni.module.fhir2addlextension.api.search.param;

import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BahmniObservationSearchParamsTest {
	
	private static final String PATIENT_UUID = "patient-uuid-123";
	
	private static final String SERVICE_REQUEST_UUID = "service-request-uuid-456";
	
	private BahmniObservationSearchParams searchParams;
	
	@Before
	public void setUp() {
		searchParams = new BahmniObservationSearchParams(null, null, null, null);
	}
	
	@Test
	public void shouldReturnFalseWhenNoPatientReferenceProvided() {
		assertFalse("Should return false when patient reference is null", searchParams.hasPatientReference());
	}
	
	@Test
	public void shouldReturnTrueWhenPatientReferenceProvided() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		ReferenceParam refParam = new ReferenceParam("Patient", PATIENT_UUID);
		orListParam.add(refParam);
		patientRef.addAnd(orListParam);
		
		searchParams = new BahmniObservationSearchParams(patientRef, null, null, null);
		assertTrue("Should return true when patient reference is provided", searchParams.hasPatientReference());
	}
	
	@Test
	public void shouldReturnFalseWhenEmptyPatientReferenceProvided() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		searchParams = new BahmniObservationSearchParams(patientRef, null, null, null);
		assertFalse("Should return false for empty patient reference", searchParams.hasPatientReference());
	}
	
	@Test
	public void shouldReturnFalseWhenNoBasedOnReferenceProvided() {
		assertFalse("Should return false when basedOn reference is null", searchParams.hasBasedOnReference());
	}
	
	@Test
	public void shouldReturnTrueWhenBasedOnReferenceProvided() {
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		ReferenceParam refParam = new ReferenceParam("ServiceRequest", SERVICE_REQUEST_UUID);
		orListParam.add(refParam);
		basedOnRef.addAnd(orListParam);
		
		searchParams = new BahmniObservationSearchParams(null, basedOnRef, null, null);
		assertTrue("Should return true when basedOn reference is provided", searchParams.hasBasedOnReference());
	}
	
	@Test
	public void shouldReturnFalseWhenEmptyBasedOnReferenceProvided() {
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam();
		searchParams = new BahmniObservationSearchParams(null, basedOnRef, null, null);
		assertFalse("Should return false for empty basedOn reference", searchParams.hasBasedOnReference());
	}
	
	@Test
	public void shouldReturnTrueWhenBothPatientAndBasedOnProvided() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam patientOrList = new ReferenceOrListParam();
		patientOrList.add(new ReferenceParam("Patient", PATIENT_UUID));
		patientRef.addAnd(patientOrList);
		
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam();
		ReferenceOrListParam basedOnOrList = new ReferenceOrListParam();
		basedOnOrList.add(new ReferenceParam("ServiceRequest", SERVICE_REQUEST_UUID));
		basedOnRef.addAnd(basedOnOrList);
		
		searchParams = new BahmniObservationSearchParams(patientRef, basedOnRef, null, null);
		
		assertTrue("Should have patient reference", searchParams.hasPatientReference());
		assertTrue("Should have basedOn reference", searchParams.hasBasedOnReference());
	}
	
	@Test
	public void shouldConvertToSearchParameterMap() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam("Patient", PATIENT_UUID));
		patientRef.addAnd(orListParam);
		
		searchParams = new BahmniObservationSearchParams(patientRef, null, null, null);
		
		SearchParameterMap paramMap = searchParams.toSearchParameterMap();
		assertNotNull("Search parameter map should not be null", paramMap);
	}
	
	@Test
	public void shouldConvertToSearchParameterMapWithAllParameters() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam patientOrList = new ReferenceOrListParam();
		patientOrList.add(new ReferenceParam("Patient", PATIENT_UUID));
		patientRef.addAnd(patientOrList);
		
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam();
		ReferenceOrListParam basedOnOrList = new ReferenceOrListParam();
		basedOnOrList.add(new ReferenceParam("ServiceRequest", SERVICE_REQUEST_UUID));
		basedOnRef.addAnd(basedOnOrList);
		
		DateRangeParam lastUpdated = new DateRangeParam("2026-01-01", "2026-12-31");
		SortSpec sort = new SortSpec("_lastUpdated");
		
		searchParams = new BahmniObservationSearchParams(patientRef, basedOnRef, lastUpdated, sort);
		
		SearchParameterMap paramMap = searchParams.toSearchParameterMap();
		assertNotNull("Search parameter map should not be null", paramMap);
	}
	
	@Test
	public void shouldHandleNullLastUpdated() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam("Patient", PATIENT_UUID));
		patientRef.addAnd(orListParam);
		
		searchParams = new BahmniObservationSearchParams(patientRef, null, null, null);
		
		SearchParameterMap paramMap = searchParams.toSearchParameterMap();
		assertNotNull("Search parameter map should not be null even with null lastUpdated", paramMap);
	}
	
	@Test
	public void shouldHandleNullSort() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam("Patient", PATIENT_UUID));
		patientRef.addAnd(orListParam);
		
		searchParams = new BahmniObservationSearchParams(patientRef, null, null, null);
		
		SearchParameterMap paramMap = searchParams.toSearchParameterMap();
		assertNotNull("Search parameter map should not be null even with null sort", paramMap);
	}
	
	@Test
	public void shouldHandleBasedOnReferenceWithResourceTypePrefix() {
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam().setValue("ServiceRequest/" + SERVICE_REQUEST_UUID));
		basedOnRef.addAnd(orListParam);
		
		searchParams = new BahmniObservationSearchParams(null, basedOnRef, null, null);
		assertTrue("Should return true when basedOn reference with prefix is provided", searchParams.hasBasedOnReference());
	}
	
	@Test
	public void shouldHandleMultiplePatientReferences() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam("Patient", "patient-uuid-1"));
		orListParam.add(new ReferenceParam("Patient", "patient-uuid-2"));
		patientRef.addAnd(orListParam);
		
		searchParams = new BahmniObservationSearchParams(patientRef, null, null, null);
		assertTrue("Should return true with multiple patient values", searchParams.hasPatientReference());
	}
	
	@Test
	public void shouldHandleMultipleBasedOnReferences() {
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam("ServiceRequest", "order-uuid-1"));
		orListParam.add(new ReferenceParam("ServiceRequest", "order-uuid-2"));
		basedOnRef.addAnd(orListParam);
		
		searchParams = new BahmniObservationSearchParams(null, basedOnRef, null, null);
		assertTrue("Should return true with multiple basedOn values", searchParams.hasBasedOnReference());
	}
	
	@Test
	public void shouldHandleDateRangeWithLowerBoundOnly() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam("Patient", PATIENT_UUID));
		patientRef.addAnd(orListParam);
		
		DateRangeParam lastUpdated = new DateRangeParam("2026-01-01", null);
		
		searchParams = new BahmniObservationSearchParams(patientRef, null, lastUpdated, null);
		SearchParameterMap paramMap = searchParams.toSearchParameterMap();
		assertNotNull("Search parameter map should handle date with lower bound only", paramMap);
	}
	
	@Test
	public void shouldHandleDateRangeWithUpperBoundOnly() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam("Patient", PATIENT_UUID));
		patientRef.addAnd(orListParam);
		
		DateRangeParam lastUpdated = new DateRangeParam(null, "2026-12-31");
		
		searchParams = new BahmniObservationSearchParams(patientRef, null, lastUpdated, null);
		SearchParameterMap paramMap = searchParams.toSearchParameterMap();
		assertNotNull("Search parameter map should handle date with upper bound only", paramMap);
	}
	
	@Test
	public void shouldHandleSortSpecWithDescendingOrder() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam("Patient", PATIENT_UUID));
		patientRef.addAnd(orListParam);
		
		SortSpec sort = new SortSpec("-_lastUpdated");
		
		searchParams = new BahmniObservationSearchParams(patientRef, null, null, sort);
		SearchParameterMap paramMap = searchParams.toSearchParameterMap();
		assertNotNull("Search parameter map should handle descending sort", paramMap);
	}
	
	@Test
	public void shouldHandleEmptySearchParameters() {
		searchParams = new BahmniObservationSearchParams(null, null, null, null);
		
		assertFalse("Should return false for patient reference", searchParams.hasPatientReference());
		assertFalse("Should return false for basedOn reference", searchParams.hasBasedOnReference());
		
		SearchParameterMap paramMap = searchParams.toSearchParameterMap();
		assertNotNull("Search parameter map should not be null even with no parameters", paramMap);
	}
	
	@Test
	public void shouldReturnFalseWhenPatientReferenceHasEmptyValue() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		ReferenceParam refParam = new ReferenceParam();
		refParam.setValue("");
		orListParam.add(refParam);
		patientRef.addAnd(orListParam);
		
		searchParams = new BahmniObservationSearchParams(patientRef, null, null, null);
		assertFalse("Should return false when patient reference has empty value", searchParams.hasPatientReference());
	}
	
	@Test
	public void shouldReturnFalseWhenBasedOnReferenceHasEmptyValue() {
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		ReferenceParam refParam = new ReferenceParam();
		refParam.setValue("");
		orListParam.add(refParam);
		basedOnRef.addAnd(orListParam);
		
		searchParams = new BahmniObservationSearchParams(null, basedOnRef, null, null);
		assertFalse("Should return false when basedOn reference has empty value", searchParams.hasBasedOnReference());
	}
	
	@Test
	public void shouldReturnFalseWhenPatientReferenceHasOnlyNullValues() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		ReferenceParam refParam = new ReferenceParam();
		refParam.setValue(null);
		orListParam.add(refParam);
		patientRef.addAnd(orListParam);
		
		searchParams = new BahmniObservationSearchParams(patientRef, null, null, null);
		assertFalse("Should return false when patient reference has only null values", searchParams.hasPatientReference());
	}
	
	@Test
	public void shouldReturnFalseWhenBasedOnReferenceHasOnlyNullValues() {
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		ReferenceParam refParam = new ReferenceParam();
		refParam.setValue(null);
		orListParam.add(refParam);
		basedOnRef.addAnd(orListParam);
		
		searchParams = new BahmniObservationSearchParams(null, basedOnRef, null, null);
		assertFalse("Should return false when basedOn reference has only null values", searchParams.hasBasedOnReference());
	}
	
	@Test
	public void shouldReturnFalseWhenMixedEmptyAndValidPatientReferencesInSameOrList() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam().setValue(""));
		orListParam.add(new ReferenceParam("Patient", PATIENT_UUID));
		patientRef.addAnd(orListParam);
		
		searchParams = new BahmniObservationSearchParams(patientRef, null, null, null);
		assertFalse("Should return false when OR list contains both empty and valid values",
		    searchParams.hasPatientReference());
	}
	
	@Test
	public void shouldReturnFalseWhenMixedEmptyAndValidBasedOnReferencesInSameOrList() {
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam().setValue(""));
		orListParam.add(new ReferenceParam("ServiceRequest", SERVICE_REQUEST_UUID));
		basedOnRef.addAnd(orListParam);
		
		searchParams = new BahmniObservationSearchParams(null, basedOnRef, null, null);
		assertFalse("Should return false when OR list contains both empty and valid values",
		    searchParams.hasBasedOnReference());
	}
	
	@Test
	public void shouldHandleMultipleAndListsWithMixedValues() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		// First AND condition with empty value
		ReferenceOrListParam orList1 = new ReferenceOrListParam();
		orList1.add(new ReferenceParam().setValue(""));
		patientRef.addAnd(orList1);
		// Second AND condition with valid value
		ReferenceOrListParam orList2 = new ReferenceOrListParam();
		orList2.add(new ReferenceParam("Patient", PATIENT_UUID));
		patientRef.addAnd(orList2);
		
		searchParams = new BahmniObservationSearchParams(patientRef, null, null, null);
		assertTrue("Should return true with multiple AND lists containing valid values", searchParams.hasPatientReference());
	}
	
	@Test
	public void shouldReturnTrueForPatientReferenceWithResourceType() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam("Patient", PATIENT_UUID));
		patientRef.addAnd(orListParam);
		
		searchParams = new BahmniObservationSearchParams(patientRef, null, null, null);
		assertTrue("Should return true for patient reference with resource type", searchParams.hasPatientReference());
	}
	
	@Test
	public void shouldReturnTrueForBasedOnReferenceWithResourceType() {
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam("ServiceRequest", SERVICE_REQUEST_UUID));
		basedOnRef.addAnd(orListParam);
		
		searchParams = new BahmniObservationSearchParams(null, basedOnRef, null, null);
		assertTrue("Should return true for basedOn reference with resource type", searchParams.hasBasedOnReference());
	}
	
	@Test
	public void shouldVerifySearchParameterMapContainsPatientReference() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam("Patient", PATIENT_UUID));
		patientRef.addAnd(orListParam);
		
		searchParams = new BahmniObservationSearchParams(patientRef, null, null, null);
		SearchParameterMap paramMap = searchParams.toSearchParameterMap();
		
		assertNotNull("Search parameter map should not be null", paramMap);
		assertTrue("Parameter map should contain patient reference",
		    paramMap.getParameters().stream().anyMatch(p -> p.getKey().equals(org.openmrs.module.fhir2.FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER)));
	}
	
	@Test
	public void shouldVerifySearchParameterMapContainsBasedOnReference() {
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam("ServiceRequest", SERVICE_REQUEST_UUID));
		basedOnRef.addAnd(orListParam);
		
		searchParams = new BahmniObservationSearchParams(null, basedOnRef, null, null);
		SearchParameterMap paramMap = searchParams.toSearchParameterMap();
		
		assertNotNull("Search parameter map should not be null", paramMap);
		assertTrue("Parameter map should contain basedOn reference",
		    paramMap.getParameters().stream().anyMatch(p -> p.getKey().equals(org.openmrs.module.fhir2.FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER)));
	}
	
	@Test
	public void shouldNotIncludeNullPatientReferenceInSearchParameterMap() {
		searchParams = new BahmniObservationSearchParams(null, null, null, null);
		SearchParameterMap paramMap = searchParams.toSearchParameterMap();
		
		assertNotNull("Search parameter map should not be null", paramMap);
		assertFalse("Parameter map should not contain patient reference when null",
		    paramMap.getParameters().stream().anyMatch(p -> p.getKey().equals(org.openmrs.module.fhir2.FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER)));
	}
	
	@Test
	public void shouldNotIncludeNullBasedOnReferenceInSearchParameterMap() {
		searchParams = new BahmniObservationSearchParams(null, null, null, null);
		SearchParameterMap paramMap = searchParams.toSearchParameterMap();
		
		assertNotNull("Search parameter map should not be null", paramMap);
		assertFalse("Parameter map should not contain basedOn reference when null",
		    paramMap.getParameters().stream().anyMatch(p -> p.getKey().equals(org.openmrs.module.fhir2.FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER)));
	}
	
	@Test
	public void shouldHandleChainedSortSpec() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam("Patient", PATIENT_UUID));
		patientRef.addAnd(orListParam);
		
		SortSpec sort = new SortSpec("_lastUpdated");
		SortSpec chainedSort = new SortSpec("date");
		sort.setChain(chainedSort);
		
		searchParams = new BahmniObservationSearchParams(patientRef, null, null, sort);
		SearchParameterMap paramMap = searchParams.toSearchParameterMap();
		assertNotNull("Search parameter map should handle chained sort", paramMap);
	}
	
	@Test
	public void shouldHandleEmptyDateRangeParam() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam("Patient", PATIENT_UUID));
		patientRef.addAnd(orListParam);
		
		DateRangeParam lastUpdated = new DateRangeParam();
		
		searchParams = new BahmniObservationSearchParams(patientRef, null, lastUpdated, null);
		SearchParameterMap paramMap = searchParams.toSearchParameterMap();
		assertNotNull("Search parameter map should handle empty date range", paramMap);
	}
	
	@Test
	public void shouldVerifyGettersAndSetters() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam();
		DateRangeParam lastUpdated = new DateRangeParam("2026-01-01", "2026-12-31");
		SortSpec sort = new SortSpec("_lastUpdated");
		
		searchParams = new BahmniObservationSearchParams(patientRef, basedOnRef, lastUpdated, sort);
		
		assertNotNull("Patient reference should not be null", searchParams.getPatientReference());
		assertNotNull("BasedOn reference should not be null", searchParams.getBasedOnReference());
		assertNotNull("Last updated should not be null", searchParams.getLastUpdated());
		assertNotNull("Sort should not be null", searchParams.getSort());
	}
	
	@Test
	public void shouldHandleComplexPatientReferenceWithMultipleAndOrs() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		// First AND with multiple ORs
		ReferenceOrListParam orList1 = new ReferenceOrListParam();
		orList1.add(new ReferenceParam("Patient", "patient-uuid-1"));
		orList1.add(new ReferenceParam("Patient", "patient-uuid-2"));
		patientRef.addAnd(orList1);
		// Second AND
		ReferenceOrListParam orList2 = new ReferenceOrListParam();
		orList2.add(new ReferenceParam("Patient", "patient-uuid-3"));
		patientRef.addAnd(orList2);
		
		searchParams = new BahmniObservationSearchParams(patientRef, null, null, null);
		assertTrue("Should handle complex patient reference with multiple AND/ORs", searchParams.hasPatientReference());
	}
	
	@Test
	public void shouldHandleComplexBasedOnReferenceWithMultipleAndOrs() {
		ReferenceAndListParam basedOnRef = new ReferenceAndListParam();
		ReferenceOrListParam orList1 = new ReferenceOrListParam();
		orList1.add(new ReferenceParam("ServiceRequest", "order-uuid-1"));
		orList1.add(new ReferenceParam("ServiceRequest", "order-uuid-2"));
		basedOnRef.addAnd(orList1);
		ReferenceOrListParam orList2 = new ReferenceOrListParam();
		orList2.add(new ReferenceParam("ServiceRequest", "order-uuid-3"));
		basedOnRef.addAnd(orList2);
		
		searchParams = new BahmniObservationSearchParams(null, basedOnRef, null, null);
		assertTrue("Should handle complex basedOn reference with multiple AND/ORs", searchParams.hasBasedOnReference());
	}
}
