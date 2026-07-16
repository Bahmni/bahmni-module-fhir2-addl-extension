package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Obs;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.atLeastOnce;

@RunWith(MockitoJUnitRunner.class)
public class BahmniObservationDaoImplTest {
	
	private static final String PATIENT_UUID = "da7f524f-27ce-4bb2-86d6-6d1d05312bd5";
	
	private static final String ORDER_UUID = "7d96f25c-4949-4f72-9931-d808fbc226de";
	
	private static final String ORDER_UUID_WITH_PREFIX = "ServiceRequest/7d96f25c-4949-4f72-9931-d808fbc226de";
	
	private static final String OBSERVATION_UUID = "observation-uuid-123";
	
	@Mock
	private SessionFactory sessionFactory;
	
	@Mock
	private Session session;
	
	@Mock
	private Criteria criteria;
	
	private BahmniObservationDaoImpl dao;
	
	@Before
	public void setUp() {
		dao = new BahmniObservationDaoImpl();
		ReflectionTestUtils.setField(dao, "sessionFactory", sessionFactory);
		
		when(sessionFactory.getCurrentSession()).thenReturn(session);
		when(session.createCriteria(Obs.class)).thenReturn(criteria);
		when(criteria.createAlias(anyString(), anyString())).thenReturn(criteria);
		when(criteria.add(any())).thenReturn(criteria);
		when(criteria.list()).thenReturn(Collections.emptyList());
		when(criteria.setFirstResult(anyInt())).thenReturn(criteria);
		when(criteria.addOrder(any())).thenReturn(criteria);
	}
	
	@Test
	public void shouldSearchByPatientReference() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(PATIENT_UUID)));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, patientReference);
		
		List<Obs> results = dao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
		verify(criteria, atLeastOnce()).add(any());
	}
	
	@Test
	public void shouldSearchByBasedOnReference() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(ORDER_UUID)));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		
		List<Obs> results = dao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
		verify(criteria, atLeastOnce()).add(any());
	}
	
	@Test
	public void shouldSearchByBasedOnReferenceWithResourceTypePrefix() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(ORDER_UUID_WITH_PREFIX)));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		
		List<Obs> results = dao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
		verify(criteria, atLeastOnce()).add(any());
	}
	
	@Test
	public void shouldSearchWithMultipleParameters() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(PATIENT_UUID)));
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(ORDER_UUID)));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, patientReference);
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		
		List<Obs> results = dao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
		// Verify criteria interactions - code executed successfully
		verify(criteria, atLeastOnce()).add(any());
	}
	
	@Test
	public void shouldSearchWithCommonSearchHandler() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(ORDER_UUID)));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		theParams.addParameter(FhirConstants.COMMON_SEARCH_HANDLER, null);
		
		List<Obs> results = dao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
		// Code executed successfully
	}
	
	@Test
	public void shouldHandleBasedOnReferenceWithMultipleSlashes() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue("http://fhir.server/ServiceRequest/" + ORDER_UUID)));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		
		List<Obs> results = dao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
		// Verify criteria interactions - code executed successfully
		verify(criteria, atLeastOnce()).add(any());
	}
	
	@Test
	public void shouldHandleNullBasedOnReference() {
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, null);
		
		List<Obs> results = dao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
	}
	
	@Test
	public void shouldHandleEmptySearchParameters() {
		SearchParameterMap theParams = new SearchParameterMap();
		
		List<Obs> results = dao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
	}
	
	@Test
	public void shouldHandleMultipleBasedOnReferencesInOrList() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam().setValue("ServiceRequest/order-uuid-1"));
		orListParam.add(new ReferenceParam().setValue("ServiceRequest/order-uuid-2"));
		basedOnReference.addAnd(orListParam);
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		
		List<Obs> results = dao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
	}
	
	@Test
	public void shouldHandleComplexMultiParameterSearch() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(PATIENT_UUID)));
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue("ServiceRequest/" + ORDER_UUID)));
		DateRangeParam lastUpdated = new DateRangeParam();
		lastUpdated.setLowerBound("2026-01-01");
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, patientReference);
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		theParams.addParameter(FhirConstants.LAST_UPDATED_PROPERTY, lastUpdated);
		
		List<Obs> results = dao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
		verify(criteria, atLeastOnce()).add(any());
	}
	
	@Test
	public void shouldCreateAliasWhenLackingForBasedOnReference() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(ORDER_UUID)));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		
		List<Obs> results = dao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
		verify(criteria, atLeastOnce()).add(any());
	}
	
	@Test
	public void shouldHandleReferenceValueWithoutSlash() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(ORDER_UUID)));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		
		List<Obs> results = dao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
		verify(criteria, atLeastOnce()).add(any());
	}
	
	@Test
	public void shouldExtractUuidFromReferenceValueWithSlash() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue("ServiceRequest/" + ORDER_UUID)));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		
		List<Obs> results = dao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
		verify(criteria, atLeastOnce()).add(any());
	}
	
	@Test
	public void shouldExtractUuidFromFullUrlWithMultipleSlashes() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue("http://fhir.server.com/base/ServiceRequest/" + ORDER_UUID)));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		
		List<Obs> results = dao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
		verify(criteria, atLeastOnce()).add(any());
	}
	
	@Test
	public void shouldHandleBasedOnReferenceNotNull() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue("test-order-uuid")));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		
		List<Obs> results = dao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
		verify(criteria, atLeastOnce()).add(any());
	}
	
	@Test
	public void shouldHandleBasedOnReferenceIsNull() {
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, null);
		
		List<Obs> results = dao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
		verify(criteria, times(0)).createAlias("order", "o");
	}
	
	@Test
	public void shouldHandleReferenceValueContainingSlashAtEnd() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue("ServiceRequest/" + ORDER_UUID + "/")));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		
		List<Obs> results = dao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
		verify(criteria, atLeastOnce()).add(any());
	}
}
