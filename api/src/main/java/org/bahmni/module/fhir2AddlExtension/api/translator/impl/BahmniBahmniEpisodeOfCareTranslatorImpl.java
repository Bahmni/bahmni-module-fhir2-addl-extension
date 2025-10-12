package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniEpisodeOfCareTranslator;
import org.hl7.fhir.r4.model.EpisodeOfCare;
import org.hl7.fhir.r4.model.Period;
import org.openmrs.Concept;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.episodes.Episode;
import org.openmrs.module.episodes.EpisodeReason;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.Validate.notNull;
import static org.openmrs.module.fhir2.api.translators.impl.FhirTranslatorUtils.getLastUpdated;
import static org.openmrs.module.fhir2.api.translators.impl.FhirTranslatorUtils.getVersionId;

@Component
public class BahmniBahmniEpisodeOfCareTranslatorImpl implements BahmniEpisodeOfCareTranslator {

    private PatientReferenceTranslator patientReferenceTranslator;
    private ConceptTranslator conceptTranslator;
    private PractitionerReferenceTranslator<Provider> providerReferenceTranslator;
    private Map<Episode.Status,EpisodeOfCare.EpisodeOfCareStatus> episodeStatusMap = new HashMap<>();

    @Autowired
    public BahmniBahmniEpisodeOfCareTranslatorImpl(PatientReferenceTranslator patientReferenceTranslator, ConceptTranslator conceptTranslator, PractitionerReferenceTranslator<Provider> providerReferenceTranslator) {
        this.patientReferenceTranslator = patientReferenceTranslator;
        this.conceptTranslator = conceptTranslator;
        this.providerReferenceTranslator = providerReferenceTranslator;
        initialize();
    }

    private void initialize() {
        episodeStatusMap.put(Episode.Status.PLANNED, EpisodeOfCare.EpisodeOfCareStatus.PLANNED);
        episodeStatusMap.put(Episode.Status.ACTIVE, EpisodeOfCare.EpisodeOfCareStatus.ACTIVE);
        episodeStatusMap.put(Episode.Status.ONHOLD, EpisodeOfCare.EpisodeOfCareStatus.ONHOLD);
        episodeStatusMap.put(Episode.Status.CANCELLED, EpisodeOfCare.EpisodeOfCareStatus.CANCELLED);
        episodeStatusMap.put(Episode.Status.FINISHED, EpisodeOfCare.EpisodeOfCareStatus.FINISHED);
        episodeStatusMap.put(Episode.Status.ENTERED_IN_ERROR, EpisodeOfCare.EpisodeOfCareStatus.ENTEREDINERROR);
    }


    @Override
    public EpisodeOfCare toFhirResource(@Nonnull Episode episode) {
        EpisodeOfCare episodeOfCare = new EpisodeOfCare();
        episodeOfCare.setId(episode.getUuid());
        //TODO: derive biz identifier from attribute
        episodeOfCare.setPatient(patientReferenceTranslator.toFhirResource(episode.getPatient()));
        episodeOfCare.setPeriod(getEpisodePeriod(episode));
        episodeOfCare.getMeta().setLastUpdated(getLastUpdated(episode));
        episodeOfCare.getMeta().setVersionId(getVersionId(episode));
        episodeOfCare.setStatus(toFhirEoCStatus(episode));
        episodeOfCare.setType(episode.getEpisodeReason().stream()
                .map(episodeReason -> conceptTranslator.toFhirResource(episodeReason.getReason()))
                .collect(Collectors.toList()));
        //TODO
        //episodeOfCare.setCareManager(providerReferenceTranslator.toFhirResource(episode.getCreator()));
        return episodeOfCare;
    }

    private EpisodeOfCare.EpisodeOfCareStatus toFhirEoCStatus(Episode episode) {
        //TODO : handle status null of older records?
        return episodeStatusMap.get(episode.getStatus());
    }

    private Period getEpisodePeriod(Episode episode) {
        Date dateStarted = Optional.ofNullable(episode.getDateStarted()).orElse(episode.getDateCreated());
        Period episodePeriod = new Period();
        episodePeriod.setStart(dateStarted);
        episodePeriod.setEnd(episode.getDateEnded());
        return episodePeriod;
    }

