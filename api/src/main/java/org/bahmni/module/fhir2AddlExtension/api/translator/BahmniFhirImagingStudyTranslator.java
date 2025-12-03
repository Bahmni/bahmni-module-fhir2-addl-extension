package org.bahmni.module.fhir2AddlExtension.api.translator;

import org.bahmni.module.fhir2AddlExtension.api.model.FhirImagingStudy;
import org.hl7.fhir.r4.model.ImagingStudy;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirUpdatableTranslator;

public interface BahmniFhirImagingStudyTranslator extends OpenmrsFhirTranslator<FhirImagingStudy, ImagingStudy>, OpenmrsFhirUpdatableTranslator<FhirImagingStudy, ImagingStudy> {
}
