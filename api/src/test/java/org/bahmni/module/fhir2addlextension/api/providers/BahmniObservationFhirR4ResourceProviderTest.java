package org.bahmni.module.fhir2addlextension.api.providers;

import ca.uhn.fhir.model.api.Include;
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
import org.openmrs.module.fhir2.api.search.param.ObservationSearchParams;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
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
	
	@Captor
	private ArgumentCaptor<ObservationSearchParams> lastnSearchParamsCaptor;
	
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
		
		when(observationService.fetchAllObservation(any(BahmniObservationSearchParams.class))).thenReturn(expectedBundle);
		
		Bundle result = resourceProvider.searchAllObservation(encounterReference, basedOnReference, requestDetails);
		
		assertSame(expectedBundle, result);
		assertEquals(Bundle.BundleType.SEARCHSET, result.getType());
		assertEquals(3, result.getTotal());
		verify(observationService).fetchAllObservation(searchParamsCaptor.capture());
		BahmniObservationSearchParams capturedParams = searchParamsCaptor.getValue();
		assertTrue(capturedParams.hasEncounterReference());
		assertTrue(capturedParams.hasBasedOnReference());
	}
	
	@Test
	public void testGetEverythingByEncounterWithoutEncounter_shouldThrowException() {
		assertThrows(InvalidRequestException.class, () -> {
			resourceProvider.searchAllObservation(null, null, requestDetails);
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
	public void lastn_shouldFilterByEncounterReference() {
		ReferenceAndListParam encounterReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam(ENCOUNTER_UUID)));
		
		when(observationService.getLastnObservations(any(), any())).thenReturn(bundleProvider);
		
		IBundleProvider result = resourceProvider.getLastnObservations(null, null, null, null, null, encounterReference,
		    null, null);
		
		assertNotNull(result);
		verify(observationService).getLastnObservations(isNull(), lastnSearchParamsCaptor.capture());
		ReferenceParam ref = lastnSearchParamsCaptor.getValue().getEncounter().getValuesAsQueryTokens().get(0)
		        .getValuesAsQueryTokens().get(0);
		assertThat(ref.getIdPart(), equalTo(ENCOUNTER_UUID));
	}
	
	@Test
	public void lastn_shouldPassIncludesToSearchParams() {
		HashSet<Include> includes = new HashSet<>();
		includes.add(new Include("Observation:encounter"));

		when(observationService.getLastnObservations(any(), any())).thenReturn(bundleProvider);

		resourceProvider.getLastnObservations(null, null, null, null, null, null, includes, null);

		verify(observationService).getLastnObservations(isNull(), lastnSearchParamsCaptor.capture());
		Set<Include> capturedIncludes = lastnSearchParamsCaptor.getValue().getIncludes();
		assertThat(capturedIncludes, hasItem(hasProperty("paramName", equalTo("encounter"))));
	}
	
	@Test
	public void lastn_shouldPassRevIncludesToSearchParams() {
		HashSet<Include> revIncludes = new HashSet<>();
		revIncludes.add(new Include("Observation:has-member", true));

		when(observationService.getLastnObservations(any(), any())).thenReturn(bundleProvider);

		resourceProvider.getLastnObservations(null, null, null, null, null, null, null, revIncludes);

		verify(observationService).getLastnObservations(isNull(), lastnSearchParamsCaptor.capture());
		Set<Include> capturedRevIncludes = lastnSearchParamsCaptor.getValue().getRevIncludes();
		assertThat(capturedRevIncludes, hasItem(hasProperty("paramName", equalTo("has-member"))));
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
