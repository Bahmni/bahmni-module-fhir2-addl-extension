package org.bahmni.module.fhir2AddlExtension.api.validators.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.AccessLevel;
import lombok.Setter;
import org.bahmni.module.fhir2AddlExtension.api.validators.ServiceRequestValidator;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.Concept;
import org.openmrs.OrderType;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Setter(value = AccessLevel.PACKAGE)
public class ServiceRequestValidatorImpl implements ServiceRequestValidator {
	
	@Autowired
	private ConceptTranslator conceptTranslator;
	
	@Autowired
	private OrderService orderService;
	
	@Override
	public void validate(ServiceRequest serviceRequest) {
		Concept conceptBeingOrdered = conceptTranslator.toOpenmrsType(serviceRequest.getCode());
		if (conceptBeingOrdered == null) {
			throw new InvalidRequestException("Invalid ServiceRequest code");
		}
		OrderType orderType = orderService.getOrderTypeByConcept(conceptBeingOrdered);
		if (orderType == null) {
			throw new InvalidRequestException("Unable to determine order type for ServiceRequest");
		}
	}
}
