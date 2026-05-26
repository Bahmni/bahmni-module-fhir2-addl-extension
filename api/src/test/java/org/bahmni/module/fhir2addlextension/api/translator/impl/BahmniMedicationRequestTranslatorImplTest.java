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
 * Unit tests for BahmniMedicationRequestTranslatorImpl. Covers the urgency-semantics layered on top
 * of the timing translation: STAT clears scheduledDate, non-STAT with a scheduledDate ends up as
 * ON_SCHEDULED_DATE. The boundsPeriod handling lives in BahmniMedicationRequestTimingTranslatorImpl
 * and is tested separately.
 */
@RunWith(MockitoJUnitRunner.class)
public class BahmniMedicationRequestTranslatorImplTest {
	
	@Mock
	private OrderService orderService;
	
	@Mock
	private CareSetting outpatientCareSetting;
	
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
	
	@Before
	public void setup() {
		when(orderService.getCareSettingByName(CareSetting.CareSettingType.OUTPATIENT.name())).thenReturn(
		    outpatientCareSetting);
		
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MILLISECOND, 0);
		today = cal.getTime();
	}
	
	@Test
	public void toOpenmrsType_givenStatOrder_shouldClearScheduledDate() {
		when(medicationRequestPriorityTranslator.toOpenmrsType(MedicationRequest.MedicationRequestPriority.STAT))
		        .thenReturn(Order.Urgency.STAT);
		// Timing translator (via super -> dosageTranslator) is mocked to set scheduledDate;
		// STAT semantics must override and clear it back to null.
		doAnswer(inv -> {
			((DrugOrder) inv.getArgument(0)).setScheduledDate(today);
			return inv.getArgument(0);
		}).when(dosageTranslator).toOpenmrsType(any(DrugOrder.class), any(Dosage.class));

		MedicationRequest fhirRequest = buildRequest();
		fhirRequest.setPriority(MedicationRequest.MedicationRequestPriority.STAT);
		fhirRequest.addDosageInstruction();

		DrugOrder result = translator.toOpenmrsType(new DrugOrder(), fhirRequest);

		assertThat(result.getScheduledDate(), nullValue());
	}
	
	@Test
	public void toOpenmrsType_givenNonStatOrderWithScheduledDate_shouldSetUrgencyToOnScheduledDate() {
		doAnswer(inv -> {
			((DrugOrder) inv.getArgument(0)).setScheduledDate(today);
			return inv.getArgument(0);
		}).when(dosageTranslator).toOpenmrsType(any(DrugOrder.class), any(Dosage.class));

		MedicationRequest fhirRequest = buildRequest();
		fhirRequest.addDosageInstruction();

		DrugOrder result = translator.toOpenmrsType(new DrugOrder(), fhirRequest);

		assertThat(result.getScheduledDate(), equalTo(today));
		assertThat(result.getUrgency(), equalTo(Order.Urgency.ON_SCHEDULED_DATE));
	}
	
	@Test
	public void toOpenmrsType_givenOrderWithoutScheduledDateOrStat_shouldNotChangeUrgency() {
		when(medicationRequestPriorityTranslator.toOpenmrsType(MedicationRequest.MedicationRequestPriority.ROUTINE))
		        .thenReturn(Order.Urgency.ROUTINE);
		
		MedicationRequest fhirRequest = buildRequest();
		fhirRequest.setPriority(MedicationRequest.MedicationRequestPriority.ROUTINE);
		
		DrugOrder result = translator.toOpenmrsType(new DrugOrder(), fhirRequest);
		
		assertThat(result.getUrgency(), equalTo(Order.Urgency.ROUTINE));
		assertThat(result.getScheduledDate(), nullValue());
	}
	
	private MedicationRequest buildRequest() {
		MedicationRequest request = new MedicationRequest();
		// Set an empty CodeableConcept to avoid NPE in parent's concept translation path
		request.setMedication(new CodeableConcept());
		return request;
	}
}
