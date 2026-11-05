package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;

import java.util.List;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.StringParam;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2addlextension.api.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.impl.BaseDao;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;
import org.openmrs.module.fhir2.api.dao.internals.BaseFhirCriteriaHolder;
import org.openmrs.module.fhir2.api.dao.internals.FhirSearchQueryHelper;
import org.openmrs.module.fhir2.api.dao.internals.OpenmrsFhirCriteriaContext;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.model.FhirTask;
import org.springframework.test.util.ReflectionTestUtils;

import javax.persistence.criteria.Predicate;

public class BahmniFhirTaskDaoImplTest {
	
	private static final String OBS_UUID_1 = "obs-uuid-1";
	
	private static final String OBS_UUID_2 = "obs-uuid-2";
	
	private static final String VISIT_UUID = "visit-uuid-123";
	
	private BahmniFhirTaskDaoImpl taskDao;
	
	private OpenmrsFhirCriteriaContext<FhirTask, FhirTask> ctx;
	
	@Before
	public void setup() {
		taskDao = new BahmniFhirTaskDaoImpl();
		org.hibernate.SessionFactory sf = mock(org.hibernate.SessionFactory.class);
		ReflectionTestUtils.setField(taskDao, BaseDao.class, "sessionFactory", sf, org.hibernate.SessionFactory.class);
		ReflectionTestUtils.setField(taskDao, BaseFhirDao.class, "searchQueryHelper", new FhirSearchQueryHelper(),
		    FhirSearchQueryHelper.class);
		ctx = TestUtils.mockCriteriaContext(FhirTask.class);
	}
	
	@Test
	public void setupSearchParams_shouldNotCreateJoinForForReferenceIfNotPresent() {
		SearchParameterMap params = new SearchParameterMap();
		taskDao.setupSearchParams(ctx, params);
		assertThat(ctx.getJoin("fr").isPresent(), is(false));
	}
	
	@Test
	public void setupSearchParams_shouldCreateForReferenceJoinWhenPresent() {
		ReferenceAndListParam forRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam(
		        "Visit", VISIT_UUID)));
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.FOR_REFERENCE_SEARCH_HANDLER, forRef);
		taskDao.setupSearchParams(ctx, params);
		assertThat(ctx.getJoin("fr").isPresent(), is(true));
	}
	
	@Test
	public void setupSearchParams_shouldNotThrowWithFocusParameter() {
		ReferenceAndListParam focusRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Observation", OBS_UUID_1)));
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("focus", focusRef);
		taskDao.setupSearchParams(ctx, params);
		assertThat(ctx, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldHandleMultiValueFocusParameter() {
		ReferenceAndListParam focusRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(
		    new ReferenceParam("Observation", OBS_UUID_1)).add(new ReferenceParam("Observation", OBS_UUID_2)));
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("focus", focusRef);
		taskDao.setupSearchParams(ctx, params);
		assertThat(params.getParameters("focus"), notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldAddPredicateForForReferenceWithIdPart() {
		ReferenceAndListParam forRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam(
		        "Visit", VISIT_UUID)));
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.FOR_REFERENCE_SEARCH_HANDLER, forRef);
		@SuppressWarnings("unchecked")
		List<Predicate> predicatesBefore = (List<Predicate>) ReflectionTestUtils.getField(ctx, BaseFhirCriteriaHolder.class,
		    "predicates");
		int countBefore = predicatesBefore != null ? predicatesBefore.size() : 0;
		taskDao.setupSearchParams(ctx, params);
		@SuppressWarnings("unchecked")
		List<Predicate> predicatesAfter = (List<Predicate>) ReflectionTestUtils.getField(ctx, BaseFhirCriteriaHolder.class,
		    "predicates");
		assertThat(predicatesAfter, notNullValue());
		assertThat(predicatesAfter.size() > countBefore, is(true));
	}
	
	@Test
	public void setupSearchParams_shouldCreateEncounterJoinWhenEncounterParamPresent() {
		ReferenceAndListParam encounterRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Encounter", "encounter-uuid")));
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, encounterRef);
		taskDao.setupSearchParams(ctx, params);
		assertThat(ctx.getJoin("er").isPresent(), is(true));
	}
	
	@Test
	public void setupSearchParams_shouldHandleNameParameter() {
		StringAndListParam name = new StringAndListParam().addAnd(new StringOrListParam().add(new StringParam("task-name")));
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(BahmniFhirConstants.NAME_SEARCH_HANDLER, name);
		taskDao.setupSearchParams(ctx, params);
		assertThat(ctx, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldHandleMultipleParameters() {
		ReferenceAndListParam forRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam(
		        "Visit", "visit-uuid")));
		StringAndListParam name = new StringAndListParam().addAnd(new StringOrListParam().add(new StringParam("task-name")));
		ReferenceAndListParam encounterRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Encounter", "encounter-uuid")));
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.FOR_REFERENCE_SEARCH_HANDLER, forRef);
		params.addParameter(BahmniFhirConstants.NAME_SEARCH_HANDLER, name);
		params.addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, encounterRef);
		taskDao.setupSearchParams(ctx, params);
		assertThat(ctx.getJoin("fr").isPresent(), is(true));
		assertThat(ctx.getJoin("er").isPresent(), is(true));
	}
	
	@Test
	public void setupSearchParams_shouldNotAddPredicateForForReferenceWithoutIdPart() {
		ReferenceAndListParam forRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam()));
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.FOR_REFERENCE_SEARCH_HANDLER, forRef);
		taskDao.setupSearchParams(ctx, params);
		// join is created but no predicate because idPart is null
		assertThat(ctx.getJoin("fr").isPresent(), is(true));
	}
}
