package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirImagingStudyDao;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirImagingStudy;
import org.bahmni.module.fhir2AddlExtension.api.search.param.BahmniImagingStudySearchParams;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirImagingStudyService;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniFhirImagingStudyTranslator;
import org.hl7.fhir.r4.model.ImagingStudy;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.impl.BaseFhirService;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Slf4j
public class BahmniFhirImagingStudyServiceImpl extends BaseFhirService<ImagingStudy, FhirImagingStudy> implements BahmniFhirImagingStudyService {
	
	private final BahmniFhirImagingStudyDao imagingStudyDao;
	
	private final BahmniFhirImagingStudyTranslator imagingStudyTranslator;
	
	private final SearchQueryInclude<ImagingStudy> searchQueryInclude;
	
	private final SearchQuery<FhirImagingStudy, ImagingStudy, BahmniFhirImagingStudyDao, BahmniFhirImagingStudyTranslator, SearchQueryInclude<ImagingStudy>> searchQuery;
	
	@Autowired
	public BahmniFhirImagingStudyServiceImpl(
	    BahmniFhirImagingStudyDao imagingStudyDao,
	    BahmniFhirImagingStudyTranslator imagingStudyTranslator,
	    SearchQueryInclude<ImagingStudy> searchQueryInclude,
	    SearchQuery<FhirImagingStudy, ImagingStudy, BahmniFhirImagingStudyDao, BahmniFhirImagingStudyTranslator, SearchQueryInclude<ImagingStudy>> searchQuery) {
		this.imagingStudyDao = imagingStudyDao;
		this.imagingStudyTranslator = imagingStudyTranslator;
		this.searchQueryInclude = searchQueryInclude;
		this.searchQuery = searchQuery;
	}
	
	@Override
	protected FhirDao<FhirImagingStudy> getDao() {
		return imagingStudyDao;
	}
	
	@Override
	protected OpenmrsFhirTranslator<FhirImagingStudy, ImagingStudy> getTranslator() {
		return imagingStudyTranslator;
	}
	
	@Override
	@Transactional(readOnly = true)
	public IBundleProvider searchImagingStudy(BahmniImagingStudySearchParams searchParams) {
		if (!searchParams.hasPatientReference() && !searchParams.hasId() && !searchParams.hasBasedOnReference()) {
			log.error("Missing patient reference, resource id or basedOn reference for ImagingStudy search");
			throw new UnsupportedOperationException(
			        "You must specify patient reference or resource _id or basedOn reference!");
		}
		return searchQuery.getQueryResults(searchParams.toSearchParameterMap(), imagingStudyDao, imagingStudyTranslator,
		    searchQueryInclude);
	}
	
}
