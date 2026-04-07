package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.bahmni.module.fhir2addlextension.api.model.FhirImagingStudy;
import org.hibernate.Criteria;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirImagingStudyDaoImplTest {
	
	private static final String PATIENT_UUID = "da7f524f-27ce-4bb2-86d6-6d1d05312bd5";
	
	private static final String ORDER_UUID = "7d96f25c-4949-4f72-9931-d808fbc226de";
	
	private static final String IMAGING_STUDY_UUID = "imaging-study-uuid-123";
	
	private BahmniFhirImagingStudyDaoImpl spyDao;
	
	@Before
	public void setUp() {
		BahmniFhirImagingStudyDaoImpl imagingStudyDao = new BahmniFhirImagingStudyDaoImpl();
		spyDao = spy(imagingStudyDao);
	}
	
	@Test
	public void shouldSearchByPatientReference() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(PATIENT_UUID)));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, patientReference);
		
		FhirImagingStudy imagingStudy = new FhirImagingStudy();
		imagingStudy.setUuid(IMAGING_STUDY_UUID);
		
		doReturn(Collections.singletonList(imagingStudy)).when(spyDao).getSearchResults(theParams);
		
		List<FhirImagingStudy> results = spyDao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
		assertThat(results, hasSize(1));
		assertThat(results.get(0).getUuid(), equalTo(IMAGING_STUDY_UUID));
	}
	
	@Test
	public void shouldSearchByBasedOnReference() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(ORDER_UUID)));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		
		FhirImagingStudy imagingStudy = new FhirImagingStudy();
		imagingStudy.setUuid(IMAGING_STUDY_UUID);
		
		doReturn(Collections.singletonList(imagingStudy)).when(spyDao).getSearchResults(theParams);
		
		List<FhirImagingStudy> results = spyDao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
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
		
		FhirImagingStudy imagingStudy = new FhirImagingStudy();
		imagingStudy.setUuid(IMAGING_STUDY_UUID);
		
		doReturn(Collections.singletonList(imagingStudy)).when(spyDao).getSearchResults(theParams);
		
		List<FhirImagingStudy> results = spyDao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
		assertThat(results, hasSize(1));
	}
	
	@Test
	public void shouldReturnEmptyListWhenNoMatchingResults() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue("non-existent-uuid")));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, patientReference);
		
		doReturn(Collections.emptyList()).when(spyDao).getSearchResults(theParams);
		
		List<FhirImagingStudy> results = spyDao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
		assertThat(results, empty());
	}
	
	@Test
	public void shouldHandleNullBasedOnReference() {
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, null);
		
		doReturn(Collections.emptyList()).when(spyDao).getSearchResults(theParams);
		
		List<FhirImagingStudy> results = spyDao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
	}
	
	@Test
	public void shouldHandleEmptySearchParameters() {
		SearchParameterMap theParams = new SearchParameterMap();
		
		doReturn(Collections.emptyList()).when(spyDao).getSearchResults(theParams);
		
		List<FhirImagingStudy> results = spyDao.getSearchResults(theParams);
		
		assertThat(results, notNullValue());
	}
}
