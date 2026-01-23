package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2AddlExtension.api.dao.OrderAttributeTypeDao;
import org.openmrs.OrderAttributeType;
import org.openmrs.OrderGroupAttributeType;
import org.openmrs.api.db.OrderDAO;
import org.openmrs.attribute.BaseAttributeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class OrderAttributeTypeDaoImpl implements OrderAttributeTypeDao {
	
	private final OrderDAO orderDao;
	
	@Autowired
	public OrderAttributeTypeDaoImpl(OrderDAO orderDao) {
		this.orderDao = orderDao;
	}
	
	@Override
	@Cacheable(value = "fhir2addlextensionOrderAttributeType")
	public List<OrderAttributeType> getOrderAttributeTypes(boolean includeRetired) {
		return filterAttributeTypes(orderDao.getAllOrderAttributeTypes(), includeRetired);
	}
	
	@Override
	@Cacheable(value = "fhir2addlextensionOrderGroupAttributeType")
	public List<OrderGroupAttributeType> getOrderGroupAttributeTypes(boolean includeRetired) {
		return filterAttributeTypes(orderDao.getAllOrderGroupAttributeTypes(), includeRetired);
	}
	
	private <T extends BaseAttributeType> List<T> filterAttributeTypes(List<T> allOrderAttributeTypes, boolean includeRetired) {
        if (allOrderAttributeTypes == null || allOrderAttributeTypes.isEmpty()) {
            return Collections.emptyList();
        }
        return allOrderAttributeTypes.stream()
            .filter(orderAttributeType -> includeRetired || !orderAttributeType.getRetired())
            .collect(Collectors.toList());
    }
}
