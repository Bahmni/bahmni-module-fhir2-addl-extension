package org.bahmni.module.fhir2AddlExtension.api.domain;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import org.hl7.fhir.r4.model.Bundle;

//TODO define IG
@ResourceDef(name = "DiagnosticReportBundle", profile = "http://fhir.bahmni.org/R4/StructureDefinition/DiagnosticReportBundle")
public class DiagnosticReportBundle extends Bundle {}
