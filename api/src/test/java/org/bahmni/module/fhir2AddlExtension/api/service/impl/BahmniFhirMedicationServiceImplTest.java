package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.StringParam;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Medication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Drug;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.FhirGlobalPropertyService;
import org.openmrs.module.fhir2.api.dao.FhirMedicationDao;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryBundleProvider;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.translators.MedicationTranslator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirMedicationServiceImplTest {
	
	private static final String MEDICATION_UUID = "c0938432-1691-11df-97a5-7038c432aaba";
	
	private static final String MEDICATION_NAME = "Aspirin";
	
	private static final String MEDICATION_NAME_PARTIAL = "Asp";
	
	@Mock
	private FhirMedicationDao dao;
	
	@Mock
	private MedicationTranslator translator;
	
	@Mock
	private SearchQueryInclude<Medication> searchQueryInclude;
	
	@Mock
	private SearchQuery<Drug, Medication, FhirMedicationDao, MedicationTranslator, SearchQueryInclude<Medication>> searchQuery;
	
	@Mock
	private FhirGlobalPropertyService globalPropertyService;
	
	private BahmniFhirMedicationServiceImpl medicationService;
	
	private Drug drug;
	
	private Medication fhirMedication;
	
	@Before
	public void setUp() {
		medicationService = new BahmniFhirMedicationServiceImpl();
		medicationService.setDao(dao);
		medicationService.setTranslator(translator);
		medicationService.setSearchQuery(searchQuery);
		medicationService.setSearchQueryInclude(searchQueryInclude);
		
		drug = new Drug();
		drug.setUuid(MEDICATION_UUID);
		drug.setName(MEDICATION_NAME);
		
		fhirMedication = new Medication();
		fhirMedication.setId(MEDICATION_UUID);
		fhirMedication.setStatus(Medication.MedicationStatus.ACTIVE);
	}
	
	private List<IBaseResource> get(IBundleProvider results) {
		return results.getResources(0, 10);
	}
	
	@Test
	public void searchMedicationsByName_shouldReturnCollectionOfMedicationsByExactName() {
		StringParam nameParam = new StringParam(MEDICATION_NAME);
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, nameParam);
		
		when(dao.getSearchResults(any())).thenReturn(Collections.singletonList(drug));
		when(translator.toFhirResource(drug)).thenReturn(fhirMedication);
		when(translator.toFhirResources(anyCollection())).thenCallRealMethod();
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		        new SearchQueryBundleProvider<>(theParams, dao, translator, globalPropertyService, searchQueryInclude));
		when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());
		
		IBundleProvider results = medicationService.searchMedicationsByName(nameParam);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, not(empty()));
		assertThat(resultList, hasSize(equalTo(1)));
		assertThat(((Medication) resultList.get(0)).getId(), equalTo(MEDICATION_UUID));
	}
	
	@Test
	public void searchMedicationsByName_shouldReturnCollectionOfMedicationsByPartialName() {
		StringParam nameParam = new StringParam(MEDICATION_NAME_PARTIAL);
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, nameParam);
		
		when(dao.getSearchResults(any())).thenReturn(Collections.singletonList(drug));
		when(translator.toFhirResource(drug)).thenReturn(fhirMedication);
		when(translator.toFhirResources(anyCollection())).thenCallRealMethod();
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		        new SearchQueryBundleProvider<>(theParams, dao, translator, globalPropertyService, searchQueryInclude));
		when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());
		
		IBundleProvider results = medicationService.searchMedicationsByName(nameParam);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, not(empty()));
		assertThat(resultList, hasSize(equalTo(1)));
	}
	
	@Test
	public void searchMedicationsByName_shouldReturnEmptyCollectionWhenNoMedicationsFound() {
		StringParam nameParam = new StringParam("NonExistentMedication");
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, nameParam);
		
		when(dao.getSearchResults(any())).thenReturn(Collections.emptyList());
		when(translator.toFhirResources(anyCollection())).thenCallRealMethod();
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		        new SearchQueryBundleProvider<>(theParams, dao, translator, globalPropertyService, searchQueryInclude));
		when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());
		
		IBundleProvider results = medicationService.searchMedicationsByName(nameParam);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, empty());
	}
	
	@Test
	public void searchMedicationsByName_shouldInvokeDaoWithProperSearchParameterMap() {
		StringParam nameParam = new StringParam(MEDICATION_NAME);
		
		// Call the service method
		medicationService.searchMedicationsByName(nameParam);
		
		// Capture the actual SearchParameterMap passed to searchQuery.getQueryResults
		ArgumentCaptor<SearchParameterMap> mapCaptor = ArgumentCaptor.forClass(SearchParameterMap.class);
		verify(searchQuery).getQueryResults(mapCaptor.capture(), eq(dao), eq(translator), eq(searchQueryInclude));
		
		// Get the captured parameter map
		SearchParameterMap actualMap = mapCaptor.getValue();
		
		// Verify the map contains the expected parameter
		assertThat(actualMap.getParameters(FhirConstants.NAME_SEARCH_HANDLER), notNullValue());
		assertThat(actualMap.getParameters(FhirConstants.NAME_SEARCH_HANDLER).size(), equalTo(1));
		assertThat(actualMap.getParameters(FhirConstants.NAME_SEARCH_HANDLER).get(0).getParam(), equalTo(nameParam));
	}
	
	@Test
	public void searchMedicationsByName_shouldHandleNullNameParam() {
		StringParam nameParam = null;
		
		IBundleProvider results = medicationService.searchMedicationsByName(nameParam);
		
		// Verify the search was still executed with null parameter
		verify(searchQuery).getQueryResults(any(), eq(dao), eq(translator), eq(searchQueryInclude));
	}
	
	@Test
	public void searchMedicationsByName_shouldHandleEmptyNameParam() {
		StringParam nameParam = new StringParam("");
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, nameParam);
		
		when(dao.getSearchResults(any())).thenReturn(Collections.emptyList());
		when(translator.toFhirResources(anyCollection())).thenCallRealMethod();
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		        new SearchQueryBundleProvider<>(theParams, dao, translator, globalPropertyService, searchQueryInclude));
		when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());
		
		IBundleProvider results = medicationService.searchMedicationsByName(nameParam);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, empty());
	}
	
	@Test
	public void searchMedicationsByName_shouldReturnMultipleMedicationsWhenFound() {
		StringParam nameParam = new StringParam("Paracetamol");
		
		Drug drug1 = new Drug();
		drug1.setUuid("uuid1");
		drug1.setName("Paracetamol 500mg");
		
		Drug drug2 = new Drug();
		drug2.setUuid("uuid2");
		drug2.setName("Paracetamol 250mg");
		
		Medication medication1 = new Medication();
		medication1.setId("uuid1");
		
		Medication medication2 = new Medication();
		medication2.setId("uuid2");
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.NAME_SEARCH_HANDLER, nameParam);
		
		when(dao.getSearchResults(any())).thenReturn(Arrays.asList(drug1, drug2));
		when(translator.toFhirResource(drug1)).thenReturn(medication1);
		when(translator.toFhirResource(drug2)).thenReturn(medication2);
		when(translator.toFhirResources(anyCollection())).thenCallRealMethod();
		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		        new SearchQueryBundleProvider<>(theParams, dao, translator, globalPropertyService, searchQueryInclude));
		when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());
		
		IBundleProvider results = medicationService.searchMedicationsByName(nameParam);
		
		List<IBaseResource> resultList = get(results);
		
		assertThat(results, notNullValue());
		assertThat(resultList, not(empty()));
		assertThat(resultList, hasSize(equalTo(2)));
		assertThat(((Medication) resultList.get(0)).getId(), equalTo("uuid1"));
		assertThat(((Medication) resultList.get(1)).getId(), equalTo("uuid2"));
	}
}
