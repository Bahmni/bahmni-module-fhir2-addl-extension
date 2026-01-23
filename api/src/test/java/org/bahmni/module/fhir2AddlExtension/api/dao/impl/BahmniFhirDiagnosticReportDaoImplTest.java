package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import org.bahmni.module.fhir2AddlExtension.api.model.FhirDiagnosticReportExt;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Order;
import org.springframework.test.util.ReflectionTestUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

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
	
	@Mock
	private SessionFactory sessionFactory;
	
	@Mock
	private Session session;
	
	@Mock
	private CriteriaBuilder criteriaBuilder;
	
	@Mock
	private CriteriaQuery<FhirDiagnosticReportExt> criteriaQuery;
	
	@Mock
	private Root<FhirDiagnosticReportExt> root;
	
	@Mock
	private Join ordersJoin;
	
	@Mock
	private Path orderIdPath;
	
	@Mock
	private Path retiredPath;
	
	@Mock
	private Query<FhirDiagnosticReportExt> query;
	
	@Mock
	private Predicate orderIdPredicate;
	
	@Mock
	private Predicate retiredPredicate;
	
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
	
	private void setupJpaCriteriaMocks() {
		when(sessionFactory.getCurrentSession()).thenReturn(session);
		when(session.getCriteriaBuilder()).thenReturn(criteriaBuilder);
		when(criteriaBuilder.createQuery(FhirDiagnosticReportExt.class)).thenReturn(criteriaQuery);
		when(criteriaQuery.from(FhirDiagnosticReportExt.class)).thenReturn(root);
		when(root.join("orders")).thenReturn(ordersJoin);
		when(ordersJoin.get("orderId")).thenReturn(orderIdPath);
		when(root.get("retired")).thenReturn(retiredPath);
		when(criteriaBuilder.equal(orderIdPath, order.getOrderId())).thenReturn(orderIdPredicate);
		when(criteriaBuilder.equal(retiredPath, false)).thenReturn(retiredPredicate);
		when(criteriaQuery.select(root)).thenReturn(criteriaQuery);
		when(criteriaQuery.where(orderIdPredicate, retiredPredicate)).thenReturn(criteriaQuery);
		when(session.createQuery(criteriaQuery)).thenReturn(query);
	}
	
	@Test
	public void findByOrder_shouldReturnDiagnosticReportWhenFound() {
		setupJpaCriteriaMocks();
		when(query.uniqueResult()).thenReturn(diagnosticReportExt);
		
		FhirDiagnosticReportExt result = diagnosticReportDao.findByOrder(order);
		
		assertNotNull("Should return diagnostic report when found", result);
		assertThat(result.getUuid(), equalTo(DIAGNOSTIC_REPORT_UUID));
		
		verify(sessionFactory).getCurrentSession();
		verify(session).getCriteriaBuilder();
		verify(criteriaBuilder).createQuery(FhirDiagnosticReportExt.class);
		verify(root).join("orders");
		verify(query).uniqueResult();
	}
	
	@Test
	public void findByOrder_shouldReturnNullWhenNotFound() {
		setupJpaCriteriaMocks();
		when(query.uniqueResult()).thenReturn(null);
		
		FhirDiagnosticReportExt result = diagnosticReportDao.findByOrder(order);
		
		assertNull("Should return null when diagnostic report not found", result);
		
		verify(sessionFactory).getCurrentSession();
		verify(session).getCriteriaBuilder();
		verify(query).uniqueResult();
	}
	
	@Test(expected = NullPointerException.class)
	public void findByOrder_shouldThrowExceptionForNullOrder() {
		diagnosticReportDao.findByOrder(null);
	}
	
	@Test
	public void findByOrder_shouldCreateCorrectOrderIdRestriction() {
		Order specificOrder = new Order();
		specificOrder.setOrderId(999);
		specificOrder.setUuid("specific-order-uuid");
		
		setupJpaCriteriaMocks();
		when(criteriaBuilder.equal(orderIdPath, specificOrder.getOrderId())).thenReturn(orderIdPredicate);
		when(query.uniqueResult()).thenReturn(diagnosticReportExt);
		
		diagnosticReportDao.findByOrder(specificOrder);
		
		verify(criteriaBuilder).equal(orderIdPath, specificOrder.getOrderId());
		verify(criteriaBuilder).equal(retiredPath, false);
	}
	
	@Test
	public void findByOrder_shouldCreateCorrectAliasForOrders() {
		setupJpaCriteriaMocks();
		when(query.uniqueResult()).thenReturn(diagnosticReportExt);
		
		diagnosticReportDao.findByOrder(order);
		
		verify(root).join("orders");
	}
	
	@Test
	public void findByOrder_shouldAddRetiredFalseRestriction() {
		setupJpaCriteriaMocks();
		when(query.uniqueResult()).thenReturn(diagnosticReportExt);
		
		diagnosticReportDao.findByOrder(order);
		
		verify(criteriaBuilder).equal(retiredPath, false);
	}
	
	@Test
	public void findByOrder_shouldUseCurrentSession() {
		setupJpaCriteriaMocks();
		when(query.uniqueResult()).thenReturn(diagnosticReportExt);
		
		diagnosticReportDao.findByOrder(order);
		
		verify(sessionFactory).getCurrentSession();
		verify(session).getCriteriaBuilder();
	}
	
	@Test
	public void findByOrder_shouldHandleOrderWithNullOrderId() {
		Order orderWithNullId = new Order();
		orderWithNullId.setOrderId(null);
		orderWithNullId.setUuid("some-uuid");
		
		when(sessionFactory.getCurrentSession()).thenReturn(session);
		when(session.getCriteriaBuilder()).thenReturn(criteriaBuilder);
		when(criteriaBuilder.createQuery(FhirDiagnosticReportExt.class)).thenReturn(criteriaQuery);
		when(criteriaQuery.from(FhirDiagnosticReportExt.class)).thenReturn(root);
		when(root.join("orders")).thenReturn(ordersJoin);
		when(ordersJoin.get("orderId")).thenReturn(orderIdPath);
		when(root.get("retired")).thenReturn(retiredPath);
		when(criteriaQuery.select(root)).thenReturn(criteriaQuery);
		when(criteriaQuery.where(any(), any())).thenReturn(criteriaQuery);
		when(session.createQuery(criteriaQuery)).thenReturn(query);
		when(query.uniqueResult()).thenReturn(null);
		
		FhirDiagnosticReportExt result = diagnosticReportDao.findByOrder(orderWithNullId);
		
		assertNull("Should handle order with null orderId gracefully", result);
		// Verify the essential behavior - that the method handles null orderId without throwing exception
		verify(query).uniqueResult();
	}
	
	@Test
	public void findByOrder_shouldReturnExactDiagnosticReportFromQuery() {
		setupJpaCriteriaMocks();
		FhirDiagnosticReportExt specificReport = new FhirDiagnosticReportExt();
		specificReport.setUuid("specific-uuid-456");
		when(query.uniqueResult()).thenReturn(specificReport);
		
		FhirDiagnosticReportExt result = diagnosticReportDao.findByOrder(order);
		
		assertNotNull("Should return diagnostic report", result);
		assertThat(result.getUuid(), equalTo("specific-uuid-456"));
		assertEquals("Should return the exact object from query", specificReport, result);
	}
	
	@Test
	public void findByOrder_shouldHandleDifferentOrderIds() {
		Order[] testOrders = { createOrderWithId(1), createOrderWithId(999999), createOrderWithId(-1), createOrderWithId(0) };
		
		for (Order testOrder : testOrders) {
			setupJpaCriteriaMocks();
			when(criteriaBuilder.equal(orderIdPath, testOrder.getOrderId())).thenReturn(orderIdPredicate);
			when(query.uniqueResult()).thenReturn(diagnosticReportExt);
			
			FhirDiagnosticReportExt result = diagnosticReportDao.findByOrder(testOrder);
			
			assertNotNull("Should return result for order ID: " + testOrder.getOrderId(), result);
			
			org.mockito.Mockito.reset(sessionFactory, session, criteriaBuilder, criteriaQuery, root, ordersJoin,
			    orderIdPath, retiredPath, query, orderIdPredicate, retiredPredicate);
		}
	}
	
	@Test
	public void findByOrder_shouldUseUniqueResultNotList() {
		setupJpaCriteriaMocks();
		when(query.uniqueResult()).thenReturn(diagnosticReportExt);
		
		diagnosticReportDao.findByOrder(order);
		
		verify(query).uniqueResult();
		verify(query, org.mockito.Mockito.never()).list();
	}
	
	private Order createOrderWithId(Integer orderId) {
		Order order = new Order();
		order.setOrderId(orderId);
		order.setUuid("uuid-" + orderId);
		return order;
	}
}
