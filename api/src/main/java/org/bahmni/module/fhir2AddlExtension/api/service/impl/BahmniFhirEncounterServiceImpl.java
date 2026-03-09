package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirEpisodeOfCareDao;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.module.episodes.Episode;
import org.openmrs.module.fhir2.api.dao.FhirVisitDao;
import org.openmrs.module.fhir2.api.impl.FhirEncounterServiceImpl;
import org.openmrs.module.fhir2.api.util.FhirUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;

@Component
@Primary
public class BahmniFhirEncounterServiceImpl extends FhirEncounterServiceImpl {
	
	public static final String INVALID_EOC_REFERENCE_FOR_ENCOUNTER = "Invalid episode-of-care reference for encounter";
	
	@Getter(value = AccessLevel.PROTECTED)
	@Setter(value = AccessLevel.PACKAGE, onMethod_ = @Autowired)
	private BahmniFhirEpisodeOfCareDao episodeOfCareDao;
	
	@Getter(value = AccessLevel.PROTECTED)
	@Setter(value = AccessLevel.PACKAGE, onMethod_ = @Autowired)
	private FhirVisitDao visitDao;
	
	private static final String EMPTY_STRING = "";
	
	@Override
	public Encounter create(@Nonnull Encounter encounter) {
		Encounter fhirEncounter = super.create(encounter);
		FhirUtils.OpenmrsEncounterType encounterType = FhirUtils.getOpenmrsEncounterType(encounter).orElse(null);
		
		String episodeUuid = EMPTY_STRING;
		Reference episodeRef = null;
		if (encounter.hasEpisodeOfCare()) {
			episodeRef = encounter.getEpisodeOfCare().get(0);
			episodeUuid = FhirUtils.referenceToId(episodeRef.getReference()).orElse(EMPTY_STRING);
		}
		
		if (!StringUtils.isEmpty(episodeUuid)) {
			Episode episode = episodeOfCareDao.get(episodeUuid);
			if (episode == null) {
				log.error(INVALID_EOC_REFERENCE_FOR_ENCOUNTER);
				throw new InvalidRequestException(INVALID_EOC_REFERENCE_FOR_ENCOUNTER);
			}
			
			if (encounterType == FhirUtils.OpenmrsEncounterType.ENCOUNTER) {
				org.openmrs.Encounter openmrsEncounter = getDao().get(fhirEncounter.getId());
				episode.addEncounter(openmrsEncounter);
				episodeOfCareDao.createOrUpdate(episode);
				fhirEncounter.getEpisodeOfCare().add(episodeRef);
			} else {
				//TBD
				//Visit visit = visitDao.get(fhirEncounter.getId());
			}
		}
		return fhirEncounter;
	}
	
}
