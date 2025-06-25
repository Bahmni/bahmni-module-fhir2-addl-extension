/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Date;

import org.hl7.fhir.r4.model.MedicationRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.DrugOrder;
import org.openmrs.Order;

/**
 * Unit tests for BahmniMedicationRequestStatusTranslatorImpl Tests the integration between the
 * translator and the status calculator Following TDD approach - these tests will initially fail
 * until implementation is complete
 */
@RunWith(MockitoJUnitRunner.class)
public class BahmniMedicationRequestStatusTranslatorImplTest {
	
	private static final String DRUG_ORDER_UUID = "44fdc8ad-fe4d-499b-93a8-8a991c1d477e";
	
	@Mock
	private BahmniMedicationStatusCalculator statusCalculator;
	
	@InjectMocks
	private BahmniMedicationRequestStatusTranslatorImpl statusTranslator;
	
	private DrugOrder drugOrder;
	
	private Date today;
	
	private Date yesterday;
	
	private Date tomorrow;
	
	@Before
	public void setup() {
		// Setup test dates
		Calendar cal = Calendar.getInstance();
		today = cal.getTime();
		
		cal.add(Calendar.DAY_OF_MONTH, -1);
		yesterday = cal.getTime();
		
		cal.add(Calendar.DAY_OF_MONTH, 2);
		tomorrow = cal.getTime();
		
		// Setup basic drug order
		drugOrder = new DrugOrder();
		drugOrder.setUuid(DRUG_ORDER_UUID);
		drugOrder.setDateActivated(yesterday);
		drugOrder.setAction(Order.Action.NEW);
		drugOrder.setVoided(false);
	}
	
	// ========== INTEGRATION TESTS WITH STATUS CALCULATOR ==========
	
	@Test
	public void toFhirResource_shouldReturnNull_whenDrugOrderIsNull() {
		// When: Translate null drug order
		MedicationRequest.MedicationRequestStatus status = statusTranslator.toFhirResource(null);
		
		// Then: Should return null
		assertThat(status, nullValue());
	}
	
	@Test
	public void toFhirResource_shouldDelegateToCalculator_andReturnActive() {
		// Given: Status calculator returns ACTIVE
		when(statusCalculator.calculateStatus(drugOrder)).thenReturn(MedicationRequest.MedicationRequestStatus.ACTIVE);
		
		// When: Translate to FHIR status
		MedicationRequest.MedicationRequestStatus status = statusTranslator.toFhirResource(drugOrder);
		
		// Then: Should return ACTIVE from calculator
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldDelegateToCalculator_andReturnOnHold() {
		// Given: Status calculator returns ON_HOLD
		when(statusCalculator.calculateStatus(drugOrder)).thenReturn(MedicationRequest.MedicationRequestStatus.ONHOLD);
		
		// When: Translate to FHIR status
		MedicationRequest.MedicationRequestStatus status = statusTranslator.toFhirResource(drugOrder);
		
		// Then: Should return ON_HOLD from calculator
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.ONHOLD));
	}
	
	@Test
	public void toFhirResource_shouldDelegateToCalculator_andReturnStopped() {
		// Given: Status calculator returns STOPPED
		when(statusCalculator.calculateStatus(drugOrder)).thenReturn(MedicationRequest.MedicationRequestStatus.STOPPED);
		
		// When: Translate to FHIR status
		MedicationRequest.MedicationRequestStatus status = statusTranslator.toFhirResource(drugOrder);
		
		// Then: Should return STOPPED from calculator
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.STOPPED));
	}
	
	@Test
	public void toFhirResource_shouldDelegateToCalculator_andReturnCancelled() {
		// Given: Status calculator returns CANCELLED
		when(statusCalculator.calculateStatus(drugOrder)).thenReturn(MedicationRequest.MedicationRequestStatus.CANCELLED);
		
		// When: Translate to FHIR status
		MedicationRequest.MedicationRequestStatus status = statusTranslator.toFhirResource(drugOrder);
		
		// Then: Should return CANCELLED from calculator
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.CANCELLED));
	}
	
	@Test
	public void toFhirResource_shouldDelegateToCalculator_andReturnCompleted() {
		// Given: Status calculator returns COMPLETED
		when(statusCalculator.calculateStatus(drugOrder)).thenReturn(MedicationRequest.MedicationRequestStatus.COMPLETED);
		
		// When: Translate to FHIR status
		MedicationRequest.MedicationRequestStatus status = statusTranslator.toFhirResource(drugOrder);
		
		// Then: Should return COMPLETED from calculator
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.COMPLETED));
	}
	
	@Test
	public void toFhirResource_shouldDelegateToCalculator_andReturnUnknown() {
		// Given: Status calculator returns UNKNOWN
		when(statusCalculator.calculateStatus(drugOrder)).thenReturn(MedicationRequest.MedicationRequestStatus.UNKNOWN);
		
		// When: Translate to FHIR status
		MedicationRequest.MedicationRequestStatus status = statusTranslator.toFhirResource(drugOrder);
		
		// Then: Should return UNKNOWN from calculator
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.UNKNOWN));
	}
	
	@Test
	public void toFhirResource_shouldHandleCalculatorReturningNull() {
		// Given: Status calculator returns null
		when(statusCalculator.calculateStatus(drugOrder)).thenReturn(null);
		
		// When: Translate to FHIR status
		MedicationRequest.MedicationRequestStatus status = statusTranslator.toFhirResource(drugOrder);
		
		// Then: Should return null
		assertThat(status, nullValue());
	}
	
	@Test
	public void toFhirResource_shouldBeConsistent_acrossMultipleCalls() {
		// Given: Status calculator consistently returns ACTIVE
		when(statusCalculator.calculateStatus(drugOrder)).thenReturn(MedicationRequest.MedicationRequestStatus.ACTIVE);
		
		// When: Translate multiple times
		MedicationRequest.MedicationRequestStatus status1 = statusTranslator.toFhirResource(drugOrder);
		MedicationRequest.MedicationRequestStatus status2 = statusTranslator.toFhirResource(drugOrder);
		MedicationRequest.MedicationRequestStatus status3 = statusTranslator.toFhirResource(drugOrder);
		
		// Then: All calls should return the same result
		assertThat(status1, equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
		assertThat(status2, equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
		assertThat(status3, equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
		assertThat(status1, equalTo(status2));
		assertThat(status2, equalTo(status3));
	}
	
	@Test
	public void toFhirResource_shouldHandleDifferentDrugOrders() {
		// Given: Different drug orders
		DrugOrder activeOrder = new DrugOrder();
		activeOrder.setUuid("active-order");
		
		DrugOrder stoppedOrder = new DrugOrder();
		stoppedOrder.setUuid("stopped-order");
		
		// And: Calculator returns different statuses for different orders
		when(statusCalculator.calculateStatus(activeOrder)).thenReturn(MedicationRequest.MedicationRequestStatus.ACTIVE);
		when(statusCalculator.calculateStatus(stoppedOrder)).thenReturn(MedicationRequest.MedicationRequestStatus.STOPPED);
		
		// When: Translate both orders
		MedicationRequest.MedicationRequestStatus activeStatus = statusTranslator.toFhirResource(activeOrder);
		MedicationRequest.MedicationRequestStatus stoppedStatus = statusTranslator.toFhirResource(stoppedOrder);
		
		// Then: Should return different statuses as calculated
		assertThat(activeStatus, equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
		assertThat(stoppedStatus, equalTo(MedicationRequest.MedicationRequestStatus.STOPPED));
	}
}
