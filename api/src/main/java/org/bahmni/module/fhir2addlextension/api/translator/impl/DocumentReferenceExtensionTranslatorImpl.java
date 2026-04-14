package org.bahmni.module.fhir2addlextension.api.translator.impl;

import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReferenceAttribute;
import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReferenceAttributeType;
import org.bahmni.module.fhir2addlextension.api.translator.DocumentReferenceAttributeTranslator;
import org.bahmni.module.fhir2addlextension.api.translator.DocumentReferenceExtensionTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DocumentReferenceExtensionTranslatorImpl extends BaseAttributeTranslatorRegistry<FhirDocumentReferenceAttribute, FhirDocumentReferenceAttributeType, DocumentReferenceAttributeTranslator> implements DocumentReferenceExtensionTranslator {
	
	@Autowired
	public DocumentReferenceExtensionTranslatorImpl(
	    DefaultDocumentReferenceAttributeTranslatorImpl defaultAttributeTranslator) {
		super(defaultAttributeTranslator);
	}
}
