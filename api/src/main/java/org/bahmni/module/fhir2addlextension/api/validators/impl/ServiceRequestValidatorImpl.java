package org.bahmni.module.fhir2addlextension.api.validators.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.AccessLevel;
import lombok.Setter;
import org.bahmni.module.fhir2addlextension.api.model.DispositionInfo;
import org.bahmni.module.fhir2addlextension.api.service.DispositionOrderLifecycleService;
import org.bahmni.module.fhir2addlextension.api.service.DispositionOrderTypeResolver;
import org.bahmni.module.fhir2addlextension.api.validators.ServiceRequestValidator;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.OrderType;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Setter(value = AccessLevel.PACKAGE)
public class ServiceRequestValidatorImpl implements ServiceRequestValidator {
	
	@Autowired
	private ConceptTranslator conceptTranslator;
	
	@Autowired
	private OrderService orderService;
	
	@Autowired
	private DispositionOrderTypeResolver dispositionOrderTypeResolver;
	
	@Autowired
	private DispositionOrderLifecycleService dispositionOrderLifecycleService;
	
	@Autowired
	private EncounterReferenceTranslator<Encounter> encounterReferenceTranslator;
	
	@Override
	public void validate(ServiceRequest serviceRequest) {
		Concept conceptBeingOrdered = conceptTranslator.toOpenmrsType(serviceRequest.getCode());
		if (conceptBeingOrdered == null) {
			throw new InvalidRequestException("Invalid ServiceRequest code");
		}
		OrderType orderType = orderService.getOrderTypeByConcept(conceptBeingOrdered);
		if (orderType == null) {
			orderType = dispositionOrderTypeResolver.resolve(conceptBeingOrdered);
		}
		if (orderType == null) {
			throw new InvalidRequestException("Unable to determine order type for ServiceRequest");
		}
		DispositionInfo disposition = dispositionOrderTypeResolver.getDisposition(conceptBeingOrdered);
		if (disposition != null && disposition.getRule() != null && disposition.getRule().isDuplicateChecked()
		        && dispositionOrderTypeResolver.getAdtOrderTypeName().equals(orderType.getName())) {
			rejectIfActiveOrderExists(serviceRequest, disposition);
		}
	}
	
	private void rejectIfActiveOrderExists(ServiceRequest serviceRequest, DispositionInfo disposition) {
		if (serviceRequest.getEncounter() == null) {
			return;
		}
		Encounter encounter = encounterReferenceTranslator.toOpenmrsType(serviceRequest.getEncounter());
		if (encounter != null && dispositionOrderLifecycleService.hasActiveOrder(encounter, disposition)) {
			throw new InvalidRequestException("An active " + disposition.getLabel() + " order already exists for this visit");
		}
	}
}
