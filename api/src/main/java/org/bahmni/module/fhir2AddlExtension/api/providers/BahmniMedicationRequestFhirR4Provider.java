package org.bahmni.module.fhir2AddlExtension.api.providers;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.openmrs.module.fhir2.api.FhirMedicationRequestService;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.providers.r4.MedicationRequestFhirResourceProvider;
import org.openmrs.module.fhir2.providers.util.FhirProviderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@R4Provider
public class BahmniMedicationRequestFhirR4Provider extends MedicationRequestFhirResourceProvider {
	
	@Autowired
	private FhirMedicationRequestService fhirMedicationRequestService;
	
	@Create
	public MethodOutcome createMedicationRequest(@ResourceParam MedicationRequest medicationRequest) {
		return FhirProviderUtils.buildCreate(fhirMedicationRequestService.create(medicationRequest));
	}
	
}
