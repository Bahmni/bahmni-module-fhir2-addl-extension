package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.api.PatchTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2AddlExtension.api.search.param.BahmniConditionSearchParams;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirConditionService;
import org.bahmni.module.fhir2AddlExtension.api.service.FhirEncounterDiagnosisService;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Condition;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.FhirGlobalPropertyService;
import org.openmrs.module.fhir2.api.dao.FhirConditionDao;
import org.openmrs.module.fhir2.api.impl.BaseFhirService;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.search.TwoSearchQueryBundleProvider;
import org.openmrs.module.fhir2.api.search.param.ConditionSearchParams;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.translators.ConditionTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.List;

@Component
@Primary
public class BahmniFhirConditionServiceImpl extends BaseFhirService<Condition, org.openmrs.Condition> implements BahmniFhirConditionService {
	
	@Getter(value = AccessLevel.PROTECTED)
	@Setter(value = AccessLevel.PACKAGE, onMethod_ = @Autowired)
	private ConditionTranslator<org.openmrs.Condition> translator;
	
	@Getter(value = AccessLevel.PROTECTED)
	@Setter(value = AccessLevel.PACKAGE, onMethod_ = @Autowired)
	private FhirConditionDao dao;
	
	@Setter(value = AccessLevel.PACKAGE, onMethod_ = @Autowired)
	private FhirEncounterDiagnosisService encounterDiagnosisService;
	
	@Setter(value = AccessLevel.PACKAGE, onMethod_ = @Autowired)
	private SearchQueryInclude<Condition> searchQueryInclude;
	
	@Getter(value = AccessLevel.PROTECTED)
	@Setter(value = AccessLevel.PACKAGE, onMethod_ = @Autowired)
	private FhirGlobalPropertyService globalPropertyService;
	
	@Setter(value = AccessLevel.PACKAGE, onMethod_ = @Autowired)
	private SearchQuery<org.openmrs.Condition, Condition, FhirConditionDao, ConditionTranslator<org.openmrs.Condition>, SearchQueryInclude<Condition>> searchQuery;
	
	@Override
	public org.hl7.fhir.r4.model.Condition get(@Nonnull String uuid) {
		Condition result;
		try {
			result = super.get(uuid);
		}
		catch (ResourceNotFoundException e) {
			result = encounterDiagnosisService.get(uuid);
		}
		return result;
	}
	
	@Override
	public Condition create(@Nonnull Condition condition) {
		String category = getCategory(condition);
		if (category.equals(BahmniFhirConstants.HL7_CONDITION_CATEGORY_CONDITION_CODE))
			return super.create(condition);
		else if (category.equals(BahmniFhirConstants.HL7_CONDITION_CATEGORY_DIAGNOSIS_CODE))
			return encounterDiagnosisService.create(condition);
		else
			throw new InvalidRequestException("Invalid type of Condition Category: " + category);
	}
	
	@Override
	public Condition update(@Nonnull String uuid, @Nonnull Condition updatedResource) {
		throw new MethodNotAllowedException("Update not supported");
	}
	
	@Override
	public Condition patch(@Nonnull String uuid, @Nonnull PatchTypeEnum patchType, @Nonnull String body,
	        RequestDetails requestDetails) {
		throw new MethodNotAllowedException("Patch not supported");
	}
	
	@Override
	public void delete(@Nonnull String uuid) {
		throw new MethodNotAllowedException("Delete not supported");
	}
	
	@Override
	public IBundleProvider searchConditions(BahmniConditionSearchParams conditionSearchParams) {
		SearchParameterMap searchParameterMap = conditionSearchParams.toSearchParameterMap();
		IBundleProvider diagnosisBundle = null;
		IBundleProvider conditionBundle = null;
		
		if (shouldSearchExplicitlyFor(conditionSearchParams.getCategory(), FhirConstants.CONDITION_CATEGORY_CODE_DIAGNOSIS)) {
			diagnosisBundle = encounterDiagnosisService.searchForDiagnosis(searchParameterMap);
		}
		
		if (shouldSearchExplicitlyFor(conditionSearchParams.getCategory(), FhirConstants.CONDITION_CATEGORY_CODE_CONDITION)) {
			conditionBundle = searchQuery.getQueryResults(searchParameterMap, dao, translator, searchQueryInclude);
		}
		
		if (conditionBundle != null && diagnosisBundle != null) {
			return new TwoSearchQueryBundleProvider(diagnosisBundle, conditionBundle, globalPropertyService);
		} else if (conditionBundle == null && diagnosisBundle != null) {
			return diagnosisBundle;
		}
		
		return conditionBundle == null ? new SimpleBundleProvider() : conditionBundle;
		
		//		String category = categoryParam.getValue();
		//		if (category.equals(BahmniFhirConstants.HL7_CONDITION_CATEGORY_CONDITION_CODE)) {
		//			return searchQuery.getQueryResults(searchParameterMap, dao, translator, searchQueryInclude);
		//		} else if (category.equals(BahmniFhirConstants.HL7_CONDITION_CATEGORY_DIAGNOSIS_CODE)) {
		//			return encounterDiagnosisService.searchForDiagnosis(searchParameterMap);
		//		} else {
		//			throw new InvalidRequestException("Unknown condition category: " + category);
		//		}
		
	}
	
	private boolean shouldSearchExplicitlyFor(TokenAndListParam tokenAndListParam, @Nonnull String valueToCheck) {
		if (tokenAndListParam == null || tokenAndListParam.size() == 0 || valueToCheck.isEmpty()) {
			return true;
		}
		
		for (TokenOrListParam orList : tokenAndListParam.getValuesAsQueryTokens()) {
			for (TokenParam tp : orList.getValuesAsQueryTokens()) {
				String sys = tp.getSystem();
				String code = tp.getValue();
				if (sys != null && !sys.isEmpty() && FhirConstants.CONDITION_CATEGORY_SYSTEM_URI.equals(sys)
				        && valueToCheck.equals(code)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	@Override
	public IBundleProvider searchConditions(ConditionSearchParams conditionSearchParams) {
		throw new InvalidRequestException("Search on Condition resource without category is not allowed");
	}
	
	private String getCategory(Condition condition) {
		List<CodeableConcept> conditionCategoryList = condition.getCategory();
		if (conditionCategoryList == null || conditionCategoryList.size() != 1) {
			throw new InvalidRequestException("Unable to determine the category of condition resource");
		}
		String category = conditionCategoryList.get(0).getCodingFirstRep().getCode();
		if (category == null || category.isEmpty()) {
			throw new InvalidRequestException("Unable to determine the category of condition resource");
		}
		return category;
	}
}
