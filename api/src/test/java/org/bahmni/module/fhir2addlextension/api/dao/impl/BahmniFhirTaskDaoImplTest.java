package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.StringParam;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
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
		params.addParameter("focus", focusRef);
		
		taskDao.setupSearchParams(criteria, params);
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldHandleMultiValueFocusParameter() {
		Criteria criteria = mock(Criteria.class);
		
		ReferenceAndListParam focusRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(
		    new ReferenceParam("Observation", OBS_UUID_1)).add(new ReferenceParam("Observation", OBS_UUID_2)));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("focus", focusRef);
		
		taskDao.setupSearchParams(criteria, params);
		
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
		
		ArgumentCaptor<Criterion> criterionCaptor = ArgumentCaptor.forClass(Criterion.class);
		verify(criteria, times(1)).add(criterionCaptor.capture());
		
		Criterion capturedCriterion = criterionCaptor.getValue();
		assertThat(capturedCriterion, notNullValue());
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
		
		taskDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldSurviveFocusParameterRebuildingWithSingleUUID() {
		Criteria criteria = mock(Criteria.class);
		
		ReferenceAndListParam focusRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Observation", OBS_UUID_1)));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("focus", focusRef);
		
		taskDao.setupSearchParams(criteria, params);
		
		assertThat(params.getParameters("focus"), notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldSurviveFocusParameterRebuildingWithMultipleUUIDs() {
		Criteria criteria = mock(Criteria.class);
		
		ReferenceAndListParam focusRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(
		    new ReferenceParam("Observation", OBS_UUID_1)).add(new ReferenceParam("Observation", OBS_UUID_2)));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("focus", focusRef);
		
		taskDao.setupSearchParams(criteria, params);
		
		assertThat(params.getParameters("focus"), notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldPreserveFocusParameterThroughParameterRebuilding() {
		Criteria criteria = mock(Criteria.class);
		
		String focusUUID = "focus-obs-uuid";
		ReferenceAndListParam focusRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Observation", focusUUID)));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter("focus", focusRef);
		int paramCountBefore = params.getParameters().size();
		
		taskDao.setupSearchParams(criteria, params);
		
		assertThat(params.getParameters().size(), equalTo(paramCountBefore));
		assertThat(params.getParameters("focus"), notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldHandleNameParameter() {
		Criteria criteria = mock(Criteria.class);
		
		StringAndListParam name = new StringAndListParam().addAnd(new StringOrListParam().add(new StringParam("task-name")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(BahmniFhirConstants.NAME_SEARCH_HANDLER, name);
		
		taskDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldHandleEncounterForTask() {
		Criteria criteria = mock(Criteria.class);
		
		ReferenceAndListParam encounterRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Encounter", "encounter-uuid")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, encounterRef);
		
		taskDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldHandleMultipleParameters() {
		Criteria criteria = mock(Criteria.class);
		
		ReferenceAndListParam forRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam(
		        "Visit", "visit-uuid")));
		StringAndListParam name = new StringAndListParam().addAnd(new StringOrListParam().add(new StringParam("task-name")));
		ReferenceAndListParam encounterRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Encounter", "encounter-uuid")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.FOR_REFERENCE_SEARCH_HANDLER, forRef);
		params.addParameter(BahmniFhirConstants.NAME_SEARCH_HANDLER, name);
		params.addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, encounterRef);
		
		taskDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldHandleNullEncounterReference() {
		Criteria criteria = mock(Criteria.class);
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, null);
		
		taskDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldHandleNullName() {
		Criteria criteria = mock(Criteria.class);
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(BahmniFhirConstants.NAME_SEARCH_HANDLER, null);
		
		taskDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldIgnoreEncounterReferenceWhenNull() {
		Criteria criteria = mock(Criteria.class);
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, null);
		
		taskDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldIgnoreForReferenceWhenNull() {
		Criteria criteria = mock(Criteria.class);
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.FOR_REFERENCE_SEARCH_HANDLER, null);
		
		taskDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldAddNameRestriction() {
		Criteria criteria = mock(Criteria.class);
		
		StringAndListParam name = new StringAndListParam().addAnd(new StringOrListParam().add(new StringParam("task-name")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(BahmniFhirConstants.NAME_SEARCH_HANDLER, name);
		
		taskDao.setupSearchParams(criteria, params);
		
		ArgumentCaptor<Criterion> criterionCaptor = ArgumentCaptor.forClass(Criterion.class);
		verify(criteria, atLeastOnce()).add(criterionCaptor.capture());
		assertThat(criterionCaptor.getAllValues().stream().anyMatch(c -> c.toString().contains("name=task-name")),
		    equalTo(true));
	}
	
	@Test
	public void setupSearchParams_shouldAddEncounterTargetUuidRestriction() {
		Criteria criteria = mock(Criteria.class);
		
		ReferenceAndListParam encounterRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(
		    new ReferenceParam("Encounter", "encounter-uuid")));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, encounterRef);
		
		taskDao.setupSearchParams(criteria, params);
		
		ArgumentCaptor<Criterion> criterionCaptor = ArgumentCaptor.forClass(Criterion.class);
		verify(criteria, atLeastOnce()).add(criterionCaptor.capture());
		assertThat(criterionCaptor.getAllValues().stream().anyMatch(c -> c.toString().contains("er.targetUuid")),
		    equalTo(true));
	}
	
	@Test
	public void setupSearchParams_shouldNotAddTargetUuidRestrictionForForReferenceWithoutIdPart() {
		Criteria criteria = mock(Criteria.class);
		
		ReferenceAndListParam forReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(
		    new ReferenceParam()));
		
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.FOR_REFERENCE_SEARCH_HANDLER, forReference);
		
		taskDao.setupSearchParams(criteria, params);
		
		ArgumentCaptor<Criterion> criterionCaptor = ArgumentCaptor.forClass(Criterion.class);
		verify(criteria, atLeastOnce()).add(criterionCaptor.capture());
		assertThat(criterionCaptor.getAllValues().stream().noneMatch(c -> c.toString().contains("fr.targetUuid")),
		    equalTo(true));
	}
}
