package org.bahmni.module.fhir2addlextension.api.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Date;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.Provider;
import org.openmrs.api.EncounterService;
import org.openmrs.api.OrderService;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirMedicationRequestServiceImplTest {
	
	private static final String ORDER_UUID = "order-uuid-123";
	
	@Mock
	private OrderService orderService;
	
	@Mock
	private EncounterService encounterService;
	
	@Spy
	@InjectMocks
	private BahmniFhirMedicationRequestServiceImpl service;
	
	// --- Resource resolution ---
	
	@Test(expected = ResourceNotFoundException.class)
	public void stopMedicationRequest_givenOrderNotFound_shouldThrowResourceNotFound() {
		when(orderService.getOrderByUuid(ORDER_UUID)).thenReturn(null);
		
		service.stopMedicationRequest(ORDER_UUID, null, null, null, null);
	}
	
	@Test(expected = UnprocessableEntityException.class)
	public void stopMedicationRequest_givenOrderIsNotDrugOrder_shouldThrowUnprocessableEntity() {
		when(orderService.getOrderByUuid(ORDER_UUID)).thenReturn(new Order());
		
		service.stopMedicationRequest(ORDER_UUID, null, null, null, null);
	}
	
	// --- Reason extraction ---
	
	@Test
	public void stopMedicationRequest_givenReasonWithText_shouldUseText() {
		DrugOrder drugOrder = buildDrugOrder();
		when(orderService.getOrderByUuid(ORDER_UUID)).thenReturn(drugOrder);
		doReturn(new MedicationRequest()).when(service).get(ORDER_UUID);
		
		CodeableConcept reason = new CodeableConcept().addCoding(
		    new Coding().setCode("uuid-1").setDisplay("Adverse reaction")).setText("Adverse reaction");
		
		service.stopMedicationRequest(ORDER_UUID, reason, null, null, null);
		
		verify(orderService).discontinueOrder(eq(drugOrder), eq("Adverse reaction"), any(Date.class),
		    eq(drugOrder.getOrderer()), eq(drugOrder.getEncounter()));
	}
	
	@Test
	public void stopMedicationRequest_givenReasonWithCodingDisplayOnly_shouldUseCodingDisplay() {
		DrugOrder drugOrder = buildDrugOrder();
		when(orderService.getOrderByUuid(ORDER_UUID)).thenReturn(drugOrder);
		doReturn(new MedicationRequest()).when(service).get(ORDER_UUID);
		
		CodeableConcept reason = new CodeableConcept().addCoding(new Coding().setCode("uuid-1")
		        .setDisplay("Patient request"));
		
		service.stopMedicationRequest(ORDER_UUID, reason, null, null, null);
		
		verify(orderService).discontinueOrder(eq(drugOrder), eq("Patient request"), any(Date.class),
		    eq(drugOrder.getOrderer()), eq(drugOrder.getEncounter()));
	}
	
	@Test
	public void stopMedicationRequest_givenNullReason_shouldPassNullReasonText() {
		DrugOrder drugOrder = buildDrugOrder();
		when(orderService.getOrderByUuid(ORDER_UUID)).thenReturn(drugOrder);
		doReturn(new MedicationRequest()).when(service).get(ORDER_UUID);
		
		service.stopMedicationRequest(ORDER_UUID, null, null, null, null);
		
		verify(orderService).discontinueOrder(eq(drugOrder), isNull(String.class), any(Date.class),
		    eq(drugOrder.getOrderer()), eq(drugOrder.getEncounter()));
	}
	
	@Test
	public void stopMedicationRequest_givenReasonAndNote_shouldConcatenate() {
		DrugOrder drugOrder = buildDrugOrder();
		when(orderService.getOrderByUuid(ORDER_UUID)).thenReturn(drugOrder);
		doReturn(new MedicationRequest()).when(service).get(ORDER_UUID);
		
		CodeableConcept reason = new CodeableConcept().setText("Side effects");
		
		service.stopMedicationRequest(ORDER_UUID, reason, null, "Rash observed", null);
		
		verify(orderService).discontinueOrder(eq(drugOrder), eq("Side effects - Rash observed"), any(Date.class),
		    eq(drugOrder.getOrderer()), eq(drugOrder.getEncounter()));
	}
	
	@Test
	public void stopMedicationRequest_givenOnlyNote_shouldUseNoteAsReason() {
		DrugOrder drugOrder = buildDrugOrder();
		when(orderService.getOrderByUuid(ORDER_UUID)).thenReturn(drugOrder);
		doReturn(new MedicationRequest()).when(service).get(ORDER_UUID);
		
		service.stopMedicationRequest(ORDER_UUID, null, null, "Patient declined", null);
		
		verify(orderService).discontinueOrder(eq(drugOrder), eq("Patient declined"), any(Date.class),
		    eq(drugOrder.getOrderer()), eq(drugOrder.getEncounter()));
	}
	
	// --- Date handling ---
	
	@Test
	public void stopMedicationRequest_givenNoEffectiveDate_shouldDefaultToNow() {
		DrugOrder drugOrder = buildDrugOrder();
		when(orderService.getOrderByUuid(ORDER_UUID)).thenReturn(drugOrder);
		doReturn(new MedicationRequest()).when(service).get(ORDER_UUID);
		
		Date before = new Date();
		service.stopMedicationRequest(ORDER_UUID, null, null, null, null);
		
		ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);
		verify(orderService).discontinueOrder(eq(drugOrder), isNull(String.class), dateCaptor.capture(),
		    eq(drugOrder.getOrderer()), eq(drugOrder.getEncounter()));
		assertThat(dateCaptor.getValue().getTime() - before.getTime(), lessThan(5000L));
	}
	
	@Test
	public void stopMedicationRequest_givenTodayAsEffectiveDate_shouldPassThatDate() {
		DrugOrder drugOrder = buildDrugOrder();
		when(orderService.getOrderByUuid(ORDER_UUID)).thenReturn(drugOrder);
		doReturn(new MedicationRequest()).when(service).get(ORDER_UUID);
		
		Date today = new Date();
		service.stopMedicationRequest(ORDER_UUID, null, today, null, null);
		
		verify(orderService).discontinueOrder(eq(drugOrder), isNull(String.class), eq(today), eq(drugOrder.getOrderer()),
		    eq(drugOrder.getEncounter()));
	}
	
	@Test
	public void stopMedicationRequest_givenFutureDate_shouldSetAutoExpireDateAndDiscontinueWithNow() {
		DrugOrder drugOrder = buildDrugOrder();
		when(orderService.getOrderByUuid(ORDER_UUID)).thenReturn(drugOrder);
		doReturn(new MedicationRequest()).when(service).get(ORDER_UUID);
		
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, 7);
		Date futureDate = cal.getTime();
		
		Date beforeCall = new Date();
		service.stopMedicationRequest(ORDER_UUID, null, futureDate, null, null);
		
		assertThat(drugOrder.getAutoExpireDate(), equalTo(futureDate));
		ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);
		verify(orderService).discontinueOrder(eq(drugOrder), isNull(String.class), dateCaptor.capture(),
		    eq(drugOrder.getOrderer()), eq(drugOrder.getEncounter()));
		
		Date capturedDate = dateCaptor.getValue();
		assertThat(capturedDate.before(futureDate), equalTo(true));
		assertThat(capturedDate.getTime() - beforeCall.getTime(), lessThan(5000L));
	}
	
	// --- Return value ---
	
	@Test
	public void stopMedicationRequest_shouldReturnUpdatedMedicationRequest() {
		DrugOrder drugOrder = buildDrugOrder();
		when(orderService.getOrderByUuid(ORDER_UUID)).thenReturn(drugOrder);
		MedicationRequest updated = new MedicationRequest();
		updated.setId(ORDER_UUID);
		doReturn(updated).when(service).get(ORDER_UUID);
		
		MedicationRequest result = service.stopMedicationRequest(ORDER_UUID, null, null, null, null);
		
		assertThat(result, notNullValue());
		assertThat(result.getId(), equalTo(ORDER_UUID));
	}
	
	// --- Error handling ---
	
	@Test(expected = UnprocessableEntityException.class)
	public void stopMedicationRequest_givenDiscontinueThrows_shouldWrapAsUnprocessableEntity() {
		DrugOrder drugOrder = buildDrugOrder();
		when(orderService.getOrderByUuid(ORDER_UUID)).thenReturn(drugOrder);
		when(
		    orderService.discontinueOrder(any(Order.class), nullable(String.class), any(Date.class), any(Provider.class),
		        any(Encounter.class))).thenThrow(new RuntimeException("DB error"));
		
		service.stopMedicationRequest(ORDER_UUID, null, null, null, null);
	}
	
	@Test(expected = UnprocessableEntityException.class)
	public void stopMedicationRequest_givenUnprocessableEntityFromDiscontinue_shouldPropagate() {
		DrugOrder drugOrder = buildDrugOrder();
		when(orderService.getOrderByUuid(ORDER_UUID)).thenReturn(drugOrder);
		when(
		    orderService.discontinueOrder(any(Order.class), nullable(String.class), any(Date.class), any(Provider.class),
		        any(Encounter.class))).thenThrow(new UnprocessableEntityException("already stopped"));
		
		service.stopMedicationRequest(ORDER_UUID, null, null, null, null);
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
