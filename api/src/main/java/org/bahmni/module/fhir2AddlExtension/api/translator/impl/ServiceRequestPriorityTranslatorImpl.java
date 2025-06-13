package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.translator.ServiceRequestPriorityTranslator;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component
public class ServiceRequestPriorityTranslatorImpl implements ServiceRequestPriorityTranslator {
	
	@Override
	public ServiceRequest.ServiceRequestPriority toFhirResource(@Nonnull Order.Urgency urgency) {
		if (urgency == null) {
			return ServiceRequest.ServiceRequestPriority.ROUTINE;
		}
		switch (urgency) {
			case STAT:
				return ServiceRequest.ServiceRequestPriority.STAT;
			case ON_SCHEDULED_DATE:
			case ROUTINE:
			default:
				return ServiceRequest.ServiceRequestPriority.ROUTINE;
		}
	}
	
	@Override
	public Order.Urgency toOpenmrsType(@Nonnull ServiceRequest.ServiceRequestPriority serviceRequestPriority) {
		if (serviceRequestPriority == null) {
			return Order.Urgency.ROUTINE;
		}
		switch (serviceRequestPriority) {
			case STAT:
				return Order.Urgency.STAT;
			default:
				return Order.Urgency.ROUTINE;
		}
	}
}
