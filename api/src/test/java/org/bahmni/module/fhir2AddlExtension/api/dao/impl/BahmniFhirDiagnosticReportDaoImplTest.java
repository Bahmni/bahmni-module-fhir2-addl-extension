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

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirDiagnosticReportDaoImplTest {
	
	private static final String ORDER_UUID = "456e7890-e89b-12d3-a456-426614174001";
	
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
	private Join<Object, Object> ordersJoin;
	
	@Mock
	private Query<FhirDiagnosticReportExt> query;
	
	@InjectMocks
	private BahmniFhirDiagnosticReportDaoImpl diagnosticReportDao;
	
	@Before
	public void setUp() {
		when(sessionFactory.getCurrentSession()).thenReturn(session);
		when(session.getCriteriaBuilder()).thenReturn(criteriaBuilder);
		when(criteriaBuilder.createQuery(FhirDiagnosticReportExt.class)).thenReturn(criteriaQuery);
		when(criteriaQuery.from(FhirDiagnosticReportExt.class)).thenReturn(root);
		when(root.join("orders")).thenReturn(ordersJoin);
		
		Path<Object> uuidPath = mock(Path.class);
		Path<Object> voidedPath = mock(Path.class);
		when(ordersJoin.get("uuid")).thenReturn(uuidPath);
		when(root.get("voided")).thenReturn(voidedPath);
		
		Predicate predicate1 = mock(Predicate.class);
		Predicate predicate2 = mock(Predicate.class);
		when(criteriaBuilder.equal(uuidPath, ORDER_UUID)).thenReturn(predicate1);
		when(criteriaBuilder.equal(voidedPath, false)).thenReturn(predicate2);
		
		when(criteriaQuery.select(root)).thenReturn(criteriaQuery);
		when(criteriaQuery.where(predicate1, predicate2)).thenReturn(criteriaQuery);
		when(session.createQuery(criteriaQuery)).thenReturn(query);
	}
	
	@Test
	public void findByOrderUuid_shouldReturnNullWhenNoResultFound() {
		when(query.uniqueResult()).thenReturn(null);
		
		FhirDiagnosticReportExt result = diagnosticReportDao.findByOrderUuid(ORDER_UUID);
		
		assertThat(result, nullValue());
		verify(session).getCriteriaBuilder();
		verify(criteriaBuilder).createQuery(FhirDiagnosticReportExt.class);
		verify(root).join("orders");
		verify(ordersJoin).get("uuid");
		verify(root).get("voided");
	}
	
	@Test
	public void findByOrderUuid_shouldReturnDiagnosticReportWhenFound() {
		FhirDiagnosticReportExt mockReport = mock(FhirDiagnosticReportExt.class);
		when(query.uniqueResult()).thenReturn(mockReport);
		
		FhirDiagnosticReportExt result = diagnosticReportDao.findByOrderUuid(ORDER_UUID);
		
		assertNotNull(result);
		verify(query).uniqueResult();
	}
	
	@Test
	public void findByOrderUuid_shouldJoinWithOrdersTable() {
		when(query.uniqueResult()).thenReturn(null);
		
		diagnosticReportDao.findByOrderUuid(ORDER_UUID);
		
		verify(root).join("orders");
	}
	
	@Test
	public void findByOrderUuid_shouldFilterByOrderUuid() {
		when(query.uniqueResult()).thenReturn(null);
		
		diagnosticReportDao.findByOrderUuid(ORDER_UUID);
		
		verify(ordersJoin).get("uuid");
		verify(criteriaBuilder).equal(any(), eq(ORDER_UUID));
	}
	
	@Test
	public void findByOrderUuid_shouldFilterByVoidedFalse() {
		when(query.uniqueResult()).thenReturn(null);
		
		diagnosticReportDao.findByOrderUuid(ORDER_UUID);
		
		verify(root).get("voided");
		verify(criteriaBuilder).equal(any(), eq(false));
	}
}
