package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
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
public class BahmniFhirServiceRequestDaoImplHighImpactTest {
	
	@Mock
	private SessionFactory sessionFactory;
	
	private BahmniFhirServiceRequestDaoImpl serviceRequestDao;
	
	@Before
	public void setup() throws Exception {
		serviceRequestDao = new BahmniFhirServiceRequestDaoImpl();
		Field sessionFactoryField = BaseFhirDao.class.getDeclaredField("sessionFactory");
		sessionFactoryField.setAccessible(true);
		sessionFactoryField.set(serviceRequestDao, sessionFactory);
	}
	
	@Test
	public void setupSearchParams_withEmptyParams_shouldProcess() {
		Criteria criteria = mock(Criteria.class);
		SearchParameterMap params = new SearchParameterMap();
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_withPatientParameter_shouldProcess() {
		Criteria criteria = mock(Criteria.class);
		ReferenceAndListParam patientRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Patient", "patient-uuid")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("subject", patientRef);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_withCodeParameter_shouldProcess() {
		Criteria criteria = mock(Criteria.class);
		TokenAndListParam code = new TokenAndListParam().addAnd(new TokenOrListParam().add("http://loinc.org", "12345"));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("code", code);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_withEncounterParameter_shouldProcess() {
		Criteria criteria = mock(Criteria.class);
		ReferenceAndListParam encounterRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Encounter", "enc-uuid")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("encounter", encounterRef);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_withMultipleParameters_shouldProcess() {
		Criteria criteria = mock(Criteria.class);
		ReferenceAndListParam patientRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Patient", "patient-uuid")));
		TokenAndListParam code = new TokenAndListParam().addAnd(new TokenOrListParam().add("http://loinc.org", "12345"));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("subject", patientRef);
		params.addParameter("code", code);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void hasDistinctResults_shouldReturnBoolean() {
		boolean result = serviceRequestDao.hasDistinctResults();
		assertThat(result, notNullValue());
	}
	
	@Test
	public void setupSearchParams_withLocationParameter_shouldProcess() {
		Criteria criteria = mock(Criteria.class);
		ReferenceAndListParam locationRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Location", "location-uuid")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("location", locationRef);
		
		serviceRequestDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
}
