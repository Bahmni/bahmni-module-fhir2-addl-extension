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

import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.annotation.Nonnull;

import org.bahmni.module.fhir2addlextension.api.utils.ModuleUtils;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.module.fhir2.api.translators.MedicationRequestStatusTranslator;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class BahmniMedicationRequestStatusTranslatorImpl implements MedicationRequestStatusTranslator {
	
	@Override
	public MedicationRequest.MedicationRequestStatus toFhirResource(@Nonnull DrugOrder drugOrder) {
		if (drugOrder == null) {
			return null;
		}
		
		Date now = new Date();
		
		if (drugOrder.getVoided()) {
			return MedicationRequest.MedicationRequestStatus.ENTEREDINERROR;
		}
		
		Order.FulfillerStatus fulfillerStatus = drugOrder.getFulfillerStatus();
		if (fulfillerStatus != null) {
			switch (fulfillerStatus) {
				case COMPLETED:
					return MedicationRequest.MedicationRequestStatus.COMPLETED;
				case IN_PROGRESS:
				case RECEIVED:
					return MedicationRequest.MedicationRequestStatus.ACTIVE;
				case EXCEPTION:
					return drugOrder.getDateStopped() != null ? MedicationRequest.MedicationRequestStatus.STOPPED
					        : MedicationRequest.MedicationRequestStatus.UNKNOWN;
				default:
					break;
			}
		}
		
		if (Order.Action.DISCONTINUE.equals(drugOrder.getAction())) {
			return MedicationRequest.MedicationRequestStatus.COMPLETED;
		}
		
		Date effectiveStartDate = drugOrder.getEffectiveStartDate();
		if (effectiveStartDate == null) {
			String NO_EFFECTIVE_START_DATE_EXCEPTION_MESSAGE = "Can not determine status for order with no effective start date";
			throw new IllegalArgumentException(NO_EFFECTIVE_START_DATE_EXCEPTION_MESSAGE);
		}
		
		if (drugOrder.getDateStopped() != null) {
			return ModuleUtils.compareDates(drugOrder.getDateStopped(), effectiveStartDate, ChronoUnit.MINUTES) < 0 ? MedicationRequest.MedicationRequestStatus.CANCELLED
			        : MedicationRequest.MedicationRequestStatus.STOPPED;
		}
		
		int activated = ModuleUtils.compareDates(now, effectiveStartDate, ChronoUnit.MINUTES);
		if (activated < 0) {
			return MedicationRequest.MedicationRequestStatus.ACTIVE;
		}
		
		Date autoExpireDate = drugOrder.getAutoExpireDate();
		if (autoExpireDate == null) {
			return MedicationRequest.MedicationRequestStatus.ACTIVE;
		}
		
		int comparisonResult = ModuleUtils.compareDates(autoExpireDate, now, ChronoUnit.MINUTES);
		return comparisonResult < 0 ? MedicationRequest.MedicationRequestStatus.COMPLETED
		        : MedicationRequest.MedicationRequestStatus.ACTIVE;
	}
}
