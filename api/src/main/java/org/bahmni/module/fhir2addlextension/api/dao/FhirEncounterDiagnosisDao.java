package org.bahmni.module.fhir2addlextension.api.dao;

import org.bahmni.module.fhir2addlextension.api.PrivilegeConstants;
import org.openmrs.Diagnosis;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

public interface FhirEncounterDiagnosisDao extends FhirDao<Diagnosis> {
	
	@Override
	@Authorized(PrivilegeConstants.GET_DIAGNOSES)
	Diagnosis get(@Nonnull String s);
	
	@Override
	@Authorized(PrivilegeConstants.GET_DIAGNOSES)
	List<Diagnosis> get(@Nonnull Collection<String> collection);
	
	@Override
	@Authorized(PrivilegeConstants.GET_DIAGNOSES)
	List<Diagnosis> getSearchResults(@Nonnull SearchParameterMap searchParameterMap);
	
	@Override
	@Authorized(PrivilegeConstants.GET_DIAGNOSES)
	int getSearchResultsCount(@Nonnull SearchParameterMap searchParameterMap);
	
	@Override
	@Authorized({ PrivilegeConstants.ADD_DIAGNOSES, PrivilegeConstants.EDIT_DIAGNOSES })
	Diagnosis createOrUpdate(@Nonnull Diagnosis diagnosis);
	
	@Override
	@Authorized(PrivilegeConstants.DELETE_DIAGNOSES)
	Diagnosis delete(@Nonnull String s);
}
