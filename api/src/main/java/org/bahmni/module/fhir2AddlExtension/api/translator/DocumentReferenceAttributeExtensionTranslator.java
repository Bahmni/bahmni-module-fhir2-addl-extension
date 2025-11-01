package org.bahmni.module.fhir2AddlExtension.api.translator;

import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReferenceAttribute;
import org.hl7.fhir.r4.model.Extension;

import java.util.List;

public interface DocumentReferenceAttributeExtensionTranslator {
    boolean supports(Extension extension);
    FhirDocumentReferenceAttribute toOpenmrsType(Extension extension);
    List<FhirDocumentReferenceAttribute> toOpenmrsType(String extUrl, List<Extension> extensions);
    void registerAttributeTranslator(DocumentReferenceAttributeTranslator translator);
}
