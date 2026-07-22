package org.bahmni.module.fhir2addlextension.api.service.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.bahmni.module.fhir2addlextension.api.context.RequestContextHolder;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniObsDao;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniObservationSearchParams;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Obs;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.search.param.ObservationSearchParams;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirObservationServiceImplTest {
	
	private static final String ENCOUNTER_UUID = "f9df3ec8-fda0-4c8a-9957-cbcdf02de89f";
	
	private static final String PATIENT_UUID = "patient-uuid-123";
	
	private static final String SERVICE_REQUEST_UUID = "service-request-uuid-456";
	
	private static final String SERVER_BASE = "https://localhost/openmrs/ws/fhir2/R4";
	
	@Mock
	private BahmniObsDao bahmniObsDao;
	
	@Mock
	private SearchQueryInclude<Observation> searchQueryInclude;
	
	@Mock
	private SearchQuery<Obs, Observation, FhirDao<Obs>, OpenmrsFhirTranslator<Obs, Observation>, SearchQueryInclude<Observation>> searchQuery;
	
	private BahmniFhirObservationServiceImpl observationService;
	
	@Mock
	private OpenmrsFhirTranslator<Obs, Observation> translator;
	
	@Before
	public void setUp() {
		observationService = org.mockito.Mockito.spy(new BahmniFhirObservationServiceImpl(bahmniObsDao, searchQueryInclude,
		        searchQuery));
	}
	
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
	
	@Test
	public void searchObservations_shouldReturnResultsWhenBasedOnReferenceProvided() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam(SERVICE_REQUEST_UUID)));
		
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(null, basedOnReference, null, null);
		
		IBundleProvider expectedResults = mock(IBundleProvider.class);
		when(searchQuery.getQueryResults(any(SearchParameterMap.class), eq(bahmniObsDao), any(), eq(searchQueryInclude)))
		        .thenReturn(expectedResults);
		
		IBundleProvider result = observationService.searchObservations(searchParams);
		
		assertNotNull(result);
		assertEquals(expectedResults, result);
		verify(searchQuery).getQueryResults(any(SearchParameterMap.class), eq(bahmniObsDao), any(), eq(searchQueryInclude));
	}
	
	@Test
	public void searchObservations_shouldReturnResultsWhenPatientReferenceProvided() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(PATIENT_UUID)));
		
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(patientReference, null, null, null);
		
		IBundleProvider expectedResults = mock(IBundleProvider.class);
		when(searchQuery.getQueryResults(any(SearchParameterMap.class), eq(bahmniObsDao), any(), eq(searchQueryInclude)))
		        .thenReturn(expectedResults);
		
		IBundleProvider result = observationService.searchObservations(searchParams);
		
		assertNotNull(result);
		assertEquals(expectedResults, result);
	}
	
	@Test
	public void searchObservations_shouldReturnResultsWhenBothPatientAndBasedOnProvided() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(PATIENT_UUID)));
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam(SERVICE_REQUEST_UUID)));
		
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(patientReference, basedOnReference,
		        null, null);
		
		IBundleProvider expectedResults = mock(IBundleProvider.class);
		when(searchQuery.getQueryResults(any(SearchParameterMap.class), eq(bahmniObsDao), any(), eq(searchQueryInclude)))
		        .thenReturn(expectedResults);
		
		IBundleProvider result = observationService.searchObservations(searchParams);
		
		assertNotNull(result);
		assertEquals(expectedResults, result);
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void searchObservations_shouldThrowExceptionWhenNoSearchParametersProvided() {
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(null, null, null, null);
		
		observationService.searchObservations(searchParams);
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void searchObservations_shouldThrowExceptionWhenOnlyEmptyParametersProvided() {
		ReferenceAndListParam emptyPatient = new ReferenceAndListParam();
		ReferenceAndListParam emptyBasedOn = new ReferenceAndListParam();
		
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(emptyPatient, emptyBasedOn, null,
		        null);
		
		observationService.searchObservations(searchParams);
	}
	
}
