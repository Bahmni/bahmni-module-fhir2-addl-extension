package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.StringParam;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirMedicationService;
import org.hl7.fhir.r4.model.Medication;
import org.openmrs.Drug;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.FhirMedicationDao;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.translators.MedicationTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BahmniFhirMedicationServiceImpl implements BahmniFhirMedicationService {
	
	@Getter(value = AccessLevel.PROTECTED)
	@Setter(value = AccessLevel.PACKAGE, onMethod_ = @Autowired)
	private FhirMedicationDao dao;
	
	@Getter(value = AccessLevel.PROTECTED)
	@Setter(value = AccessLevel.PACKAGE, onMethod_ = @Autowired)
	private MedicationTranslator translator;
	
	@Getter(value = AccessLevel.PROTECTED)
	@Setter(value = AccessLevel.PACKAGE, onMethod_ = @Autowired)
	private SearchQueryInclude<Medication> searchQueryInclude;
	
	@Getter(value = AccessLevel.PROTECTED)
	@Setter(value = AccessLevel.PACKAGE, onMethod_ = @Autowired)
	private SearchQuery<Drug, Medication, FhirMedicationDao, MedicationTranslator, SearchQueryInclude<Medication>> searchQuery;
	
	@Override
	public IBundleProvider searchMedicationsByName(StringParam nameParam) {
		return searchQuery.getQueryResults(
		    new SearchParameterMap().addParameter(FhirConstants.NAME_SEARCH_HANDLER, nameParam), dao, translator,
		    searchQueryInclude);
	}
}
