package org.bahmni.module.fhir2addlextension.api.providers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirMedicationRequestService;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BahmniMedicationRequestFhirR4ProviderTest {
	
	@Mock
	private BahmniFhirMedicationRequestService bahmniFhirMedicationRequestService;
	
	@InjectMocks
	private BahmniMedicationRequestFhirR4Provider provider;
	
	@Test
	public void createMedicationRequest_shouldDelegateToService() {
		MedicationRequest request = new MedicationRequest();
		MedicationRequest created = new MedicationRequest();
		created.setId("new-id");
		when(bahmniFhirMedicationRequestService.create(request)).thenReturn(created);
		
		provider.createMedicationRequest(request);
		
		verify(bahmniFhirMedicationRequestService).create(request);
	}
}
