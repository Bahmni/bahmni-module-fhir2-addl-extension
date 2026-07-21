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
public class BahmniObsDaoImplTest {
	
	private static final String PATIENT_UUID = "da7f524f-27ce-4bb2-86d6-6d1d05312bd5";
	
	private static final String ORDER_UUID = "7d96f25c-4949-4f72-9931-d808fbc226de";
	
	private static final String OBS_UUID = "obs-uuid-123";
	
	private BahmniObsDaoImpl spyDao;
	
	@Before
	public void setUp() {
		BahmniObsDaoImpl obsDao = new BahmniObsDaoImpl();
		spyDao = spy(obsDao);
	}
	
	@Test
	public void shouldSearchByPatientReference() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(PATIENT_UUID)));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, patientReference);
		
		Obs obs = new Obs();
		obs.setUuid(OBS_UUID);
		
		doReturn(Collections.singletonList(obs)).when(spyDao).getSearchResults(theParams);
		
		List<Obs> results = spyDao.getSearchResults(theParams);
		
		assertThat(results, hasSize(1));
		assertThat(results.get(0).getUuid(), equalTo(OBS_UUID));
	}
	
	@Test
	public void shouldSearchByBasedOnReference() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(ORDER_UUID)));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		
		Obs obs = new Obs();
		obs.setUuid(OBS_UUID);
		
		doReturn(Collections.singletonList(obs)).when(spyDao).getSearchResults(theParams);
		
		List<Obs> results = spyDao.getSearchResults(theParams);
		
		assertThat(results, hasSize(1));
	}
	
	@Test
	public void shouldSearchByBasedOnReferenceWithServiceRequestPrefix() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("ServiceRequest/" + ORDER_UUID)));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		
		Obs obs = new Obs();
		obs.setUuid(OBS_UUID);
		
		doReturn(Collections.singletonList(obs)).when(spyDao).getSearchResults(theParams);
		
		List<Obs> results = spyDao.getSearchResults(theParams);
		
		assertThat(results, hasSize(1));
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
		
		Obs obs = new Obs();
		obs.setUuid(OBS_UUID);
		
		doReturn(Collections.singletonList(obs)).when(spyDao).getSearchResults(theParams);
		
		List<Obs> results = spyDao.getSearchResults(theParams);
		
		assertThat(results, hasSize(1));
	}
	
	@Test
	public void shouldReturnEmptyListWhenNoMatchingResults() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue("non-existent-uuid")));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, patientReference);
		
		doReturn(Collections.emptyList()).when(spyDao).getSearchResults(theParams);
		
		List<Obs> results = spyDao.getSearchResults(theParams);
		
		assertThat(results, empty());
	}
	
	@Test
	public void shouldHandleNullBasedOnReference() {
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, null);
		
		doReturn(Collections.emptyList()).when(spyDao).getSearchResults(theParams);
		
		List<Obs> results = spyDao.getSearchResults(theParams);
		
		assertThat(results, empty());
	}
	
	@Test
	public void shouldHandleEmptySearchParameters() {
		SearchParameterMap theParams = new SearchParameterMap();
		
		doReturn(Collections.emptyList()).when(spyDao).getSearchResults(theParams);
		
		List<Obs> results = spyDao.getSearchResults(theParams);
		
		assertThat(results, empty());
	}
}
