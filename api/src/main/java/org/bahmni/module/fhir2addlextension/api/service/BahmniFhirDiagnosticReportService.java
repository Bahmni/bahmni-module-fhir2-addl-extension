package org.bahmni.module.fhir2addlextension.api.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.bahmni.module.fhir2addlextension.api.PrivilegeConstants;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.fhir2.api.FhirService;
import org.openmrs.module.fhir2.api.search.param.DiagnosticReportSearchParams;

public interface BahmniFhirDiagnosticReportService extends FhirService<DiagnosticReport> {
	
	@Authorized({ PrivilegeConstants.GET_DIAGNOSTIC_REPORT, PrivilegeConstants.GET_OBSERVATIONS })
	IBundleProvider searchForDiagnosticReports(DiagnosticReportSearchParams diagnosticReportSearchParams);
	
}
