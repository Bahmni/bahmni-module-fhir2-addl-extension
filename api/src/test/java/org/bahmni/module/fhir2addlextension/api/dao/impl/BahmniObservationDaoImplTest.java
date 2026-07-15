package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Obs;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@RunWith(MockitoJUnitRunner.class)
public class BahmniObservationDaoImplTest {
	
	private static final String PATIENT_UUID = "da7f524f-27ce-4bb2-86d6-6d1d05312bd5";
	
	private static final String ORDER_UUID = "7d96f25c-4949-4f72-9931-d808fbc226de";
	
	private static final String ORDER_UUID_WITH_PREFIX = "ServiceRequest/7d96f25c-4949-4f72-9931-d808fbc226de";
	
	private static final String OBSERVATION_UUID = "observation-uuid-123";
	
	private BahmniObservationDaoImpl spyDao;
	
	@Before
	public void setUp() {
		BahmniObservationDaoImpl observationDao = new BahmniObservationDaoImpl();
		spyDao = spy(observationDao);
	}
	
	@Test
	public void shouldSearchByPatientReference() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(PATIENT_UUID)));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, patientReference);
		
		Obs observation = new Obs();
		observation.setUuid(OBSERVATION_UUID);
		
		doReturn(Collections.singletonList(observation)).when(spyDao).getSearchResults(theParams);
		
		List<Obs> results = spyDao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
		assertThat(results, hasSize(1));
		assertThat(results.get(0).getUuid(), equalTo(OBSERVATION_UUID));
	}
	
	@Test
	public void shouldSearchByBasedOnReference() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(ORDER_UUID)));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		
		Obs observation = new Obs();
		observation.setUuid(OBSERVATION_UUID);
		
		doReturn(Collections.singletonList(observation)).when(spyDao).getSearchResults(theParams);
		
		List<Obs> results = spyDao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
		assertThat(results, hasSize(1));
		assertThat(results.get(0).getUuid(), equalTo(OBSERVATION_UUID));
	}
	
	@Test
	public void shouldSearchByBasedOnReferenceWithResourceTypePrefix() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(ORDER_UUID_WITH_PREFIX)));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		
		Obs observation = new Obs();
		observation.setUuid(OBSERVATION_UUID);
		
		doReturn(Collections.singletonList(observation)).when(spyDao).getSearchResults(theParams);
		
		List<Obs> results = spyDao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
		assertThat(results, hasSize(1));
		assertThat(results.get(0).getUuid(), equalTo(OBSERVATION_UUID));
	}
	
	@Test
	public void shouldSearchWithMultipleParameters() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(PATIENT_UUID)));
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(ORDER_UUID)));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, patientReference);
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		
		Obs observation = new Obs();
		observation.setUuid(OBSERVATION_UUID);
		
		doReturn(Collections.singletonList(observation)).when(spyDao).getSearchResults(theParams);
		
		List<Obs> results = spyDao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
		assertThat(results, hasSize(1));
		assertThat(results.get(0).getUuid(), equalTo(OBSERVATION_UUID));
	}
	
	@Test
	public void shouldSearchWithMultipleObservations() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(ORDER_UUID)));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		
		Obs observation1 = new Obs();
		observation1.setUuid("obs-uuid-1");
		Obs observation2 = new Obs();
		observation2.setUuid("obs-uuid-2");
		Obs observation3 = new Obs();
		observation3.setUuid("obs-uuid-3");
		
		doReturn(java.util.Arrays.asList(observation1, observation2, observation3)).when(spyDao).getSearchResults(theParams);
		
		List<Obs> results = spyDao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
		assertThat(results, hasSize(3));
		assertThat(results.get(0).getUuid(), equalTo("obs-uuid-1"));
		assertThat(results.get(1).getUuid(), equalTo("obs-uuid-2"));
		assertThat(results.get(2).getUuid(), equalTo("obs-uuid-3"));
	}
	
	@Test
	public void shouldReturnEmptyListWhenNoMatchingResults() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue("non-existent-uuid")));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, patientReference);
		
		doReturn(Collections.emptyList()).when(spyDao).getSearchResults(theParams);
		
		List<Obs> results = spyDao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
		assertThat(results, empty());
	}
	
	@Test
	public void shouldHandleNullBasedOnReference() {
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, null);
		
		doReturn(Collections.emptyList()).when(spyDao).getSearchResults(theParams);
		
		List<Obs> results = spyDao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
	}
	
	@Test
	public void shouldHandleEmptySearchParameters() {
		SearchParameterMap theParams = new SearchParameterMap();
		
		doReturn(Collections.emptyList()).when(spyDao).getSearchResults(theParams);
		
		List<Obs> results = spyDao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
	}
	
	@Test
	public void shouldHandleNullPatientReference() {
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, null);
		
		doReturn(Collections.emptyList()).when(spyDao).getSearchResults(theParams);
		
		List<Obs> results = spyDao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
	}
	
	@Test
	public void shouldSearchWithCommonSearchHandler() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(ORDER_UUID)));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		theParams.addParameter(FhirConstants.COMMON_SEARCH_HANDLER, null);
		
		Obs observation = new Obs();
		observation.setUuid(OBSERVATION_UUID);
		
		doReturn(Collections.singletonList(observation)).when(spyDao).getSearchResults(theParams);
		
		List<Obs> results = spyDao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
		assertThat(results, hasSize(1));
	}
}
