package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2AddlExtension.api.utils.ModuleUtils;
import org.hibernate.criterion.Criterion;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openmrs.Drug;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

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
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ModuleUtils.class)
@PowerMockIgnore({ "javax.management.*", "javax.script.*", "org.apache.logging.log4j.*" })
public class BahmniFhirMedicationDaoImplTest {
	
	private static final String DRUG_NAME = "Aspirin";
	
	private static final String DRUG_UUID = "123e4567-e89b-12d3-a456-426614174000";
	
	private static final int FROM_INDEX = 0;
	
	private static final int TO_INDEX = 10;
	
	private static final String SYSTEM_URL = "http://example.com/system";
	
	private static final String CONCEPT_REFERENCE_TERM_ALIAS = "crt";
	
	@Mock
	private ConceptService conceptService;
	
	@Mock
	private Criterion mockCriterion;
	
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
		// Setup
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam(DRUG_NAME));
		searchParams.setFromIndex(FROM_INDEX);
		searchParams.setToIndex(TO_INDEX);
		
		when(conceptService.getDrugs(DRUG_NAME, null, true, true, false, FROM_INDEX, TO_INDEX - FROM_INDEX)).thenReturn(
		    drugList);
		
		// Execute
		List<Drug> results = bahmniFhirMedicationDao.getSearchResults(searchParams);
		
		// Verify
		assertThat(results.size(), equalTo(1));
		assertThat(results.get(0).getUuid(), equalTo(DRUG_UUID));
		verify(conceptService).getDrugs(DRUG_NAME, null, true, true, false, FROM_INDEX, TO_INDEX - FROM_INDEX);
	}
	
	@Test
	public void getSearchResults_shouldCallSuperMethodWhenNameSearchParamIsEmpty() {
		// Setup
		SearchParameterMap searchParams = new SearchParameterMap();
		// No name search parameter added
		
		// Create a spy to verify super method call
		BahmniFhirMedicationDaoImpl spyDao = spy(bahmniFhirMedicationDao);
		
		// Mock the super method to return a specific value
		List<Drug> expectedResults = Collections.singletonList(drug);
		doReturn(expectedResults).when(spyDao).getSearchResults(searchParams);
		
		// Execute
		List<Drug> results = spyDao.getSearchResults(searchParams);
		
		// Verify that conceptService.getDrugs was not called
		verify(conceptService, never()).getDrugs(anyString(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyInt(),
		    any());
		
		// Verify that the method was called and the result is returned
		verify(spyDao).getSearchResults(searchParams);
		assertEquals(expectedResults, results);
	}
	
	@Test
	public void getSearchResults_shouldHandleMaxIntegerToIndex() {
		// Setup
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam(DRUG_NAME));
		searchParams.setFromIndex(FROM_INDEX);
		searchParams.setToIndex(Integer.MAX_VALUE);
		
		when(conceptService.getDrugs(DRUG_NAME, null, true, true, false, FROM_INDEX, null)).thenReturn(drugList);
		
		// Execute
		List<Drug> results = bahmniFhirMedicationDao.getSearchResults(searchParams);
		
		// Verify
		assertThat(results.size(), equalTo(1));
		verify(conceptService).getDrugs(DRUG_NAME, null, true, true, false, FROM_INDEX, null);
	}
	
	@Test
	public void getSearchResults_shouldReturnEmptyListWhenNoDrugsFound() {
		// Setup
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam("NonExistentDrug"));
		searchParams.setFromIndex(FROM_INDEX);
		searchParams.setToIndex(TO_INDEX);
		
		when(conceptService.getDrugs("NonExistentDrug", null, true, true, false, FROM_INDEX, TO_INDEX - FROM_INDEX))
		        .thenReturn(Collections.emptyList());
		
		// Execute
		List<Drug> results = bahmniFhirMedicationDao.getSearchResults(searchParams);
		
		// Verify
		assertThat(results.size(), equalTo(0));
	}
	
	@Test
	public void getSearchResultsCount_shouldReturnCountWhenNameSearchParamIsProvided() {
		// Setup
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam(DRUG_NAME));
		
		when(conceptService.getCountOfDrugs(DRUG_NAME, null, true, true, false)).thenReturn(5);
		
		// Execute
		int count = bahmniFhirMedicationDao.getSearchResultsCount(searchParams);
		
		// Verify
		assertEquals(5, count);
		verify(conceptService).getCountOfDrugs(DRUG_NAME, null, true, true, false);
	}
	
	@Test
	public void getSearchResultsCount_shouldCallSuperMethodWhenNameSearchParamIsEmpty() {
		// Setup
		SearchParameterMap searchParams = new SearchParameterMap();
		// No name search parameter added
		
		// Create a spy to verify super method call
		BahmniFhirMedicationDaoImpl spyDao = spy(bahmniFhirMedicationDao);
		
		// Mock the super method to return a specific value
		doReturn(42).when(spyDao).getSearchResultsCount(searchParams);
		
		// Execute
		int result = spyDao.getSearchResultsCount(searchParams);
		
		// Verify that conceptService.getCountOfDrugs was not called
		verify(conceptService, never()).getCountOfDrugs(anyString(), any(), anyBoolean(), anyBoolean(), anyBoolean());
		
		// Verify that the method was called and the result is returned
		verify(spyDao).getSearchResultsCount(searchParams);
		assertEquals(42, result);
	}
	
	@Test
	public void getSearchResultsCount_shouldReturnZeroWhenNoDrugsFound() {
		// Setup
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam("NonExistentDrug"));
		
		when(conceptService.getCountOfDrugs("NonExistentDrug", null, true, true, false)).thenReturn(0);
		
		// Execute
		int count = bahmniFhirMedicationDao.getSearchResultsCount(searchParams);
		
		// Verify
		assertEquals(0, count);
	}
	
	@Test
	public void getSearchResults_shouldHandleMultipleNameSearchParams() {
		// Setup - testing with multiple name params (should use the first one)
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam("Aspirin"));
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam("Paracetamol"));
		searchParams.setFromIndex(FROM_INDEX);
		searchParams.setToIndex(TO_INDEX);
		
		when(conceptService.getDrugs("Aspirin", null, true, true, false, FROM_INDEX, TO_INDEX - FROM_INDEX)).thenReturn(
		    drugList);
		
		// Execute
		List<Drug> results = bahmniFhirMedicationDao.getSearchResults(searchParams);
		
		// Verify - should use the first parameter
		assertThat(results.size(), equalTo(1));
		verify(conceptService).getDrugs("Aspirin", null, true, true, false, FROM_INDEX, TO_INDEX - FROM_INDEX);
		verify(conceptService, never()).getDrugs(eq("Paracetamol"), any(), anyBoolean(), anyBoolean(), anyBoolean(),
		    anyInt(), any());
	}
	
	@Test
	public void getSearchResultsCount_shouldHandleMultipleNameSearchParams() {
		// Setup - testing with multiple name params (should use the first one)
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam("Aspirin"));
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam("Paracetamol"));
		
		when(conceptService.getCountOfDrugs("Aspirin", null, true, true, false)).thenReturn(3);
		
		// Execute
		int count = bahmniFhirMedicationDao.getSearchResultsCount(searchParams);
		
		// Verify - should use the first parameter
		assertEquals(3, count);
		verify(conceptService).getCountOfDrugs("Aspirin", null, true, true, false);
		verify(conceptService, never()).getCountOfDrugs(eq("Paracetamol"), any(), anyBoolean(), anyBoolean(), anyBoolean());
	}
	
	@Test(expected = InvalidRequestException.class)
	public void getSearchResults_shouldThrowExceptionWhenSearchPhraseIsNull() {
		// Setup
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam(null));
		searchParams.setFromIndex(FROM_INDEX);
		searchParams.setToIndex(TO_INDEX);
		
		// Execute - should throw InvalidRequestException
		bahmniFhirMedicationDao.getSearchResults(searchParams);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void getSearchResultsCount_shouldThrowExceptionWhenSearchPhraseIsNull() {
		// Setup
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam(null));
		
		// Execute - should throw InvalidRequestException
		bahmniFhirMedicationDao.getSearchResultsCount(searchParams);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void getSearchResults_shouldThrowExceptionWhenSearchPhraseIsEmpty() {
		// Setup
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam(""));
		searchParams.setFromIndex(FROM_INDEX);
		searchParams.setToIndex(TO_INDEX);
		
		// Execute - should throw InvalidRequestException
		bahmniFhirMedicationDao.getSearchResults(searchParams);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void getSearchResultsCount_shouldThrowExceptionWhenSearchPhraseIsEmpty() {
		// Setup
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam(""));
		
		// Execute - should throw InvalidRequestException
		bahmniFhirMedicationDao.getSearchResultsCount(searchParams);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void getSearchResults_shouldThrowExceptionWhenSearchPhraseIsOnlyWhitespace() {
		// Setup
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam("   "));
		searchParams.setFromIndex(FROM_INDEX);
		searchParams.setToIndex(TO_INDEX);
		
		// Execute - should throw InvalidRequestException
		bahmniFhirMedicationDao.getSearchResults(searchParams);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void getSearchResultsCount_shouldThrowExceptionWhenSearchPhraseIsOnlyWhitespace() {
		// Setup
		SearchParameterMap searchParams = new SearchParameterMap();
		searchParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam("   "));
		
		// Execute - should throw InvalidRequestException
		bahmniFhirMedicationDao.getSearchResultsCount(searchParams);
	}
	
	@Test
	public void generateSystemQuery_shouldCallModuleUtilsWhenCodesIsEmpty() {
		PowerMockito.mockStatic(ModuleUtils.class);
		
		when(ModuleUtils.isConceptReferenceCodeEmpty(null)).thenReturn(true);
		when(ModuleUtils.generateSystemQueryForEmptyCodes(SYSTEM_URL, CONCEPT_REFERENCE_TERM_ALIAS)).thenReturn(
		    mockCriterion);
		
		bahmniFhirMedicationDao.generateSystemQuery(SYSTEM_URL, null, CONCEPT_REFERENCE_TERM_ALIAS);
		
		verifyStatic(ModuleUtils.class);
		ModuleUtils.isConceptReferenceCodeEmpty(null);
		
		verifyStatic(ModuleUtils.class);
		ModuleUtils.generateSystemQueryForEmptyCodes(SYSTEM_URL, CONCEPT_REFERENCE_TERM_ALIAS);
	}
	
	@Test
	public void generateSystemQuery_shouldNotCallGenerateSystemQueryForEmptyCodesWhenCodesHasValues() {
		List<String> codes = Collections.singletonList("validCode");
		
		PowerMockito.mockStatic(ModuleUtils.class);
		
		when(ModuleUtils.isConceptReferenceCodeEmpty(codes)).thenReturn(false);
		
		bahmniFhirMedicationDao.generateSystemQuery(SYSTEM_URL, codes, CONCEPT_REFERENCE_TERM_ALIAS);
		
		verifyStatic(ModuleUtils.class);
		ModuleUtils.isConceptReferenceCodeEmpty(codes);
		
		verifyStatic(ModuleUtils.class, never());
		ModuleUtils.generateSystemQueryForEmptyCodes(SYSTEM_URL, CONCEPT_REFERENCE_TERM_ALIAS);
	}
}
