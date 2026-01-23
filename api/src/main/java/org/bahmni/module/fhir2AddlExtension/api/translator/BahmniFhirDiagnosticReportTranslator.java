package org.bahmni.module.fhir2AddlExtension.api.translator;

import org.bahmni.module.fhir2AddlExtension.api.model.FhirDiagnosticReportExt;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirUpdatableTranslator;

public interface BahmniFhirDiagnosticReportTranslator extends OpenmrsFhirTranslator<FhirDiagnosticReportExt, DiagnosticReport>, OpenmrsFhirUpdatableTranslator<FhirDiagnosticReportExt, DiagnosticReport> {}
