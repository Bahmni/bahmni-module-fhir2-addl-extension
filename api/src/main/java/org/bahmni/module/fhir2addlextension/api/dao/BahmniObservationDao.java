package org.bahmni.module.fhir2addlextension.api.dao;

import org.openmrs.Obs;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.util.PrivilegeConstants;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

public interface BahmniObservationDao extends FhirDao<Obs> {
	
	@Override
	@Authorized({ PrivilegeConstants.GET_OBS })
	Obs get(@Nonnull String uuid);
	
	@Override
	@Authorized({ PrivilegeConstants.GET_OBS })
	List<Obs> get(@Nonnull Collection<String> uuids);
	
	@Override
	@Authorized({ PrivilegeConstants.GET_OBS })
	List<Obs> getSearchResults(@Nonnull SearchParameterMap searchParameterMap);
	
	@Override
	@Authorized({ PrivilegeConstants.GET_OBS })
	int getSearchResultsCount(@Nonnull SearchParameterMap searchParameterMap);
	
	@Override
	@Authorized({ PrivilegeConstants.ADD_OBS, PrivilegeConstants.EDIT_OBS })
	Obs createOrUpdate(@Nonnull Obs obs);
	
	@Override
	@Authorized({ PrivilegeConstants.DELETE_OBS })
	Obs delete(@Nonnull String uuid);
}
