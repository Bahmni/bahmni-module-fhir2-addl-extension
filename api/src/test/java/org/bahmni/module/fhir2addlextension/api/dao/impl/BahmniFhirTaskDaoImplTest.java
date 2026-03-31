package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.hibernate.criterion.Criterion;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirServiceRequestDao;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Order;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;
import org.openmrs.module.fhir2.model.FhirReference;
import org.openmrs.module.fhir2.model.FhirTask;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirTaskDaoImplTest {
	
	private static final String ORDER_UUID = "order-uuid-123";
	
	@Mock
	private BahmniFhirServiceRequestDao<Order> serviceRequestDao;
	
	@Mock
	private SessionFactory sessionFactory;
	
	@Mock
	private Session session;
	
	private BahmniFhirTaskDaoImpl taskDao;
	
	@Before
	public void setup() throws Exception {
		taskDao = new BahmniFhirTaskDaoImpl(serviceRequestDao);
		Field sessionFactoryField = BaseFhirDao.class.getDeclaredField("sessionFactory");
		sessionFactoryField.setAccessible(true);
		sessionFactoryField.set(taskDao, sessionFactory);
		when(sessionFactory.getCurrentSession()).thenReturn(session);
	}
	
	@Test
	public void createOrUpdate_shouldSaveTaskAndUpdateFulfillerStatus() {
		FhirTask fhirTask = createFhirTaskWithBasedOn(FhirTask.TaskStatus.ACCEPTED, ORDER_UUID);
		Order order = new Order();
		order.setUuid(ORDER_UUID);
		
		when(serviceRequestDao.get(ORDER_UUID)).thenReturn(order);
		
		FhirTask result = taskDao.createOrUpdate(fhirTask);
		
		assertThat(result, notNullValue());
		verify(session).saveOrUpdate(fhirTask);
		
		ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
		verify(serviceRequestDao).updateOrder(orderCaptor.capture());
		assertThat(orderCaptor.getValue().getFulfillerStatus(), equalTo(Order.FulfillerStatus.IN_PROGRESS));
	}
	
	@Test
	public void createOrUpdate_shouldMapRequestedStatusToReceived() {
		FhirTask fhirTask = createFhirTaskWithBasedOn(FhirTask.TaskStatus.REQUESTED, ORDER_UUID);
		Order order = new Order();
		order.setUuid(ORDER_UUID);
		
		when(serviceRequestDao.get(ORDER_UUID)).thenReturn(order);
		
		taskDao.createOrUpdate(fhirTask);
		
		ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
		verify(serviceRequestDao).updateOrder(orderCaptor.capture());
		assertThat(orderCaptor.getValue().getFulfillerStatus(), equalTo(Order.FulfillerStatus.RECEIVED));
	}
	
	@Test
	public void createOrUpdate_shouldMapAcceptedStatusToInProgress() {
		FhirTask fhirTask = createFhirTaskWithBasedOn(FhirTask.TaskStatus.ACCEPTED, ORDER_UUID);
		Order order = new Order();
		order.setUuid(ORDER_UUID);
		
		when(serviceRequestDao.get(ORDER_UUID)).thenReturn(order);
		
		taskDao.createOrUpdate(fhirTask);
		
		ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
		verify(serviceRequestDao).updateOrder(orderCaptor.capture());
		assertThat(orderCaptor.getValue().getFulfillerStatus(), equalTo(Order.FulfillerStatus.IN_PROGRESS));
	}
	
	@Test
	public void createOrUpdate_shouldMapCompletedStatusToCompleted() {
		FhirTask fhirTask = createFhirTaskWithBasedOn(FhirTask.TaskStatus.COMPLETED, ORDER_UUID);
		Order order = new Order();
		order.setUuid(ORDER_UUID);
		
		when(serviceRequestDao.get(ORDER_UUID)).thenReturn(order);
		
		taskDao.createOrUpdate(fhirTask);
		
		ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
		verify(serviceRequestDao).updateOrder(orderCaptor.capture());
		assertThat(orderCaptor.getValue().getFulfillerStatus(), equalTo(Order.FulfillerStatus.COMPLETED));
	}
	
	@Test
	public void createOrUpdate_shouldMapRejectedStatusToException() {
		FhirTask fhirTask = createFhirTaskWithBasedOn(FhirTask.TaskStatus.REJECTED, ORDER_UUID);
		Order order = new Order();
		order.setUuid(ORDER_UUID);
		
		when(serviceRequestDao.get(ORDER_UUID)).thenReturn(order);
		
		taskDao.createOrUpdate(fhirTask);
		
		ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
		verify(serviceRequestDao).updateOrder(orderCaptor.capture());
		assertThat(orderCaptor.getValue().getFulfillerStatus(), equalTo(Order.FulfillerStatus.EXCEPTION));
	}
	
	@Test
	public void createOrUpdate_shouldNotUpdateFulfillerStatusForUnmappedStatus() {
		FhirTask fhirTask = createFhirTaskWithBasedOn(FhirTask.TaskStatus.UNKNOWN, ORDER_UUID);
		
		taskDao.createOrUpdate(fhirTask);
		
		verify(serviceRequestDao, never()).updateOrder(any());
	}
	
	@Test
	public void createOrUpdate_shouldNotUpdateFulfillerStatusWhenNoBasedOnReference() {
		FhirTask fhirTask = new FhirTask();
		fhirTask.setStatus(FhirTask.TaskStatus.ACCEPTED);
		
		taskDao.createOrUpdate(fhirTask);
		
		verify(serviceRequestDao, never()).updateOrder(any());
	}
	
	@Test
	public void createOrUpdate_shouldNotUpdateFulfillerStatusWhenBasedOnIsNotServiceRequest() {
		FhirTask fhirTask = new FhirTask();
		fhirTask.setStatus(FhirTask.TaskStatus.ACCEPTED);
		FhirReference ref = new FhirReference();
		ref.setType("MedicationRequest");
		ref.setTargetUuid("some-uuid");
		fhirTask.setBasedOnReferences(Collections.singleton(ref));
		
		taskDao.createOrUpdate(fhirTask);
		
		verify(serviceRequestDao, never()).updateOrder(any());
	}
	
	@Test
	public void createOrUpdate_shouldNotUpdateFulfillerStatusWhenOrderNotFound() {
		FhirTask fhirTask = createFhirTaskWithBasedOn(FhirTask.TaskStatus.ACCEPTED, ORDER_UUID);
		
		when(serviceRequestDao.get(ORDER_UUID)).thenReturn(null);
		
		taskDao.createOrUpdate(fhirTask);
		
		verify(serviceRequestDao, never()).updateOrder(any());
	}
	
	@Test
	public void createOrUpdate_shouldSetFulfillerCommentFromTaskComment() {
		FhirTask fhirTask = createFhirTaskWithBasedOnAndComment(FhirTask.TaskStatus.ACCEPTED, ORDER_UUID,
		    "Patient needs follow-up");
		Order order = new Order();
		order.setUuid(ORDER_UUID);
		
		when(serviceRequestDao.get(ORDER_UUID)).thenReturn(order);
		
		taskDao.createOrUpdate(fhirTask);
		
		ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
		verify(serviceRequestDao).updateOrder(orderCaptor.capture());
		assertThat(orderCaptor.getValue().getFulfillerStatus(), equalTo(Order.FulfillerStatus.IN_PROGRESS));
		assertThat(orderCaptor.getValue().getFulfillerComment(), equalTo("Patient needs follow-up"));
	}
	
	@Test
	public void createOrUpdate_shouldNotSetFulfillerCommentWhenTaskHasNoComment() {
		FhirTask fhirTask = createFhirTaskWithBasedOn(FhirTask.TaskStatus.ACCEPTED, ORDER_UUID);
		Order order = new Order();
		order.setUuid(ORDER_UUID);
		
		when(serviceRequestDao.get(ORDER_UUID)).thenReturn(order);
		
		taskDao.createOrUpdate(fhirTask);
		
		ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
		verify(serviceRequestDao).updateOrder(orderCaptor.capture());
		assertThat(orderCaptor.getValue().getFulfillerComment(), equalTo(null));
	}
	
	@Test
	public void createOrUpdate_shouldNotSetFulfillerCommentWhenTaskCommentIsBlank() {
		FhirTask fhirTask = createFhirTaskWithBasedOnAndComment(FhirTask.TaskStatus.ACCEPTED, ORDER_UUID, "   ");
		Order order = new Order();
		order.setUuid(ORDER_UUID);
		
		when(serviceRequestDao.get(ORDER_UUID)).thenReturn(order);
		
		taskDao.createOrUpdate(fhirTask);
		
		ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
		verify(serviceRequestDao).updateOrder(orderCaptor.capture());
		assertThat(orderCaptor.getValue().getFulfillerComment(), equalTo(null));
	}
	
	@Test
	public void createOrUpdate_shouldSaveOwnerReferenceBeforeTask() {
		FhirTask fhirTask = createFhirTaskWithBasedOn(FhirTask.TaskStatus.ACCEPTED, ORDER_UUID);
		FhirReference ownerRef = new FhirReference();
		ownerRef.setReference("Practitioner/practitioner-uuid");
		fhirTask.setOwnerReference(ownerRef);
		Order order = new Order();
		order.setUuid(ORDER_UUID);
		
		when(serviceRequestDao.get(ORDER_UUID)).thenReturn(order);
		
		taskDao.createOrUpdate(fhirTask);
		
		verify(session).saveOrUpdate(ownerRef);
		verify(session).saveOrUpdate(fhirTask);
	}
	
	@Test
	public void createOrUpdate_shouldNotUpdateFulfillerStatusWhenUpdatingExistingTask() {
		FhirTask fhirTask = createFhirTaskWithBasedOn(FhirTask.TaskStatus.ACCEPTED, ORDER_UUID);
		fhirTask.setId(42);
		
		taskDao.createOrUpdate(fhirTask);
		
		verify(session).saveOrUpdate(fhirTask);
		verify(serviceRequestDao, never()).get(ORDER_UUID);
		verify(serviceRequestDao, never()).updateOrder(any());
	}
	
	private FhirTask createFhirTaskWithBasedOn(FhirTask.TaskStatus status, String orderUuid) {
		FhirTask fhirTask = new FhirTask();
		fhirTask.setStatus(status);
		FhirReference ref = new FhirReference();
		ref.setType("ServiceRequest");
		ref.setReference("ServiceRequest/" + orderUuid);
		ref.setTargetUuid(orderUuid);
		fhirTask.setBasedOnReferences(Collections.singleton(ref));
		return fhirTask;
	}
	
	private FhirTask createFhirTaskWithBasedOnAndComment(FhirTask.TaskStatus status, String orderUuid, String comment) {
		FhirTask fhirTask = createFhirTaskWithBasedOn(status, orderUuid);
		fhirTask.setComment(comment);
		return fhirTask;
	}
	
	@Test
	public void setupSearchParams_shouldFilterByPatientUuidUsingTargetUuid() {
		Criteria criteria = org.mockito.Mockito.mock(Criteria.class);
		when(criteria.add(any(Criterion.class))).thenReturn(criteria);
		
		String patientUuid = "623a646e-4df0-44d6-85d1-f77c87bc1e2e";
		ReferenceAndListParam forReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam(patientUuid)));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.FOR_REFERENCE_SEARCH_HANDLER, forReference);
		
		taskDao.setupSearchParams(criteria, params);
		
		verify(criteria, times(1)).add(any(Criterion.class));
	}
	
	@Test
	public void setupSearchParams_shouldNotAddForReferenceAliasWhenParamAbsent() {
		Criteria criteria = org.mockito.Mockito.mock(Criteria.class);
		
		SearchParameterMap params = new SearchParameterMap();
		
		taskDao.setupSearchParams(criteria, params);
		
		verify(criteria, never()).createAlias(org.mockito.ArgumentMatchers.eq("forReference"),
		    org.mockito.ArgumentMatchers.eq("fr"));
	}
	
	@Test
	public void createOrUpdate_shouldNotUpdateFulfillerStatusWhenBasedOnIsEncounterType() {
		FhirTask fhirTask = new FhirTask();
		fhirTask.setStatus(FhirTask.TaskStatus.ACCEPTED);
		FhirReference ref = new FhirReference();
		ref.setType("Encounter");
		ref.setTargetUuid("encounter-uuid-456");
		fhirTask.setBasedOnReferences(Collections.singleton(ref));
		
		taskDao.createOrUpdate(fhirTask);
		
		verify(serviceRequestDao, never()).updateOrder(any());
	}
	
	@Test
	public void getTaskByOrderUuid_shouldReturnTaskWhenFound() {
		FhirTask expectedTask = createFhirTaskWithBasedOn(FhirTask.TaskStatus.ACCEPTED, ORDER_UUID);
		Criteria criteria = org.mockito.Mockito.mock(Criteria.class);
		
		when(session.createCriteria(FhirTask.class)).thenReturn(criteria);
		when(criteria.createAlias("basedOnReferences", "bor")).thenReturn(criteria);
		when(criteria.add(any())).thenReturn(criteria);
		when(criteria.addOrder(any(org.hibernate.criterion.Order.class))).thenReturn(criteria);
		when(criteria.setMaxResults(1)).thenReturn(criteria);
		when(criteria.uniqueResult()).thenReturn(expectedTask);
		
		FhirTask result = taskDao.getTaskByOrderUuid(ORDER_UUID);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(FhirTask.TaskStatus.ACCEPTED));
	}
	
	@Test
	public void getTaskByOrderUuid_shouldReturnNullWhenNoTaskFound() {
		Criteria criteria = org.mockito.Mockito.mock(Criteria.class);
		
		when(session.createCriteria(FhirTask.class)).thenReturn(criteria);
		when(criteria.createAlias("basedOnReferences", "bor")).thenReturn(criteria);
		when(criteria.add(any())).thenReturn(criteria);
		when(criteria.addOrder(any(org.hibernate.criterion.Order.class))).thenReturn(criteria);
		when(criteria.setMaxResults(1)).thenReturn(criteria);
		when(criteria.uniqueResult()).thenReturn(null);
		
		FhirTask result = taskDao.getTaskByOrderUuid(ORDER_UUID);
		
		assertThat(result, nullValue());
	}
}
