package org.bahmni.module.fhir2addlextension.api.providers;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import lombok.Setter;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirMedicationRequestService;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.providers.r4.MedicationRequestFhirResourceProvider;
import org.openmrs.module.fhir2.providers.util.FhirProviderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@R4Provider
public class BahmniMedicationRequestFhirR4Provider extends MedicationRequestFhirResourceProvider {
	
	@Setter(onMethod_ = @Autowired)
	private BahmniFhirMedicationRequestService bahmniFhirMedicationRequestService;
	
	@Create
	public MethodOutcome createMedicationRequest(@ResourceParam MedicationRequest medicationRequest) {
		return FhirProviderUtils.buildCreate(bahmniFhirMedicationRequestService.create(medicationRequest));
	}
}
