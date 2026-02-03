package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.User;
import org.openmrs.api.OrderService;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirServiceRequestDaoImplTest {
	
	private static final String RADIOLOGY_ORDER_TYPE_UUID = "52a447d3-a64a-11e3-9aeb-50e549534c5e";
	
	@Mock
	private OrderService orderService;
	
	private BahmniFhirServiceRequestDaoImpl serviceRequestDao;
	
	private Order parentOrder;
	
	private Order additionalViewOrder;
	
	private Order regularOrder;
	
	private Concept chestXrayConcept;
	
	private Concept posteroanteriorConcept;
	
	private OrderType radiologyOrderType;
	
	@Before
	public void setUp() throws Exception {
		serviceRequestDao = new BahmniFhirServiceRequestDaoImpl();
		
		Field orderServiceField = BahmniFhirServiceRequestDaoImpl.class.getDeclaredField("orderService");
		orderServiceField.setAccessible(true);
		orderServiceField.set(serviceRequestDao, orderService);
		
		radiologyOrderType = new OrderType();
		radiologyOrderType.setUuid(RADIOLOGY_ORDER_TYPE_UUID);
		
		chestXrayConcept = new Concept();
		chestXrayConcept.setId(1);
		
		posteroanteriorConcept = new Concept();
		posteroanteriorConcept.setId(2);
		
		parentOrder = new Order();
		parentOrder.setOrderType(radiologyOrderType);
		parentOrder.setConcept(chestXrayConcept);
		
		additionalViewOrder = new Order();
		additionalViewOrder.setOrderType(radiologyOrderType);
		additionalViewOrder.setConcept(posteroanteriorConcept);
		additionalViewOrder.setPreviousOrder(parentOrder);
		
		regularOrder = new Order();
		regularOrder.setOrderType(radiologyOrderType);
		regularOrder.setConcept(chestXrayConcept);
		regularOrder.setPreviousOrder(null);
		
		when(orderService.getNextOrderNumberSeedSequenceValue()).thenReturn(12345L);
	}

	@Test
	public void isAdditionalViewOrder_shouldReturnFalse_whenPreviousOrderIsNull() throws Exception {
		Method method = BahmniFhirServiceRequestDaoImpl.class.getDeclaredMethod("isAdditionalViewOrder", Order.class);
		method.setAccessible(true);
		
		boolean result = (boolean) method.invoke(serviceRequestDao, regularOrder);
		
		assertFalse("Should return false when previousOrder is null", result);
	}

	@Test
	public void isAdditionalViewOrder_shouldReturnFalse_whenConceptsMatch() throws Exception {
		Order revisionOrder = new Order();
		revisionOrder.setConcept(chestXrayConcept);
		revisionOrder.setPreviousOrder(parentOrder);

		Method method = BahmniFhirServiceRequestDaoImpl.class.getDeclaredMethod("isAdditionalViewOrder", Order.class);
		method.setAccessible(true);

		boolean result = (boolean) method.invoke(serviceRequestDao, revisionOrder);

		assertFalse("Should return false when concepts match", result);
	}

	@Test
	public void isAdditionalViewOrder_shouldReturnTrue_whenConceptsDiffer() throws Exception {
		Method method = BahmniFhirServiceRequestDaoImpl.class.getDeclaredMethod("isAdditionalViewOrder", Order.class);
		method.setAccessible(true);
		
		boolean result = (boolean) method.invoke(serviceRequestDao, additionalViewOrder);
		
		assertTrue("Should return true when concepts differ (additional view)", result);
	}

	@Test
	public void populateRequiredOrderFields_shouldSetOrderNumber_withOrdPrefix() throws Exception {
		Method method = BahmniFhirServiceRequestDaoImpl.class.getDeclaredMethod("populateRequiredOrderFields", Order.class);
		method.setAccessible(true);
		
		Order order = new Order();
		User mockCreator = new User();
		mockCreator.setId(1);
		order.setCreator(mockCreator);
		assertNull(order.getOrderNumber());
		
		method.invoke(serviceRequestDao, order);
		
		String orderNumber = order.getOrderNumber();
		assertNotNull("Order number should be populated", orderNumber);
		assertTrue("Order number should have ORD- prefix", orderNumber.startsWith("ORD-"));
		assertEquals("ORD-12345", orderNumber);
		
		verify(orderService).getNextOrderNumberSeedSequenceValue();
	}

	@Test
	public void populateRequiredOrderFields_shouldSetDateActivated_whenNull() throws Exception {
		Method method = BahmniFhirServiceRequestDaoImpl.class.getDeclaredMethod("populateRequiredOrderFields", Order.class);
		method.setAccessible(true);
		
		Order order = new Order();
		User mockCreator = new User();
		mockCreator.setId(1);
		order.setCreator(mockCreator);
		assertNull(order.getDateActivated());
		
		method.invoke(serviceRequestDao, order);
		
		assertNotNull("dateActivated should be populated", order.getDateActivated());
	}

	@Test
	public void setOrderNumberViaReflection_shouldSetOrderNumber() throws Exception {
		Method method = BahmniFhirServiceRequestDaoImpl.class.getDeclaredMethod("setOrderNumberViaReflection", Order.class,
		    String.class);
		method.setAccessible(true);
		
		Order order = new Order();
		assertNull(order.getOrderNumber());
		
		method.invoke(serviceRequestDao, order, "TEST-12345");
		
		assertEquals("TEST-12345", order.getOrderNumber());
	}
}
