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
	public void toFhirResource_shouldHandleMinimalDrugOrder() {
		DrugOrder minimalOrder = new DrugOrder();
		minimalOrder.setUuid("minimal-uuid");
		assertThat(statusTranslator.toFhirResource(minimalOrder), equalTo(MedicationRequest.MedicationRequestStatus.UNKNOWN));
	}
	
	@Test
	public void toFhirResource_shouldReturnCancelled_whenActionIsDiscontinue() {
		drugOrder.setAction(Order.Action.DISCONTINUE);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.CANCELLED));
	}
	
	@Test
	public void toFhirResource_shouldReturnStopped_whenDateStoppedIsYesterday() throws Exception {
		setDateStopped(drugOrder, yesterday);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.STOPPED));
	}
	
	@Test
	public void toFhirResource_shouldReturnStopped_whenDateStoppedIsToday() throws Exception {
		setDateStopped(drugOrder, today);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.STOPPED));
	}
	
	@Test
	public void toFhirResource_shouldReturnActive_whenDateStoppedIsTomorrow() throws Exception {
		setDateStopped(drugOrder, tomorrow);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldReturnOnHold_whenScheduledForFutureWithUrgency() {
		drugOrder.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		drugOrder.setScheduledDate(tomorrow);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.ONHOLD));
	}
	
	@Test
	public void toFhirResource_shouldReturnOnHold_whenDateActivatedIsInFuture() {
		drugOrder.setDateActivated(tomorrow);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.ONHOLD));
	}
	
	@Test
	public void toFhirResource_shouldReturnActive_whenScheduledDateIsToday() {
		drugOrder.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		drugOrder.setScheduledDate(today);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldReturnActive_whenScheduledDateIsYesterday() {
		drugOrder.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		drugOrder.setScheduledDate(yesterday);
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
	public void toFhirResource_shouldReturnCompleted_whenAutoExpireDateIsYesterday() {
		drugOrder.setAutoExpireDate(yesterday);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.COMPLETED));
	}
	
	@Test
	public void toFhirResource_shouldReturnCompleted_whenAutoExpireDateIsToday() {
		drugOrder.setAutoExpireDate(today);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.COMPLETED));
	}
	
	@Test
	public void toFhirResource_shouldReturnUnknown_whenNoDateActivatedAndNotScheduled() {
		drugOrder.setDateActivated(null);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.UNKNOWN));
	}
	
	@Test
	public void toFhirResource_shouldReturnStopped_whenBothStoppedAndExpired() throws Exception {
		setDateStopped(drugOrder, yesterday);
		drugOrder.setAutoExpireDate(yesterday);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.STOPPED));
	}
	
	@Test
	public void toFhirResource_shouldReturnCancelled_whenDiscontinuedAndExpired() {
		drugOrder.setAction(Order.Action.DISCONTINUE);
		drugOrder.setAutoExpireDate(yesterday);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.CANCELLED));
	}
	
	@Test
	public void toFhirResource_shouldHandleReviseAction() {
		drugOrder.setAction(Order.Action.REVISE);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldHandleRenewAction() {
		drugOrder.setAction(Order.Action.RENEW);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldIgnoreUrgencyWhenNotScheduled() {
		drugOrder.setUrgency(Order.Urgency.STAT);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldIgnoreUrgencyRoutine() {
		drugOrder.setUrgency(Order.Urgency.ROUTINE);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldHandleNullDates() throws Exception {
		drugOrder.setDateActivated(null);
		setDateStopped(drugOrder, null);
		drugOrder.setAutoExpireDate(null);
		drugOrder.setScheduledDate(null);
		assertThat(statusTranslator.toFhirResource(drugOrder), equalTo(MedicationRequest.MedicationRequestStatus.UNKNOWN));
	}
	
	private void setDateStopped(DrugOrder drugOrder, Date dateStopped) throws Exception {
		Field dateStoppedField = Order.class.getDeclaredField("dateStopped");
		dateStoppedField.setAccessible(true);
		dateStoppedField.set(drugOrder, dateStopped);
	}
}
