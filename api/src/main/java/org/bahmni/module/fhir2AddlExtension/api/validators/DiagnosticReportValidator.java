package org.bahmni.module.fhir2AddlExtension.api.validators;

import org.hl7.fhir.r4.model.DiagnosticReport;

import java.util.EnumSet;

public interface DiagnosticReportValidator {
	
	void validate(DiagnosticReport diagnosticReport);
	
	EnumSet<DiagnosticReport.DiagnosticReportStatus> DIAGNOSTIC_REPORT_DRAFT_STATES = EnumSet.of(
	    DiagnosticReport.DiagnosticReportStatus.REGISTERED, DiagnosticReport.DiagnosticReportStatus.PARTIAL);
}
