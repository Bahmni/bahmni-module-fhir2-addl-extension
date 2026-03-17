package org.bahmni.module.fhir2addlextension.api.validators;

import org.hl7.fhir.r4.model.Bundle;

public interface ConsultationBundleValidator {
	
	void validateBundleType(Bundle bundle);
	
	void validateBundleEntries(Bundle bundle);
}
