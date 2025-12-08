package org.bahmni.module.fhir2AddlExtension.api.dao;

import org.bahmni.module.fhir2AddlExtension.api.model.FhirImagingStudy;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

import static org.bahmni.module.fhir2AddlExtension.api.PrivilegeConstants.*;

public interface BahmniFhirImagingStudyDao extends FhirDao<FhirImagingStudy> {
	
	@Override
	@Authorized(GET_IMAGING_STUDY)
	FhirImagingStudy get(@Nonnull String s);
	
	@Override
	@Authorized(GET_IMAGING_STUDY)
	List<FhirImagingStudy> get(@Nonnull Collection<String> collection);
	
	@Override
	@Authorized(GET_IMAGING_STUDY)
	List<FhirImagingStudy> getSearchResults(@Nonnull SearchParameterMap searchParameterMap);
	
	@Override
	@Authorized(GET_IMAGING_STUDY)
	int getSearchResultsCount(@Nonnull SearchParameterMap searchParameterMap);
	
	@Override
	@Authorized(CREATE_IMAGING_STUDY)
	FhirImagingStudy createOrUpdate(@Nonnull FhirImagingStudy fhirImagingStudy);
	
	@Override
	@Authorized(DELETE_IMAGING_STUDY)
	FhirImagingStudy delete(@Nonnull String s);
}
