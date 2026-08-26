package org.bahmni.module.fhir2addlextension.api.translator;

import org.bahmni.module.fhir2addlextension.api.model.FhirDiagnosticReportExt;
import org.hl7.fhir.r4.model.Bundle;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirUpdatableTranslator;

public interface BahmniFhirDiagnosticReportBundleTranslator extends OpenmrsFhirTranslator<FhirDiagnosticReportExt, Bundle>, OpenmrsFhirUpdatableTranslator<FhirDiagnosticReportExt, Bundle> {}
