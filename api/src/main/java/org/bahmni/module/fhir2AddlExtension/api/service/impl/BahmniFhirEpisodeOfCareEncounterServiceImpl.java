package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniEpisodeOfCareEncounterDao;
import org.bahmni.module.fhir2AddlExtension.api.providers.BahmniSimpleBundleProvider;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirEpisodeOfCareEncounterService;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.module.fhir2.api.translators.EncounterTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Transactional
@Slf4j
public class BahmniFhirEpisodeOfCareEncounterServiceImpl implements BahmniFhirEpisodeOfCareEncounterService {
	
	private final BahmniEpisodeOfCareEncounterDao episodeOfCareEncounterDao;
	
	private final EncounterTranslator<org.openmrs.Encounter> encounterTranslator;
	
	private final EncounterTranslator<org.openmrs.Visit> visitTranslator;
	
	@Autowired
	public BahmniFhirEpisodeOfCareEncounterServiceImpl(BahmniEpisodeOfCareEncounterDao episodeOfCareEncounterDao,
	    EncounterTranslator<org.openmrs.Encounter> encounterTranslator,
	    EncounterTranslator<org.openmrs.Visit> visitTranslator) {
		this.episodeOfCareEncounterDao = episodeOfCareEncounterDao;
		this.encounterTranslator = encounterTranslator;
		this.visitTranslator = visitTranslator;
	}
	
	@Override
	public IBundleProvider encountersForEpisodes(ReferenceAndListParam episodesReference) {
		List<ReferenceOrListParam> refOrListParams = episodesReference.getValuesAsQueryTokens();
		if (refOrListParams.isEmpty()) {
			return new BahmniSimpleBundleProvider(Collections.EMPTY_LIST);
		}

		List<ReferenceParam> referenceParams = refOrListParams.get(0).getValuesAsQueryTokens();
		List<String> episodeUuids = new ArrayList<>();
		referenceParams.forEach(referenceParam -> episodeUuids.add(referenceParam.getValue()));
		if (episodeUuids.isEmpty()) {
			return new BahmniSimpleBundleProvider(Collections.EMPTY_LIST);
		}
		Map<String, List<org.openmrs.Encounter>> episodeEncounters = episodeOfCareEncounterDao.getEncountersForEpisodes(episodeUuids);
		List<Encounter> allFhirEncounterResources = new ArrayList<>();
		episodeEncounters.forEach((episodeUuid, encounters) -> {
			List<Encounter> fhirEncounters = mapFhirResourcesFromEncounters(episodeUuid, encounters);
			allFhirEncounterResources.addAll(fhirEncounters);
		});

		Map<String, List<org.openmrs.Visit>> episodeVisits = episodeOfCareEncounterDao.getVisitsForEpisodes(episodeUuids);
		episodeVisits.forEach((episodeUuid, visits) -> {
			List<Encounter> fhirEncounters = mapFhirResourcesFromVisits(episodeUuid, visits);
			allFhirEncounterResources.addAll(fhirEncounters);
		});

		BahmniSimpleBundleProvider bundleProvider = new BahmniSimpleBundleProvider(allFhirEncounterResources);
		bundleProvider.setPreferredPageSize(allFhirEncounterResources.size());
		return bundleProvider;
	}
	
	private List<Encounter> mapFhirResourcesFromEncounters(String episodeUuid, List<org.openmrs.Encounter> encounters) {
		List<Encounter> fhirEncounters = encounterTranslator.toFhirResources(encounters);
		fhirEncounters.forEach(encounter -> {
			Reference reference = new Reference();
			reference.setReference("EpisodeOfCare/".concat(episodeUuid));
			encounter.setEpisodeOfCare(Collections.singletonList(reference));
		});
		return fhirEncounters;
	}
	
	private List<Encounter> mapFhirResourcesFromVisits(String episodeUuid, List<org.openmrs.Visit> visits) {
		List<Encounter> fhirEncounters = visitTranslator.toFhirResources(visits);
		fhirEncounters.forEach(encounter -> {
			Reference reference = new Reference();
			reference.setReference("EpisodeOfCare/".concat(episodeUuid));
			encounter.setEpisodeOfCare(Collections.singletonList(reference));
		});
		return fhirEncounters;
	}
}
