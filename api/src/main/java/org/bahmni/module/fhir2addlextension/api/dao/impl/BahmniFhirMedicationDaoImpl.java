package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import org.bahmni.module.fhir2addlextension.api.dao.SystemOnlyCodeQuery;
import org.openmrs.module.fhir2.api.dao.internals.OpenmrsFhirCriteriaContext;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
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

import javax.annotation.Nonnull;
import javax.persistence.criteria.Join;
import java.util.List;

@Component
@Primary
public class BahmniFhirMedicationDaoImpl extends FhirMedicationDaoImpl implements FhirMedicationDao {
	
	@Autowired
	private ConceptService conceptService;
	
	@Override
	protected <U> void setupSearchParams(OpenmrsFhirCriteriaContext<Drug, U> ctx, SearchParameterMap theParams) {
		List<? extends PropParam<?>> codedParams = theParams.getParameters(FhirConstants.CODED_SEARCH_HANDLER);
		if (codedParams.stream().noneMatch(p -> SystemOnlyCodeQuery.hasSystemOnlyToken((TokenAndListParam) p.getParam()))) {
			super.setupSearchParams(ctx, theParams);
			return;
		}
		SearchParameterMap rest = new SearchParameterMap(theParams.getSortSpec(), theParams.getFromIndex(),
		        theParams.getToIndex());
		theParams.getParameters().stream().filter(e -> !FhirConstants.CODED_SEARCH_HANDLER.equals(e.getKey()))
		        .forEach(e -> e.getValue().forEach(p -> rest.addParameter(e.getKey(), p.getPropertyName(), p.getParam())));
		super.setupSearchParams(ctx, rest);
		codedParams.forEach(p -> handleCode(ctx, (TokenAndListParam) p.getParam()));
	}
	
	private <U> void handleCode(OpenmrsFhirCriteriaContext<Drug, U> ctx, TokenAndListParam code) {
		Join<?, ?> cJoin = ctx.getJoin("c").orElseGet(() -> ctx.addJoin("concept", "c"));
		Join<?, ?> cmJoin = ctx.getJoin("ccm").orElseGet(() -> ctx.addJoin(cJoin, "conceptMappings", "ccm"));
		Join<?, ?> crtJoin = ctx.getJoin("ccrt").orElseGet(() -> ctx.addJoin(cmJoin, "conceptReferenceTerm", "ccrt"));
		handleAndListParam(ctx.getCriteriaBuilder(), code,
		    token -> SystemOnlyCodeQuery.tokenPredicate(ctx, getSearchQueryHelper(), crtJoin, "ccrt", token))
		        .ifPresent(ctx::addPredicate);
	}
	
	@Override
	public List<Drug> getSearchResults(@Nonnull SearchParameterMap theParams) {
		List<? extends PropParam<?>> nameSearchParams = theParams.getParameters(FhirConstants.NAME_SEARCH_HANDLER);
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
		List<? extends PropParam<?>> nameSearchParams = theParams.getParameters(FhirConstants.NAME_SEARCH_HANDLER);
		if (nameSearchParams.isEmpty())
			return super.getSearchResultsCount(theParams);
		String searchPhrase = getSearchPhrase(theParams);
		return conceptService.getCountOfDrugs(searchPhrase, null, true, true, false);
	}
	
	private String getSearchPhrase(@Nonnull SearchParameterMap theParams) {
		List<? extends PropParam<?>> nameSearchParams = theParams.getParameters(FhirConstants.NAME_SEARCH_HANDLER);
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
}
