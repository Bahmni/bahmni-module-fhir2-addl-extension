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

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.springframework.stereotype.Component;

/**
 * Calculates FHIR MedicationRequest status based on DrugOrder state. Mirrors Bahmni frontend logic
 * using server timezone for date comparisons.
 */
@Component
public class BahmniMedicationStatusCalculator {
	
	private static final Log log = LogFactory.getLog(BahmniMedicationStatusCalculator.class);
	
	/**
	 * Calculate FHIR medication request status based on DrugOrder state. Uses server timezone for
	 * all date comparisons. Ignores fulfiller status as per requirements.
	 * 
	 * @param drugOrder the OpenMRS DrugOrder to evaluate (can be null)
	 * @return the appropriate FHIR MedicationRequestStatus, or null if drugOrder is null
	 */
	public MedicationRequest.MedicationRequestStatus calculateStatus(DrugOrder drugOrder) {
		if (drugOrder == null) {
			return null;
		}
		
		Date now = new Date();
		
		if (drugOrder.getVoided()) {
			return MedicationRequest.MedicationRequestStatus.CANCELLED;
		}
		
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
	
	/**
	 * Determines if the drug order is active on the specified date. An order is active if it has
	 * started and has not stopped or expired.
	 * 
	 * @param drugOrder the drug order to check
	 * @param checkDate the date to check against
	 * @return true if the order is active on the check date
	 */
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
	
	/**
	 * Determines if the drug order is scheduled for the future.
	 * 
	 * @param drugOrder the drug order to check
	 * @param checkDate the date to check against
	 * @return true if the order is scheduled for the future
	 */
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
	
	/**
	 * Gets the effective start date for the order. Uses scheduledDate if urgency is
	 * ON_SCHEDULED_DATE, otherwise uses dateActivated.
	 * 
	 * @param drugOrder the drug order
	 * @return the effective start date
	 */
	private Date getEffectiveStartDate(DrugOrder drugOrder) {
		if (Order.Urgency.ON_SCHEDULED_DATE.equals(drugOrder.getUrgency())) {
			return drugOrder.getScheduledDate();
		}
		return drugOrder.getDateActivated();
	}
	
	/**
	 * Gets the effective stop date for the order. Uses dateStopped if available, otherwise uses
	 * autoExpireDate.
	 * 
	 * @param drugOrder the drug order
	 * @return the effective stop date, or null if no stop date is set
	 */
	private Date getEffectiveStopDate(DrugOrder drugOrder) {
		if (drugOrder.getDateStopped() != null) {
			return drugOrder.getDateStopped();
		}
		return drugOrder.getAutoExpireDate();
	}
	
	/**
	 * Checks if date1 is before or equal to date2. Uses server timezone for comparison.
	 * 
	 * @param date1 the first date
	 * @param date2 the second date
	 * @return true if date1 <= date2
	 */
	private boolean isDateBeforeOrEqual(Date date1, Date date2) {
		if (date1 == null || date2 == null) {
			return false;
		}
		return date1.compareTo(date2) <= 0;
	}
	
	/**
	 * Checks if date1 is after date2. Uses server timezone for comparison.
	 * 
	 * @param date1 the first date
	 * @param date2 the second date
	 * @return true if date1 > date2
	 */
	private boolean isDateAfter(Date date1, Date date2) {
		if (date1 == null || date2 == null) {
			return false;
		}
		return date1.compareTo(date2) > 0;
	}
}
