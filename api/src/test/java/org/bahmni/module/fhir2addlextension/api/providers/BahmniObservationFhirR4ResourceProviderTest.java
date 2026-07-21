package org.bahmni.module.fhir2addlextension.api.providers;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniObservationSearchParams;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirObservationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniObservationFhirR4ResourceProviderTest {
	
	private static final String ENCOUNTER_UUID = "f9df3ec8-fda0-4c8a-9957-cbcdf02de89f";
	
	private static final String PATIENT_UUID = "patient-uuid-123";
	
	private static final String SERVICE_REQUEST_UUID = "service-request-uuid-456";
	
	private static final String SERVER_BASE = "https://localhost/openmrs/ws/fhir2/R4";
	
	@Mock
	private BahmniFhirObservationService observationService;
	
	@Mock
	private RequestDetails requestDetails;
	
	@InjectMocks
	private BahmniObservationFhirR4ResourceProvider resourceProvider;
	
	@Before
	public void setUp() {
		when(requestDetails.getFhirServerBase()).thenReturn(SERVER_BASE);
	}
	
	@Test
	public void testGetResourceType() {
		assertEquals(Observation.class, resourceProvider.getResourceType());
	}
	
	@Test
	public void testGetEverythingByEncounter_shouldDelegateToService() {
		Bundle expectedBundle = new Bundle();
		expectedBundle.setType(Bundle.BundleType.SEARCHSET);
		expectedBundle.setTotal(3);
		
		ReferenceAndListParam encounterReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam(ENCOUNTER_UUID)));
		
		when(observationService.fetchAllByEncounter(any(ReferenceAndListParam.class))).thenReturn(expectedBundle);
		
		Bundle result = resourceProvider.getEverythingByEncounter(encounterReference, requestDetails);
		
		assertNotNull(result);
		assertEquals(Bundle.BundleType.SEARCHSET, result.getType());
		assertEquals(3, result.getTotal());
		verify(observationService).fetchAllByEncounter(encounterReference);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void testGetEverythingByEncounter_shouldThrowExceptionWhenEncounterIsNull() {
		resourceProvider.getEverythingByEncounter(null, requestDetails);
	}
	
	@Test
	public void testSearchObservation_shouldSearchByBasedOn() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam(SERVICE_REQUEST_UUID)));
		
		IBundleProvider expectedResults = mock(IBundleProvider.class);
		when(observationService.searchObservations(any(BahmniObservationSearchParams.class))).thenReturn(expectedResults);
		
		IBundleProvider result = resourceProvider.searchObservation(null, basedOnReference, null, null);
		
		assertNotNull(result);
		assertEquals(expectedResults, result);
		verify(observationService).searchObservations(any(BahmniObservationSearchParams.class));
	}
	
	@Test
	public void testSearchObservation_shouldSearchByPatient() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(PATIENT_UUID)));
		
		IBundleProvider expectedResults = mock(IBundleProvider.class);
		when(observationService.searchObservations(any(BahmniObservationSearchParams.class))).thenReturn(expectedResults);
		
		IBundleProvider result = resourceProvider.searchObservation(patientReference, null, null, null);
		
		assertNotNull(result);
		assertEquals(expectedResults, result);
		verify(observationService).searchObservations(any(BahmniObservationSearchParams.class));
	}
	
	@Test
	public void testSearchObservation_shouldSearchByPatientAndBasedOn() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(PATIENT_UUID)));
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam(SERVICE_REQUEST_UUID)));
		
		IBundleProvider expectedResults = mock(IBundleProvider.class);
		when(observationService.searchObservations(any(BahmniObservationSearchParams.class))).thenReturn(expectedResults);
		
		IBundleProvider result = resourceProvider.searchObservation(patientReference, basedOnReference, null, null);
		
		assertNotNull(result);
		assertEquals(expectedResults, result);
		verify(observationService).searchObservations(any(BahmniObservationSearchParams.class));
	}
	
	@Test
	public void testSearchObservation_shouldSearchWithLastUpdated() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(PATIENT_UUID)));
		DateRangeParam lastUpdated = new DateRangeParam();
		
		IBundleProvider expectedResults = mock(IBundleProvider.class);
		when(observationService.searchObservations(any(BahmniObservationSearchParams.class))).thenReturn(expectedResults);
		
		IBundleProvider result = resourceProvider.searchObservation(patientReference, null, lastUpdated, null);
		
		assertNotNull(result);
		assertEquals(expectedResults, result);
		verify(observationService).searchObservations(any(BahmniObservationSearchParams.class));
	}
}
