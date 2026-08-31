package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.hibernate.criterion.Criterion;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirTaskDaoImplTest {
	
	private static final String ORDER_UUID = "order-uuid-123";
	
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
	public void setupSearchParams_shouldNotCreateAliasForReferenceIfNotPresent() {
		Criteria criteria = org.mockito.Mockito.mock(Criteria.class);
		SearchParameterMap params = new SearchParameterMap();
		
		taskDao.setupSearchParams(criteria, params);
		
		verify(criteria, times(0)).createAlias(org.mockito.ArgumentMatchers.eq("forReference"),
		    org.mockito.ArgumentMatchers.eq("fr"));
	}
	
	@Test
	public void setupSearchParams_shouldNotCreateAliasForReferenceIfAlreadyPresent() {
		Criteria criteria = org.mockito.Mockito.mock(Criteria.class);
		SearchParameterMap params = new SearchParameterMap();
		
		taskDao.setupSearchParams(criteria, params);
		
		verify(criteria, never()).createAlias(org.mockito.ArgumentMatchers.eq("forReference"),
		    org.mockito.ArgumentMatchers.eq("fr"));
	}
	
	@Test
	public void setupSearchParams_shouldNotCreateAliasForForReferenceIfNotRequired() {
		Criteria criteria = org.mockito.Mockito.mock(Criteria.class);
		SearchParameterMap params = new SearchParameterMap();
		
		taskDao.setupSearchParams(criteria, params);
		
		verify(criteria, never()).createAlias(org.mockito.ArgumentMatchers.eq("forReference"),
		    org.mockito.ArgumentMatchers.eq("fr"));
	}
	
}
