package org.bahmni.module.fhir2addlextension.api.search.param;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.junit.Test;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BahmniDocumentReferenceSearchParamsTest {
	
	private ReferenceAndListParam patientReference() {
		return new ReferenceAndListParam()
		        .addAnd(new ReferenceOrListParam().add(new ReferenceParam("Patient/patient-uuid")));
	}
	
	private TokenAndListParam docType(String system, String code) {
		return new TokenAndListParam().addAnd(new TokenOrListParam().add(new TokenParam(system, code)));
	}
	
	@Test
	public void shouldAddTypeUnderCodedSearchHandlerWhenTypeIsPresent() {
		TokenAndListParam type = docType("http://bahmni.org/document-type", "discharge-summary");
		BahmniDocumentReferenceSearchParams searchParams = new BahmniDocumentReferenceSearchParams(patientReference(), null,
		        null, null, type, null);
		
		SearchParameterMap searchParameterMap = searchParams.toSearchParameterMap();
		
		assertTrue(searchParams.hasType());
		assertEquals(1, searchParameterMap.getParameters(FhirConstants.CODED_SEARCH_HANDLER).size());
		assertEquals(type, searchParameterMap.getParameters(FhirConstants.CODED_SEARCH_HANDLER).get(0).getParam());
	}
	
	@Test
	public void shouldNotAddCodedSearchHandlerWhenTypeIsAbsent() {
		BahmniDocumentReferenceSearchParams searchParams = new BahmniDocumentReferenceSearchParams(patientReference(), null,
		        null, null, null, null);
		
		SearchParameterMap searchParameterMap = searchParams.toSearchParameterMap();
		
		assertFalse(searchParams.hasType());
		assertTrue(searchParameterMap.getParameters(FhirConstants.CODED_SEARCH_HANDLER).isEmpty());
	}
	
	@Test
	public void shouldNotAddCodedSearchHandlerWhenTypeHasNoTokens() {
		BahmniDocumentReferenceSearchParams searchParams = new BahmniDocumentReferenceSearchParams(patientReference(), null,
		        null, null, new TokenAndListParam(), null);
		
		SearchParameterMap searchParameterMap = searchParams.toSearchParameterMap();
		
		assertFalse(searchParams.hasType());
		assertTrue(searchParameterMap.getParameters(FhirConstants.CODED_SEARCH_HANDLER).isEmpty());
	}
	
	@Test
	public void shouldAddEncounterReferenceHandlerWhenEncounterIsSupplied() {
		ReferenceAndListParam encounterReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Encounter/encounter-uuid")));
		BahmniDocumentReferenceSearchParams searchParams = new BahmniDocumentReferenceSearchParams(patientReference(), null,
		        null, encounterReference, null, null);
		
		SearchParameterMap searchParameterMap = searchParams.toSearchParameterMap();
		
		assertEquals(1, searchParameterMap.getParameters(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER).size());
		assertEquals(encounterReference, searchParameterMap.getParameters(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER)
		        .get(0).getParam());
	}
	
	@Test
	public void shouldNotAddEncounterReferenceHandlerWhenEncounterIsAbsent() {
		BahmniDocumentReferenceSearchParams searchParams = new BahmniDocumentReferenceSearchParams(patientReference(), null,
		        null, null, null, null);
		
		SearchParameterMap searchParameterMap = searchParams.toSearchParameterMap();
		
		assertTrue(searchParameterMap.getParameters(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER).isEmpty());
	}
	
	@Test
	public void shouldAlwaysAddPatientReferenceHandler() {
		BahmniDocumentReferenceSearchParams searchParams = new BahmniDocumentReferenceSearchParams(patientReference(), null,
		        null, null, null, null);
		
		SearchParameterMap searchParameterMap = searchParams.toSearchParameterMap();
		
		assertEquals(1, searchParameterMap.getParameters(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER).size());
	}
	
	@Test
	public void shouldReportPatientReferencePresentWhenAValueIsSupplied() {
		BahmniDocumentReferenceSearchParams searchParams = new BahmniDocumentReferenceSearchParams(patientReference(), null,
		        null, null, null, null);
		
		assertTrue(searchParams.hasPatientReference());
	}
	
	@Test
	public void shouldReportPatientReferenceAbsentWhenNull() {
		BahmniDocumentReferenceSearchParams searchParams = new BahmniDocumentReferenceSearchParams(null, null, null, null,
		        null, null);
		
		assertFalse(searchParams.hasPatientReference());
	}
	
	@Test
	public void shouldReportPatientReferenceAbsentWhenValueIsEmpty() {
		ReferenceAndListParam emptyValue = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("")));
		BahmniDocumentReferenceSearchParams searchParams = new BahmniDocumentReferenceSearchParams(emptyValue, null, null,
		        null, null, null);
		
		assertFalse(searchParams.hasPatientReference());
	}
	
	@Test
	public void shouldReportIdPresentWhenSupplied() {
		TokenAndListParam id = new TokenAndListParam().addAnd(new TokenOrListParam().add(new TokenParam("doc-uuid")));
		BahmniDocumentReferenceSearchParams searchParams = new BahmniDocumentReferenceSearchParams(null, id, null, null,
		        null, null);
		
		assertTrue(searchParams.hasId());
	}
	
	@Test
	public void shouldReportIdAbsentWhenNull() {
		BahmniDocumentReferenceSearchParams searchParams = new BahmniDocumentReferenceSearchParams(null, null, null, null,
		        null, null);
		
		assertFalse(searchParams.hasId());
	}
}
