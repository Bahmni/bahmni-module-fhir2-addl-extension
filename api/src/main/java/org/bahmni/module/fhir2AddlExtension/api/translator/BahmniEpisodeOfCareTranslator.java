package org.bahmni.module.fhir2AddlExtension.api.translator;

import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.EpisodeOfCare;
import org.openmrs.Allergy;
import org.openmrs.module.episodes.Episode;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirUpdatableTranslator;

public interface BahmniEpisodeOfCareTranslator extends OpenmrsFhirTranslator<Episode, EpisodeOfCare>, OpenmrsFhirUpdatableTranslator<Episode, EpisodeOfCare> {}
