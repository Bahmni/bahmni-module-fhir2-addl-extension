package org.bahmni.module.fhir2addlextension.api.translator.impl;

import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2addlextension.api.dao.OrderAttributeTypeDao;
import org.bahmni.module.fhir2addlextension.api.translator.ServiceRequestAttributeTranslator;
import org.hl7.fhir.r4.model.ResourceType;
import org.openmrs.OrderAttribute;
import org.openmrs.OrderAttributeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ServiceRequestAttributeTranslatorImpl extends BaseAttributeTranslator<OrderAttribute, OrderAttributeType> implements ServiceRequestAttributeTranslator {
	
	private final OrderAttributeTypeDao attributeTypeDao;
	
	@Autowired
	public ServiceRequestAttributeTranslatorImpl(OrderAttributeTypeDao attributeTypeDao) {
		this.attributeTypeDao = attributeTypeDao;
	}
	
	@Override
	protected String getExtensionUrlPrefix() {
		return BahmniFhirConstants.FHIR_EXT_SERVICE_REQUEST_ATTRIBUTE_PREFIX;
	}
	
	@Override
	protected ResourceType getResourceType() {
		return ResourceType.ServiceRequest;
	}
	
	@Override
	protected List<OrderAttributeType> getActiveAttributeTypes() {
		return attributeTypeDao.getOrderAttributeTypes(false);
	}
	
	@Override
	protected OrderAttribute createAttribute() {
		return new OrderAttribute();
	}
}
