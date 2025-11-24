package org.bahmni.module.fhir2AddlExtension.api.dao;

import org.openmrs.Encounter;
import org.openmrs.Visit;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

public interface BahmniEpisodeOfCareEncounterDao {
	
	Map<String, List<Encounter>> getEncountersForEpisodes(@Nonnull List<String> episodeUuids);
	
	Map<String, List<Visit>> getVisitsForEpisodes(@Nonnull List<String> episodeUuids);
}
