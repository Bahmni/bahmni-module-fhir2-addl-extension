package org.bahmni.module.fhir2AddlExtension.api.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.openmrs.Order;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class FhirDiagnosticReportExt extends org.openmrs.module.fhir2.model.FhirDiagnosticReport {
    private String conclusion;
    private Set<Order> orders = new HashSet<>();
    private Set<Attachment> presentedForms = new HashSet<>();
}
