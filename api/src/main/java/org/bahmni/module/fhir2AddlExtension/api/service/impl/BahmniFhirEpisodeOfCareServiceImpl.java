package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.api.PatchTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirEpisodeOfCareDao;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirEpisodeOfCareService;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniEpisodeOfCareTranslator;
import org.hl7.fhir.r4.model.EpisodeOfCare;
import org.openmrs.module.episodes.Episode;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.impl.BaseFhirService;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

import static org.openmrs.module.fhir2.FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER;

@Component
@Primary
@Transactional
public class BahmniFhirEpisodeOfCareServiceImpl extends BaseFhirService<EpisodeOfCare, Episode> implements BahmniFhirEpisodeOfCareService {
	
	private BahmniFhirEpisodeOfCareDao fhirEpisodeOfCareDao;
	
	private BahmniEpisodeOfCareTranslator episodeTranslator;
	
	private SearchQueryInclude<EpisodeOfCare> searchQueryInclude;
	
	private SearchQuery<org.openmrs.module.episodes.Episode, EpisodeOfCare, BahmniFhirEpisodeOfCareDao, BahmniEpisodeOfCareTranslator, SearchQueryInclude<EpisodeOfCare>> searchQuery;
	
	@Autowired
	public BahmniFhirEpisodeOfCareServiceImpl(
	    BahmniFhirEpisodeOfCareDao fhirEpisodeOfCareDao,
	    BahmniEpisodeOfCareTranslator episodeTranslator,
	    SearchQueryInclude<EpisodeOfCare> searchQueryInclude,
	    SearchQuery<Episode, EpisodeOfCare, BahmniFhirEpisodeOfCareDao, BahmniEpisodeOfCareTranslator, SearchQueryInclude<EpisodeOfCare>> searchQuery) {
		this.fhirEpisodeOfCareDao = fhirEpisodeOfCareDao;
		this.episodeTranslator = episodeTranslator;
		this.searchQueryInclude = searchQueryInclude;
		this.searchQuery = searchQuery;
	}
	
	@Override
	public EpisodeOfCare get(@Nonnull String s) {
		return super.get(s);
	}
	
	@Override
	public List<EpisodeOfCare> get(@Nonnull Collection<String> collection) {
		return super.get(collection);
	}
	
	@Override
	public EpisodeOfCare create(@Nonnull EpisodeOfCare episodeOfCare) {
		return super.create(episodeOfCare);
	}
	
	@Override
	public EpisodeOfCare update(@Nonnull String uuid, @Nonnull EpisodeOfCare episodeOfCare) {
		return super.update(uuid, episodeOfCare);
	}
	
	@Override
	public EpisodeOfCare patch(@Nonnull String uuid, @Nonnull PatchTypeEnum patchType, @Nonnull String body,
	        RequestDetails requestDetails) {
		return super.patch(uuid, patchType, body, requestDetails);
		//throw new UnsupportedOperationException("Patch Operation on FHIR resource EpisodeOfCare is not supported yet!");
	}
	
	@Override
	public void delete(@Nonnull String uuid) {
		super.delete(uuid);
	}
	
	@Override
	protected FhirDao<Episode> getDao() {
		return fhirEpisodeOfCareDao;
	}
	
	@Override
	protected OpenmrsFhirTranslator<Episode, EpisodeOfCare> getTranslator() {
		return episodeTranslator;
	}
	
	@Override
	public IBundleProvider episodesForPatient(ReferenceAndListParam patientReference) {
		//TODO: check the reference is not empty. No need to make unncessary db query
		//throw new UnsupportedOperationException("You must specify patient identifier or reference!");
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(PATIENT_REFERENCE_SEARCH_HANDLER, patientReference);
		return searchQuery.getQueryResults(params, fhirEpisodeOfCareDao, episodeTranslator, searchQueryInclude);
	}
}
