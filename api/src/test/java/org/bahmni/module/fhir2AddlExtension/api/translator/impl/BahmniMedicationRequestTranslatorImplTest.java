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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Date;

import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Timing;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.CareSetting;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.api.OrderService;

/**
 * Unit tests for BahmniMedicationRequestTranslatorImpl. Verifies that STAT orders have
 * autoExpireDate set from timing.repeat.boundsPeriod.end, and that scheduled (non-STAT) orders have
 * urgency set to ON_SCHEDULED_DATE.
 */
@RunWith(MockitoJUnitRunner.class)
public class BahmniMedicationRequestTranslatorImplTest {
	
	@Mock
	private OrderService orderService;
	
	@Mock
	private CareSetting outpatientCareSetting;
	
	private BahmniMedicationRequestTranslatorImpl translator;
	
	private Date today;
	
	private Date endOfDay;
	
	@Before
	public void setup() {
		translator = spy(new BahmniMedicationRequestTranslatorImpl());
		translator.setOrderService(orderService);
		
		when(orderService.getCareSettingByName(CareSetting.CareSettingType.OUTPATIENT.name())).thenReturn(
		    outpatientCareSetting);
		
		Calendar cal = Calendar.getInstance();
		today = cal.getTime();
		
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		endOfDay = cal.getTime();
	}
	
	// ========== STAT ORDERS — boundsPeriod ==========
	
	@Test
	public void toOpenmrsType_givenStatOrderWithBoundsPeriod_shouldSetAutoExpireDate() {
		// Given: A FHIR MedicationRequest with boundsPeriod.end set
		MedicationRequest fhirRequest = buildStatRequestWithBoundsPeriod(today, endOfDay);
		
		// And: super call returns a STAT DrugOrder
		DrugOrder statOrder = buildStatDrugOrder();
		doReturn(statOrder).when(translator).superToOpenmrsType(any(DrugOrder.class), any(MedicationRequest.class));
		
		// When: translating
		DrugOrder result = translator.toOpenmrsType(new DrugOrder(), fhirRequest);
		
		// Then: scheduledDate cleared and autoExpireDate set to boundsPeriod.end
		assertThat(result.getScheduledDate(), nullValue());
		assertThat(result.getAutoExpireDate(), notNullValue());
		assertThat(result.getAutoExpireDate(), equalTo(endOfDay));
	}
	
	@Test
	public void toOpenmrsType_givenStatOrderWithoutBoundsPeriod_shouldNotSetAutoExpireDate() {
		// Given: A FHIR MedicationRequest with no dosage timing
		MedicationRequest fhirRequest = new MedicationRequest();
		
		DrugOrder statOrder = buildStatDrugOrder();
		doReturn(statOrder).when(translator).superToOpenmrsType(any(DrugOrder.class), any(MedicationRequest.class));
		
		// When: translating
		DrugOrder result = translator.toOpenmrsType(new DrugOrder(), fhirRequest);
		
		// Then: scheduledDate cleared but autoExpireDate remains null
		assertThat(result.getScheduledDate(), nullValue());
		assertThat(result.getAutoExpireDate(), nullValue());
	}
	
	@Test
	public void toOpenmrsType_givenStatOrderWithBoundsPeriodButNoEnd_shouldNotSetAutoExpireDate() {
		// Given: boundsPeriod exists but has only a start, no end
		MedicationRequest fhirRequest = buildStatRequestWithBoundsPeriod(today, null);
		
		DrugOrder statOrder = buildStatDrugOrder();
		doReturn(statOrder).when(translator).superToOpenmrsType(any(DrugOrder.class), any(MedicationRequest.class));
		
		// When: translating
		DrugOrder result = translator.toOpenmrsType(new DrugOrder(), fhirRequest);
		
		// Then: autoExpireDate is not set
		assertThat(result.getScheduledDate(), nullValue());
		assertThat(result.getAutoExpireDate(), nullValue());
	}
	
	// ========== SCHEDULED (NON-STAT) ORDERS ==========
	
	@Test
	public void toOpenmrsType_givenScheduledOrder_shouldSetUrgencyToOnScheduledDate() {
		// Given: A FHIR MedicationRequest that super translates with a future scheduledDate
		MedicationRequest fhirRequest = new MedicationRequest();
		
		DrugOrder scheduledOrder = new DrugOrder();
		scheduledOrder.setScheduledDate(endOfDay);
		// urgency not yet set by super
		doReturn(scheduledOrder).when(translator).superToOpenmrsType(any(DrugOrder.class), any(MedicationRequest.class));
		
		// When: translating
		DrugOrder result = translator.toOpenmrsType(new DrugOrder(), fhirRequest);
		
		// Then: urgency is set to ON_SCHEDULED_DATE
		assertThat(result.getUrgency(), equalTo(Order.Urgency.ON_SCHEDULED_DATE));
	}
	
	@Test
	public void toOpenmrsType_givenOrderWithNoScheduledDateAndNoStat_shouldNotChangeUrgency() {
		// Given: A regular order with no scheduledDate and no STAT urgency
		MedicationRequest fhirRequest = new MedicationRequest();
		
		DrugOrder regularOrder = new DrugOrder();
		regularOrder.setUrgency(Order.Urgency.ROUTINE);
		doReturn(regularOrder).when(translator).superToOpenmrsType(any(DrugOrder.class), any(MedicationRequest.class));
		
		// When: translating
		DrugOrder result = translator.toOpenmrsType(new DrugOrder(), fhirRequest);
		
		// Then: urgency remains unchanged, scheduledDate remains null
		assertThat(result.getUrgency(), equalTo(Order.Urgency.ROUTINE));
		assertThat(result.getScheduledDate(), nullValue());
	}
	
	// ========== HELPERS ==========
	
	private DrugOrder buildStatDrugOrder() {
		DrugOrder order = new DrugOrder();
		order.setUrgency(Order.Urgency.STAT);
		order.setScheduledDate(today); // super would have set this; we expect it to be cleared
		return order;
	}
	
	private MedicationRequest buildStatRequestWithBoundsPeriod(Date start, Date end) {
		Period boundsPeriod = new Period();
		if (start != null) {
			boundsPeriod.setStart(start);
		}
		if (end != null) {
			boundsPeriod.setEnd(end);
		}
		Timing.TimingRepeatComponent repeat = new Timing.TimingRepeatComponent();
		repeat.setBounds(boundsPeriod);
		Timing timing = new Timing();
		timing.setRepeat(repeat);
		MedicationRequest request = new MedicationRequest();
		request.addDosageInstruction().setTiming(timing);
		return request;
	}
}
