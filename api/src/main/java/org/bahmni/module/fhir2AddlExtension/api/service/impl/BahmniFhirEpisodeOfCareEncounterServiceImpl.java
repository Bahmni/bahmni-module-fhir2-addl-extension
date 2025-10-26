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
	
	@Autowired
	public BahmniFhirEpisodeOfCareEncounterServiceImpl(BahmniEpisodeOfCareEncounterDao episodeOfCareEncounterDao,
	    EncounterTranslator<org.openmrs.Encounter> encounterTranslator) {
		this.episodeOfCareEncounterDao = episodeOfCareEncounterDao;
		this.encounterTranslator = encounterTranslator;
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
			List<Encounter> fhirEncounters = encounterTranslator.toFhirResources(encounters);
			fhirEncounters.forEach(encounter -> {
				Reference reference = new Reference();
				reference.setReference("EpisodeOfCare/".concat(episodeUuid));
				encounter.setEpisodeOfCare(Collections.singletonList(reference));
			});
			allFhirEncounterResources.addAll(fhirEncounters);
		});

		BahmniSimpleBundleProvider simpleBundleProvider = new BahmniSimpleBundleProvider(allFhirEncounterResources);
		simpleBundleProvider.setPreferredPageSize(allFhirEncounterResources.size());
		return simpleBundleProvider;
	}
}
