package org.bahmni.module.fhir2AddlExtension.api.translator;

import org.hl7.fhir.r4.model.EpisodeOfCare;
import org.openmrs.module.episodes.Episode;
import org.openmrs.module.episodes.EpisodeStatusHistory;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirUpdatableTranslator;

import java.util.Date;

public interface BahmniEpisodeOfCareTranslator extends OpenmrsFhirTranslator<Episode, EpisodeOfCare>, OpenmrsFhirUpdatableTranslator<Episode, EpisodeOfCare> {
	EpisodeStatusHistory toOpenmrsEpisodeStatusHistory(EpisodeOfCare episodeOfCare, Date defaultStartDate);
}
