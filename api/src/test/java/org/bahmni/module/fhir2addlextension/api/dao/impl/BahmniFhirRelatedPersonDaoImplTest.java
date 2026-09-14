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
	public void handlePatientBothSides_shouldAddSqlRestrictionForValidUuid() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		patientRef.addAnd(new ReferenceOrListParam().add(new ReferenceParam("patient-uuid-123")));
		
		dao.handlePatientBothSides(criteria, patientRef);
		
		verify(criteria, times(1)).add(any());
	}
	
	@Test
	public void handlePatientBothSides_shouldSkipTokenWithNullUuid() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		patientRef.addAnd(new ReferenceOrListParam().add(new ReferenceParam((String) null)));
		
		dao.handlePatientBothSides(criteria, patientRef);
		
		verifyNoMoreInteractions(criteria);
	}
	
	@Test
	public void handlePatientBothSides_shouldSkipTokenWithEmptyUuid() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		patientRef.addAnd(new ReferenceOrListParam().add(new ReferenceParam("")));
		
		dao.handlePatientBothSides(criteria, patientRef);
		
		verifyNoMoreInteractions(criteria);
	}
	
	@Test
	public void handlePatientBothSides_shouldOrMultipleTokensInSameAndGroup() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		patientRef.addAnd(new ReferenceOrListParam().add(new ReferenceParam("uuid-1")).add(new ReferenceParam("uuid-2")));
		
		dao.handlePatientBothSides(criteria, patientRef);
		
		verify(criteria, times(1)).add(any());
	}
	
	@Test
	public void handlePatientBothSides_shouldAddOneRestrictionPerAndGroup() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		patientRef.addAnd(new ReferenceOrListParam().add(new ReferenceParam("uuid-1")));
		patientRef.addAnd(new ReferenceOrListParam().add(new ReferenceParam("uuid-2")));
		
		dao.handlePatientBothSides(criteria, patientRef);
		
		verify(criteria, times(2)).add(any());
	}
	
	@Test
	public void daoIsInstantiable() {
		assertThat(dao, notNullValue());
	}
}
