package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Drug;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirMedicationDaoImplTest {
	
	private static final String DRUG_NAME = "Aspirin";
	
	private static final String DRUG_UUID = "123e4567-e89b-12d3-a456-426614174000";
	
	private static final int FROM_INDEX = 0;
	
	private static final int TO_INDEX = 10;
	
	@Mock
	private ConceptService conceptService;
	
	@InjectMocks
	private BahmniFhirMedicationDaoImpl bahmniFhirMedicationDao;
	
	private Drug drug;
	
	private List<Drug> drugList;
	
	@Before
	public void setUp() {
		drug = new Drug();
		drug.setUuid(DRUG_UUID);
		drug.setName(DRUG_NAME);

		drugList = new ArrayList<>();
		drugList.add(drug);
	}
	
	@Test
	public void getSearchResults_shouldReturnDrugsWhenNameSearchParamIsProvided() {
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam(DRUG_NAME));
		searchParams.setFromIndex(FROM_INDEX);
		searchParams.setToIndex(TO_INDEX);
		
		when(conceptService.getDrugs(DRUG_NAME, null, true, true, false, FROM_INDEX, TO_INDEX - FROM_INDEX)).thenReturn(
		    drugList);
		
		List<Drug> results = bahmniFhirMedicationDao.getSearchResults(searchParams);
		
		assertThat(results.size(), equalTo(1));
		assertThat(results.get(0).getUuid(), equalTo(DRUG_UUID));
		verify(conceptService).getDrugs(DRUG_NAME, null, true, true, false, FROM_INDEX, TO_INDEX - FROM_INDEX);
	}
	
	@Test
	public void getSearchResults_shouldCallSuperMethodWhenNameSearchParamIsEmpty() {
		SearchParameterMap searchParams = new SearchParameterMap();
		
		BahmniFhirMedicationDaoImpl spyDao = spy(bahmniFhirMedicationDao);
		
		List<Drug> expectedResults = Collections.singletonList(drug);
		doReturn(expectedResults).when(spyDao).getSearchResults(searchParams);
		
		List<Drug> results = spyDao.getSearchResults(searchParams);
		
		verify(conceptService, never()).getDrugs(anyString(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyInt(),
		    any());
		verify(spyDao).getSearchResults(searchParams);
		assertEquals(expectedResults, results);
	}
	
	@Test
	public void getSearchResults_shouldHandleMaxIntegerToIndex() {
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam(DRUG_NAME));
		searchParams.setFromIndex(FROM_INDEX);
		searchParams.setToIndex(Integer.MAX_VALUE);
		
		when(conceptService.getDrugs(DRUG_NAME, null, true, true, false, FROM_INDEX, null)).thenReturn(drugList);
		
		List<Drug> results = bahmniFhirMedicationDao.getSearchResults(searchParams);
		
		assertThat(results.size(), equalTo(1));
		verify(conceptService).getDrugs(DRUG_NAME, null, true, true, false, FROM_INDEX, null);
	}
	
	@Test
	public void getSearchResults_shouldReturnEmptyListWhenNoDrugsFound() {
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam("NonExistentDrug"));
		searchParams.setFromIndex(FROM_INDEX);
		searchParams.setToIndex(TO_INDEX);
		
		when(conceptService.getDrugs("NonExistentDrug", null, true, true, false, FROM_INDEX, TO_INDEX - FROM_INDEX))
		        .thenReturn(Collections.emptyList());
		
		List<Drug> results = bahmniFhirMedicationDao.getSearchResults(searchParams);
		
		assertThat(results.size(), equalTo(0));
	}
	
	@Test
	public void getSearchResultsCount_shouldReturnCountWhenNameSearchParamIsProvided() {
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam(DRUG_NAME));
		
		when(conceptService.getCountOfDrugs(DRUG_NAME, null, true, true, false)).thenReturn(5);
		
		int count = bahmniFhirMedicationDao.getSearchResultsCount(searchParams);
		
		assertEquals(5, count);
		verify(conceptService).getCountOfDrugs(DRUG_NAME, null, true, true, false);
	}
	
	@Test
	public void getSearchResultsCount_shouldCallSuperMethodWhenNameSearchParamIsEmpty() {
		SearchParameterMap searchParams = new SearchParameterMap();
		
		BahmniFhirMedicationDaoImpl spyDao = spy(bahmniFhirMedicationDao);
		
		doReturn(42).when(spyDao).getSearchResultsCount(searchParams);
		
		int result = spyDao.getSearchResultsCount(searchParams);
		
		verify(conceptService, never()).getCountOfDrugs(anyString(), any(), anyBoolean(), anyBoolean(), anyBoolean());
		verify(spyDao).getSearchResultsCount(searchParams);
		assertEquals(42, result);
	}
	
	@Test
	public void getSearchResultsCount_shouldReturnZeroWhenNoDrugsFound() {
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam("NonExistentDrug"));
		
		when(conceptService.getCountOfDrugs("NonExistentDrug", null, true, true, false)).thenReturn(0);
		
		int count = bahmniFhirMedicationDao.getSearchResultsCount(searchParams);
		
		assertEquals(0, count);
	}
	
	@Test
	public void getSearchResults_shouldHandleMultipleNameSearchParams() {
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam("Aspirin"));
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam("Paracetamol"));
		searchParams.setFromIndex(FROM_INDEX);
		searchParams.setToIndex(TO_INDEX);
		
		when(conceptService.getDrugs("Aspirin", null, true, true, false, FROM_INDEX, TO_INDEX - FROM_INDEX)).thenReturn(
		    drugList);
		
		List<Drug> results = bahmniFhirMedicationDao.getSearchResults(searchParams);
		
		assertThat(results.size(), equalTo(1));
		verify(conceptService).getDrugs("Aspirin", null, true, true, false, FROM_INDEX, TO_INDEX - FROM_INDEX);
		verify(conceptService, never()).getDrugs(eq("Paracetamol"), any(), anyBoolean(), anyBoolean(), anyBoolean(),
		    anyInt(), any());
	}
	
	@Test
	public void getSearchResultsCount_shouldHandleMultipleNameSearchParams() {
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam("Aspirin"));
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam("Paracetamol"));
		
		when(conceptService.getCountOfDrugs("Aspirin", null, true, true, false)).thenReturn(3);
		
		int count = bahmniFhirMedicationDao.getSearchResultsCount(searchParams);
		
		assertEquals(3, count);
		verify(conceptService).getCountOfDrugs("Aspirin", null, true, true, false);
		verify(conceptService, never()).getCountOfDrugs(eq("Paracetamol"), any(), anyBoolean(), anyBoolean(), anyBoolean());
	}
	
	@Test(expected = InvalidRequestException.class)
	public void getSearchResults_shouldThrowExceptionWhenSearchPhraseIsNull() {
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam(null));
		searchParams.setFromIndex(FROM_INDEX);
		searchParams.setToIndex(TO_INDEX);
		
		bahmniFhirMedicationDao.getSearchResults(searchParams);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void getSearchResultsCount_shouldThrowExceptionWhenSearchPhraseIsNull() {
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam(null));
		
		bahmniFhirMedicationDao.getSearchResultsCount(searchParams);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void getSearchResults_shouldThrowExceptionWhenSearchPhraseIsEmpty() {
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam(""));
		searchParams.setFromIndex(FROM_INDEX);
		searchParams.setToIndex(TO_INDEX);
		
		bahmniFhirMedicationDao.getSearchResults(searchParams);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void getSearchResultsCount_shouldThrowExceptionWhenSearchPhraseIsEmpty() {
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam(""));
		
		bahmniFhirMedicationDao.getSearchResultsCount(searchParams);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void getSearchResults_shouldThrowExceptionWhenSearchPhraseIsOnlyWhitespace() {
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam("   "));
		searchParams.setFromIndex(FROM_INDEX);
		searchParams.setToIndex(TO_INDEX);
		
		bahmniFhirMedicationDao.getSearchResults(searchParams);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void getSearchResultsCount_shouldThrowExceptionWhenSearchPhraseIsOnlyWhitespace() {
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam("   "));
		
		bahmniFhirMedicationDao.getSearchResultsCount(searchParams);
	}
}
