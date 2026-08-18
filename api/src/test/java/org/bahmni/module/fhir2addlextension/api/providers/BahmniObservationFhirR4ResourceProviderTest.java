package org.bahmni.module.fhir2addlextension.api.providers;

import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniObservationSearchParams;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirObservationService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
	private IBundleProvider bundleProvider;
	
	@Mock
	private RequestDetails requestDetails;
	
	@Captor
	private ArgumentCaptor<BahmniObservationSearchParams> searchParamsCaptor;
	
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
	public void testGetEverythingByEncounter() {
		Bundle expectedBundle = new Bundle();
		expectedBundle.setType(Bundle.BundleType.SEARCHSET);
		expectedBundle.setTotal(3);
		
		ReferenceAndListParam encounterReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam(ENCOUNTER_UUID)));
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam(SERVICE_REQUEST_UUID)));
		
		when(observationService.fetchAllByEncounter(any(ReferenceAndListParam.class), any(ReferenceAndListParam.class)))
		        .thenReturn(expectedBundle);
		
		Bundle result = resourceProvider.getEverythingByEncounter(encounterReference, basedOnReference, requestDetails);
		
		assertSame(expectedBundle, result);
		assertEquals(Bundle.BundleType.SEARCHSET, result.getType());
		assertEquals(3, result.getTotal());
		verify(observationService).fetchAllByEncounter(eq(encounterReference), eq(basedOnReference));
	}
	
	@Test
	public void testGetEverythingByEncounterWithoutEncounter_shouldThrowException() {
		assertThrows(InvalidRequestException.class, () -> {
			resourceProvider.getEverythingByEncounter(null, null, requestDetails);
		});
	}
	
	@Test
	public void testSearchObservationWithPatientReference() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(PATIENT_UUID)));
		
		when(observationService.searchObservations(any(BahmniObservationSearchParams.class))).thenReturn(bundleProvider);
		
		IBundleProvider result = resourceProvider.searchObservation(patientReference, null, null, null);
		
		assertNotNull(result);
		assertEquals(bundleProvider, result);
		verify(observationService).searchObservations(searchParamsCaptor.capture());
		
		BahmniObservationSearchParams capturedParams = searchParamsCaptor.getValue();
		assertNotNull(capturedParams);
	}
	
	@Test
	public void testSearchObservationWithBasedOnReference() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam(SERVICE_REQUEST_UUID)));
		
		when(observationService.searchObservations(any(BahmniObservationSearchParams.class))).thenReturn(bundleProvider);
		
		IBundleProvider result = resourceProvider.searchObservation(null, basedOnReference, null, null);
		
		assertNotNull(result);
		verify(observationService).searchObservations(searchParamsCaptor.capture());
		
		BahmniObservationSearchParams capturedParams = searchParamsCaptor.getValue();
		assertNotNull(capturedParams);
	}
	
	@Test
	public void testSearchObservationWithBasedOnReferenceWithServiceRequestPrefix() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("ServiceRequest/" + SERVICE_REQUEST_UUID)));
		
		when(observationService.searchObservations(any(BahmniObservationSearchParams.class))).thenReturn(bundleProvider);
		
		IBundleProvider result = resourceProvider.searchObservation(null, basedOnReference, null, null);
		
		assertNotNull(result);
		verify(observationService).searchObservations(searchParamsCaptor.capture());
		
		BahmniObservationSearchParams capturedParams = searchParamsCaptor.getValue();
		assertNotNull(capturedParams);
	}
	
	@Test
	public void testSearchObservationWithMultipleParameters() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(PATIENT_UUID)));
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam(SERVICE_REQUEST_UUID)));
		
		when(observationService.searchObservations(any(BahmniObservationSearchParams.class))).thenReturn(bundleProvider);
		
		IBundleProvider result = resourceProvider.searchObservation(patientReference, basedOnReference, null, null);
		
		assertNotNull(result);
		verify(observationService).searchObservations(searchParamsCaptor.capture());
		
		BahmniObservationSearchParams capturedParams = searchParamsCaptor.getValue();
		assertNotNull(capturedParams);
	}
	
	@Test
	public void testSearchObservationWithLastUpdated() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(PATIENT_UUID)));
		DateRangeParam lastUpdated = new DateRangeParam();
		
		when(observationService.searchObservations(any(BahmniObservationSearchParams.class))).thenReturn(bundleProvider);
		
		IBundleProvider result = resourceProvider.searchObservation(patientReference, null, lastUpdated, null);
		
		assertNotNull(result);
		verify(observationService).searchObservations(searchParamsCaptor.capture());
		
		BahmniObservationSearchParams capturedParams = searchParamsCaptor.getValue();
		assertNotNull(capturedParams);
	}
	
	@Test
	public void testSearchObservationWithAllParameters() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(PATIENT_UUID)));
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam(SERVICE_REQUEST_UUID)));
		DateRangeParam lastUpdated = new DateRangeParam();
		SortSpec sort = new SortSpec("_lastUpdated");
		
		when(observationService.searchObservations(any(BahmniObservationSearchParams.class))).thenReturn(bundleProvider);
		
		IBundleProvider result = resourceProvider.searchObservation(patientReference, basedOnReference, lastUpdated, sort);
		
		assertNotNull(result);
		verify(observationService).searchObservations(searchParamsCaptor.capture());
		
		BahmniObservationSearchParams capturedParams = searchParamsCaptor.getValue();
		assertNotNull(capturedParams);
	}
}
