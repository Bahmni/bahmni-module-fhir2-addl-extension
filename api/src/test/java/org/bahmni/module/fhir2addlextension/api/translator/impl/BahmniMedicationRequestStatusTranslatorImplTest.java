/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.bahmni.module.fhir2addlextension.api.translator.impl;

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

public class BahmniMedicationRequestStatusTranslatorImplTest {
	
	private static final String DRUG_ORDER_UUID = "44fdc8ad-fe4d-499b-93a8-8a991c1d477e";
	
	private BahmniMedicationRequestStatusTranslatorImpl statusTranslator;
	
	private DrugOrder drugOrder;
	
	private Date today;
	
	private Date yesterday;
	
	private Date tomorrow;
	
	@Before
	public void setup() {
		statusTranslator = new BahmniMedicationRequestStatusTranslatorImpl();
		
		Calendar cal = Calendar.getInstance();
		today = cal.getTime();
		
		cal.add(Calendar.DAY_OF_MONTH, -1);
		yesterday = cal.getTime();
		
		cal.add(Calendar.DAY_OF_MONTH, 2);
		tomorrow = cal.getTime();
		
		drugOrder = new DrugOrder();
		drugOrder.setUuid(DRUG_ORDER_UUID);
		drugOrder.setDateActivated(yesterday);
		drugOrder.setAction(Order.Action.NEW);
		drugOrder.setVoided(false);
	}
	
	@Test
	public void toFhirResource_shouldReturnNull_whenDrugOrderIsNull() {
		assertThat(statusTranslator.toFhirResource(null), nullValue());
	}
	
	@Test
	public void toFhirResource_shouldReturnEnteredInError_whenOrderIsVoided() {
		drugOrder.setVoided(true);
		assertThat(statusTranslator.toFhirResource(drugOrder),
		    equalTo(MedicationRequest.MedicationRequestStatus.ENTEREDINERROR));
	}
	