    @Override
    public Episode toOpenmrsType(@Nonnull EpisodeOfCare episodeOfCare) {
        notNull(episodeOfCare.getPatient(), "The EpisodeOfCare object must have a patient reference");
        return this.toOpenmrsType(new Episode(), episodeOfCare);
    }

    private Episode.Status toOmrsEpisodeStatus(EpisodeOfCare.EpisodeOfCareStatus status) {
        //TODO : handle status UNKNOWN
        Optional<Episode.Status> key = episodeStatusMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(status))
                .map(Map.Entry::getKey)
                .findFirst();
        return key.orElse(Episode.Status.PLANNED);
    }

    @Override
    public Episode toOpenmrsType(@Nonnull Episode episode, @Nonnull EpisodeOfCare episodeOfCare) {
        notNull(episode, "The existing OpenMRS Episode object should not be null");
        notNull(episodeOfCare, "The EpisodeOfCare object should not be null");

        if (episodeOfCare.hasStatus()) {
            episode.setStatus(toOmrsEpisodeStatus(episodeOfCare.getStatus()));
        }
        //TODO - if the date in future, planned, if date is today - active
        if (episode.getStatus() == null) {
            episode.setStatus(Episode.Status.ACTIVE);
        }

        if (episodeOfCare.hasPatient() && episode.getPatient() == null) {
            //For older model support where patient reference may be null. This would set the patient reference
            episode.setPatient(patientReferenceTranslator.toOpenmrsType(episodeOfCare.getPatient()));
        }

        User authenticatedUser = Context.getUserContext().getAuthenticatedUser();
        if (episode.getUuid() == null) {
            episode.setCreator(authenticatedUser);
            episode.setDateCreated(new Date());
        } else {
            episode.setChangedBy(authenticatedUser);
            episode.setDateChanged(new Date());
        }
        if (episodeOfCare.hasPeriod()) {
            episode.setDateStarted(episodeOfCare.getPeriod().getStart());
            episode.setDateEnded(episodeOfCare.getPeriod().getEnd());
        }
        if (episode.getDateStarted() == null) {
            //TODO check if episode date started is null
            episode.setDateStarted(new Date());
        }

        if (episodeOfCare.hasType()) {
            Set<Concept> specifiedReasons = episodeOfCare.getType().stream()
                    .map(codeableConcept -> conceptTranslator.toOpenmrsType(codeableConcept))
                    .collect(Collectors.toSet());
            if (episode.getEpisodeReason().isEmpty()) {
                Set<EpisodeReason> episodeReasons = specifiedReasons.stream()
                        .map(concept -> constructEpisodeReason(episode, concept, authenticatedUser))
                        .collect(Collectors.toSet());
                episode.setEpisodeReason(episodeReasons);
            } else {
                //merge
                Set<Concept> existingReasons = episode.getEpisodeReason().stream()
                        .map(existingReason -> existingReason.getReason())
                        .collect(Collectors.toSet());
                Set<Concept> additionalReasons = specifiedReasons.stream()
                        .filter(reason -> !existingReasons.stream().anyMatch(concept -> concept.getUuid().equals(reason.getUuid())))
                        .collect(Collectors.toSet());
                Set<EpisodeReason> addlEpisodeReasons = additionalReasons.stream()
                        .map(reason -> constructEpisodeReason(episode, reason, authenticatedUser))
                        .collect(Collectors.toSet());
                episode.getEpisodeReason().addAll(addlEpisodeReasons);
            }
        }
        return episode;
    }

    private EpisodeReason constructEpisodeReason(Episode episode, Concept reason, User authenticatedUser) {
        EpisodeReason episodeReason = new EpisodeReason();
        episodeReason.setReason(reason);
        episodeReason.setEpisode(episode);
        episodeReason.setCreator(authenticatedUser);
        episodeReason.setUuid(UUID.randomUUID().toString());
        episodeReason.setDateCreated(new Date());
        return episodeReason;
    }
}
