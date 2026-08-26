package org.bahmni.module.fhir2addlextension.api.dao;

import org.bahmni.module.fhir2addlextension.api.PrivilegeConstants;
import org.bahmni.module.fhir2addlextension.api.model.FhirDiagnosticReportExt;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import java.util.List;

import javax.annotation.Nonnull;

public interface BahmniFhirDiagnosticReportDao extends FhirDao<FhirDiagnosticReportExt> {
	
	@Authorized({ PrivilegeConstants.GET_DIAGNOSTIC_REPORT, org.openmrs.util.PrivilegeConstants.GET_OBS })
	FhirDiagnosticReportExt get(@Nonnull String uuid);
	
	@Override
	@Authorized(PrivilegeConstants.DELETE_DIAGNOSTIC_REPORT)
	FhirDiagnosticReportExt delete(@Nonnull String uuid);
	
	@Authorized({ PrivilegeConstants.GET_DIAGNOSTIC_REPORT, org.openmrs.util.PrivilegeConstants.GET_OBS })
	List<FhirDiagnosticReportExt> getSearchResults(@Nonnull SearchParameterMap theParams);
	
	@Authorized({ PrivilegeConstants.GET_DIAGNOSTIC_REPORT, org.openmrs.util.PrivilegeConstants.GET_OBS })
	FhirDiagnosticReportExt findByOrderUuid(@Nonnull String orderUuid);
	
}
