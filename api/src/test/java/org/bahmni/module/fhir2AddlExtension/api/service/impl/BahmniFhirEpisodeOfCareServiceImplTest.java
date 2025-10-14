package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirEpisodeOfCareDao;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirEpisodeOfCareService;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniEpisodeOfCareTranslator;
import org.hl7.fhir.r4.model.EpisodeOfCare;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.episodes.Episode;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirEpisodeOfCareServiceImplTest {
	
	private BahmniFhirEpisodeOfCareService episodeOfCareService;
	
	@Mock
	private BahmniFhirEpisodeOfCareDao fhirEpisodeOfCareDao;
	
	@Mock
	private BahmniEpisodeOfCareTranslator episodeTranslator;
	
	@Mock
	private SearchQueryInclude<EpisodeOfCare> searchQueryInclude;
	
	@Mock
	private SearchQuery<Episode, EpisodeOfCare, BahmniFhirEpisodeOfCareDao, BahmniEpisodeOfCareTranslator, SearchQueryInclude<EpisodeOfCare>> searchQuery;
	
	@Before
	public void setup() {
		episodeOfCareService = new BahmniFhirEpisodeOfCareServiceImpl(fhirEpisodeOfCareDao, episodeTranslator,
		        searchQueryInclude, searchQuery);
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void shouldThrowErrorIfPatientReferenceIsNotProvided() {
		episodeOfCareService.episodesForPatient(new ReferenceAndListParam());
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void shouldThrowErrorIfPatientReferenceValueIsNotProvided() {
		ReferenceAndListParam listParam = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue("").setChain(EpisodeOfCare.SP_PATIENT)));
		episodeOfCareService.episodesForPatient(listParam);
	}
	
}
