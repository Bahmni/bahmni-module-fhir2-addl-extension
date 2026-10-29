package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import org.bahmni.module.fhir2addlextension.api.TestUtils;
import org.bahmni.module.fhir2addlextension.api.model.FhirDiagnosticReportExt;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.impl.BaseDao;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;
import org.openmrs.module.fhir2.api.dao.internals.FhirSearchQueryHelper;
import org.openmrs.module.fhir2.api.dao.internals.OpenmrsFhirCriteriaContext;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.test.util.ReflectionTestUtils;

public class BahmniFhirDiagnosticReportDaoImplHighImpactTest {
	
	private BahmniFhirDiagnosticReportDaoImpl dao;
	
	private OpenmrsFhirCriteriaContext<FhirDiagnosticReportExt, FhirDiagnosticReportExt> ctx;
	
	@Before
	public void setup() {
		dao = new BahmniFhirDiagnosticReportDaoImpl();
		org.hibernate.SessionFactory sf = mock(org.hibernate.SessionFactory.class);
		ReflectionTestUtils.setField(dao, BaseDao.class, "sessionFactory", sf, org.hibernate.SessionFactory.class);
		ReflectionTestUtils.setField(dao, BaseFhirDao.class, "searchQueryHelper", new FhirSearchQueryHelper(),
		    FhirSearchQueryHelper.class);
		ctx = TestUtils.mockCriteriaContext(FhirDiagnosticReportExt.class);
	}
	
	@Test
	public void setupSearchParams_withEmptyParams_shouldNotThrow() {
		dao.setupSearchParams(ctx, new SearchParameterMap());
		assertThat(ctx, notNullValue());
	}
	
	@Test
	public void setupSearchParams_withPatientParameter_shouldNotThrow() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Patient", "patient-uuid")));
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, patientRef);
		dao.setupSearchParams(ctx, params);
		assertThat(ctx, notNullValue());
	}
	
	@Test
	public void setupSearchParams_withCodeParameter_shouldCreateCodeJoin() {
		TokenAndListParam code = new TokenAndListParam().addAnd(new TokenOrListParam().add("http://loinc.org", "12345"));
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.CODED_SEARCH_HANDLER, code);
		dao.setupSearchParams(ctx, params);
		assertThat(ctx.getJoin("c").isPresent(), is(true));
	}
	
	@Test
	public void setupSearchParams_withEncounterParameter_shouldNotThrow() {
		ReferenceAndListParam encounterRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Encounter", "enc-uuid")));
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, encounterRef);
		dao.setupSearchParams(ctx, params);
		assertThat(ctx, notNullValue());
	}
	
	@Test
	public void setupSearchParams_withResultParameter_shouldCreateObsJoin() {
		ReferenceAndListParam resultRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Observation", "obs-uuid")));
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.RESULT_SEARCH_HANDLER, resultRef);
		dao.setupSearchParams(ctx, params);
		assertThat(ctx.getJoin("obs").isPresent(), is(true));
	}
	
	@Test
	public void setupSearchParams_withDateRangeParameter_shouldNotThrow() {
		DateRangeParam dateRange = new DateRangeParam().setLowerBound("2023-01-01").setUpperBound("2023-12-31");
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.DATE_RANGE_SEARCH_HANDLER, dateRange);
		dao.setupSearchParams(ctx, params);
		assertThat(ctx, notNullValue());
	}
}
