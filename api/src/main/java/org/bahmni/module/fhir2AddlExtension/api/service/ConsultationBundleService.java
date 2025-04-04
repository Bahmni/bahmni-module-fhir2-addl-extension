/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. Bahmni amd OpenMRS are also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright 2025 (C) Thoughtworks Inc.
 */

package org.bahmni.module.fhir2AddlExtension.api.service;

import org.hl7.fhir.r4.model.Bundle;

public interface ConsultationBundleService {
	
	Bundle create(Bundle bundle);
}
