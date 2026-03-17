package org.bahmni.module.fhir2addlextension.api.validators;

import org.hl7.fhir.r4.model.ServiceRequest;

public interface ServiceRequestValidator {
	
	void validate(ServiceRequest serviceRequest);
}
