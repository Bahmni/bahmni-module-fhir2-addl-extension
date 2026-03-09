package org.bahmni.module.fhir2AddlExtension.api.dao;

import org.bahmni.module.fhir2AddlExtension.api.model.FhirDiagnosticReportExt;
import org.bahmni.module.fhir2AddlExtension.api.utils.BahmniPrivilegeConstants;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.util.PrivilegeConstants;

import javax.annotation.Nonnull;

public interface BahmniFhirDiagnosticReportDao extends FhirDao<FhirDiagnosticReportExt> {
	
	@Override
	@Authorized({ BahmniPrivilegeConstants.GET_DIAGNOSTIC_REPORT, PrivilegeConstants.GET_OBS })
	FhirDiagnosticReportExt get(@Nonnull String uuid);
	
	@Override
	@Authorized(BahmniPrivilegeConstants.DELETE_DIAGNOSTIC_REPORT)
	FhirDiagnosticReportExt delete(@Nonnull String uuid);
	
	@Authorized({ BahmniPrivilegeConstants.GET_DIAGNOSTIC_REPORT, PrivilegeConstants.GET_OBS })
	FhirDiagnosticReportExt findByOrderUuid(@Nonnull String orderUuid);
	
}
