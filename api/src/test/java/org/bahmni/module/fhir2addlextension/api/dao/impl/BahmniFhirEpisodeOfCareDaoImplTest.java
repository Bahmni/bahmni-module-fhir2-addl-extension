package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.bahmni.module.fhir2addlextension.api.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.episodes.Episode;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.impl.BaseDao;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;
import org.openmrs.module.fhir2.api.dao.internals.FhirSearchQueryHelper;
import org.openmrs.module.fhir2.api.dao.internals.OpenmrsFhirCriteriaContext;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.test.util.ReflectionTestUtils;

public class BahmniFhirEpisodeOfCareDaoImplTest {
	
	private BahmniFhirEpisodeOfCareDaoImpl dao;
	
	private OpenmrsFhirCriteriaContext<Episode, Episode> ctx;
	
	@Before
	public void setup() {
		dao = new BahmniFhirEpisodeOfCareDaoImpl();
		org.hibernate.SessionFactory sf = mock(org.hibernate.SessionFactory.class);
		ReflectionTestUtils.setField(dao, BaseDao.class, "sessionFactory", sf, org.hibernate.SessionFactory.class);
		ReflectionTestUtils.setField(dao, BaseFhirDao.class, "searchQueryHelper", new FhirSearchQueryHelper(),
		    FhirSearchQueryHelper.class);
		ctx = TestUtils.mockCriteriaContext(Episode.class);
	}
	
	@Test
	public void setupSearchParams_withEmptyParams_shouldNotThrow() {
		dao.setupSearchParams(ctx, new SearchParameterMap());
		assertThat(ctx, notNullValue());
	}
	
	@Test
	public void setupSearchParams_withPatientReference_shouldNotThrow() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Patient", "patient-uuid")));
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, patientRef);
		dao.setupSearchParams(ctx, params);
		assertThat(ctx, notNullValue());
	}
	
	@Test
	public void daoIsInstantiable() {
		assertThat(dao, notNullValue());
		assertThat(dao instanceof BahmniFhirEpisodeOfCareDaoImpl, is(true));
	}
}
