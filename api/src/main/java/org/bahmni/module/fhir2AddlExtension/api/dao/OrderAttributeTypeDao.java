package org.bahmni.module.fhir2AddlExtension.api.dao;

import org.openmrs.OrderAttributeType;
import org.openmrs.OrderGroupAttributeType;

import java.util.List;

public interface OrderAttributeTypeDao {
	
	List<OrderAttributeType> getOrderAttributeTypes(boolean includeRetired);
	
	List<OrderGroupAttributeType> getOrderGroupAttributeTypes(boolean includeRetired);
}
