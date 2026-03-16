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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Date;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Dosage;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Timing;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.CareSetting;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.Provider;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.DosageTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.MedicationReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.MedicationRequestDispenseRequestComponentTranslator;
import org.openmrs.module.fhir2.api.translators.MedicationRequestPriorityTranslator;
import org.openmrs.module.fhir2.api.translators.MedicationRequestReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.MedicationRequestStatusTranslator;
import org.openmrs.module.fhir2.api.translators.OrderIdentifierTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;

/**
 * Unit tests for BahmniMedicationRequestTranslatorImpl. Verifies that STAT orders have
 * autoExpireDate set from timing.repeat.boundsPeriod.end, and that scheduled (non-STAT) orders have
 * urgency set to ON_SCHEDULED_DATE. Uses @InjectMocks so the real toOpenmrsType implementation runs
 * end-to-end with all parent-class dependencies mocked. The dosageTranslator mock simulates the
 * parent setting scheduledDate via doAnswer, since the parent calls dosageTranslator for its
 * side-effect on the DrugOrder.
 */
@RunWith(MockitoJUnitRunner.class)
public class BahmniMedicationRequestTranslatorImplTest {
	
	@Mock
	private OrderService orderService;
	
	@Mock
	private CareSetting outpatientCareSetting;
	
	// Parent class (MedicationRequestTranslatorImpl) dependencies injected via @InjectMocks
	@Mock
	private MedicationRequestPriorityTranslator medicationRequestPriorityTranslator;
	
	@Mock
	private DosageTranslator dosageTranslator;
	
	@Mock
	private ConceptTranslator conceptTranslator;
	
	@Mock
	private MedicationReferenceTranslator medicationReferenceTranslator;
	
	@Mock
	private MedicationRequestStatusTranslator statusTranslator;
	
	@Mock
	private PractitionerReferenceTranslator<Provider> practitionerReferenceTranslator;
	
	@Mock
	private EncounterReferenceTranslator<Encounter> encounterReferenceTranslator;
	
	@Mock
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Mock
	private OrderIdentifierTranslator orderIdentifierTranslator;
	
	@Mock
	private MedicationRequestDispenseRequestComponentTranslator medicationRequestDispenseRequestComponentTranslator;
	
	@Mock
	private MedicationRequestReferenceTranslator medicationRequestReferenceTranslator;
	
	@InjectMocks
	private BahmniMedicationRequestTranslatorImpl translator;
	
	private Date today;
	
	private Date endOfDay;
	
