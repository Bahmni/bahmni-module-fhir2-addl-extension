package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniEpisodeOfCareEncounterDao;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirEpisodeOfCareEncounterService;
import org.hl7.fhir.r4.model.Encounter;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.translators.EncounterTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
@Slf4j
public class BahmniFhirEpisodeOfCareEncounterServiceImpl implements BahmniFhirEpisodeOfCareEncounterService {
	
	private final BahmniEpisodeOfCareEncounterDao episodeOfCareEncounterDao;
	
	private final EncounterTranslator<org.openmrs.Encounter> encounterTranslator;
	
	@Autowired
	public BahmniFhirEpisodeOfCareEncounterServiceImpl(BahmniEpisodeOfCareEncounterDao episodeOfCareEncounterDao,
	    EncounterTranslator<org.openmrs.Encounter> encounterTranslator) {
		this.episodeOfCareEncounterDao = episodeOfCareEncounterDao;
		this.encounterTranslator = encounterTranslator;
	}
	
	@Override
	public IBundleProvider encountersForEpisodes(ReferenceAndListParam episodesReference) {
		SearchParameterMap encounterSearchParams = new SearchParameterMap();
		encounterSearchParams.addParameter(BahmniFhirConstants.EPISODE_OF_CARE_REFERENCE_SEARCH_PARAM, episodesReference);
		List<org.openmrs.Encounter> searchResults = episodeOfCareEncounterDao.getSearchResults(encounterSearchParams);
		List<Encounter> encounters = encounterTranslator.toFhirResources(searchResults);
		return new SimpleBundleProvider(encounters);
	}
}
