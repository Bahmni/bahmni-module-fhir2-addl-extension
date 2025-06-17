package org.bahmni.module.fhir2AddlExtension.api.translator;

import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.Order;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;

import javax.annotation.Nonnull;

public interface ServiceRequestPriorityTranslator extends OpenmrsFhirTranslator<Order.Urgency, ServiceRequest.ServiceRequestPriority> {
	
	@Override
	ServiceRequest.ServiceRequestPriority toFhirResource(@Nonnull Order.Urgency urgency);
	
	@Override
	Order.Urgency toOpenmrsType(@Nonnull ServiceRequest.ServiceRequestPriority serviceRequestPriority);
}