	@Before
	public void setup() {
		when(orderService.getCareSettingByName(CareSetting.CareSettingType.OUTPATIENT.name())).thenReturn(
		    outpatientCareSetting);
		
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MILLISECOND, 0);
		today = cal.getTime();
		
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		endOfDay = cal.getTime();
	}
	
	// ========== STAT ORDERS — boundsPeriod ==========
	
	@Test
	public void toOpenmrsType_givenStatOrderWithBoundsPeriod_shouldSetAutoExpireDate() {
		// Given: priority translator maps STAT → STAT urgency
		when(medicationRequestPriorityTranslator.toOpenmrsType(MedicationRequest.MedicationRequestPriority.STAT))
		        .thenReturn(Order.Urgency.STAT);
		// And: dosageTranslator simulates parent setting a scheduledDate (which STAT logic must clear)
		doAnswer(inv -> {
			((DrugOrder) inv.getArgument(0)).setScheduledDate(today);
			return inv.getArgument(0);
		}).when(dosageTranslator).toOpenmrsType(any(DrugOrder.class), any(Dosage.class));

		MedicationRequest fhirRequest = buildStatRequestWithBoundsPeriod(today, endOfDay);

		// When
		DrugOrder result = translator.toOpenmrsType(new DrugOrder(), fhirRequest);

		// Then: scheduledDate is cleared and autoExpireDate is extracted from boundsPeriod.end
		assertThat(result.getScheduledDate(), nullValue());
		assertThat(result.getAutoExpireDate(), equalTo(endOfDay));
	}
	
	@Test
	public void toOpenmrsType_givenStatOrderWithoutBoundsPeriod_shouldNotSetAutoExpireDate() {
		when(medicationRequestPriorityTranslator.toOpenmrsType(MedicationRequest.MedicationRequestPriority.STAT))
		        .thenReturn(Order.Urgency.STAT);
		doAnswer(inv -> {
			((DrugOrder) inv.getArgument(0)).setScheduledDate(today);
			return inv.getArgument(0);
		}).when(dosageTranslator).toOpenmrsType(any(DrugOrder.class), any(Dosage.class));

		// No dosage instruction with boundsPeriod
		MedicationRequest fhirRequest = buildBaseRequest();
		fhirRequest.setPriority(MedicationRequest.MedicationRequestPriority.STAT);

		DrugOrder result = translator.toOpenmrsType(new DrugOrder(), fhirRequest);

		assertThat(result.getScheduledDate(), nullValue());
		assertThat(result.getAutoExpireDate(), nullValue());
	}
	
	@Test
	public void toOpenmrsType_givenStatOrderWithBoundsPeriodButNoEnd_shouldNotSetAutoExpireDate() {
		when(medicationRequestPriorityTranslator.toOpenmrsType(MedicationRequest.MedicationRequestPriority.STAT))
		        .thenReturn(Order.Urgency.STAT);
		doAnswer(inv -> {
			((DrugOrder) inv.getArgument(0)).setScheduledDate(today);
			return inv.getArgument(0);
		}).when(dosageTranslator).toOpenmrsType(any(DrugOrder.class), any(Dosage.class));

		// boundsPeriod has only a start, no end
		MedicationRequest fhirRequest = buildStatRequestWithBoundsPeriod(today, null);

		DrugOrder result = translator.toOpenmrsType(new DrugOrder(), fhirRequest);

		assertThat(result.getScheduledDate(), nullValue());
		assertThat(result.getAutoExpireDate(), nullValue());
	}
	
	// ========== SCHEDULED (NON-STAT) ORDERS ==========
	
	@Test
	public void toOpenmrsType_givenScheduledOrder_shouldSetUrgencyToOnScheduledDate() {
		// dosageTranslator simulates parent setting a future scheduledDate
		doAnswer(inv -> {
			((DrugOrder) inv.getArgument(0)).setScheduledDate(endOfDay);
			return inv.getArgument(0);
		}).when(dosageTranslator).toOpenmrsType(any(DrugOrder.class), any(Dosage.class));

		MedicationRequest fhirRequest = buildBaseRequest();

		DrugOrder result = translator.toOpenmrsType(new DrugOrder(), fhirRequest);

		assertThat(result.getUrgency(), equalTo(Order.Urgency.ON_SCHEDULED_DATE));
	}
	
	@Test
	public void toOpenmrsType_givenOrderWithNoScheduledDateAndNoStat_shouldNotChangeUrgency() {
		when(medicationRequestPriorityTranslator.toOpenmrsType(MedicationRequest.MedicationRequestPriority.ROUTINE))
		        .thenReturn(Order.Urgency.ROUTINE);
		
		MedicationRequest fhirRequest = buildBaseRequest();
		fhirRequest.setPriority(MedicationRequest.MedicationRequestPriority.ROUTINE);
		
		DrugOrder result = translator.toOpenmrsType(new DrugOrder(), fhirRequest);
		
		assertThat(result.getUrgency(), equalTo(Order.Urgency.ROUTINE));
		assertThat(result.getScheduledDate(), nullValue());
	}
	
	// ========== HELPERS ==========
	
	private MedicationRequest buildBaseRequest() {
		MedicationRequest request = new MedicationRequest();
		// Set an empty CodeableConcept to avoid NPE in parent's concept translation path
		request.setMedication(new CodeableConcept());
		return request;
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
		
		MedicationRequest request = buildBaseRequest();
		request.setPriority(MedicationRequest.MedicationRequestPriority.STAT);
		request.addDosageInstruction().setTiming(timing);
		return request;
	}
}
