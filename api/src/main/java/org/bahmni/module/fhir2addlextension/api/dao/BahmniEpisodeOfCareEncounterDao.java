package org.bahmni.module.fhir2addlextension.api.dao;

import org.bahmni.module.fhir2addlextension.api.PrivilegeConstants;
import org.openmrs.Encounter;
import org.openmrs.annotation.Authorized;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

public interface BahmniEpisodeOfCareEncounterDao {
	
	@Authorized({ PrivilegeConstants.GET_EPISODES })
	Map<String, List<Encounter>> getEncountersForEpisodes(@Nonnull List<String> episodeUuids);
}
