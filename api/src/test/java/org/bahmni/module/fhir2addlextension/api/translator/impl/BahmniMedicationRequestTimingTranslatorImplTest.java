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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.Calendar;
import java.util.Date;

import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Timing;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.DrugOrder;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.MedicationRequestTimingRepeatComponentTranslator;

/**
 * Unit tests for BahmniMedicationRequestTimingTranslatorImpl. Verifies the additional boundsPeriod
 * handling layered on top of the upstream timing translator, for both directions.
 */
@RunWith(MockitoJUnitRunner.class)
public class BahmniMedicationRequestTimingTranslatorImplTest {
	
	@Mock
	private MedicationRequestTimingRepeatComponentTranslator timingRepeatComponentTranslator;
	
	@Mock
	private ConceptTranslator conceptTranslator;
	
	@InjectMocks
	private BahmniMedicationRequestTimingTranslatorImpl translator;
	
	private Date today;
	
	private Date endOfDay;
	
	@Before
	public void setup() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MILLISECOND, 0);
		today = cal.getTime();
		
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		endOfDay = cal.getTime();
	}
	
	// ========== toOpenmrsType ==========
	
	@Test
	public void toOpenmrsType_givenBoundsPeriodStart_shouldPopulateScheduledDate() {
		Timing timing = timingWithBoundsPeriod(today, null);
		
		DrugOrder result = translator.toOpenmrsType(new DrugOrder(), timing);
		
		assertThat(result.getScheduledDate(), equalTo(today));
		assertThat(result.getAutoExpireDate(), nullValue());
	}
	
	@Test
	public void toOpenmrsType_givenBoundsPeriodEnd_shouldPopulateAutoExpireDate() {
		Timing timing = timingWithBoundsPeriod(null, endOfDay);
		
		DrugOrder result = translator.toOpenmrsType(new DrugOrder(), timing);
		
		assertThat(result.getScheduledDate(), nullValue());
		assertThat(result.getAutoExpireDate(), equalTo(endOfDay));
	}
	
	@Test
	public void toOpenmrsType_givenBoundsPeriodStartAndEnd_shouldPopulateBothFields() {
		Timing timing = timingWithBoundsPeriod(today, endOfDay);
		
		DrugOrder result = translator.toOpenmrsType(new DrugOrder(), timing);
		
		assertThat(result.getScheduledDate(), equalTo(today));
		assertThat(result.getAutoExpireDate(), equalTo(endOfDay));
	}
	
	@Test
	public void toOpenmrsType_givenTimingWithoutBoundsPeriod_shouldNotPopulateScheduledOrAutoExpire() {
		Timing timing = new Timing();
		timing.setRepeat(new Timing.TimingRepeatComponent());
		
		DrugOrder result = translator.toOpenmrsType(new DrugOrder(), timing);
		
		assertThat(result.getScheduledDate(), nullValue());
		assertThat(result.getAutoExpireDate(), nullValue());
	}
	
	@Test
	public void toOpenmrsType_givenTimingEventAlreadySetsScheduledDate_shouldNotOverwriteWithBoundsPeriodStart() {
		// Upstream sets scheduledDate from timing.event. boundsPeriod.start is a fallback only.
		Date eventDate = today;
		Date boundsStart = new Date(today.getTime() + 86_400_000L);
		Timing timing = new Timing();
		timing.addEvent(eventDate);
		Period boundsPeriod = new Period();
		boundsPeriod.setStart(boundsStart);
		Timing.TimingRepeatComponent repeat = new Timing.TimingRepeatComponent();
		repeat.setBounds(boundsPeriod);
		timing.setRepeat(repeat);

		DrugOrder result = translator.toOpenmrsType(new DrugOrder(), timing);

		assertThat(result.getScheduledDate(), equalTo(eventDate));
	}
	
	// ========== toFhirResource ==========
	
	@Test
	public void toFhirResource_givenScheduledDate_shouldEmitBoundsPeriodStart() {
		DrugOrder drugOrder = new DrugOrder();
		drugOrder.setScheduledDate(today);
		
		Timing timing = translator.toFhirResource(drugOrder);
		
		assertThat(timing, notNullValue());
		assertThat(timing.getRepeat().hasBoundsPeriod(), is(true));
		assertThat(timing.getRepeat().getBoundsPeriod().getStart(), equalTo(today));
		assertThat(timing.getRepeat().getBoundsPeriod().hasEnd(), is(false));
	}
	
	@Test
	public void toFhirResource_givenAutoExpireDate_shouldEmitBoundsPeriodEnd() {
		DrugOrder drugOrder = new DrugOrder();
		drugOrder.setAutoExpireDate(endOfDay);
		
		Timing timing = translator.toFhirResource(drugOrder);
		
		assertThat(timing, notNullValue());
		assertThat(timing.getRepeat().hasBoundsPeriod(), is(true));
		assertThat(timing.getRepeat().getBoundsPeriod().getEnd(), equalTo(endOfDay));
		assertThat(timing.getRepeat().getBoundsPeriod().hasStart(), is(false));
	}
	
	@Test
	public void toFhirResource_givenScheduledAndAutoExpireDates_shouldEmitBoundsPeriodStartAndEnd() {
		DrugOrder drugOrder = new DrugOrder();
		drugOrder.setScheduledDate(today);
		drugOrder.setAutoExpireDate(endOfDay);
		
		Timing timing = translator.toFhirResource(drugOrder);
		
		assertThat(timing, notNullValue());
		assertThat(timing.getRepeat().getBoundsPeriod().getStart(), equalTo(today));
		assertThat(timing.getRepeat().getBoundsPeriod().getEnd(), equalTo(endOfDay));
		// timing.event keeps the legacy representation for backward-compat consumers.
		assertThat(timing.getEvent().get(0).getValue(), equalTo(today));
	}
	
	@Test
	public void toFhirResource_givenNeitherDate_shouldNotEmitBoundsPeriod() {
		DrugOrder drugOrder = new DrugOrder();
		
		Timing timing = translator.toFhirResource(drugOrder);
		
		assertThat(timing, notNullValue());
		assertThat(timing.getRepeat().hasBoundsPeriod(), is(false));
	}
	
	// ========== HELPERS ==========
	
	private Timing timingWithBoundsPeriod(Date start, Date end) {
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
		return timing;
	}
}
