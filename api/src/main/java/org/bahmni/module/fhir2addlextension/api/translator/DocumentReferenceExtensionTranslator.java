package org.bahmni.module.fhir2addlextension.api.translator;

import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReferenceAttribute;
import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReferenceAttributeType;

public interface DocumentReferenceExtensionTranslator extends AttributeTranslatorRegistry<FhirDocumentReferenceAttribute, FhirDocumentReferenceAttributeType, DocumentReferenceAttributeTranslator> {
	
}
