package org.bahmni.module.fhir2addlextension.api.providers;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirObservationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.fhir2.api.search.param.ObservationSearchParams;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniObservationFhirR4ResourceProviderTest {
	
	private static final String ENCOUNTER_UUID = "f9df3ec8-fda0-4c8a-9957-cbcdf02de89f";
	
	private static final String SERVER_BASE = "https://localhost/openmrs/ws/fhir2/R4";
	
	@Mock
	private BahmniFhirObservationService observationService;
	
	@Mock
	private IBundleProvider bundleProvider;
	
	@Mock
	private RequestDetails requestDetails;
	
	@Captor
	private ArgumentCaptor<ObservationSearchParams> searchParamsCaptor;
	
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
	public void testGetEverythingByEncounter_shouldReturnBundleWithAllObservations() {
		Observation obs1 = new Observation();
		obs1.setId("obs-uuid-1");
		Observation obs2 = new Observation();
		obs2.setId("obs-uuid-2");
		Observation obs3 = new Observation();
		obs3.setId("obs-uuid-3");
		List<IBaseResource> observations = Arrays.asList(obs1, obs2, obs3);
		
		ReferenceAndListParam encounterReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam(ENCOUNTER_UUID)));
		
		when(observationService.searchForObservations(any(ObservationSearchParams.class))).thenReturn(bundleProvider);
		when(bundleProvider.getResources(0, Integer.MAX_VALUE)).thenReturn(observations);
		
		Bundle result = resourceProvider.getEverythingByEncounter(encounterReference, requestDetails);
		
		assertNotNull(result);
		assertEquals(Bundle.BundleType.SEARCHSET, result.getType());
		assertEquals(3, result.getTotal());
		assertEquals(3, result.getEntry().size());
		assertEquals(SERVER_BASE + "/Observation/obs-uuid-1", result.getEntry().get(0).getFullUrl());
		assertEquals(SERVER_BASE + "/Observation/obs-uuid-2", result.getEntry().get(1).getFullUrl());
		assertEquals(SERVER_BASE + "/Observation/obs-uuid-3", result.getEntry().get(2).getFullUrl());
		
		verify(observationService).searchForObservations(searchParamsCaptor.capture());
		ObservationSearchParams capturedParams = searchParamsCaptor.getValue();
		assertNotNull(capturedParams.getEncounter());
	}
	
	@Test
	public void testGetEverythingByEncounter_shouldReturnEmptyBundleWhenNoObservationsFound() {
		ReferenceAndListParam encounterReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam(ENCOUNTER_UUID)));
		
		when(observationService.searchForObservations(any(ObservationSearchParams.class))).thenReturn(bundleProvider);
		when(bundleProvider.getResources(0, Integer.MAX_VALUE)).thenReturn(Collections.emptyList());
		
		Bundle result = resourceProvider.getEverythingByEncounter(encounterReference, requestDetails);
		
		assertNotNull(result);
		assertEquals(Bundle.BundleType.SEARCHSET, result.getType());
		assertEquals(0, result.getTotal());
		assertEquals(0, result.getEntry().size());
	}
	
	@Test(expected = InvalidRequestException.class)
	public void testGetEverythingByEncounter_shouldThrowExceptionWhenEncounterIsNull() {
		resourceProvider.getEverythingByEncounter(null, requestDetails);
	}
	
	@Test
	public void testGetEverythingByEncounter_shouldCallSearchWithCorrectEncounterParam() {
		ReferenceAndListParam encounterReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam(ENCOUNTER_UUID)));
		
		when(observationService.searchForObservations(any(ObservationSearchParams.class))).thenReturn(bundleProvider);
		when(bundleProvider.getResources(0, Integer.MAX_VALUE)).thenReturn(Collections.emptyList());
		
		resourceProvider.getEverythingByEncounter(encounterReference, requestDetails);
		
		verify(observationService).searchForObservations(searchParamsCaptor.capture());
		verify(bundleProvider).getResources(0, Integer.MAX_VALUE);
		
		ObservationSearchParams capturedParams = searchParamsCaptor.getValue();
		assertNotNull(capturedParams.getEncounter());
		assertEquals(encounterReference, capturedParams.getEncounter());
	}
	
	@Test
	public void testGetEverythingByEncounter_shouldPopulateBundleMetadata() {
		Observation obs = new Observation();
		obs.setId("obs-uuid-1");
		List<IBaseResource> observations = Arrays.asList(obs);
		
		ReferenceAndListParam encounterReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam(ENCOUNTER_UUID)));
		
		when(observationService.searchForObservations(any(ObservationSearchParams.class))).thenReturn(bundleProvider);
		when(bundleProvider.getResources(0, Integer.MAX_VALUE)).thenReturn(observations);
		
		Bundle result = resourceProvider.getEverythingByEncounter(encounterReference, requestDetails);
		
		assertNotNull(result);
		assertNotNull(result.getId());
		assertNotNull(result.getMeta());
		assertNotNull(result.getMeta().getLastUpdated());
	}
}
