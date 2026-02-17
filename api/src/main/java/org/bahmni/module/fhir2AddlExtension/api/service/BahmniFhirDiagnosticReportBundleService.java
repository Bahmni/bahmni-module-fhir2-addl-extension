package org.bahmni.module.fhir2AddlExtension.api.service;

import ca.uhn.fhir.rest.api.PatchTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.bahmni.module.fhir2AddlExtension.api.domain.DiagnosticReportBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.openmrs.module.fhir2.api.FhirService;

import javax.annotation.Nonnull;

public interface BahmniFhirDiagnosticReportBundleService extends FhirService<Bundle> {}
