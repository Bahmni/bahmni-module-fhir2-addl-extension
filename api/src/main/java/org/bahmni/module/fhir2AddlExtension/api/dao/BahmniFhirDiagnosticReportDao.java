package org.bahmni.module.fhir2AddlExtension.api.dao;

import org.bahmni.module.fhir2AddlExtension.api.model.FhirDiagnosticReportExt;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.util.PrivilegeConstants;

import javax.annotation.Nonnull;

public interface BahmniFhirDiagnosticReportDao extends FhirDao<FhirDiagnosticReportExt> {
	
	@Override
	@Authorized(PrivilegeConstants.GET_OBS)
	FhirDiagnosticReportExt get(@Nonnull String uuid);
	
	@Override
	@Authorized(PrivilegeConstants.DELETE_OBS)
	FhirDiagnosticReportExt delete(@Nonnull String uuid);
	
}
