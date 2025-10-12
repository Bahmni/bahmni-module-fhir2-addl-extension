package org.bahmni.module.fhir2AddlExtension.api.dao;

import org.openmrs.annotation.Authorized;
import org.openmrs.module.episodes.Episode;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

public interface BahmniFhirEpisodeOfCareDao extends FhirDao<Episode> {
	
	@Authorized({"Get Episodes"})
	Episode get(@Nonnull String uuid);
	
	@Authorized({"Get Episodes"})
	List<Episode> get(@Nonnull Collection<String> uuids);

	@Authorized({"Add Episodes"})
	Episode createOrUpdate(@Nonnull Episode newEntry);

	@Authorized({"Delete Episodes"})
	Episode delete(@Nonnull String uuid);
	
	int getSearchResultsCount(@Nonnull SearchParameterMap theParams);

	@Authorized({"Get Episodes"})
	List<Episode> getSearchResults(@Nonnull SearchParameterMap theParams);
}
