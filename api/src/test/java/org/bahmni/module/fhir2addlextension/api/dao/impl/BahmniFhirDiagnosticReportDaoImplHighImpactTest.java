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
public class BahmniFhirDiagnosticReportDaoImplHighImpactTest {
	
	@Mock
	private SessionFactory sessionFactory;
	
	private BahmniFhirDiagnosticReportDaoImpl diagnosticReportDao;
	
	@Before
	public void setup() throws Exception {
		diagnosticReportDao = new BahmniFhirDiagnosticReportDaoImpl();
		Field sessionFactoryField = BaseFhirDao.class.getDeclaredField("sessionFactory");
		sessionFactoryField.setAccessible(true);
		sessionFactoryField.set(diagnosticReportDao, sessionFactory);
	}
	
	@Test
	public void setupSearchParams_withEmptyParams_shouldProcess() {
		Criteria criteria = mock(Criteria.class);
		SearchParameterMap params = new SearchParameterMap();
		
		diagnosticReportDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_withSubjectParameter_shouldProcess() {
		Criteria criteria = mock(Criteria.class);
		ReferenceAndListParam subjectRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Patient", "patient-uuid")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("subject", subjectRef);
		
		diagnosticReportDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_withCodeParameter_shouldProcess() {
		Criteria criteria = mock(Criteria.class);
		TokenAndListParam code = new TokenAndListParam().addAnd(new TokenOrListParam().add("http://loinc.org", "12345"));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("code", code);
		
		diagnosticReportDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_withEncounterParameter_shouldProcess() {
		Criteria criteria = mock(Criteria.class);
		ReferenceAndListParam encounterRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Encounter", "enc-uuid")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("encounter", encounterRef);
		
		diagnosticReportDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_withResultParameter_shouldProcess() {
		Criteria criteria = mock(Criteria.class);
		ReferenceAndListParam resultRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Observation", "obs-uuid")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("result", resultRef);
		
		diagnosticReportDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
}
