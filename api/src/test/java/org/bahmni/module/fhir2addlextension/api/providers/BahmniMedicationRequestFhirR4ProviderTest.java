package org.bahmni.module.fhir2addlextension.api.providers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Date;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.Provider;
import org.openmrs.api.EncounterService;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.api.FhirMedicationRequestService;

@RunWith(MockitoJUnitRunner.class)
public class BahmniMedicationRequestFhirR4ProviderTest {
	
	private static final String ORDER_UUID = "order-uuid-123";
	
	@Mock
	private FhirMedicationRequestService fhirMedicationRequestService;
	
	@Mock
	private OrderService orderService;
	
	@Mock
	private EncounterService encounterService;
	
	@InjectMocks
	private BahmniMedicationRequestFhirR4Provider provider;
	
	@Test
	public void createMedicationRequest_shouldDelegateToFhirService() {
		MedicationRequest request = new MedicationRequest();
		MedicationRequest created = new MedicationRequest();
		created.setId("new-id");
		when(fhirMedicationRequestService.create(request)).thenReturn(created);
		
		provider.createMedicationRequest(request);
		
		verify(fhirMedicationRequestService).create(request);
	}
	
	@Test(expected = ResourceNotFoundException.class)
	public void stopMedicationRequest_givenOrderNotFound_shouldThrowResourceNotFound() {
		when(orderService.getOrderByUuid(ORDER_UUID)).thenReturn(null);
		
		provider.stopMedicationRequest(new IdType(ORDER_UUID), null, null, null);
	}
	
	@Test(expected = UnprocessableEntityException.class)
	public void stopMedicationRequest_givenOrderIsNotDrugOrder_shouldThrowUnprocessableEntity() {
		Order nonDrugOrder = new Order();
		when(orderService.getOrderByUuid(ORDER_UUID)).thenReturn(nonDrugOrder);
		
		provider.stopMedicationRequest(new IdType(ORDER_UUID), null, null, null);
	}
	
	@Test
	public void stopMedicationRequest_givenValidDrugOrder_shouldDiscontinueAndReturnUpdated() {
		DrugOrder drugOrder = buildDrugOrder();
		when(orderService.getOrderByUuid(ORDER_UUID)).thenReturn(drugOrder);
		
		MedicationRequest updatedRequest = new MedicationRequest();
		updatedRequest.setId(ORDER_UUID);
		when(fhirMedicationRequestService.get(ORDER_UUID)).thenReturn(updatedRequest);
		
		MedicationRequest result = provider.stopMedicationRequest(new IdType(ORDER_UUID), new StringType("Patient request"),
		    null, null);
		
		assertThat(result, notNullValue());
		assertThat(result.getId(), equalTo(ORDER_UUID));
		verify(orderService).discontinueOrder(eq(drugOrder), eq("Patient request"), any(Date.class),
		    eq(drugOrder.getOrderer()), eq(drugOrder.getEncounter()));
	}
	
	@Test
	public void stopMedicationRequest_givenNoReason_shouldPassNullReason() {
		DrugOrder drugOrder = buildDrugOrder();
		when(orderService.getOrderByUuid(ORDER_UUID)).thenReturn(drugOrder);
		when(fhirMedicationRequestService.get(ORDER_UUID)).thenReturn(new MedicationRequest());
		
		provider.stopMedicationRequest(new IdType(ORDER_UUID), null, null, null);
		
		verify(orderService).discontinueOrder(eq(drugOrder), isNull(String.class), any(Date.class),
		    eq(drugOrder.getOrderer()), eq(drugOrder.getEncounter()));
	}
	
	@Test
	public void stopMedicationRequest_givenReasonAndNote_shouldConcatenate() {
		DrugOrder drugOrder = buildDrugOrder();
		when(orderService.getOrderByUuid(ORDER_UUID)).thenReturn(drugOrder);
		when(fhirMedicationRequestService.get(ORDER_UUID)).thenReturn(new MedicationRequest());
		
		provider.stopMedicationRequest(new IdType(ORDER_UUID), new StringType("Side effects"), null, new StringType(
		        "Rash observed"));
		
		verify(orderService).discontinueOrder(eq(drugOrder), eq("Side effects - Rash observed"), any(Date.class),
		    eq(drugOrder.getOrderer()), eq(drugOrder.getEncounter()));
	}
	
	@Test
	public void stopMedicationRequest_givenOnlyNote_shouldUseNoteAsReason() {
		DrugOrder drugOrder = buildDrugOrder();
		when(orderService.getOrderByUuid(ORDER_UUID)).thenReturn(drugOrder);
		when(fhirMedicationRequestService.get(ORDER_UUID)).thenReturn(new MedicationRequest());
		
		provider.stopMedicationRequest(new IdType(ORDER_UUID), null, null, new StringType("Patient declined"));
		
		verify(orderService).discontinueOrder(eq(drugOrder), eq("Patient declined"), any(Date.class),
		    eq(drugOrder.getOrderer()), eq(drugOrder.getEncounter()));
	}
	
	@Test
	public void stopMedicationRequest_givenEffectiveDate_shouldUseProvidedDate() {
		DrugOrder drugOrder = buildDrugOrder();
		when(orderService.getOrderByUuid(ORDER_UUID)).thenReturn(drugOrder);
		when(fhirMedicationRequestService.get(ORDER_UUID)).thenReturn(new MedicationRequest());
		
		Date today = new Date();
		provider.stopMedicationRequest(new IdType(ORDER_UUID), null, new DateType(today), null);
		
		verify(orderService).discontinueOrder(eq(drugOrder), isNull(String.class), eq(today), eq(drugOrder.getOrderer()),
		    eq(drugOrder.getEncounter()));
	}
	
	@Test
	public void stopMedicationRequest_givenFutureDate_shouldSetAutoExpireDateAndDiscontinueWithNow() {
		DrugOrder drugOrder = buildDrugOrder();
		when(orderService.getOrderByUuid(ORDER_UUID)).thenReturn(drugOrder);
		when(fhirMedicationRequestService.get(ORDER_UUID)).thenReturn(new MedicationRequest());
		
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, 7);
		Date futureDate = cal.getTime();
		
		provider.stopMedicationRequest(new IdType(ORDER_UUID), new StringType("Tapering"), new DateType(futureDate), null);
		
		assertThat(drugOrder.getAutoExpireDate(), equalTo(futureDate));
		verify(orderService).discontinueOrder(eq(drugOrder), eq("Tapering"), any(Date.class), eq(drugOrder.getOrderer()),
		    eq(drugOrder.getEncounter()));
	}
	
	@Test(expected = UnprocessableEntityException.class)
	public void stopMedicationRequest_givenDiscontinueThrowsException_shouldThrowUnprocessableEntity() {
		DrugOrder drugOrder = buildDrugOrder();
		when(orderService.getOrderByUuid(ORDER_UUID)).thenReturn(drugOrder);
		when(
		    orderService.discontinueOrder(any(Order.class), nullable(String.class), any(Date.class), any(Provider.class),
		        any(Encounter.class))).thenThrow(new RuntimeException("DB error"));
		
		provider.stopMedicationRequest(new IdType(ORDER_UUID), null, null, null);
	}
	
	private DrugOrder buildDrugOrder() {
		DrugOrder drugOrder = new DrugOrder();
		drugOrder.setUuid(ORDER_UUID);
		Provider orderer = new Provider();
		orderer.setUuid("provider-uuid");
		drugOrder.setOrderer(orderer);
		Encounter encounter = new Encounter();
		encounter.setUuid("encounter-uuid");
		drugOrder.setEncounter(encounter);
		return drugOrder;
	}
}
