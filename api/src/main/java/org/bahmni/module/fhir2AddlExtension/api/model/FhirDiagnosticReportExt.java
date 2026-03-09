package org.bahmni.module.fhir2AddlExtension.api.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openmrs.Order;

import java.util.HashSet;
import java.util.Set;

/**
 * Extended FHIR Diagnostic Report model to include additional fields like conclusion, orders, presented forms, and extended status.
 * TODO:
 * As part of improvement through https://openmrs.atlassian.net/browse/FM2-675,
 * We have to refactor the below code in future as conclusion, orders and statusExt would be part of the base FhirDiagnosticReport.
 * At the same time, we should also migrate Attachment from bahmni module to fhir2 module so that presentedForms can also be part of the base FhirDiagnosticReport.
 */
@Getter
@Setter
@NoArgsConstructor
public class FhirDiagnosticReportExt extends org.openmrs.module.fhir2.model.FhirDiagnosticReport {
    private String conclusion;
    private Set<Order> orders = new HashSet<>();
    private Set<Attachment> presentedForms = new HashSet<>();
    private DiagnosticReportStatusExt statusExt;

    public enum DiagnosticReportStatusExt {
        REGISTERED,
        PARTIAL,
        PRELIMINARY,
        AMENDED,
        CANCELLED,
        FINAL,
        UNKNOWN
    }
}
