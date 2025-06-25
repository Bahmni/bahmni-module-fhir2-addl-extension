package org.bahmni.module.fhir2AddlExtension.api.providers;

import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.StringParam;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirMedicationService;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.providers.r4.MedicationFhirResourceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@R4Provider
public class BahmniMedicationFhirR4ResourceProvider extends MedicationFhirResourceProvider {
	
	@Autowired
	private BahmniFhirMedicationService bahmniFhirMedicationService;
	
	@Search
	public IBundleProvider searchMedicationsByName(@RequiredParam(name = "name") StringParam nameParam) {
		return bahmniFhirMedicationService.searchMedicationsByName(nameParam);
	}
}
