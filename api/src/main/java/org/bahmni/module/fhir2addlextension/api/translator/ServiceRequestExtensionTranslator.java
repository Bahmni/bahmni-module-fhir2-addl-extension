package org.bahmni.module.fhir2addlextension.api.translator;

import org.openmrs.OrderAttribute;
import org.openmrs.OrderAttributeType;

public interface ServiceRequestExtensionTranslator extends AttributeTranslatorRegistry<OrderAttribute, OrderAttributeType, ServiceRequestAttributeTranslator> {
	
}
