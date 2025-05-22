package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.api.PatchTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2AddlExtension.api.search.param.BahmniConditionSearchParams;
import org.bahmni.module.fhir2AddlExtension.api.service.FhirEncounterDiagnosisService;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.fhir2.api.dao.FhirConditionDao;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.search.param.ConditionSearchParams;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.translators.ConditionTranslator;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirConditionServiceImplTest {
	
	private static final String CONDITION_UUID = "249b9094-b812-4b0c-a204-0052a05c657f";
	
	@Mock
	private ConditionTranslator<org.openmrs.Condition> translator;
	
	@Mock
	private FhirConditionDao dao;
	
	@Mock
	private FhirEncounterDiagnosisService encounterDiagnosisService;
	
	@Mock
	private SearchQueryInclude<Condition> searchQueryInclude;
	
	@Mock
	private SearchQuery<org.openmrs.Condition, Condition, FhirConditionDao, ConditionTranslator<org.openmrs.Condition>, SearchQueryInclude<Condition>> searchQuery;
	
	private BahmniFhirConditionServiceImpl conditionService;
	
	private Condition fhirCondition;
	
	private org.openmrs.Condition openmrsCondition;
	
	@Before
	public void setUp() {
		conditionService = new BahmniFhirConditionServiceImpl();
		
		conditionService.setDao(dao);
		conditionService.setTranslator(translator);
		conditionService.setEncounterDiagnosisService(encounterDiagnosisService);
		conditionService.setSearchQuery(searchQuery);
		conditionService.setSearchQueryInclude(searchQueryInclude);
		
		openmrsCondition = new org.openmrs.Condition();
		openmrsCondition.setUuid(CONDITION_UUID);
		
		fhirCondition = new Condition();
		fhirCondition.setId(CONDITION_UUID);
	}
	
	@Test
	public void shouldRetrieveConditionByUUID() {
		when(dao.get(CONDITION_UUID)).thenReturn(openmrsCondition);
		when(translator.toFhirResource(openmrsCondition)).thenReturn(fhirCondition);
		
		Condition result = conditionService.get(CONDITION_UUID);
		
		assertThat(result, notNullValue());
		assertThat(result.getId(), equalTo(CONDITION_UUID));
	}
	
	@Test
	public void shouldRetrieveConditionFromEncounterDiagnosisServiceWhenNotFoundInDao() {
		when(dao.get(CONDITION_UUID)).thenThrow(new ResourceNotFoundException("Not found"));
		when(encounterDiagnosisService.get(CONDITION_UUID)).thenReturn(fhirCondition);
		
		Condition result = conditionService.get(CONDITION_UUID);
		
		assertThat(result, notNullValue());
		assertThat(result.getId(), equalTo(CONDITION_UUID));
	}
	
	@Ignore("Find out a way to assert super method is called")
	@Test
	public void shouldCreateConditionUsingParentClassWhenCategoryIsProblemListItem() {
		// Create a condition with problem-list-item category
		Condition condition = createConditionWithCategory(BahmniFhirConstants.HL7_CONDITION_CATEGORY_CONDITION_CODE);
		
		// Mock the parent class behavior
		when(dao.createOrUpdate(any())).thenReturn(openmrsCondition);
		when(translator.toFhirResource(any())).thenReturn(condition);
		when(translator.toOpenmrsType(any())).thenReturn(openmrsCondition);
		
		Condition result = conditionService.create(condition);
		
		assertThat(result, notNullValue());
		assertEquals(condition, result);
	}
	
	@Test
	public void shouldCreateConditionUsingEncounterDiagnosisServiceWhenCategoryIsEncounterDiagnosis() {
		// Create a condition with encounter-diagnosis category
		Condition condition = createConditionWithCategory(BahmniFhirConstants.HL7_CONDITION_CATEGORY_DIAGNOSIS_CODE);
		
		// Mock the encounterDiagnosisService behavior
		when(encounterDiagnosisService.create(condition)).thenReturn(condition);
		
		Condition result = conditionService.create(condition);
		
		assertThat(result, notNullValue());
		assertEquals(condition, result);
		
		// Verify that encounterDiagnosisService.create was called with the condition
		verify(encounterDiagnosisService).create(condition);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void shouldThrowInvalidRequestExceptionWhenCreatingConditionWithInvalidCategory() {
		// Create a condition with an invalid category
		Condition condition = createConditionWithCategory("invalid-category");
		
		conditionService.create(condition);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void shouldThrowNotImplementedExceptionWhenCreatingConditionWithNullCategory() {
		// Create a condition with null category
		Condition condition = new Condition();
		
		conditionService.create(condition);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void shouldThrowNotImplementedExceptionWhenCreatingConditionWithMultipleCategories() {
		// Create a condition with multiple categories
		Condition condition = new Condition();
		condition.addCategory(new CodeableConcept().addCoding(new Coding().setCode("category1")));
		condition.addCategory(new CodeableConcept().addCoding(new Coding().setCode("category2")));
		
		conditionService.create(condition);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void shouldThrowNotImplementedExceptionWhenCreatingConditionWithEmptyCategory() {
		// Create a condition with empty category code
		Condition condition = new Condition();
		condition.addCategory(new CodeableConcept().addCoding(new Coding().setCode("")));
		
		conditionService.create(condition);
	}
	
	@Test(expected = MethodNotAllowedException.class)
	public void shouldThrowNotImplementedExceptionWhenUpdatingCondition() {
		conditionService.update(CONDITION_UUID, fhirCondition);
	}
	
	@Test(expected = MethodNotAllowedException.class)
	public void shouldThrowNotImplementedExceptionWhenPatchingCondition() {
		conditionService.patch(CONDITION_UUID, PatchTypeEnum.JSON_PATCH, "{}", null);
	}
	
	@Test(expected = MethodNotAllowedException.class)
	public void shouldThrowNotImplementedExceptionWhenDeletingCondition() {
		conditionService.delete(CONDITION_UUID);
	}
	
	@Test
	public void shouldSearchConditionsUsingSearchQueryWhenCategoryIsProblemListItem() {
		// Create search params with problem-list-item category
		BahmniConditionSearchParams searchParams = new BahmniConditionSearchParams();
		searchParams.setCategory(new StringParam(BahmniFhirConstants.HL7_CONDITION_CATEGORY_CONDITION_CODE));
		
		// Mock the searchQuery behavior
		SearchParameterMap parameterMap = searchParams.toSearchParameterMap();
		when(searchQuery.getQueryResults(eq(parameterMap), eq(dao), eq(translator), eq(searchQueryInclude))).thenReturn(
		    mock(IBundleProvider.class));
		
		IBundleProvider result = conditionService.searchConditions(searchParams);
		
		assertThat(result, notNullValue());
		
		// Verify that searchQuery.getQueryResults was called with the correct parameters
		verify(searchQuery).getQueryResults(any(SearchParameterMap.class), eq(dao), eq(translator), eq(searchQueryInclude));
	}
	
	@Test
	public void shouldSearchConditionsUsingEncounterDiagnosisServiceWhenCategoryIsEncounterDiagnosis() {
		// Create search params with encounter-diagnosis category
		BahmniConditionSearchParams searchParams = new BahmniConditionSearchParams();
		searchParams.setCategory(new StringParam(BahmniFhirConstants.HL7_CONDITION_CATEGORY_DIAGNOSIS_CODE));
		
		// Mock the encounterDiagnosisService behavior
		SearchParameterMap parameterMap = searchParams.toSearchParameterMap();
		when(encounterDiagnosisService.searchForDiagnosis(parameterMap)).thenReturn(mock(IBundleProvider.class));
		
		IBundleProvider result = conditionService.searchConditions(searchParams);
		
		assertThat(result, notNullValue());
		
		// Verify that encounterDiagnosisService.searchForDiagnosis was called with the correct parameters
		verify(encounterDiagnosisService).searchForDiagnosis(any(SearchParameterMap.class));
	}
	
	@Test(expected = InvalidRequestException.class)
	public void shouldThrowInvalidRequestExceptionWhenSearchingConditionsWithInvalidCategory() {
		// Create search params with an invalid category
		BahmniConditionSearchParams searchParams = new BahmniConditionSearchParams();
		searchParams.setCategory(new StringParam("invalid-category"));
		
		conditionService.searchConditions(searchParams);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void shouldThrowInvalidRequestExceptionWhenSearchingConditionsWithoutCategory() {
		conditionService.searchConditions(new ConditionSearchParams());
	}
	
	// Helper method to create a condition with a specific category
	private Condition createConditionWithCategory(String categoryCode) {
		Condition condition = new Condition();
		CodeableConcept category = new CodeableConcept();
		Coding coding = new Coding();
		coding.setSystem(BahmniFhirConstants.HL7_CONDITION_CATEGORY_CODE_SYSTEM);
		coding.setCode(categoryCode);
		category.addCoding(coding);
		condition.addCategory(category);
		return condition;
	}
	
	// Helper method to create a mock IBundleProvider
	private IBundleProvider mock(Class<IBundleProvider> clazz) {
		return org.mockito.Mockito.mock(clazz);
	}
}
