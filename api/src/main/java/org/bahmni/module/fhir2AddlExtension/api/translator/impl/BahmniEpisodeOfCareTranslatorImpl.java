package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniEpisodeOfCareTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.EpisodeOfCareStatusTranslator;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.EpisodeOfCare;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Type;
import org.openmrs.Concept;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.episodes.Episode;
import org.openmrs.module.episodes.EpisodeReason;
import org.openmrs.module.episodes.EpisodeStatusHistory;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.Validate.notNull;
import static org.openmrs.module.fhir2.api.translators.impl.FhirTranslatorUtils.getLastUpdated;
import static org.openmrs.module.fhir2.api.translators.impl.FhirTranslatorUtils.getVersionId;

@Component
public class BahmniEpisodeOfCareTranslatorImpl implements BahmniEpisodeOfCareTranslator {
	
	private final PatientReferenceTranslator patientReferenceTranslator;
	
	private final ConceptTranslator conceptTranslator;
	
	private final PractitionerReferenceTranslator<Provider> providerReferenceTranslator;
	
	private final EpisodeOfCareStatusTranslator statusTranslator;
	
	@Autowired
	public BahmniEpisodeOfCareTranslatorImpl(PatientReferenceTranslator patientReferenceTranslator,
	    ConceptTranslator conceptTranslator, PractitionerReferenceTranslator<Provider> providerReferenceTranslator,
	    EpisodeOfCareStatusTranslator statusTranslator) {
		this.patientReferenceTranslator = patientReferenceTranslator;
		this.conceptTranslator = conceptTranslator;
		this.providerReferenceTranslator = providerReferenceTranslator;
		this.statusTranslator = statusTranslator;
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
		episodeOfCare.setStatus(statusTranslator.toFhirType(episode.getStatus()));
		episodeOfCare.setType(Collections.singletonList(conceptTranslator.toFhirResource(episode.getConcept())));
		toFhirEpisodeOfCareReasonAsExtension(episodeOfCare, episode);
		toFhirEpisodeOfCareStatusHistory(episodeOfCare, episode);
		if (episode.getCareManager() != null) {
			episodeOfCare.setCareManager(providerReferenceTranslator.toFhirResource(episode.getCareManager()));
		}
		return episodeOfCare;
	}
	
	protected void toFhirEpisodeOfCareStatusHistory(EpisodeOfCare episodeOfCare, Episode episode) {
        if (episode.hasStatusHistory()) {
            List<EpisodeOfCare.EpisodeOfCareStatusHistoryComponent> episodeHistory = episode.getStatusHistory().stream().map(statusHistory -> {
                EpisodeOfCare.EpisodeOfCareStatusHistoryComponent statusHistoryComponent = new EpisodeOfCare.EpisodeOfCareStatusHistoryComponent();
                statusHistoryComponent.setStatus(statusTranslator.toFhirType(statusHistory.getStatus()));
                Period historyPeriod = new Period();
                historyPeriod.setStart(statusHistory.getDateStarted());
                historyPeriod.setEnd(statusHistory.getDateEnded());
                statusHistoryComponent.setPeriod(historyPeriod);
                statusHistoryComponent.setId(statusHistory.getUuid());
                return statusHistoryComponent;
            }).collect(Collectors.toList());
            episodeOfCare.setStatusHistory(episodeHistory);
        }
    }
	
