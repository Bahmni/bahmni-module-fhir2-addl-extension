package org.bahmni.module.fhir2addlextension.api.domain;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.r4.model.Bundle;

/**
 * TODO: Define IG for this bundle. This is a placeholder bundle resource which can be used to wrap
 * DiagnosticReport and its related resources (e.g. ServiceRequest, Observation etc.) in a single
 * request. Note, this is not intended to be persisted as a resource. This is just a wrapper for
 * handling bundles in FHIR providers. The structure validations for the bundle and its entries
 * would be handled in the service layer. Eventually, we would purge/delete this resource, although
 * there is some benefit to create a wrapper bundle to ensure that the structure definition is
 * adhered to and to delegate to different types of bundle handler/processor. Alternatively, we
 * handle the bundle as a generic Bundle resource and handle the structure validations in the
 * service layer. We can explore both approaches and decide on the best one.
 */
@ResourceDef(name = "DiagnosticReportBundle", profile = "http://fhir.bahmni.org/R4/StructureDefinition/DiagnosticReportBundle")
public class DiagnosticReportBundle extends Bundle {}
