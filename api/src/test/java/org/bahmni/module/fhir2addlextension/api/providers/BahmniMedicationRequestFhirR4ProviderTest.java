package org.bahmni.module.fhir2addlextension.api.providers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirMedicationRequestService;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BahmniMedicationRequestFhirR4ProviderTest {
	
	private static final String ORDER_UUID = "order-uuid-123";
	
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
	
	@Test
	public void stopMedicationRequest_shouldDelegateToServiceWithUnpackedParams() {
		CodeableConcept reason = new CodeableConcept().addCoding(
		    new Coding().setCode("uuid-1").setDisplay("Adverse reaction")).setText("Adverse reaction");
		Date date = new Date();
		MedicationRequest expected = new MedicationRequest();
		expected.setId(ORDER_UUID);
		when(
		    bahmniFhirMedicationRequestService.stopMedicationRequest(eq(ORDER_UUID), eq(reason), any(Date.class),
		        eq("rash"), isNull())).thenReturn(expected);
		
		MedicationRequest result = provider.stopMedicationRequest(new IdType(ORDER_UUID), reason, new DateType(date),
		    new StringType("rash"), null);
		
		assertThat(result, notNullValue());
		assertThat(result.getId(), equalTo(ORDER_UUID));
		verify(bahmniFhirMedicationRequestService).stopMedicationRequest(eq(ORDER_UUID), eq(reason), any(Date.class),
		    eq("rash"), isNull());
	}
	
	@Test
	public void stopMedicationRequest_givenNullOptionalParams_shouldPassNulls() {
		MedicationRequest expected = new MedicationRequest();
		when(
		    bahmniFhirMedicationRequestService.stopMedicationRequest(eq(ORDER_UUID), isNull(), isNull(), isNull(), isNull()))
		        .thenReturn(expected);
		
		provider.stopMedicationRequest(new IdType(ORDER_UUID), null, null, null, null);
		
		verify(bahmniFhirMedicationRequestService).stopMedicationRequest(ORDER_UUID, null, null, null, null);
	}
}
