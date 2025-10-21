package org.bahmni.module.fhir2AddlExtension.api.dao;

import org.openmrs.Encounter;
import org.openmrs.module.episodes.Episode;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import javax.annotation.Nonnull;
import java.util.List;

public interface BahmniEpisodeOfCareEncounterDao {
	
	List<Encounter> getSearchResults(@Nonnull SearchParameterMap theParams);
}
