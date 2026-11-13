package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThrows;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2addlextension.api.BaseFhir2AddlContextSensitiveTest;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Drug;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.FhirMedicationDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

public class BahmniFhirMedicationDaoImplTest extends BaseFhir2AddlContextSensitiveTest {
	
	private static final String DATASET = "org/bahmni/module/fhir2addlextension/api/dao/impl/BahmniFhirMedicationDaoImplTest_data.xml";
	
	private static final String PARACETAMOL_500 = "drug-9301-0000-0000-0000-000000000001";
	
	private static final String PARACETAMOL_650 = "drug-9302-0000-0000-0000-000000000002";
	
	private static final String PARACETAMOL_RETIRED = "drug-9303-0000-0000-0000-000000000003";
	
	private static final String IBUPROFEN = "drug-9304-0000-0000-0000-000000000004";
	
	private static final String ASPIRIN = "05ec820a-d297-44e3-be6e-698531d9dd3f";
	
	private static final String TRIOMUNE = "3cfcf118-931c-46f7-8ff6-7b876f0d4202";
	
	@Autowired
	private FhirMedicationDao dao;
	
	@Before
	public void loadDataset() throws Exception {
		executeDataSet(DATASET);
		// drug name search runs against the Hibernate Search index, which dbunit inserts bypass
		Context.updateSearchIndexForType(Drug.class);
	}
	
	private static SearchParameterMap nameSearch(String name) {
		return new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam(name));
	}
	
	private List<String> uuids(SearchParameterMap params) {
		return dao.getSearchResults(params).stream().map(Drug::getUuid).collect(Collectors.toList());
	}
	
	@Test
	public void search_withoutNameParameter_usesDefaultSearchAndReturnsAllNonRetiredDrugs() {
		List<String> result = uuids(new SearchParameterMap());
		
		assertThat(result, containsInAnyOrder(PARACETAMOL_500, PARACETAMOL_650, IBUPROFEN, ASPIRIN, TRIOMUNE));
	}
	
	@Test
	public void count_withoutNameParameter_usesDefaultCount() {
		assertThat(dao.getSearchResultsCount(new SearchParameterMap()), equalTo(5));
	}
	
	@Test
	public void search_byNamePhrase_matchesAnywhereInDrugNameAndExcludesRetiredAndNonMatching() {
		List<String> result = uuids(nameSearch("Paracetamol"));
		
		assertThat(result, containsInAnyOrder(PARACETAMOL_500, PARACETAMOL_650));
		assertThat(result, not(hasItem(PARACETAMOL_RETIRED)));
	}
	
	@Test
	public void search_byNamePhrase_isCaseInsensitiveAndTrimmed() {
		assertThat(uuids(nameSearch("  ibuprofen ")), containsInAnyOrder(IBUPROFEN));
	}
	
	@Test
	public void search_byNamePhraseMatchingDrugConceptName_returnsDrugOfThatConcept() {
		// concept 88 is named "ASPIRIN" and drug 3 is built on it; 5497/5089-based drugs must not match
		assertThat(uuids(nameSearch("aspirin")), containsInAnyOrder(ASPIRIN));
	}
	
	@Test
	public void search_byNameWithNoMatch_returnsEmptyList() {
		assertThat(uuids(nameSearch("no-such-drug")), empty());
	}
	
	@Test
	public void search_byName_appliesPagingWindowToResults() {
		SearchParameterMap firstPage = nameSearch("Paracetamol");
		firstPage.setFromIndex(0);
		firstPage.setToIndex(1);
		SearchParameterMap secondPage = nameSearch("Paracetamol");
		secondPage.setFromIndex(1);
		secondPage.setToIndex(2);
		
		List<String> first = uuids(firstPage);
		List<String> second = uuids(secondPage);
		
		assertThat(first, hasSize(1));
		assertThat(second, hasSize(1));
		assertThat(first.get(0), not(equalTo(second.get(0))));
		assertThat(first, not(hasItem(PARACETAMOL_RETIRED)));
	}
	
	@Test
	public void count_byName_countsOnlyNonRetiredMatchingDrugs() {
		assertThat(dao.getSearchResultsCount(nameSearch("Paracetamol")), equalTo(2));
		assertThat(dao.getSearchResultsCount(nameSearch("no-such-drug")), equalTo(0));
	}
	
	@Test
	public void search_byNullNameValue_isRejected() {
		SearchParameterMap params = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam());

		InvalidRequestException ex = assertThrows(InvalidRequestException.class, () -> dao.getSearchResults(params));
		assertThat(ex.getMessage(), equalTo("Name search parameter cannot be null"));
	}
	
	@Test
	public void count_byNullNameValue_isRejected() {
		SearchParameterMap params = new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER, new StringParam());

		assertThrows(InvalidRequestException.class, () -> dao.getSearchResultsCount(params));
	}
	
	@Test
	public void search_byBlankNameValue_isRejected() {
		InvalidRequestException ex = assertThrows(InvalidRequestException.class, () -> dao.getSearchResults(nameSearch("   ")));
		assertThat(ex.getMessage(), equalTo("Name search parameter cannot be empty"));
	}
	
	@Test
	public void count_byBlankNameValue_isRejected() {
		assertThrows(InvalidRequestException.class, () -> dao.getSearchResultsCount(nameSearch("")));
	}
}
