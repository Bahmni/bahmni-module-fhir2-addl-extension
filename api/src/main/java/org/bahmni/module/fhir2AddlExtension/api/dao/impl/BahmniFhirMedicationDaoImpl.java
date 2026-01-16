package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import org.bahmni.module.fhir2AddlExtension.api.utils.ModuleUtils;
import org.hibernate.criterion.Criterion;
import org.openmrs.Drug;
import org.openmrs.api.ConceptService;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.FhirMedicationDao;
import org.openmrs.module.fhir2.api.dao.impl.FhirMedicationDaoImpl;
import org.openmrs.module.fhir2.api.search.param.PropParam;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;

import javax.annotation.Nonnull;
import java.util.List;

@Component
@Primary
public class BahmniFhirMedicationDaoImpl extends FhirMedicationDaoImpl implements FhirMedicationDao {
	
	@Autowired
	private ConceptService conceptService;
	
	@Override
	public List<Drug> getSearchResults(@Nonnull SearchParameterMap theParams) {
		List<PropParam<?>> nameSearchParams = theParams.getParameters(FhirConstants.NAME_SEARCH_HANDLER);
		if (nameSearchParams.isEmpty())
			return super.getSearchResults(theParams);
		String searchPhrase = getSearchPhrase(theParams);
		Integer maxResults = null;
		if (theParams.getToIndex() != Integer.MAX_VALUE) {
			maxResults = theParams.getToIndex() - theParams.getFromIndex();
		}
		return conceptService.getDrugs(searchPhrase, null, true, true, false, theParams.getFromIndex(), maxResults);
	}
	
	@Override
	public int getSearchResultsCount(@Nonnull SearchParameterMap theParams) {
		List<PropParam<?>> nameSearchParams = theParams.getParameters(FhirConstants.NAME_SEARCH_HANDLER);
		if (nameSearchParams.isEmpty())
			return super.getSearchResultsCount(theParams);
		String searchPhrase = getSearchPhrase(theParams);
		return conceptService.getCountOfDrugs(searchPhrase, null, true, true, false);
	}
	
	private String getSearchPhrase(@Nonnull SearchParameterMap theParams) {
		List<PropParam<?>> nameSearchParams = theParams.getParameters(FhirConstants.NAME_SEARCH_HANDLER);
		if (nameSearchParams.isEmpty()) {
			return null;
		}
		StringParam nameSearchParam = (StringParam) nameSearchParams.get(0).getParam();
		if (nameSearchParam == null || nameSearchParam.getValue() == null) {
			throw new InvalidRequestException("Name search parameter cannot be null");
		}
		String searchPhrase = nameSearchParam.getValue().trim();
		if (searchPhrase.isEmpty()) {
			throw new InvalidRequestException("Name search parameter cannot be empty");
		}
		return searchPhrase;
	}
	
	@Override
	protected Criterion generateSystemQuery(String system, List<String> codes, String conceptReferenceTermAlias) {
		if (ModuleUtils.isConceptReferenceCodeEmpty(codes)) {
			return ModuleUtils.generateSystemQueryForEmptyCodes(system, conceptReferenceTermAlias);
		}
		return super.generateSystemQuery(system, codes, conceptReferenceTermAlias);
	}
}
