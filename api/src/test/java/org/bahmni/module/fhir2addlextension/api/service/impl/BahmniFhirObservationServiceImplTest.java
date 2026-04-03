package org.bahmni.module.fhir2addlextension.api.service.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.bahmni.module.fhir2addlextension.api.context.RequestContextHolder;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.fhir2.api.search.param.ObservationSearchParams;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirObservationServiceImplTest {
	
	private static final String ENCOUNTER_UUID = "f9df3ec8-fda0-4c8a-9957-cbcdf02de89f";
	
	private static final String SERVER_BASE = "https://localhost/openmrs/ws/fhir2/R4";
	
	@Spy
	private BahmniFhirObservationServiceImpl observationService;
	
	@After
	public void tearDown() {
		RequestContextHolder.clear();
	}
	
	@Test
	public void fetchAllByEncounter_shouldReturnBundleWithAllObservations() {
		Observation obs1 = new Observation();
		obs1.setId("obs-uuid-1");
		Observation obs2 = new Observation();
		obs2.setId("obs-uuid-2");
		Observation obs3 = new Observation();
		obs3.setId("obs-uuid-3");
		List<IBaseResource> observations = Arrays.asList(obs1, obs2, obs3);
		
		ReferenceAndListParam encounterReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam(ENCOUNTER_UUID)));
		
		IBundleProvider bundleProvider = mock(IBundleProvider.class);
		doReturn(bundleProvider).when(observationService).searchForObservations(any(ObservationSearchParams.class));
		when(bundleProvider.getResources(0, Integer.MAX_VALUE)).thenReturn(observations);
		
		RequestContextHolder.setValue(SERVER_BASE);
		
		Bundle result = observationService.fetchAllByEncounter(encounterReference);
		
		assertNotNull(result);
		assertEquals(Bundle.BundleType.SEARCHSET, result.getType());
		assertEquals(3, result.getTotal());
		assertEquals(3, result.getEntry().size());
		assertEquals(SERVER_BASE + "/Observation/obs-uuid-1", result.getEntry().get(0).getFullUrl());
		assertEquals(SERVER_BASE + "/Observation/obs-uuid-2", result.getEntry().get(1).getFullUrl());
		assertEquals(SERVER_BASE + "/Observation/obs-uuid-3", result.getEntry().get(2).getFullUrl());
	}
	
	@Test
	public void fetchAllByEncounter_shouldReturnEmptyBundleWhenNoObservationsFound() {
		ReferenceAndListParam encounterReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam(ENCOUNTER_UUID)));
		
		IBundleProvider bundleProvider = mock(IBundleProvider.class);
		doReturn(bundleProvider).when(observationService).searchForObservations(any(ObservationSearchParams.class));
		when(bundleProvider.getResources(0, Integer.MAX_VALUE)).thenReturn(Collections.emptyList());
		
		RequestContextHolder.setValue(SERVER_BASE);
		
		Bundle result = observationService.fetchAllByEncounter(encounterReference);
		
		assertNotNull(result);
		assertEquals(Bundle.BundleType.SEARCHSET, result.getType());
		assertEquals(0, result.getTotal());
		assertEquals(0, result.getEntry().size());
	}
	
	@Test
	public void fetchAllByEncounter_shouldPopulateBundleMetadata() {
		Observation obs = new Observation();
		obs.setId("obs-uuid-1");
		List<IBaseResource> observations = Arrays.asList(obs);
		
		ReferenceAndListParam encounterReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam(ENCOUNTER_UUID)));
		
		IBundleProvider bundleProvider = mock(IBundleProvider.class);
		doReturn(bundleProvider).when(observationService).searchForObservations(any(ObservationSearchParams.class));
		when(bundleProvider.getResources(0, Integer.MAX_VALUE)).thenReturn(observations);
		
		RequestContextHolder.setValue(SERVER_BASE);
		
		Bundle result = observationService.fetchAllByEncounter(encounterReference);
		
		assertNotNull(result);
		assertNotNull(result.getId());
		assertNotNull(result.getMeta());
		assertNotNull(result.getMeta().getLastUpdated());
	}
	
}
