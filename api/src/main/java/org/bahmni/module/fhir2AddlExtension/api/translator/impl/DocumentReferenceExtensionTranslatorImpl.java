package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReferenceAttribute;
import org.bahmni.module.fhir2AddlExtension.api.translator.DocumentReferenceExtensionTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.DocumentReferenceAttributeTranslator;
import org.hl7.fhir.r4.model.Extension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class DocumentReferenceExtensionTranslatorImpl implements DocumentReferenceExtensionTranslator {

    private Set<DocumentReferenceAttributeTranslator> attributeTranslators = new HashSet<>();
    private final DefaultDocumentReferenceAttributeTranslatorImpl defaultAttributeTranslator;


    @Autowired
    public DocumentReferenceExtensionTranslatorImpl(DefaultDocumentReferenceAttributeTranslatorImpl defaultAttributeTranslator) {
        this.defaultAttributeTranslator = defaultAttributeTranslator;
    }

    @Override
    public boolean hasAttributeTranslator(Extension extension) {
        return getAttributeTranslator(extension.getUrl()).isPresent();
    }

    @Override
    public Optional<DocumentReferenceAttributeTranslator> getAttributeTranslator(String extensionUrl) {
        return Optional.ofNullable(attributeTranslators.stream()
                .filter(translator -> translator.getAttributeType(extensionUrl).isPresent())
                .findFirst()
                .orElseGet(() -> {
                    if (defaultAttributeTranslator.getAttributeType(extensionUrl).isPresent()) {
                        return defaultAttributeTranslator;
                    } else {
                        return null;
                    }
                }));
    }

    @Override
    public Optional<DocumentReferenceAttributeTranslator> getAttributeTranslator(FhirDocumentReferenceAttribute attribute) {
        return Optional.ofNullable(attributeTranslators.stream()
                .filter(translator -> translator.supports(attribute))
                .findFirst()
                .orElseGet(() -> {
                    if (defaultAttributeTranslator.supports(attribute)) {
                        return defaultAttributeTranslator;
                    } else {
                        return null;
                    }
                }));
    }

    @Override
    public void registerAttributeTranslator(DocumentReferenceAttributeTranslator translator) {
        if (attributeTranslators != null) {
            attributeTranslators.add(translator);
        }
    }
}
