package org.bahmni.module.fhir2addlextension.api.providers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bahmni.module.fhir2addlextension.api.search.param.BahmniMedicationRequestSearchParams;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.fhir2.api.FhirMedicationRequestService;

import java.util.HashSet;

@RunWith(MockitoJUnitRunner.class)
public class BahmniMedicationRequestFhirR4ProviderTest {
	
	@Mock
	private FhirMedicationRequestService fhirMedicationRequestService;
	
	@InjectMocks
	private BahmniMedicationRequestFhirR4Provider provider;
	
	@Test
	public void createMedicationRequest_shouldDelegateToService() {
		MedicationRequest request = new MedicationRequest();
		MedicationRequest created = new MedicationRequest();
		created.setId("new-id");
		when(fhirMedicationRequestService.create(request)).thenReturn(created);
		
		provider.createMedicationRequest(request);
		
		verify(fhirMedicationRequestService).create(request);
	}
	
	@Test
	public void searchForMedicationRequestsWithLocation_shouldCallSearchService() {
		when(fhirMedicationRequestService.searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class)))
		    .thenReturn(null);

		provider.searchForMedicationRequestsWithLocation(null, null, null, null, null, null, null, null, null, null,
		    null, new HashSet<>(), new HashSet<>(), null);

		verify(fhirMedicationRequestService).searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class));
	}
	
	@Test
	public void searchForMedicationRequestsWithLocation_shouldConvertEmptyIncludesToNull() {
		when(fhirMedicationRequestService.searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class)))
		    .thenReturn(null);

		provider.searchForMedicationRequestsWithLocation(null, null, null, null, null, null, null, null, null, null,
		    null, new HashSet<>(), new HashSet<>(), null);

		verify(fhirMedicationRequestService).searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class));
	}
}
