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

import javax.annotation.Nonnull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.openmrs.DrugOrder;
import org.openmrs.module.fhir2.api.translators.MedicationRequestStatusTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Calculates FHIR medication request status based on DrugOrder state. Delegates to
 * BahmniMedicationStatusCalculator for status calculation logic.
 */
@Component
@Primary
public class BahmniMedicationRequestStatusTranslatorImpl implements MedicationRequestStatusTranslator {
	
	private static final Log log = LogFactory.getLog(BahmniMedicationRequestStatusTranslatorImpl.class);
	
	@Autowired
	private BahmniMedicationStatusCalculator statusCalculator;
	
	@Override
	public MedicationRequest.MedicationRequestStatus toFhirResource(@Nonnull DrugOrder drugOrder) {
		if (drugOrder == null) {
			return null;
		}
		
		return statusCalculator.calculateStatus(drugOrder);
	}
}
