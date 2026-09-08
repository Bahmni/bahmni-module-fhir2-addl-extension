package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.hibernate.Criteria;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.PropParam;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirRelatedPersonDaoImplTest {
	
	@Mock
	private Criteria criteria;
	
	private BahmniFhirRelatedPersonDaoImpl dao;
	
	@Before
	public void setUp() {
		dao = new BahmniFhirRelatedPersonDaoImpl();
	}
	
	@Test
	public void handlePatientBothSides_shouldAddSqlRestrictionForValidUuid() throws Exception {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		patientRef.addAnd(new ReferenceOrListParam().add(new ReferenceParam("patient-uuid-123")));
		
		Method method = BahmniFhirRelatedPersonDaoImpl.class.getDeclaredMethod("handlePatientBothSides", Criteria.class,
		    ReferenceAndListParam.class);
		method.setAccessible(true);
		method.invoke(dao, criteria, patientRef);
		
		verify(criteria, times(1)).add(any());
	}
	
	@Test
	public void handlePatientBothSides_shouldSkipTokenWithNullUuid() throws Exception {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		patientRef.addAnd(new ReferenceOrListParam().add(new ReferenceParam((String) null)));
		
		Method method = BahmniFhirRelatedPersonDaoImpl.class.getDeclaredMethod("handlePatientBothSides", Criteria.class,
		    ReferenceAndListParam.class);
		method.setAccessible(true);
		method.invoke(dao, criteria, patientRef);
		
		verifyNoMoreInteractions(criteria);
	}
	
	@Test
	public void handlePatientBothSides_shouldSkipTokenWithEmptyUuid() throws Exception {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		patientRef.addAnd(new ReferenceOrListParam().add(new ReferenceParam("")));
		
		Method method = BahmniFhirRelatedPersonDaoImpl.class.getDeclaredMethod("handlePatientBothSides", Criteria.class,
		    ReferenceAndListParam.class);
		method.setAccessible(true);
		method.invoke(dao, criteria, patientRef);
		
		verifyNoMoreInteractions(criteria);
	}
	
	@Test
	public void handlePatientBothSides_shouldAddOneRestrictionPerToken() throws Exception {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		patientRef.addAnd(new ReferenceOrListParam().add(new ReferenceParam("uuid-1")).add(new ReferenceParam("uuid-2")));
		
		Method method = BahmniFhirRelatedPersonDaoImpl.class.getDeclaredMethod("handlePatientBothSides", Criteria.class,
		    ReferenceAndListParam.class);
		method.setAccessible(true);
		method.invoke(dao, criteria, patientRef);
		
		verify(criteria, times(2)).add(any());
	}
	
	@Test
	public void setupSearchParams_shouldHandlePatientReferenceSearchHandler() throws Exception {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		patientRef.addAnd(new ReferenceOrListParam().add(new ReferenceParam("patient-uuid-123")));

		PropParam<ReferenceAndListParam> propParam = new PropParam<>(null, patientRef);
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, propParam);

		Method method = BahmniFhirRelatedPersonDaoImpl.class
		        .getDeclaredMethod("setupSearchParams", Criteria.class, SearchParameterMap.class);
		method.setAccessible(true);
		method.invoke(dao, criteria, theParams);

		verify(criteria, times(1)).add(any());
	}
	
	@Test
	public void setupSearchParams_shouldIgnoreNonPatientHandlers() throws Exception {
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter("some.other.handler",
		        new PropParam<>(null, new ReferenceAndListParam()));

		Method method = BahmniFhirRelatedPersonDaoImpl.class
		        .getDeclaredMethod("setupSearchParams", Criteria.class, SearchParameterMap.class);
		method.setAccessible(true);
		method.invoke(dao, criteria, theParams);

		verifyNoMoreInteractions(criteria);
	}
	
	@Test
	public void daoIsInstantiable() {
		assertThat(dao, notNullValue());
	}
}
