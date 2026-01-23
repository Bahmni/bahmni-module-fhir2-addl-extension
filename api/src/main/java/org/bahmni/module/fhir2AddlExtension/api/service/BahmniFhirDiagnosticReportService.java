package org.bahmni.module.fhir2AddlExtension.api.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.openmrs.module.fhir2.api.FhirService;
import org.openmrs.module.fhir2.api.search.param.DiagnosticReportSearchParams;

public interface BahmniFhirDiagnosticReportService extends FhirService<DiagnosticReport> {
	
	IBundleProvider searchForDiagnosticReports(DiagnosticReportSearchParams diagnosticReportSearchParams);
	
}