	protected void toFhirEpisodeOfCareReasonAsExtension(EpisodeOfCare episodeOfCare, Episode episode) {
        episode.getEpisodeReason()
           .forEach(episodeReason -> {
			   Extension reasonExtension = episodeOfCare.addExtension();
			   reasonExtension.setUrl(BahmniFhirConstants.FHIR_EXT_EPISODE_OF_CARE_REASON);
			   Optional.ofNullable(episodeReason.getReasonUse())
					   .ifPresent(concept -> {
						   CodeableConcept reasonUse = conceptTranslator.toFhirResource(concept);
						   if (reasonUse != null) {
							   reasonExtension.addExtension("use", reasonUse);
						   }
					   });

			   if ((episodeReason.getValueConcept() != null) || !StringUtils.isEmpty(episodeReason.getValueReference())) {
				   Extension valueExtension = reasonExtension.addExtension();
				   valueExtension.setUrl("value");
				   Optional.ofNullable(episodeReason.getValueConcept())
						   .ifPresent(concept -> {
							   CodeableConcept reasonValue = conceptTranslator.toFhirResource(concept);
							   if (reasonValue != null) {
								   valueExtension.addExtension("concept", reasonValue);
							   }
						   });
				   Optional.ofNullable(episodeReason.getValueReference())
						   .ifPresent(valueRef -> {
							   if (!"".equals(valueRef)) {
								   Reference reference = new Reference();
								   reference.setReference(valueRef);
								   valueExtension.addExtension("reference", reference);
							   }
						   });
			   }

		   });
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
		User authenticatedUser = Context.getUserContext().getAuthenticatedUser();
		Episode newEpisode = new Episode();
		EpisodeStatusHistory episodeStatusHistory = toOpenmrsEpisodeStatusHistory(episodeOfCare, null);
		episodeStatusHistory.setCreator(authenticatedUser);
		newEpisode.addEpisodeStatusHistory(episodeStatusHistory);
		return this.toOpenmrsType(newEpisode, episodeOfCare);
	}
	
