package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2addlextension.api.context.AppContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.api.OrderService;

import java.lang.reflect.Field;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirServiceRequestDaoImplTest {
	
	private static final String ORDER_UUID = "order-uuid-123";
	
	@Mock
	private OrderService orderService;
	
	@Mock
	private AppContext appContext;
	
	@InjectMocks
	private BahmniFhirServiceRequestDaoImpl serviceRequestDao;
	
	@Before
	public void setup() throws Exception {
		Field orderServiceField = BahmniFhirServiceRequestDaoImpl.class.getDeclaredField("orderService");
		orderServiceField.setAccessible(true);
		orderServiceField.set(serviceRequestDao, orderService);
		
		Field appContextField = BahmniFhirServiceRequestDaoImpl.class.getDeclaredField("appContext");
		appContextField.setAccessible(true);
		appContextField.set(serviceRequestDao, appContext);
	}
	
	@Test
	public void hasDistinctResults_shouldReturnFalse() {
		assertThat(serviceRequestDao.hasDistinctResults(), is(false));
	}
	
	@Test
	public void createOrUpdate_shouldThrowExceptionForDrugOrder() {
		Order order = new Order();
		OrderType drugOrderType = new OrderType();
		drugOrderType.setUuid(OrderType.DRUG_ORDER_TYPE_UUID);
		order.setOrderType(drugOrderType);

		assertThrows(InvalidRequestException.class, () -> {
			serviceRequestDao.createOrUpdate(order);
		});
	}
	
	@Test
	public void createOrUpdate_shouldSaveOrderForNonDrugOrder() {
		Order order = new Order();
		OrderType labOrderType = new OrderType();
		labOrderType.setUuid("lab-order-uuid");
		order.setOrderType(labOrderType);
		
		Order savedOrder = new Order();
		savedOrder.setUuid(ORDER_UUID);
		when(orderService.saveOrder(order, null)).thenReturn(savedOrder);
		
		Order result = serviceRequestDao.createOrUpdate(order);
		
		assertThat(result, notNullValue());
	}
	
	@Test
	public void createOrUpdate_shouldRejectDrugOrderType() {
		Order order = new Order();
		OrderType drugOrderType = new OrderType();
		drugOrderType.setUuid(OrderType.DRUG_ORDER_TYPE_UUID);
		order.setOrderType(drugOrderType);

		InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
			serviceRequestDao.createOrUpdate(order);
		});

		assertThat(exception.getMessage(), notNullValue());
	}
}
