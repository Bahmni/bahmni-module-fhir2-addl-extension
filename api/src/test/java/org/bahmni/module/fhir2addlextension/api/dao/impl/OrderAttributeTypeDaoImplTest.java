package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

import org.bahmni.module.fhir2addlextension.api.BaseFhir2AddlContextSensitiveTest;
import org.bahmni.module.fhir2addlextension.api.dao.OrderAttributeTypeDao;
import org.junit.Test;
import org.openmrs.OrderAttributeType;
import org.openmrs.OrderGroupAttributeType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

public class OrderAttributeTypeDaoImplTest extends BaseFhir2AddlContextSensitiveTest {
	
	private static final String DATASET = "org/bahmni/module/fhir2addlextension/api/dao/impl/OrderAttributeTypeDaoImplTest_data.xml";
	
	@Autowired
	private OrderAttributeTypeDao dao;
	
	private static List<String> names(List<? extends org.openmrs.attribute.BaseAttributeType<?>> types) {
		return types.stream().map(org.openmrs.attribute.BaseAttributeType::getName).collect(Collectors.toList());
	}
	
	@Test
	public void getOrderAttributeTypes_withoutAnyConfigured_returnsEmptyList() {
		assertThat(dao.getOrderAttributeTypes(true), empty());
	}
	
	@Test
	public void getOrderAttributeTypes_excludingRetired_returnsOnlyActiveTypes() throws Exception {
		executeDataSet(DATASET);
		
		List<OrderAttributeType> result = dao.getOrderAttributeTypes(false);
		
		assertThat(names(result), containsInAnyOrder("Active Attr"));
	}
	
	@Test
	public void getOrderAttributeTypes_includingRetired_returnsActiveAndRetiredTypes() throws Exception {
		executeDataSet(DATASET);
		
		List<OrderAttributeType> result = dao.getOrderAttributeTypes(true);
		
		assertThat(names(result), containsInAnyOrder("Active Attr", "Retired Attr"));
	}
	
	@Test
	public void getOrderGroupAttributeTypes_withoutAnyConfigured_returnsEmptyList() {
		assertThat(dao.getOrderGroupAttributeTypes(true), empty());
	}
	
	@Test
	public void getOrderGroupAttributeTypes_excludingRetired_returnsOnlyActiveTypes() throws Exception {
		executeDataSet(DATASET);
		
		List<OrderGroupAttributeType> result = dao.getOrderGroupAttributeTypes(false);
		
		assertThat(names(result), containsInAnyOrder("Active Group Attr"));
	}
	
	@Test
	public void getOrderGroupAttributeTypes_includingRetired_returnsActiveAndRetiredTypes() throws Exception {
		executeDataSet(DATASET);
		
		List<OrderGroupAttributeType> result = dao.getOrderGroupAttributeTypes(true);
		
		assertThat(names(result), containsInAnyOrder("Active Group Attr", "Retired Group Attr"));
	}
}
