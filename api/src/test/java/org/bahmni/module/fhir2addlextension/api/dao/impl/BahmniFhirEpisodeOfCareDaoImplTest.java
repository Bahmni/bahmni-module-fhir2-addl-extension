package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirEpisodeOfCareDaoImplTest {
	
	@Mock
	private SessionFactory sessionFactory;
	
	private BahmniFhirEpisodeOfCareDaoImpl episodeOfCareDao;
	
	@Before
	public void setup() throws Exception {
		episodeOfCareDao = new BahmniFhirEpisodeOfCareDaoImpl();
		Field sessionFactoryField = BaseFhirDao.class.getDeclaredField("sessionFactory");
		sessionFactoryField.setAccessible(true);
		sessionFactoryField.set(episodeOfCareDao, sessionFactory);
	}
	
	@Test
	public void setupSearchParams_shouldNotThrowException() {
		Criteria criteria = mock(Criteria.class);
		SearchParameterMap params = new SearchParameterMap();
		
		episodeOfCareDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_withEmptyParams_shouldReturnValid() {
		Criteria criteria = mock(Criteria.class);
		SearchParameterMap params = new SearchParameterMap();
		
		episodeOfCareDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
}
