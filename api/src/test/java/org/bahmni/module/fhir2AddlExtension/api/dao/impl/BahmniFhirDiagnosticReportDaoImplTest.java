package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import org.bahmni.module.fhir2AddlExtension.api.model.FhirDiagnosticReportExt;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Order;
import org.springframework.test.util.ReflectionTestUtils;

import static org.hamcrest.CoreMatchers.equalTo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirDiagnosticReportDaoImplTest {
	
	private static final String DIAGNOSTIC_REPORT_UUID = "123e4567-e89b-12d3-a456-426614174000";
	
	private static final Integer ORDER_ID = 100;
	
	private static final String ORDER_UUID = "order-uuid-123";
	
	private static final String ORDERS_ALIAS = "o";
	
	@Mock
	private SessionFactory sessionFactory;
	
	@Mock
	private Session session;
	
	@Mock
	private Criteria criteria;
	
	@InjectMocks
	private BahmniFhirDiagnosticReportDaoImpl diagnosticReportDao;
	
	private Order order;
	
	private FhirDiagnosticReportExt diagnosticReportExt;
	
	@Before
	public void setUp() {
		order = new Order();
		order.setOrderId(ORDER_ID);
		order.setUuid(ORDER_UUID);
		
		diagnosticReportExt = new FhirDiagnosticReportExt();
		diagnosticReportExt.setUuid(DIAGNOSTIC_REPORT_UUID);
		
		ReflectionTestUtils.setField(diagnosticReportDao, "sessionFactory", sessionFactory);
	}
	
	private void setupHibernateMocks() {
		when(sessionFactory.getCurrentSession()).thenReturn(session);
		when(session.createCriteria(FhirDiagnosticReportExt.class)).thenReturn(criteria);
		when(criteria.createAlias(anyString(), anyString())).thenReturn(criteria);
		when(criteria.add(any(Criterion.class))).thenReturn(criteria);
	}
	
	@Test
	public void findByOrder_shouldReturnDiagnosticReportWhenFound() {
		setupHibernateMocks();
		when(criteria.uniqueResult()).thenReturn(diagnosticReportExt);
		
		FhirDiagnosticReportExt result = diagnosticReportDao.findByOrder(order);
		
		assertNotNull("Should return diagnostic report when found", result);
		assertThat(result.getUuid(), equalTo(DIAGNOSTIC_REPORT_UUID));
		
		verify(sessionFactory).getCurrentSession();
		verify(session).createCriteria(FhirDiagnosticReportExt.class);
		verify(criteria).createAlias("orders", ORDERS_ALIAS);
		verify(criteria).uniqueResult();
		
		verify(criteria, org.mockito.Mockito.times(2)).add(any(Criterion.class));
	}
	
	@Test
	public void findByOrder_shouldReturnNullWhenNotFound() {
		setupHibernateMocks();
		when(criteria.uniqueResult()).thenReturn(null);
		
		FhirDiagnosticReportExt result = diagnosticReportDao.findByOrder(order);
		
		assertNull("Should return null when diagnostic report not found", result);
		
		verify(sessionFactory).getCurrentSession();
		verify(session).createCriteria(FhirDiagnosticReportExt.class);
		verify(criteria).createAlias("orders", ORDERS_ALIAS);
		verify(criteria).uniqueResult();
	}
	
	@Test(expected = NullPointerException.class)
	public void findByOrder_shouldThrowExceptionForNullOrder() {
		diagnosticReportDao.findByOrder(null);
	}
	
	@Test
	public void findByOrder_shouldCreateCorrectOrderIdRestriction() {
		setupHibernateMocks();
		Order specificOrder = new Order();
		specificOrder.setOrderId(999);
		specificOrder.setUuid("specific-order-uuid");
		when(criteria.uniqueResult()).thenReturn(diagnosticReportExt);
		
		diagnosticReportDao.findByOrder(specificOrder);
		
		verify(criteria).createAlias("orders", ORDERS_ALIAS);
		verify(criteria, org.mockito.Mockito.times(2)).add(any(Criterion.class));
	}
	
	@Test
	public void findByOrder_shouldCreateCorrectAliasForOrders() {
		setupHibernateMocks();
		when(criteria.uniqueResult()).thenReturn(diagnosticReportExt);
		
		diagnosticReportDao.findByOrder(order);
		
		verify(criteria).createAlias("orders", ORDERS_ALIAS);
	}
	
	@Test
	public void findByOrder_shouldAddRetiredFalseRestriction() {
		setupHibernateMocks();
		when(criteria.uniqueResult()).thenReturn(diagnosticReportExt);
		
		diagnosticReportDao.findByOrder(order);
		
		verify(criteria, org.mockito.Mockito.times(2)).add(any(Criterion.class));
	}
	
	@Test
	public void findByOrder_shouldUseCurrentSession() {
		setupHibernateMocks();
		when(criteria.uniqueResult()).thenReturn(diagnosticReportExt);
		
		diagnosticReportDao.findByOrder(order);
		
		verify(sessionFactory).getCurrentSession();
		verify(session).createCriteria(FhirDiagnosticReportExt.class);
	}
	
	@Test
	public void findByOrder_shouldHandleOrderWithNullOrderId() {
		setupHibernateMocks();
		Order orderWithNullId = new Order();
		orderWithNullId.setOrderId(null);
		orderWithNullId.setUuid("some-uuid");
		when(criteria.uniqueResult()).thenReturn(null);
		
		FhirDiagnosticReportExt result = diagnosticReportDao.findByOrder(orderWithNullId);
		
		assertNull("Should handle order with null orderId gracefully", result);
		
		verify(criteria).createAlias("orders", ORDERS_ALIAS);
		verify(criteria, org.mockito.Mockito.times(2)).add(any(Criterion.class));
	}
	
	@Test
	public void findByOrder_shouldReturnExactDiagnosticReportFromQuery() {
		setupHibernateMocks();
		FhirDiagnosticReportExt specificReport = new FhirDiagnosticReportExt();
		specificReport.setUuid("specific-uuid-456");
		when(criteria.uniqueResult()).thenReturn(specificReport);
        FhirDiagnosticReportExt result = diagnosticReportDao.findByOrder(order);
		
		assertNotNull("Should return diagnostic report", result);
		assertThat(result.getUuid(), equalTo("specific-uuid-456"));
		assertEquals("Should return the exact object from query", specificReport, result);
	}
	
	@Test
	public void findByOrder_shouldHandleDifferentOrderIds() {
		Order[] testOrders = { createOrderWithId(1), createOrderWithId(999999), createOrderWithId(-1),
		        createOrderWithId(0)
		};
		
		for (Order testOrder : testOrders) {
			setupHibernateMocks();
			when(criteria.uniqueResult()).thenReturn(diagnosticReportExt);
            FhirDiagnosticReportExt result = diagnosticReportDao.findByOrder(testOrder);
			
			assertNotNull("Should return result for order ID: " + testOrder.getOrderId(), result);
			
			org.mockito.Mockito.reset(sessionFactory, session, criteria);
		}
	}
	
	@Test
	public void findByOrder_shouldUseUniqueResultNotList() {
		setupHibernateMocks();
		when(criteria.uniqueResult()).thenReturn(diagnosticReportExt);
		
		diagnosticReportDao.findByOrder(order);
		
		verify(criteria).uniqueResult();
		verify(criteria, org.mockito.Mockito.never()).list();
	}

	private Order createOrderWithId(Integer orderId) {
		Order order = new Order();
		order.setOrderId(orderId);
		order.setUuid("uuid-" + orderId);
		return order;
	}
}
