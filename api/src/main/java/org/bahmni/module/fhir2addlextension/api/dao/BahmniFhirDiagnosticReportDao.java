package org.bahmni.module.fhir2addlextension.api.dao;

import org.bahmni.module.fhir2addlextension.api.PrivilegeConstants;
import org.bahmni.module.fhir2addlextension.api.model.FhirDiagnosticReportExt;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.fhir2.api.dao.FhirDao;

import javax.annotation.Nonnull;

public interface BahmniFhirDiagnosticReportDao extends FhirDao<FhirDiagnosticReportExt> {
	
	@Override
	@Authorized({ PrivilegeConstants.GET_DIAGNOSTIC_REPORT, PrivilegeConstants.GET_OBSERVATIONS })
	FhirDiagnosticReportExt get(@Nonnull String uuid);
	
	@Override
	@Authorized(PrivilegeConstants.DELETE_DIAGNOSTIC_REPORT)
	FhirDiagnosticReportExt delete(@Nonnull String uuid);
	
	@Authorized({ PrivilegeConstants.GET_DIAGNOSTIC_REPORT, PrivilegeConstants.GET_OBSERVATIONS })
	FhirDiagnosticReportExt findByOrderUuid(@Nonnull String orderUuid);
	
}
