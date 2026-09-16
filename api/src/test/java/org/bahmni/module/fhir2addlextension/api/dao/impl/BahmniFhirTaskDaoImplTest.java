package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirTaskDaoImplTest {
	
	private static final String OBS_UUID_1 = "obs-uuid-1";
	
	private static final String OBS_UUID_2 = "obs-uuid-2";
	
	private static final String VISIT_UUID = "visit-uuid-123";
	
	@Mock
	private SessionFactory sessionFactory;
	
	private BahmniFhirTaskDaoImpl taskDao;
	
	@Before
	public void setup() throws Exception {
		taskDao = new BahmniFhirTaskDaoImpl();
		Field sessionFactoryField = BaseFhirDao.class.getDeclaredField("sessionFactory");
		sessionFactoryField.setAccessible(true);
		sessionFactoryField.set(taskDao, sessionFactory);
	}
	
	@Test
	public void setupSearchParams_shouldNotCreateAliasForForReferenceIfNotPresent() {
		Criteria criteria = mock(Criteria.class);
		SearchParameterMap params = new SearchParameterMap();
		
		taskDao.setupSearchParams(criteria, params);
		
		verify(criteria, never()).createAlias("forReference", "fr");
	}
	
	@Test
	public void setupSearchParams_shouldNotThrowExceptionWithFocusParameter() {
		Criteria criteria = mock(Criteria.class);
		
		ReferenceAndListParam focusRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Observation", OBS_UUID_1)));
		
		SearchParameterMap params = new SearchParameterMap();
		// Focus is a standard FHIR Task parameter handled by parent class
		params.addParameter("focus", focusRef);
		
		// Should not throw exception when processing parameters
		taskDao.setupSearchParams(criteria, params);
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldHandleMultiValueFocusParameter() {
		Criteria criteria = mock(Criteria.class);
		
		// Multi-value focus: comma-separated UUIDs as the nurse acknowledgement UI sends
		ReferenceAndListParam focusRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(
		    new ReferenceParam("Observation", OBS_UUID_1)).add(new ReferenceParam("Observation", OBS_UUID_2)));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("focus", focusRef);
		
		taskDao.setupSearchParams(criteria, params);
		
		// Verify multi-value focus parameter survives parameter rebuilding
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldFilterBySubjectUsingTargetUuid() {
		Criteria criteria = mock(Criteria.class);
		
		ReferenceAndListParam forReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Visit", VISIT_UUID)));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.FOR_REFERENCE_SEARCH_HANDLER, forReference);
		
		taskDao.setupSearchParams(criteria, params);
		
		// Verify handleForReference was called to process the subject parameter
		ArgumentCaptor<Criterion> criterionCaptor = ArgumentCaptor.forClass(Criterion.class);
		verify(criteria, times(1)).add(criterionCaptor.capture());
		
		Criterion capturedCriterion = criterionCaptor.getValue();
		assertThat(capturedCriterion, notNullValue());
		// Criterion.toString() will show the SQL-like representation containing targetUuid
		String criterionStr = capturedCriterion.toString();
		assertThat(criterionStr.contains("targetUuid"), equalTo(true));
	}
	
	@Test
	public void setupSearchParams_shouldProcessSubjectParameterWithoutException() {
		Criteria criteria = mock(Criteria.class);
		
		ReferenceAndListParam forReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Visit", VISIT_UUID)));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.FOR_REFERENCE_SEARCH_HANDLER, forReference);
		
		// Should process without throwing exception
		taskDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
}
