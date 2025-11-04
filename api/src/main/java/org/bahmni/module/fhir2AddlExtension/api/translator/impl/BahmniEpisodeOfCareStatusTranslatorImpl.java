package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.translator.EpisodeOfCareStatusTranslator;
import org.hl7.fhir.r4.model.EpisodeOfCare;
import org.openmrs.module.episodes.Episode;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.Validate.notNull;

@Component
public class BahmniEpisodeOfCareStatusTranslatorImpl implements EpisodeOfCareStatusTranslator {

    private final Map<Episode.Status,EpisodeOfCare.EpisodeOfCareStatus> episodeStatusMap = new HashMap<>();

    @Override
    public EpisodeOfCare.EpisodeOfCareStatus toFhirType(@Nonnull Episode.Status status) {
        return (status != null) ? episodeStatusMap.get(status) : null;
    }

    @Override
    public Episode.Status toOpenmrsType(@Nonnull EpisodeOfCare.EpisodeOfCareStatus episodeOfCareStatus) {
        Optional<Episode.Status> key = episodeStatusMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(episodeOfCareStatus))
                .map(Map.Entry::getKey)
                .findFirst();
        return key.orElse(Episode.Status.PLANNED);
    }

    @PostConstruct
    public void initialize() {
        episodeStatusMap.put(Episode.Status.PLANNED, EpisodeOfCare.EpisodeOfCareStatus.PLANNED);
        episodeStatusMap.put(Episode.Status.ACTIVE, EpisodeOfCare.EpisodeOfCareStatus.ACTIVE);
        episodeStatusMap.put(Episode.Status.ONHOLD, EpisodeOfCare.EpisodeOfCareStatus.ONHOLD);
        episodeStatusMap.put(Episode.Status.CANCELLED, EpisodeOfCare.EpisodeOfCareStatus.CANCELLED);
        episodeStatusMap.put(Episode.Status.FINISHED, EpisodeOfCare.EpisodeOfCareStatus.FINISHED);
        episodeStatusMap.put(Episode.Status.ENTERED_IN_ERROR, EpisodeOfCare.EpisodeOfCareStatus.ENTEREDINERROR);
    }
}
