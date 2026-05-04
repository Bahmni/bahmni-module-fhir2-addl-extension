package org.bahmni.module.fhir2addlextension.api.translator;

import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.Order;
import org.openmrs.module.fhir2.api.translators.ToFhirTranslator;

public interface ServiceRequestStatusTranslator extends ToFhirTranslator<Order, ServiceRequest.ServiceRequestStatus> {
}
