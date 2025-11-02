package org.bahmni.module.fhir2AddlExtension.api.translator;

import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReferenceAttribute;
import org.hl7.fhir.r4.model.Extension;

import java.util.List;
import java.util.Optional;

public interface DocumentReferenceExtensionTranslator {
	
	boolean hasAttributeTranslator(Extension extension);
	
	Optional<DocumentReferenceAttributeTranslator> getAttributeTranslator(String extensionUrl);
	
	Optional<DocumentReferenceAttributeTranslator> getAttributeTranslator(FhirDocumentReferenceAttribute attribute);
	
	void registerAttributeTranslator(DocumentReferenceAttributeTranslator translator);
}
