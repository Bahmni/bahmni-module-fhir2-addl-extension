package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.api.PatchTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirEpisodeOfCareDao;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirEpisodeOfCareService;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniEpisodeOfCareTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.EpisodeOfCareStatusTranslator;
import org.hl7.fhir.r4.model.EpisodeOfCare;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.episodes.Episode;
import org.openmrs.module.episodes.EpisodeStatusHistory;
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
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.openmrs.module.fhir2.FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER;

@Component
@Primary
@Transactional
@Slf4j
public class BahmniFhirEpisodeOfCareServiceImpl extends BaseFhirService<EpisodeOfCare, Episode> implements BahmniFhirEpisodeOfCareService {
	
	public static final String PATIENT_IDENTIFIER_OR_REFERENCE_MUST_BE_SPECIFIED = "You must specify patient identifier or reference!";
	
	public static final String MISSING_PATIENT_IDENTIFIER_OR_ID = "Missing patient identifier or id";
	
	private final BahmniFhirEpisodeOfCareDao fhirEpisodeOfCareDao;
	
	private final BahmniEpisodeOfCareTranslator episodeTranslator;
	
	private final SearchQueryInclude<EpisodeOfCare> searchQueryInclude;
	
	private final EpisodeOfCareStatusTranslator statusTranslator;
	
	private final SearchQuery<org.openmrs.module.episodes.Episode, EpisodeOfCare, BahmniFhirEpisodeOfCareDao, BahmniEpisodeOfCareTranslator, SearchQueryInclude<EpisodeOfCare>> searchQuery;
	
	@Autowired
	public BahmniFhirEpisodeOfCareServiceImpl(
	    BahmniFhirEpisodeOfCareDao fhirEpisodeOfCareDao,
	    BahmniEpisodeOfCareTranslator episodeTranslator,
	    SearchQueryInclude<EpisodeOfCare> searchQueryInclude,
	    EpisodeOfCareStatusTranslator statusTranslator,
	    SearchQuery<Episode, EpisodeOfCare, BahmniFhirEpisodeOfCareDao, BahmniEpisodeOfCareTranslator, SearchQueryInclude<EpisodeOfCare>> searchQuery) {
		this.fhirEpisodeOfCareDao = fhirEpisodeOfCareDao;
		this.episodeTranslator = episodeTranslator;
		this.searchQueryInclude = searchQueryInclude;
		this.statusTranslator = statusTranslator;
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
		//throw new UnsupportedOperationException("Patch Operation on FHIR resource EpisodeOfCare is not supported yet!");
		return super.patch(uuid, patchType, body, requestDetails);
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
		if (patientReference.getValuesAsQueryTokens().isEmpty()) {
			logAndThrowUnsupportedExceptionForMissingPatientReference();
		}
		patientReference.getValuesAsQueryTokens().forEach(referenceOrListParam -> {
			if (referenceOrListParam.getValuesAsQueryTokens().isEmpty()) {
				logAndThrowUnsupportedExceptionForMissingPatientReference();
			}
			boolean match = referenceOrListParam.getValuesAsQueryTokens().stream().anyMatch(referenceParam -> StringUtils.isEmpty(referenceParam.getValue()));
			if (match) {
				logAndThrowUnsupportedExceptionForMissingPatientReference();
			}
		});
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(PATIENT_REFERENCE_SEARCH_HANDLER, patientReference);
		return searchQuery.getQueryResults(params, fhirEpisodeOfCareDao, episodeTranslator, searchQueryInclude);
	}
	
	private void logAndThrowUnsupportedExceptionForMissingPatientReference() {
		log.error(MISSING_PATIENT_IDENTIFIER_OR_ID);
		throw new UnsupportedOperationException(PATIENT_IDENTIFIER_OR_REFERENCE_MUST_BE_SPECIFIED);
	}
	
	/**
	 * Overriding because we want to setup the status history. Alternative would have been to do
	 * this in the episodeTranslator, which is not preferred as its core responsibilities should be
	 * just limited to translation. Unfortunately for create operation, we still need to depend on
	 * the translation to set history as there is no hook like applyUpdate below
	 * 
	 * @param existingObject existing openmrs episode
	 * @param updatedResource submitted episodeOfCare
	 * @return either new or mapped episodeOfCare
	 */
	@Override
	protected EpisodeOfCare applyUpdate(Episode existingObject, EpisodeOfCare updatedResource) {
		checkAndUpdateEpisodeStatusHistory(existingObject, updatedResource);
		return super.applyUpdate(existingObject, updatedResource);
	}
	
	protected void checkAndUpdateEpisodeStatusHistory(Episode existingEpisode, EpisodeOfCare episodeOfCare) {
		User authenticatedUser = Context.getUserContext().getAuthenticatedUser();
		if (!existingEpisode.hasStatusHistory()) {
			EpisodeStatusHistory episodeStatusHistory = episodeTranslator.toOpenmrsEpisodeStatusHistory(episodeOfCare,
			    existingEpisode.getDateStarted());
			episodeStatusHistory.setCreator(authenticatedUser);
			existingEpisode.addEpisodeStatusHistory(episodeStatusHistory);
			return;
		}
		Episode.Status status = statusTranslator.toOpenmrsType(episodeOfCare.getStatus());
		if (existingEpisode.getStatus().equals(status)) {
			return;
		}
		EpisodeStatusHistory lastHistory = identifyLastStatusHistory(existingEpisode);
		lastHistory.setDateEnded(new Date());
		//create a new history entry corresponding to current status
		EpisodeStatusHistory newStatusHistoryEntry = new EpisodeStatusHistory();
		newStatusHistoryEntry.setDateStarted(new Date());
		newStatusHistoryEntry.setStatus(status);
		newStatusHistoryEntry.setCreator(authenticatedUser);
		newStatusHistoryEntry.setDateCreated(new Date());
		existingEpisode.addEpisodeStatusHistory(newStatusHistoryEntry);
	}
	
	private EpisodeStatusHistory identifyLastStatusHistory(Episode episode) {
		ArrayList<EpisodeStatusHistory> existingHistoryList = new ArrayList<>(episode.getStatusHistory());
		List<EpisodeStatusHistory> sortedListDescending = existingHistoryList.stream()
				.sorted(Comparator.comparingInt(EpisodeStatusHistory::getId).reversed())
				.collect(Collectors.toList());
		return sortedListDescending.get(0);
	}
}
