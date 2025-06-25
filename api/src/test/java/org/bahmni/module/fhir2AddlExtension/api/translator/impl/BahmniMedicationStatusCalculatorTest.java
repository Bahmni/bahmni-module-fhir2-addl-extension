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
import static org.hamcrest.Matchers.nullValue;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;

import org.hl7.fhir.r4.model.MedicationRequest;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.DrugOrder;
import org.openmrs.Order;

/**
 * Unit tests for BahmniMedicationStatusCalculator Tests the core status calculation logic based on
 * DrugOrder state Following TDD approach - these tests will initially fail until implementation is
 * complete
 */
public class BahmniMedicationStatusCalculatorTest {
	
	private static final String DRUG_ORDER_UUID = "44fdc8ad-fe4d-499b-93a8-8a991c1d477e";
	
	private BahmniMedicationStatusCalculator statusCalculator;
	
	private DrugOrder drugOrder;
	
	private Date today;
	
	private Date yesterday;
	
	private Date tomorrow;
	
	@Before
	public void setup() {
		statusCalculator = new BahmniMedicationStatusCalculator();
		
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
		drugOrder.setDateActivated(yesterday); // Default: activated yesterday
		drugOrder.setAction(Order.Action.NEW); // Default: new order
		drugOrder.setVoided(false); // Default: not voided
	}
	
	// ========== NULL/INVALID INPUT TESTS ==========
	
	@Test
	public void calculateStatus_shouldReturnNull_whenDrugOrderIsNull() {
		// When: Calculate status for null drug order
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(null);
		
		// Then: Should return null
		assertThat(status, nullValue());
	}
	
