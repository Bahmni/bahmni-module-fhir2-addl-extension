package org.bahmni.module.fhir2AddlExtension.api.search.param;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

public class BahmniAppointmentSearchParamsTest {
	
	private BahmniAppointmentSearchParams searchParams;
	
	@Before
	public void setUp() {
		searchParams = new BahmniAppointmentSearchParams();
	}
	
	@Test
	public void shouldReturnFalseWhenNoPatientReferenceProvided() {
		searchParams.setPatientReference(null);
		assertFalse("Should return false when patient reference is null", searchParams.hasPatientReference());
	}
	
	@Test
	public void shouldReturnTrueWhenPatientReferenceProvided() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		ReferenceParam refParam = new ReferenceParam("Patient", "patient-uuid");
		orListParam.add(refParam);
		patientRef.addAnd(orListParam);
		
		searchParams.setPatientReference(patientRef);
		assertTrue("Should return true when patient reference is provided", searchParams.hasPatientReference());
	}
	
	@Test
	public void shouldReturnFalseWhenEmptyPatientReferenceProvided() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		searchParams.setPatientReference(patientRef);
		assertFalse("Should return false for empty patient reference", searchParams.hasPatientReference());
	}
	
	@Test
	public void shouldReturnFalseWhenNoStatusProvided() {
		searchParams.setStatus(null);
		assertFalse("Should return false when status is null", searchParams.hasStatus());
	}
	
	@Test
	public void shouldReturnTrueWhenStatusProvided() {
		TokenAndListParam status = new TokenAndListParam();
		TokenOrListParam orListParam = new TokenOrListParam();
		TokenParam tokenParam = new TokenParam("booked");
		orListParam.add(tokenParam);
		status.addAnd(orListParam);
		
		searchParams.setStatus(status);
		assertTrue("Should return true when status is provided", searchParams.hasStatus());
	}
	
	@Test
	public void shouldReturnFalseWhenEmptyStatusProvided() {
		TokenAndListParam status = new TokenAndListParam();
		searchParams.setStatus(status);
		assertFalse("Should return false for empty status", searchParams.hasStatus());
	}
	
	@Test
	public void shouldReturnFalseWhenNoIdProvided() {
		searchParams.setId(null);
		assertFalse("Should return false when id is null", searchParams.hasId());
	}
	
	@Test
	public void shouldReturnTrueWhenIdProvided() {
		TokenAndListParam id = new TokenAndListParam();
		TokenOrListParam orListParam = new TokenOrListParam();
		TokenParam tokenParam = new TokenParam("appointment-uuid");
		orListParam.add(tokenParam);
		id.addAnd(orListParam);
		
		searchParams.setId(id);
		assertTrue("Should return true when id is provided", searchParams.hasId());
	}
	
	@Test
	public void shouldReturnFalseWhenNoDateProvided() {
		searchParams.setDate(null);
		assertFalse("Should return false when date is null", searchParams.hasDate());
	}
	
	@Test
	public void shouldReturnTrueWhenDateProvided() {
		DateRangeParam date = new DateRangeParam("2026-01-01", "2026-12-31");
		searchParams.setDate(date);
		assertTrue("Should return true when date is provided", searchParams.hasDate());
	}
	
	@Test
	public void shouldReturnFalseWhenEmptyDateProvided() {
		DateRangeParam date = new DateRangeParam();
		searchParams.setDate(date);
		assertFalse("Should return false for empty date range", searchParams.hasDate());
	}
	
	@Test
	public void shouldConvertToSearchParameterMap() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		ReferenceParam refParam = new ReferenceParam("Patient", "patient-uuid");
		orListParam.add(refParam);
		patientRef.addAnd(orListParam);
		
		searchParams.setPatientReference(patientRef);
		
		assertNotNull("Search parameter map should not be null", searchParams.toSearchParameterMap());
	}
	
	@Test
	public void shouldValidateMultipleSearchParameters() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		ReferenceParam refParam = new ReferenceParam("Patient", "patient-uuid");
		orListParam.add(refParam);
		patientRef.addAnd(orListParam);
		
		TokenAndListParam status = new TokenAndListParam();
		TokenOrListParam statusOrList = new TokenOrListParam();
		TokenParam statusToken = new TokenParam("booked");
		statusOrList.add(statusToken);
		status.addAnd(statusOrList);
		
		searchParams.setPatientReference(patientRef);
		searchParams.setStatus(status);
		
		assertTrue("Should have patient reference", searchParams.hasPatientReference());
		assertTrue("Should have status", searchParams.hasStatus());
	}
	
	@Test
	public void shouldHandleMultipleStatusCodes() {
		TokenAndListParam status = new TokenAndListParam();
		TokenOrListParam statusOrList = new TokenOrListParam();
		statusOrList.add(new TokenParam("booked"));
		statusOrList.add(new TokenParam("fulfilled"));
		status.addAnd(statusOrList);
		
		searchParams.setStatus(status);
		assertTrue("Should return true with multiple status values", searchParams.hasStatus());
	}
}