	@Override
    public Episode toOpenmrsType(@Nonnull Episode episode, @Nonnull EpisodeOfCare episodeOfCare) {
        notNull(episode, "The existing OpenMRS Episode object should not be null");
        notNull(episodeOfCare, "The EpisodeOfCare object should not be null");

        if (episodeOfCare.hasPatient() && episode.getPatient() == null) {
            //For older model support where patient reference may be null. This would set the patient reference
            episode.setPatient(patientReferenceTranslator.toOpenmrsType(episodeOfCare.getPatient()));
        }

        User authenticatedUser = Context.getUserContext().getAuthenticatedUser();
        if (episode.getUuid() == null) {
            episode.setCreator(authenticatedUser);
            episode.setDateCreated(new Date());
        } else {
            if (episodeOfCare.hasId()) {
                episode.setUuid(episodeOfCare.getIdElement().getIdPart());
            }
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
		if (episodeOfCare.hasCareManager()) {
			episode.setCareManager(providerReferenceTranslator.toOpenmrsType(episodeOfCare.getCareManager()));
		}

        if (episodeOfCare.hasType()) {
            Set<Concept> episodeType = episodeOfCare.getType().stream()
                    .map(conceptTranslator::toOpenmrsType)
                    .collect(Collectors.toSet());
            if (episodeType.isEmpty()) {
                throw new InvalidRequestException("Can not find reference to episode type");
            }
            //if there are more than 1 distinct, then taking the first one.
            episode.setConcept(episodeType.stream().findFirst().get());
        }

        if (episodeOfCare.hasStatus()) {
            episode.setStatus(statusTranslator.toOpenmrsType(episodeOfCare.getStatus()));
        }
        //TODO - if the date in future, planned, if date is today - active
        if (episode.getStatus() == null) {
            episode.setStatus(Episode.Status.ACTIVE);
        }
        toOpenmrsEpisodeReason(episode, episodeOfCare, authenticatedUser);
        return episode;
    }
	
	@Override
	public EpisodeStatusHistory toOpenmrsEpisodeStatusHistory(EpisodeOfCare episodeOfCare, Date defaultStartDate) {
		EpisodeStatusHistory statusHistory = new EpisodeStatusHistory();
		if (episodeOfCare.hasPeriod()) {
			statusHistory.setDateStarted(episodeOfCare.getPeriod().getStart());
			statusHistory.setDateEnded(episodeOfCare.getPeriod().getEnd());
		} else {
			if (defaultStartDate != null) {
				statusHistory.setDateStarted(episodeOfCare.getPeriod().getStart());
			} else {
				statusHistory.setDateStarted(new Date());
			}
		}
		statusHistory.setStatus(statusTranslator.toOpenmrsType(episodeOfCare.getStatus()));
		statusHistory.setDateCreated(new Date());
		return statusHistory;
	}
	
	protected void toOpenmrsEpisodeReason(Episode episode, EpisodeOfCare episodeOfCare, User authenticatedUser) {
		List<Extension> episodeOfCareReasons = episodeOfCare
				.getExtensionsByUrl(BahmniFhirConstants.FHIR_EXT_EPISODE_OF_CARE_REASON);
		Set<EpisodeReason> specifiedReasons = new HashSet<>();
		//map to openmrs episodeReason first
		for (Extension eocReason : episodeOfCareReasons) {
			//although a EpisodeOfCare.reason object can have multiple "use" and multiple "values"
			//we are processing one "use" to multiple "values" per "reason" extension
			//resulting in multiple openmrs episodeReason objects
			Optional<Concept> reasonUseConcept = eocReason.getExtensionsByUrl("use").stream().findFirst()
					.map(extension -> {
						Type value = extension.getValue();
						if (value instanceof CodeableConcept) {
							return conceptTranslator.toOpenmrsType((CodeableConcept) value);
						}
						return null;
					}).filter(Objects::nonNull);
			List<Extension> valueExtensions = eocReason.getExtensionsByUrl("value");
			if (valueExtensions.isEmpty()) {
				continue;
			}
			for (Extension valueExtn : valueExtensions) {
				Optional<Concept> valueConcept = valueExtn.getExtensionsByUrl("concept").stream().findFirst().map(extension -> {
					Type value = extension.getValue();
					return value instanceof CodeableConcept ? conceptTranslator.toOpenmrsType((CodeableConcept) value) : null;
				}).filter(Objects::nonNull);

				if (valueConcept.isPresent()) {
					Optional<String> valueReference = valueExtn.getExtensionsByUrl("valueReference").stream().findFirst().map(extension -> {
						Type value = extension.getValue();
						return value instanceof Reference ? ((Reference) value).getReference() : null;
					}).filter(Objects::nonNull);
					EpisodeReason aReason = constructEpisodeReason(reasonUseConcept, valueConcept, valueReference, authenticatedUser);
					specifiedReasons.add(aReason);
				}
			}
		}
		//identify, add or merge
        if (episode.getEpisodeReason().isEmpty()) {
			for (EpisodeReason specifiedReason : specifiedReasons) {
				specifiedReason.setUuid(UUID.randomUUID().toString());
				episode.addEpisodeReason(specifiedReason);
			}
			return;
        }
		//merge
		//TODO: Void nonMatchingExistingReasons but with what void_reason?
		Set<EpisodeReason> nonMatchingExistingReasons = episode.getEpisodeReason().stream()
				.filter(existing -> specifiedReasons.stream().noneMatch(specified -> {
					return isSameValueReference(existing.getValueReference(), specified.getValueReference())
							&& isSameConcept(existing.getReasonUse(), specified.getReasonUse())
							&& isSameConcept(existing.getValueConcept(), specified.getValueConcept());
				})).collect(Collectors.toSet());


		Set<EpisodeReason> nonMatchingNewReasons = specifiedReasons.stream()
				.filter(sr -> episode.getEpisodeReason().stream().noneMatch(existingReason -> {
					return isSameValueReference(existingReason.getValueReference(), sr.getValueReference())
							&& isSameConcept(existingReason.getReasonUse(), sr.getReasonUse())
							&& isSameConcept(existingReason.getValueConcept(), sr.getValueConcept());
				})).collect(Collectors.toSet());
		//add them to episode
		for (EpisodeReason nonMatchingNewReason : nonMatchingNewReasons) {
			nonMatchingNewReason.setUuid(UUID.randomUUID().toString());
			episode.addEpisodeReason(nonMatchingNewReason);
		}
    }
	
	private boolean isSameValueReference(String s1, String s2) {
		if (s1 == s2) return true;
		return Optional.ofNullable(s1).map(s -> s.equals(s2)).orElse(false);
	}
	
	private boolean isSameConcept(Concept c1, Concept c2) {
		if (c1 == c2) return true;
		return Optional.ofNullable(c1).map(c -> c.equals(c2)).orElse(false);
	}
	
	private EpisodeReason constructEpisodeReason(Optional<Concept> reasonUseConcept, Optional<Concept> valueConcept, Optional<String> valueReference, User authenticatedUser) {
		EpisodeReason episodeReason = new EpisodeReason();
		episodeReason.setCreator(authenticatedUser);
		episodeReason.setDateCreated(new Date());
		reasonUseConcept.ifPresent(concept -> episodeReason.setReasonUse(concept));
		valueConcept.ifPresent(concept -> episodeReason.setValueConcept(concept));
		valueReference.ifPresent(valueRef -> episodeReason.setValueReference(valueRef));
		return episodeReason;
	}
}
