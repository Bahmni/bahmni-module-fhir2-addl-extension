package org.bahmni.module.fhir2addlextension.api.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openmrs.Order;

import java.util.HashSet;
import java.util.Set;

/**
 * TODO: Migrate Attachment from bahmni module to fhir2 module so that presentedForms can also be part of the base FhirDiagnosticReport.
 */
@Getter
@Setter
@NoArgsConstructor
public class FhirDiagnosticReportExt extends org.openmrs.module.fhir2.model.FhirDiagnosticReport {
    private Set<Attachment> presentedForms = new HashSet<>();
}
