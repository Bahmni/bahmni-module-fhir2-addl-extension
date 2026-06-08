package org.bahmni.module.fhir2addlextension.api.providers;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniObservationFhirR4ResourceProviderTest {
	
	private static final String ENCOUNTER_UUID = "f9df3ec8-fda0-4c8a-9957-cbcdf02de89f";
	
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
}
