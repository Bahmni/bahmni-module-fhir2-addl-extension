package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2addlextension.api.context.AppContext;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Visit;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirServiceRequestDaoImplTest {
	
	private static final String ORDER_UUID = "order-uuid-123";
	
	@Mock
	private OrderService orderService;
	
	@Mock
	private AppContext appContext;
	
	@Mock
	private SessionFactory sessionFactory;
	
	@Mock
	private Session session;
	
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
		
		Field sessionFactoryField = BaseFhirDao.class.getDeclaredField("sessionFactory");
		sessionFactoryField.setAccessible(true);
		sessionFactoryField.set(serviceRequestDao, sessionFactory);
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
		org.mockito.Mockito.when(orderService.saveOrder(order, null)).thenReturn(savedOrder);
		
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
	
	@Test
	public void setupSearchParams_shouldHandleEncounterReference() {
		Criteria criteria = mock(Criteria.class);
		ca.uhn.fhir.rest.param.ReferenceAndListParam encounterRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		        .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam().add(new ca.uhn.fhir.rest.param.ReferenceParam(
		                "Encounter", "enc-123")));
		
		org.openmrs.module.fhir2.api.search.param.SearchParameterMap params = new org.openmrs.module.fhir2.api.search.param.SearchParameterMap();
		params.addParameter("encounter", encounterRef);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldHandlePatientReference() {
		Criteria criteria = mock(Criteria.class);
		ca.uhn.fhir.rest.param.ReferenceAndListParam patientRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		        .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam().add(new ca.uhn.fhir.rest.param.ReferenceParam(
		                "Patient", "pat-123")));
		
		org.openmrs.module.fhir2.api.search.param.SearchParameterMap params = new org.openmrs.module.fhir2.api.search.param.SearchParameterMap();
		params.addParameter("subject", patientRef);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldHandleCodeParameter() {
		Criteria criteria = mock(Criteria.class);
		ca.uhn.fhir.rest.param.TokenAndListParam code = new ca.uhn.fhir.rest.param.TokenAndListParam()
		        .addAnd(new ca.uhn.fhir.rest.param.TokenOrListParam().add("order-code"));
		
		org.openmrs.module.fhir2.api.search.param.SearchParameterMap params = new org.openmrs.module.fhir2.api.search.param.SearchParameterMap();
		params.addParameter("code", code);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldHandleLocationReference() {
		Criteria criteria = mock(Criteria.class);
		ca.uhn.fhir.rest.param.ReferenceAndListParam locationRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		        .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam().add(new ca.uhn.fhir.rest.param.ReferenceParam(
		                "Location", "loc-123")));
		
		org.openmrs.module.fhir2.api.search.param.SearchParameterMap params = new org.openmrs.module.fhir2.api.search.param.SearchParameterMap();
		params.addParameter("order-location", locationRef);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldHandleCategoryReference() {
		Criteria criteria = mock(Criteria.class);
		ca.uhn.fhir.rest.param.ReferenceAndListParam categoryRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		        .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam().add(new ca.uhn.fhir.rest.param.ReferenceParam(
		                "OrderType", "lab")));
		
		org.openmrs.module.fhir2.api.search.param.SearchParameterMap params = new org.openmrs.module.fhir2.api.search.param.SearchParameterMap();
		params.addParameter("category", categoryRef);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldHandleBasedOnReference() {
		Criteria criteria = mock(Criteria.class);
		ca.uhn.fhir.rest.param.ReferenceAndListParam basedOnRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		        .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam().add(new ca.uhn.fhir.rest.param.ReferenceParam(
		                "Order", "prev-order")));
		
		org.openmrs.module.fhir2.api.search.param.SearchParameterMap params = new org.openmrs.module.fhir2.api.search.param.SearchParameterMap();
		params.addParameter("based-on", basedOnRef);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldHandleCodedConceptParameter() {
		Criteria criteria = mock(Criteria.class);
		ca.uhn.fhir.rest.param.TokenAndListParam code = new ca.uhn.fhir.rest.param.TokenAndListParam()
		        .addAnd(new ca.uhn.fhir.rest.param.TokenOrListParam().add("lab-code"));
		
		org.openmrs.module.fhir2.api.search.param.SearchParameterMap params = new org.openmrs.module.fhir2.api.search.param.SearchParameterMap();
		params.addParameter("code", code);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldHandleParticipantReference() {
		Criteria criteria = mock(Criteria.class);
		ca.uhn.fhir.rest.param.ReferenceAndListParam participantRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		        .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam().add(new ca.uhn.fhir.rest.param.ReferenceParam(
		                "Practitioner", "provider-123")));
		
		org.openmrs.module.fhir2.api.search.param.SearchParameterMap params = new org.openmrs.module.fhir2.api.search.param.SearchParameterMap();
		params.addParameter("participant", participantRef);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldHandleMultipleParametersInSingleQuery() {
		Criteria criteria = mock(Criteria.class);
		
		ca.uhn.fhir.rest.param.ReferenceAndListParam patientRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		        .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam().add(new ca.uhn.fhir.rest.param.ReferenceParam(
		                "Patient", "pat-123")));
		ca.uhn.fhir.rest.param.TokenAndListParam code = new ca.uhn.fhir.rest.param.TokenAndListParam()
		        .addAnd(new ca.uhn.fhir.rest.param.TokenOrListParam().add("order-code"));
		ca.uhn.fhir.rest.param.ReferenceAndListParam locationRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		        .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam().add(new ca.uhn.fhir.rest.param.ReferenceParam(
		                "Location", "loc-123")));
		
		org.openmrs.module.fhir2.api.search.param.SearchParameterMap params = new org.openmrs.module.fhir2.api.search.param.SearchParameterMap();
		params.addParameter("subject", patientRef);
		params.addParameter("code", code);
		params.addParameter("order-location", locationRef);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldIgnoreNullCodeParameter() {
		Criteria criteria = mock(Criteria.class);
		org.openmrs.module.fhir2.api.search.param.SearchParameterMap params = new org.openmrs.module.fhir2.api.search.param.SearchParameterMap();
		params.addParameter("code", null);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldIgnoreNullDateRange() {
		Criteria criteria = mock(Criteria.class);
		org.openmrs.module.fhir2.api.search.param.SearchParameterMap params = new org.openmrs.module.fhir2.api.search.param.SearchParameterMap();
		params.addParameter(org.openmrs.module.fhir2.FhirConstants.DATE_RANGE_SEARCH_HANDLER, null);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldIgnoreNullCategoryReference() {
		Criteria criteria = mock(Criteria.class);
		org.openmrs.module.fhir2.api.search.param.SearchParameterMap params = new org.openmrs.module.fhir2.api.search.param.SearchParameterMap();
		params.addParameter(org.openmrs.module.fhir2.FhirConstants.CATEGORY_SEARCH_HANDLER, null);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldIgnoreNullBasedOnReference() {
		Criteria criteria = mock(Criteria.class);
		org.openmrs.module.fhir2.api.search.param.SearchParameterMap params = new org.openmrs.module.fhir2.api.search.param.SearchParameterMap();
		params.addParameter(org.openmrs.module.fhir2.FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, null);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldIgnoreNullLocationReference() {
		Criteria criteria = mock(Criteria.class);
		org.openmrs.module.fhir2.api.search.param.SearchParameterMap params = new org.openmrs.module.fhir2.api.search.param.SearchParameterMap();
		params.addParameter(BahmniFhirConstants.ORDER_LOCATION_SEARCH_HANDLER, null);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void get_shouldReturnOrderForNonDrugOrder() {
		org.mockito.Mockito.when(sessionFactory.getCurrentSession()).thenReturn(session);
		javax.persistence.criteria.CriteriaBuilder cb = mock(javax.persistence.criteria.CriteriaBuilder.class);
		org.mockito.Mockito.when(session.getCriteriaBuilder()).thenReturn(cb);
		javax.persistence.criteria.CriteriaQuery<Order> cq = mock(javax.persistence.criteria.CriteriaQuery.class);
		org.mockito.Mockito.when(cb.createQuery(Order.class)).thenReturn(cq);
		javax.persistence.criteria.Root<Order> root = mock(javax.persistence.criteria.Root.class);
		org.mockito.Mockito.when(cq.from(Order.class)).thenReturn(root);
		org.mockito.Mockito.when(cq.select(root)).thenReturn(cq);
		org.mockito.Mockito.when(root.join("orderType")).thenReturn(mock(javax.persistence.criteria.Join.class));
		org.hibernate.query.Query<Order> query = mock(org.hibernate.query.Query.class);
		org.mockito.Mockito.when(session.createQuery(cq)).thenReturn(query);
		Order order = new Order();
		order.setUuid(ORDER_UUID);
		org.mockito.Mockito.when(query.uniqueResult()).thenReturn(order);
		
		Order result = serviceRequestDao.get(ORDER_UUID);
		
		assertThat(result, notNullValue());
		assertThat(result.getUuid(), is(ORDER_UUID));
	}
	
	@Test
	public void get_shouldReturnNullWhenOrderNotFound() {
		org.mockito.Mockito.when(sessionFactory.getCurrentSession()).thenReturn(session);
		javax.persistence.criteria.CriteriaBuilder cb = mock(javax.persistence.criteria.CriteriaBuilder.class);
		org.mockito.Mockito.when(session.getCriteriaBuilder()).thenReturn(cb);
		javax.persistence.criteria.CriteriaQuery<Order> cq = mock(javax.persistence.criteria.CriteriaQuery.class);
		org.mockito.Mockito.when(cb.createQuery(Order.class)).thenReturn(cq);
		javax.persistence.criteria.Root<Order> root = mock(javax.persistence.criteria.Root.class);
		org.mockito.Mockito.when(cq.from(Order.class)).thenReturn(root);
		org.mockito.Mockito.when(cq.select(root)).thenReturn(cq);
		org.mockito.Mockito.when(root.join("orderType")).thenReturn(mock(javax.persistence.criteria.Join.class));
		org.hibernate.query.Query<Order> query = mock(org.hibernate.query.Query.class);
		org.mockito.Mockito.when(session.createQuery(cq)).thenReturn(query);
		org.mockito.Mockito.when(query.uniqueResult()).thenReturn(null);
		
		assertThat(serviceRequestDao.get(ORDER_UUID), nullValue());
	}
	
	@Test
	public void get_shouldQueryByUuidAndExcludeDrugOrderType() {
		org.mockito.Mockito.when(sessionFactory.getCurrentSession()).thenReturn(session);
		javax.persistence.criteria.CriteriaBuilder cb = mock(javax.persistence.criteria.CriteriaBuilder.class);
		org.mockito.Mockito.when(session.getCriteriaBuilder()).thenReturn(cb);
		javax.persistence.criteria.CriteriaQuery<Order> cq = mock(javax.persistence.criteria.CriteriaQuery.class);
		org.mockito.Mockito.when(cb.createQuery(Order.class)).thenReturn(cq);
		javax.persistence.criteria.Root<Order> root = mock(javax.persistence.criteria.Root.class);
		org.mockito.Mockito.when(cq.from(Order.class)).thenReturn(root);
		org.mockito.Mockito.when(cq.select(root)).thenReturn(cq);
		org.mockito.Mockito.when(root.join("orderType")).thenReturn(mock(javax.persistence.criteria.Join.class));
		org.hibernate.query.Query<Order> query = mock(org.hibernate.query.Query.class);
		org.mockito.Mockito.when(session.createQuery(cq)).thenReturn(query);
		org.mockito.Mockito.when(query.uniqueResult()).thenReturn(new Order());
		
		serviceRequestDao.get(ORDER_UUID);
		
		verify(cq).where(any(), any());
	}
	
	@Test
	public void get_shouldReturnOrdersForUuids() {
		org.mockito.Mockito.when(sessionFactory.getCurrentSession()).thenReturn(session);
		Criteria criteria = mock(Criteria.class);
		org.mockito.Mockito.when(session.createCriteria(Order.class)).thenReturn(criteria);
		Order order = new Order();
		order.setUuid(ORDER_UUID);
		org.mockito.Mockito.when(criteria.list()).thenReturn(Collections.singletonList(order));
		
		List<Order> results = serviceRequestDao.get(Collections.singletonList(ORDER_UUID));
		
		assertThat(results, hasSize(1));
		assertThat(results.get(0).getUuid(), is(ORDER_UUID));
	}
	
	@Test
	public void get_shouldReturnEmptyListWhenNoOrdersFound() {
		org.mockito.Mockito.when(sessionFactory.getCurrentSession()).thenReturn(session);
		Criteria criteria = mock(Criteria.class);
		org.mockito.Mockito.when(session.createCriteria(Order.class)).thenReturn(criteria);
		org.mockito.Mockito.when(criteria.list()).thenReturn(Collections.emptyList());
		
		assertThat(serviceRequestDao.get(Collections.singletonList(ORDER_UUID)), hasSize(0));
	}
	
	@Test
	public void get_shouldExcludeDrugOrdersFromCollectionLookup() {
		org.mockito.Mockito.when(sessionFactory.getCurrentSession()).thenReturn(session);
		Criteria criteria = mock(Criteria.class);
		org.mockito.Mockito.when(session.createCriteria(Order.class)).thenReturn(criteria);
		org.mockito.Mockito.when(criteria.list()).thenReturn(Collections.singletonList(new Order()));
		
		serviceRequestDao.get(Collections.singletonList(ORDER_UUID));
		
		ArgumentCaptor<Criterion> captor = ArgumentCaptor.forClass(Criterion.class);
		verify(criteria, times(3)).add(captor.capture());
		assertThat(captor.getAllValues().stream().anyMatch(c -> c.toString().contains("ot.uuid")), is(true));
	}
	
	@Test
	public void getEncounterReferencesByNumberOfVisit_shouldReturnEncounterReferencesForRecentVisits() {
		org.mockito.Mockito.when(sessionFactory.getCurrentSession()).thenReturn(session);
		Criteria visitCriteria = mock(Criteria.class);
		org.mockito.Mockito.when(session.createCriteria(Visit.class, "v")).thenReturn(visitCriteria);
		org.mockito.Mockito.when(visitCriteria.list()).thenReturn(Arrays.asList(10, 9, 8));
		Criteria encounterCriteria = mock(Criteria.class);
		org.mockito.Mockito.when(session.createCriteria(Encounter.class, "e")).thenReturn(encounterCriteria);
		org.mockito.Mockito.when(encounterCriteria.createAlias("e.visit", "v")).thenReturn(encounterCriteria);
		org.mockito.Mockito.when(encounterCriteria.add(any(org.hibernate.criterion.Criterion.class))).thenReturn(
		    encounterCriteria);
		org.mockito.Mockito.when(encounterCriteria.setProjection(any(org.hibernate.criterion.Projection.class))).thenReturn(
		    encounterCriteria);
		org.mockito.Mockito.when(encounterCriteria.list()).thenReturn(Arrays.asList("enc-1", "enc-2"));
		
		ca.uhn.fhir.rest.param.ReferenceAndListParam result = serviceRequestDao.getEncounterReferencesByNumberOfVisit(
		    new ca.uhn.fhir.rest.param.NumberParam(2), new ca.uhn.fhir.rest.param.ReferenceParam("Patient", "pat-123"));
		
		assertThat(result, notNullValue());
		List<ca.uhn.fhir.rest.param.ReferenceOrListParam> andList = result.getValuesAsQueryTokens();
		assertThat(andList, hasSize(1));
		assertThat(andList.get(0).getValuesAsQueryTokens(), hasSize(2));
		verify(visitCriteria).addOrder(any(org.hibernate.criterion.Order.class));
		verify(visitCriteria).setMaxResults(2);
	}
	
	@Test
	public void getEncounterReferencesByNumberOfVisit_shouldReturnNullWhenNoEncountersFound() {
		org.mockito.Mockito.when(sessionFactory.getCurrentSession()).thenReturn(session);
		Criteria visitCriteria = mock(Criteria.class);
		org.mockito.Mockito.when(session.createCriteria(Visit.class, "v")).thenReturn(visitCriteria);
		org.mockito.Mockito.when(visitCriteria.list()).thenReturn(Arrays.asList(10, 9));
		Criteria encounterCriteria = mock(Criteria.class);
		org.mockito.Mockito.when(session.createCriteria(Encounter.class, "e")).thenReturn(encounterCriteria);
		org.mockito.Mockito.when(encounterCriteria.createAlias("e.visit", "v")).thenReturn(encounterCriteria);
		org.mockito.Mockito.when(encounterCriteria.add(any(org.hibernate.criterion.Criterion.class))).thenReturn(
		    encounterCriteria);
		org.mockito.Mockito.when(encounterCriteria.setProjection(any(org.hibernate.criterion.Projection.class))).thenReturn(
		    encounterCriteria);
		org.mockito.Mockito.when(encounterCriteria.list()).thenReturn(Collections.emptyList());
		
		ca.uhn.fhir.rest.param.ReferenceAndListParam result = serviceRequestDao.getEncounterReferencesByNumberOfVisit(
		    new ca.uhn.fhir.rest.param.NumberParam(2), new ca.uhn.fhir.rest.param.ReferenceParam("Patient", "pat-123"));
		
		assertThat(result, nullValue());
	}
	
	@Test
	public void setupSearchParams_shouldAddPatientRestriction() {
		Criteria criteria = mock(Criteria.class);
		ca.uhn.fhir.rest.param.ReferenceAndListParam patientRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		        .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam().add(new ca.uhn.fhir.rest.param.ReferenceParam(
		                "Patient", "pat-123")));
		org.openmrs.module.fhir2.api.search.param.SearchParameterMap params = new org.openmrs.module.fhir2.api.search.param.SearchParameterMap();
		params.addParameter(org.openmrs.module.fhir2.FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, patientRef);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		verify(criteria).createAlias("patient", "p");
		verify(criteria, times(2)).add(any(Criterion.class));
	}
	
	@Test
	public void setupSearchParams_shouldAddEncounterRestriction() {
		Criteria criteria = mock(Criteria.class);
		ca.uhn.fhir.rest.param.ReferenceAndListParam encounterRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		        .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam().add(new ca.uhn.fhir.rest.param.ReferenceParam(
		                "Encounter", "enc-123")));
		org.openmrs.module.fhir2.api.search.param.SearchParameterMap params = new org.openmrs.module.fhir2.api.search.param.SearchParameterMap();
		params.addParameter(org.openmrs.module.fhir2.FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, encounterRef);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		verify(criteria, times(2)).add(any(Criterion.class));
	}
	
	@Test
	public void setupSearchParams_shouldAddBasedOnRestriction() {
		Criteria criteria = mock(Criteria.class);
		ca.uhn.fhir.rest.param.ReferenceAndListParam basedOnRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		        .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam().add(new ca.uhn.fhir.rest.param.ReferenceParam(
		                "Order", "prev-order")));
		org.openmrs.module.fhir2.api.search.param.SearchParameterMap params = new org.openmrs.module.fhir2.api.search.param.SearchParameterMap();
		params.addParameter(org.openmrs.module.fhir2.FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnRef);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		ArgumentCaptor<Criterion> captor = ArgumentCaptor.forClass(Criterion.class);
		verify(criteria, times(2)).add(captor.capture());
		assertThat(captor.getAllValues().stream().anyMatch(c -> c.toString().contains("po.uuid")), is(true));
	}
	
	@Test
	public void setupSearchParams_shouldAddLocationRestriction() {
		Criteria criteria = mock(Criteria.class);
		ca.uhn.fhir.rest.param.ReferenceAndListParam locationRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		        .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam().add(new ca.uhn.fhir.rest.param.ReferenceParam(
		                "Location", "loc-123")));
		org.openmrs.module.fhir2.api.search.param.SearchParameterMap params = new org.openmrs.module.fhir2.api.search.param.SearchParameterMap();
		params.addParameter(BahmniFhirConstants.ORDER_LOCATION_SEARCH_HANDLER, locationRef);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		ArgumentCaptor<Criterion> captor = ArgumentCaptor.forClass(Criterion.class);
		verify(criteria, times(2)).add(captor.capture());
		assertThat(captor.getAllValues().stream().anyMatch(c -> c.toString().contains("l.uuid")), is(true));
	}
	
	@Test
	public void setupSearchParams_shouldAddParticipantRestriction() {
		Criteria criteria = mock(Criteria.class);
		ca.uhn.fhir.rest.param.ReferenceAndListParam participantRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		        .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam().add(new ca.uhn.fhir.rest.param.ReferenceParam(
		                "Practitioner", "provider-123")));
		org.openmrs.module.fhir2.api.search.param.SearchParameterMap params = new org.openmrs.module.fhir2.api.search.param.SearchParameterMap();
		params.addParameter(org.openmrs.module.fhir2.FhirConstants.PARTICIPANT_REFERENCE_SEARCH_HANDLER, participantRef);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		verify(criteria, times(2)).add(any(Criterion.class));
	}
	
	@Test
	public void setupSearchParams_shouldHandleDateRangeWithBounds() {
		Criteria criteria = mock(Criteria.class);
		ca.uhn.fhir.rest.param.DateRangeParam dateRange = new ca.uhn.fhir.rest.param.DateRangeParam(
		        new ca.uhn.fhir.rest.param.DateParam("ge2020-01-01"), new ca.uhn.fhir.rest.param.DateParam("le2020-12-31"));
		org.openmrs.module.fhir2.api.search.param.SearchParameterMap params = new org.openmrs.module.fhir2.api.search.param.SearchParameterMap();
		params.addParameter(org.openmrs.module.fhir2.FhirConstants.DATE_RANGE_SEARCH_HANDLER, dateRange);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		ArgumentCaptor<Criterion> captor = ArgumentCaptor.forClass(Criterion.class);
		verify(criteria, times(2)).add(captor.capture());
		assertThat(captor.getAllValues().stream().anyMatch(c -> c.toString().contains("scheduledDate")), is(true));
	}
	
	@Test
	public void setupSearchParams_shouldResolveCategoryToOrderTypeUuid() {
		Criteria criteria = mock(Criteria.class);
		Map<String, String> categoryMap = new HashMap<>();
		categoryMap.put("Lab Order", "lab");
		org.mockito.Mockito.when(appContext.getOrderTypeToCategoryMap()).thenReturn(categoryMap);
		OrderType labOrderType = new OrderType();
		labOrderType.setUuid("lab-order-type-uuid");
		org.mockito.Mockito.when(orderService.getOrderTypeByName("Lab Order")).thenReturn(labOrderType);
		ca.uhn.fhir.rest.param.ReferenceAndListParam categoryRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		        .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam().add(new ca.uhn.fhir.rest.param.ReferenceParam(
		                "OrderType", "lab")));
		org.openmrs.module.fhir2.api.search.param.SearchParameterMap params = new org.openmrs.module.fhir2.api.search.param.SearchParameterMap();
		params.addParameter(org.openmrs.module.fhir2.FhirConstants.CATEGORY_SEARCH_HANDLER, categoryRef);
		
serviceRequestDao.setupSearchParams(criteria, params);
		
		verify(appContext).getOrderTypeToCategoryMap();
		verify(orderService).getOrderTypeByName("Lab Order");
		verify(criteria, times(2)).add(any(Criterion.class));
	}
	
	@Test
	public void setupSearchParams_shouldFallbackToCategoryValueWhenOrderTypeMappingMissing() {
		Criteria criteria = mock(Criteria.class);
		org.mockito.Mockito.when(appContext.getOrderTypeToCategoryMap()).thenReturn(Collections.emptyMap());
		ca.uhn.fhir.rest.param.ReferenceAndListParam categoryRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		        .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam().add(new ca.uhn.fhir.rest.param.ReferenceParam(
		                "OrderType", "lab")));
		org.openmrs.module.fhir2.api.search.param.SearchParameterMap params = new org.openmrs.module.fhir2.api.search.param.SearchParameterMap();
		params.addParameter(org.openmrs.module.fhir2.FhirConstants.CATEGORY_SEARCH_HANDLER, categoryRef);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		verify(appContext).getOrderTypeToCategoryMap();
		verify(orderService, never()).getOrderTypeByName(any());
		verify(criteria, times(2)).add(any(Criterion.class));
	}
}
