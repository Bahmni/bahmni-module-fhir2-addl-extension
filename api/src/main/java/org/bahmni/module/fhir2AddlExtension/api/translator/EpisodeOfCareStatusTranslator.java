package org.bahmni.module.fhir2AddlExtension.api.translator;

import org.hl7.fhir.r4.model.EpisodeOfCare;
import org.openmrs.module.episodes.Episode;

import javax.annotation.Nonnull;

public interface EpisodeOfCareStatusTranslator {
	
	EpisodeOfCare.EpisodeOfCareStatus toFhirType(@Nonnull Episode.Status status);
	
	Episode.Status toOpenmrsType(@Nonnull EpisodeOfCare.EpisodeOfCareStatus episodeOfCareStatus);
}
