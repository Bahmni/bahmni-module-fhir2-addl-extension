package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bahmni.module.fhir2AddlExtension.api.dao.FhirEncounterDiagnosisDao;
import org.bahmni.module.fhir2AddlExtension.api.service.FhirEncounterDiagnosisService;
import org.hl7.fhir.r4.model.Condition;
import org.openmrs.Diagnosis;
import org.openmrs.module.fhir2.api.impl.BaseFhirService;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.translators.ConditionTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FhirEncounterDiagnosisServiceImpl extends BaseFhirService<Condition, Diagnosis> implements FhirEncounterDiagnosisService {
	
	@Getter(AccessLevel.PROTECTED)
	@Setter(value = AccessLevel.PACKAGE, onMethod_ = @Autowired)
	private FhirEncounterDiagnosisDao dao;
	
	@Getter(AccessLevel.PROTECTED)
	@Setter(value = AccessLevel.PACKAGE, onMethod_ = @Autowired)
	private ConditionTranslator<Diagnosis> translator;
	
	@Setter(value = AccessLevel.PACKAGE, onMethod_ = @Autowired)
	private SearchQueryInclude<Condition> searchQueryInclude;
	
	@Setter(value = AccessLevel.PACKAGE, onMethod_ = @Autowired)
	private SearchQuery<org.openmrs.Diagnosis, Condition, FhirEncounterDiagnosisDao, ConditionTranslator<org.openmrs.Diagnosis>, SearchQueryInclude<Condition>> searchQuery;
	
	@Override
	public IBundleProvider searchForDiagnosis(SearchParameterMap searchParameterMap) {
		return searchQuery.getQueryResults(searchParameterMap, dao, translator, searchQueryInclude);
	}
}
