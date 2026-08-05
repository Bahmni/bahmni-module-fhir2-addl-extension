package org.bahmni.module.fhir2addlextension.api.service;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.openmrs.module.fhir2.api.FhirMedicationRequestService;

import javax.annotation.Nullable;
import java.util.Date;

public interface BahmniFhirMedicationRequestService extends FhirMedicationRequestService {
	
	MedicationRequest stopMedicationRequest(String medicationRequestUuid, @Nullable CodeableConcept reason,
	        @Nullable Date effectiveDate, @Nullable String note);
}