	@Test
	public void toFhirResource_shouldReturnCompleted_whenFulfillerStatusIsCompleted() {
		drugOrder.setFulfillerStatus(Order.FulfillerStatus.COMPLETED);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.COMPLETED));
	}
	
	@Test
	public void toFhirResource_shouldReturnActive_whenFulfillerStatusIsInProgress() {
		drugOrder.setFulfillerStatus(Order.FulfillerStatus.IN_PROGRESS);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldReturnActive_whenFulfillerStatusIsReceived() {
		drugOrder.setFulfillerStatus(Order.FulfillerStatus.RECEIVED);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldReturnStopped_whenFulfillerStatusIsExceptionAndDateStoppedIsSet() throws Exception {
		drugOrder.setFulfillerStatus(Order.FulfillerStatus.EXCEPTION);
		setDateStopped(drugOrder, yesterday);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.STOPPED));
	}
	
	@Test
	public void toFhirResource_shouldReturnUnknown_whenFulfillerStatusIsExceptionAndNoDateStopped() {
		drugOrder.setFulfillerStatus(Order.FulfillerStatus.EXCEPTION);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.UNKNOWN));
	}
	
	@Test
	public void toFhirResource_shouldReturnCompleted_whenActionIsDiscontinue() {
		drugOrder.setAction(Order.Action.DISCONTINUE);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.COMPLETED));
	}
	
	@Test
	public void toFhirResource_shouldReturnCompleted_whenDiscontinuedEvenIfAutoExpired() {
		drugOrder.setAction(Order.Action.DISCONTINUE);
		drugOrder.setAutoExpireDate(yesterday);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.COMPLETED));
	}
	
	@Test
	public void toFhirResource_shouldReturnStopped_whenDateStoppedAfterStart() throws Exception {
		setDateStopped(drugOrder, today);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.STOPPED));
	}
	
	@Test
	public void toFhirResource_shouldReturnStopped_whenDateStoppedSameMinuteAsStart() throws Exception {
		setDateStopped(drugOrder, yesterday);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.STOPPED));
	}
	
	@Test
	public void toFhirResource_shouldReturnStopped_whenDateStoppedInFutureAfterStart() throws Exception {
		setDateStopped(drugOrder, tomorrow);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.STOPPED));
	}
	
	@Test
	public void toFhirResource_shouldReturnStopped_whenBothStoppedAndAutoExpired() throws Exception {
		setDateStopped(drugOrder, yesterday);
		drugOrder.setAutoExpireDate(yesterday);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.STOPPED));
	}
	
	@Test
	public void toFhirResource_shouldReturnCancelled_whenDateStoppedBeforeEffectiveStartDate() throws Exception {
		drugOrder.setDateActivated(tomorrow);
		setDateStopped(drugOrder, yesterday);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.CANCELLED));
	}
	
	@Test
	public void toFhirResource_shouldReturnCancelled_whenDateStoppedBeforeScheduledStartDate() throws Exception {
		drugOrder.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		drugOrder.setScheduledDate(tomorrow);
		setDateStopped(drugOrder, yesterday);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.CANCELLED));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void toFhirResource_shouldThrowException_whenNoEffectiveStartDate() {
		drugOrder.setDateActivated(null);
		statusTranslator.toFhirResource(drugOrder);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void toFhirResource_shouldThrowException_whenMinimalOrderWithNoDates() {
		DrugOrder minimalOrder = new DrugOrder();
		minimalOrder.setUuid("minimal-uuid");
		statusTranslator.toFhirResource(minimalOrder);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void toFhirResource_shouldThrowException_whenScheduledUrgencyButNoScheduledDate() {
		drugOrder.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		drugOrder.setScheduledDate(null);
		drugOrder.setDateActivated(null);
		statusTranslator.toFhirResource(drugOrder);
	}
	
	@Test
	public void toFhirResource_shouldReturnCompleted_whenAutoExpireDateIsInPast() {
		drugOrder.setAutoExpireDate(yesterday);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.COMPLETED));
	}
	
	@Test
	public void toFhirResource_shouldReturnActive_whenAutoExpireDateIsCurrentMinute() {
		drugOrder.setAutoExpireDate(today);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldReturnActive_whenOrderIsCurrentlyActive() {
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldReturnActive_whenActivatedTodayWithNoEndDate() {
		drugOrder.setDateActivated(today);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldReturnActive_whenAutoExpireDateIsInFuture() {
		drugOrder.setAutoExpireDate(tomorrow);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldReturnActive_whenDateActivatedIsInFuture() {
		drugOrder.setDateActivated(tomorrow);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldReturnActive_whenScheduledForFutureWithOnScheduledDateUrgency() {
		drugOrder.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		drugOrder.setScheduledDate(tomorrow);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldReturnActive_whenScheduledDateIsYesterday() {
		drugOrder.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		drugOrder.setScheduledDate(yesterday);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldReturnActive_whenActionIsRevise() {
		drugOrder.setAction(Order.Action.REVISE);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldReturnActive_whenActionIsRenew() {
		drugOrder.setAction(Order.Action.RENEW);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldReturnActive_whenUrgencyIsStatWithActiveOrder() {
		drugOrder.setUrgency(Order.Urgency.STAT);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldReturnActive_whenUrgencyIsRoutineWithActiveOrder() {
		drugOrder.setUrgency(Order.Urgency.ROUTINE);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldReturnCompleted_whenFulfillerCompletedEvenIfNotExpired() {
		drugOrder.setFulfillerStatus(Order.FulfillerStatus.COMPLETED);
		drugOrder.setAutoExpireDate(tomorrow);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.COMPLETED));
	}
	
	@Test
	public void toFhirResource_shouldReturnActive_whenFulfillerInProgressEvenIfAutoExpired() {
		drugOrder.setFulfillerStatus(Order.FulfillerStatus.IN_PROGRESS);
		drugOrder.setAutoExpireDate(yesterday);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	private void setDateStopped(DrugOrder drugOrder, Date dateStopped) throws Exception {
		Field dateStoppedField = Order.class.getDeclaredField("dateStopped");
		dateStoppedField.setAccessible(true);
		dateStoppedField.set(drugOrder, dateStopped);
	}
}
