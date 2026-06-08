package org.bahmni.module.fhir2addlextension.api.translator.impl;

import org.bahmni.module.fhir2addlextension.api.translator.ServiceRequestAttributeTranslator;
import org.bahmni.module.fhir2addlextension.api.translator.ServiceRequestExtensionTranslator;
import org.openmrs.OrderAttribute;
import org.openmrs.OrderAttributeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServiceRequestExtensionTranslatorImpl extends BaseAttributeTranslatorRegistry<OrderAttribute, OrderAttributeType, ServiceRequestAttributeTranslator> implements ServiceRequestExtensionTranslator {
	
	@Autowired
	public ServiceRequestExtensionTranslatorImpl(ServiceRequestAttributeTranslatorImpl defaultAttributeTranslator) {
		super(defaultAttributeTranslator);
	}
}
