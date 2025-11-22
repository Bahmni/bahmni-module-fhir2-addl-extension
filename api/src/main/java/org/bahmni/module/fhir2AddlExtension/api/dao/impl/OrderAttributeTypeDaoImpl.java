package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2AddlExtension.api.dao.OrderAttributeTypeDao;
import org.openmrs.OrderAttributeType;
import org.openmrs.OrderGroupAttributeType;
import org.openmrs.api.db.OrderDAO;
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
    @Cacheable(value = "fhir2extensionOrderAttributeType")
    public List<OrderAttributeType> getOrderAttributeTypes(boolean includeRetired) {
        List<OrderAttributeType> allOrderAttributeTypes = orderDao.getAllOrderAttributeTypes();
        if (allOrderAttributeTypes == null) {
            return Collections.emptyList();
        }
        return allOrderAttributeTypes.stream().filter(orderAttributeType -> {
            return includeRetired || !orderAttributeType.getRetired();
        }).collect(Collectors.toList());
    }
	
	@Override
    @Cacheable(value = "fhir2extensionOrderGroupAttributeType")
    public List<OrderGroupAttributeType> getOrderGroupAttributeTypes(boolean includeRetired) {
        List<OrderGroupAttributeType> allOrderGroupAttributeTypes = orderDao.getAllOrderGroupAttributeTypes();
        if (allOrderGroupAttributeTypes == null) {
            return Collections.emptyList();
        }
        return allOrderGroupAttributeTypes.stream().filter(orderGroupAttributeType -> {
            return includeRetired || !orderGroupAttributeType.getRetired();
        }).collect(Collectors.toList());
    }
}
