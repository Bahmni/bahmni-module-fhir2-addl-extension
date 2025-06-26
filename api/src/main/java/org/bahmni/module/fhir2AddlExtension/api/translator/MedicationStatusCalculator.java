/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.bahmni.module.fhir2AddlExtension.api.translator;

import javax.annotation.Nonnull;

import org.hl7.fhir.r4.model.MedicationRequest;
import org.openmrs.DrugOrder;

/**
 * Interface for calculating FHIR MedicationRequest status based on DrugOrder state.
 */
public interface MedicationStatusCalculator {
	
	/**
	 * Calculate FHIR medication request status based on DrugOrder state.
	 * 
	 * @param drugOrder the OpenMRS DrugOrder to evaluate (can be null)
	 * @return the appropriate FHIR MedicationRequestStatus, or null if drugOrder is null
	 */
	MedicationRequest.MedicationRequestStatus calculateStatus(@Nonnull DrugOrder drugOrder);
}
