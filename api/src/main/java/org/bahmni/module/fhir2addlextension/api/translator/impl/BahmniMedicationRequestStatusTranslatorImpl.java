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

import java.util.Date;

import javax.annotation.Nonnull;

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
		
		if (Order.Action.DISCONTINUE.equals(drugOrder.getAction())) {
			return MedicationRequest.MedicationRequestStatus.CANCELLED;
		}
		
		if (drugOrder.getDateStopped() != null) {
			return isDateBeforeOrEqual(drugOrder.getDateStopped(), now) ? MedicationRequest.MedicationRequestStatus.STOPPED
			        : MedicationRequest.MedicationRequestStatus.ACTIVE;
		}
		
		if (isScheduledForFuture(drugOrder, now)) {
			return MedicationRequest.MedicationRequestStatus.ONHOLD;
		}
		
		if (isActiveOnDate(drugOrder, now)) {
			return MedicationRequest.MedicationRequestStatus.ACTIVE;
		}
		
		if (isExpiredOnDate(drugOrder, now)) {
			return MedicationRequest.MedicationRequestStatus.COMPLETED;
		}
		
		return MedicationRequest.MedicationRequestStatus.UNKNOWN;
	}
	
	private boolean isActiveOnDate(DrugOrder drugOrder, Date checkDate) {
		Date effectiveStartDate = getEffectiveStartDate(drugOrder);
		Date effectiveStopDate = getEffectiveStopDate(drugOrder);
		
		if (effectiveStartDate == null) {
			return false;
		}
		
		boolean startedOnOrBefore = isDateBeforeOrEqual(effectiveStartDate, checkDate);
		boolean notStoppedYet = effectiveStopDate == null || isDateAfter(effectiveStopDate, checkDate);
		
		return startedOnOrBefore && notStoppedYet;
	}
	
	private boolean isScheduledForFuture(DrugOrder drugOrder, Date checkDate) {
		if (Order.Urgency.ON_SCHEDULED_DATE.equals(drugOrder.getUrgency())) {
			Date scheduledDate = drugOrder.getScheduledDate();
			return scheduledDate != null && isDateAfter(scheduledDate, checkDate);
		}
		
		Date dateActivated = drugOrder.getDateActivated();
		return dateActivated != null && isDateAfter(dateActivated, checkDate);
	}
	
	private boolean isExpiredOnDate(DrugOrder drugOrder, Date checkDate) {
		Date autoExpireDate = drugOrder.getAutoExpireDate();
		return autoExpireDate != null && isDateBeforeOrEqual(autoExpireDate, checkDate);
	}
	
	private Date getEffectiveStartDate(DrugOrder drugOrder) {
		if (Order.Urgency.ON_SCHEDULED_DATE.equals(drugOrder.getUrgency())) {
			return drugOrder.getScheduledDate();
		}
		return drugOrder.getDateActivated();
	}
	
	private Date getEffectiveStopDate(DrugOrder drugOrder) {
		if (drugOrder.getDateStopped() != null) {
			return drugOrder.getDateStopped();
		}
		return drugOrder.getAutoExpireDate();
	}
	
	private boolean isDateBeforeOrEqual(Date date1, Date date2) {
		if (date1 == null || date2 == null) {
			return false;
		}
		return date1.compareTo(date2) <= 0;
	}
	
	private boolean isDateAfter(Date date1, Date date2) {
		if (date1 == null || date2 == null) {
			return false;
		}
		return date1.compareTo(date2) > 0;
	}
}
