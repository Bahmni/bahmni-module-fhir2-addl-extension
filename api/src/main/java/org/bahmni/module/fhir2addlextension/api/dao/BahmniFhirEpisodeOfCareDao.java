package org.bahmni.module.fhir2addlextension.api.dao;

import org.bahmni.module.fhir2addlextension.api.PrivilegeConstants;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.episodes.Episode;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

public interface BahmniFhirEpisodeOfCareDao extends FhirDao<Episode> {
	
	@Override
	@Authorized({ PrivilegeConstants.GET_EPISODES })
	Episode get(@Nonnull String uuid);
	
	@Override
	@Authorized({ PrivilegeConstants.GET_EPISODES })
	List<Episode> get(@Nonnull Collection<String> uuids);
	
	@Override
	@Authorized({ PrivilegeConstants.ADD_EPISODES, PrivilegeConstants.EDIT_EPISODES })
	Episode createOrUpdate(@Nonnull Episode newEntry);
	
	@Override
	@Authorized({ PrivilegeConstants.DELETE_EPISODES })
	Episode delete(@Nonnull String uuid);
	
	@Override
	@Authorized({ PrivilegeConstants.GET_EPISODES })
	int getSearchResultsCount(@Nonnull SearchParameterMap theParams);
	
	@Override
	@Authorized({ PrivilegeConstants.GET_EPISODES })
	List<Episode> getSearchResults(@Nonnull SearchParameterMap theParams);
}