	@Test
	public void calculateStatus_shouldHandleMinimalDrugOrder() {
		// Given: Minimal drug order with only UUID
		DrugOrder minimalOrder = new DrugOrder();
		minimalOrder.setUuid("minimal-uuid");
		
		// When: Calculate status
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(minimalOrder);
		
		// Then: Should return UNKNOWN (no dates set)
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.UNKNOWN));
	}
	
	// ========== CANCELLED STATUS TESTS ==========
	
	@Test
	public void calculateStatus_shouldReturnCancelled_whenDrugOrderIsVoided() {
		// Given: Voided drug order
		drugOrder.setVoided(true);
		
		// When: Calculate status
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(drugOrder);
		
		// Then: Should return CANCELLED
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.CANCELLED));
	}
	
	@Test
	public void calculateStatus_shouldReturnCancelled_whenActionIsDiscontinue() {
		// Given: Drug order with DISCONTINUE action
		drugOrder.setAction(Order.Action.DISCONTINUE);
		
		// When: Calculate status
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(drugOrder);
		
		// Then: Should return CANCELLED
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.CANCELLED));
	}
	
	@Test
	public void calculateStatus_shouldReturnCancelled_whenBothVoidedAndDiscontinued() {
		// Given: Drug order that is both voided and discontinued
		drugOrder.setVoided(true);
		drugOrder.setAction(Order.Action.DISCONTINUE);
		
		// When: Calculate status
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(drugOrder);
		
		// Then: Should return CANCELLED (voided takes precedence)
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.CANCELLED));
	}
	
	// ========== STOPPED STATUS TESTS ==========
	
	@Test
	public void calculateStatus_shouldReturnStopped_whenDateStoppedIsYesterday() throws Exception {
		// Given: Drug order stopped yesterday
		setDateStopped(drugOrder, yesterday);
		
		// When: Calculate status
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(drugOrder);
		
		// Then: Should return STOPPED
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.STOPPED));
	}
	
	@Test
	public void calculateStatus_shouldReturnStopped_whenDateStoppedIsToday() throws Exception {
		// Given: Drug order stopped today
		setDateStopped(drugOrder, today);
		
		// When: Calculate status
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(drugOrder);
		
		// Then: Should return STOPPED
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.STOPPED));
	}
	
	@Test
	public void calculateStatus_shouldReturnActive_whenDateStoppedIsTomorrow() throws Exception {
		// Given: Drug order to be stopped tomorrow (still active)
		setDateStopped(drugOrder, tomorrow);
		
		// When: Calculate status
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(drugOrder);
		
		// Then: Should return ACTIVE (not stopped yet)
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	// ========== ON-HOLD STATUS TESTS ==========
	
	@Test
	public void calculateStatus_shouldReturnOnHold_whenScheduledForFutureWithUrgency() {
		// Given: Drug order scheduled for future with ON_SCHEDULED_DATE urgency
		drugOrder.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		drugOrder.setScheduledDate(tomorrow);
		
		// When: Calculate status
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(drugOrder);
		
		// Then: Should return ON_HOLD
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.ONHOLD));
	}
	
	@Test
	public void calculateStatus_shouldReturnOnHold_whenDateActivatedIsInFuture() {
		// Given: Drug order with future activation date
		drugOrder.setDateActivated(tomorrow);
		
		// When: Calculate status
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(drugOrder);
		
		// Then: Should return ON_HOLD
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.ONHOLD));
	}
	
	@Test
	public void calculateStatus_shouldReturnActive_whenScheduledDateIsToday() {
		// Given: Drug order scheduled for today
		drugOrder.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		drugOrder.setScheduledDate(today);
		
		// When: Calculate status
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(drugOrder);
		
		// Then: Should return ACTIVE (started today)
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void calculateStatus_shouldReturnActive_whenScheduledDateIsYesterday() {
		// Given: Drug order scheduled for yesterday
		drugOrder.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		drugOrder.setScheduledDate(yesterday);
		
		// When: Calculate status
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(drugOrder);
		
		// Then: Should return ACTIVE (already started)
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	// ========== ACTIVE STATUS TESTS ==========
	
	@Test
	public void calculateStatus_shouldReturnActive_whenOrderIsCurrentlyActive() {
		// Given: Active drug order (activated yesterday, no stop date, no expire date)
		// drugOrder already set up with dateActivated=yesterday in @Before
		
		// When: Calculate status
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(drugOrder);
		
		// Then: Should return ACTIVE
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void calculateStatus_shouldReturnActive_whenActivatedTodayWithNoEndDate() {
		// Given: Drug order activated today with no end date
		drugOrder.setDateActivated(today);
		
		// When: Calculate status
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(drugOrder);
		
		// Then: Should return ACTIVE
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void calculateStatus_shouldReturnActive_whenAutoExpireDateIsInFuture() {
		// Given: Drug order with future auto expire date
		drugOrder.setAutoExpireDate(tomorrow);
		
		// When: Calculate status
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(drugOrder);
		
		// Then: Should return ACTIVE (not expired yet)
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	// ========== COMPLETED STATUS TESTS ==========
	
	@Test
	public void calculateStatus_shouldReturnCompleted_whenAutoExpireDateIsYesterday() {
		// Given: Drug order that expired yesterday
		drugOrder.setAutoExpireDate(yesterday);
		
		// When: Calculate status
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(drugOrder);
		
		// Then: Should return COMPLETED
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.COMPLETED));
	}
	
	@Test
	public void calculateStatus_shouldReturnCompleted_whenAutoExpireDateIsToday() {
		// Given: Drug order that expires today
		drugOrder.setAutoExpireDate(today);
		
		// When: Calculate status
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(drugOrder);
		
		// Then: Should return COMPLETED
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.COMPLETED));
	}
	
	// ========== UNKNOWN STATUS TESTS ==========
	
	@Test
	public void calculateStatus_shouldReturnUnknown_whenNoDateActivatedAndNotScheduled() {
		// Given: Drug order with no dateActivated and not scheduled
		drugOrder.setDateActivated(null);
		
		// When: Calculate status
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(drugOrder);
		
		// Then: Should return UNKNOWN
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.UNKNOWN));
	}
	
	// ========== COMPLEX SCENARIO TESTS ==========
	
	@Test
	public void calculateStatus_shouldReturnStopped_whenBothStoppedAndExpired() throws Exception {
		// Given: Drug order that is both stopped and expired (stopped takes precedence)
		setDateStopped(drugOrder, yesterday);
		drugOrder.setAutoExpireDate(yesterday);
		
		// When: Calculate status
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(drugOrder);
		
		// Then: Should return STOPPED (dateStopped takes precedence)
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.STOPPED));
	}
	
	@Test
	public void calculateStatus_shouldReturnCancelled_whenVoidedAndStopped() throws Exception {
		// Given: Drug order that is both voided and stopped (voided takes precedence)
		drugOrder.setVoided(true);
		setDateStopped(drugOrder, yesterday);
		
		// When: Calculate status
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(drugOrder);
		
		// Then: Should return CANCELLED (voided takes precedence)
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.CANCELLED));
	}
	
	@Test
	public void calculateStatus_shouldReturnCancelled_whenDiscontinuedAndExpired() {
		// Given: Drug order that is both discontinued and expired (discontinued takes precedence)
		drugOrder.setAction(Order.Action.DISCONTINUE);
		drugOrder.setAutoExpireDate(yesterday);
		
		// When: Calculate status
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(drugOrder);
		
		// Then: Should return CANCELLED (discontinued takes precedence)
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.CANCELLED));
	}
	
	// ========== EDGE CASE TESTS ==========
	
	@Test
	public void calculateStatus_shouldHandleReviseAction() {
		// Given: Drug order with REVISE action
		drugOrder.setAction(Order.Action.REVISE);
		
		// When: Calculate status
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(drugOrder);
		
		// Then: Should return ACTIVE (revise is treated as active)
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void calculateStatus_shouldHandleRenewAction() {
		// Given: Drug order with RENEW action
		drugOrder.setAction(Order.Action.RENEW);
		
		// When: Calculate status
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(drugOrder);
		
		// Then: Should return ACTIVE (renew is treated as active)
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void calculateStatus_shouldIgnoreUrgencyWhenNotScheduled() {
		// Given: Drug order with STAT urgency but no scheduled date
		drugOrder.setUrgency(Order.Urgency.STAT);
		
		// When: Calculate status
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(drugOrder);
		
		// Then: Should return ACTIVE (urgency doesn't affect status unless ON_SCHEDULED_DATE)
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void calculateStatus_shouldIgnoreUrgencyRoutine() {
		// Given: Drug order with ROUTINE urgency
		drugOrder.setUrgency(Order.Urgency.ROUTINE);
		
		// When: Calculate status
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(drugOrder);
		
		// Then: Should return ACTIVE (routine urgency doesn't affect status)
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	// ========== BOUNDARY CONDITION TESTS ==========
	
	@Test
	public void calculateStatus_shouldBeConsistent_acrossMultipleCalls() {
		// Given: Same drug order
		
		// When: Calculate status multiple times
		MedicationRequest.MedicationRequestStatus status1 = statusCalculator.calculateStatus(drugOrder);
		MedicationRequest.MedicationRequestStatus status2 = statusCalculator.calculateStatus(drugOrder);
		MedicationRequest.MedicationRequestStatus status3 = statusCalculator.calculateStatus(drugOrder);
		
		// Then: All calls should return the same result
		assertThat(status1, equalTo(status2));
		assertThat(status2, equalTo(status3));
		assertThat(status1, equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void calculateStatus_shouldHandleNullDates() throws Exception {
		// Given: Drug order with all null dates
		drugOrder.setDateActivated(null);
		setDateStopped(drugOrder, null);
		drugOrder.setAutoExpireDate(null);
		drugOrder.setScheduledDate(null);
		
		// When: Calculate status
		MedicationRequest.MedicationRequestStatus status = statusCalculator.calculateStatus(drugOrder);
		
		// Then: Should return UNKNOWN
		assertThat(status, equalTo(MedicationRequest.MedicationRequestStatus.UNKNOWN));
	}
	
	/**
	 * Helper method to set dateStopped field using reflection since it's private
	 */
	private void setDateStopped(DrugOrder drugOrder, Date dateStopped) throws Exception {
		Field dateStoppedField = Order.class.getDeclaredField("dateStopped");
		dateStoppedField.setAccessible(true);
		dateStoppedField.set(drugOrder, dateStopped);
	}
}
