package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReferenceAttribute;
import org.bahmni.module.fhir2AddlExtension.api.translator.DocumentReferenceAttributeExtensionTranslator;
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
public class DocumentReferenceAttributeExtensionTranslatorImpl implements DocumentReferenceAttributeExtensionTranslator {

    private Set<DocumentReferenceAttributeTranslator> attributeTranslators = new HashSet<>();
    private final DefaultDocumentReferenceAttributeTranslatorImpl defaultAttributeTranslator;


    @Autowired
    public DocumentReferenceAttributeExtensionTranslatorImpl(DefaultDocumentReferenceAttributeTranslatorImpl defaultAttributeTranslator) {
        this.defaultAttributeTranslator = defaultAttributeTranslator;
    }

    @Override
    public boolean supports(Extension extension) {
        Optional<DocumentReferenceAttributeTranslator> attributeTranslator = getAttributeTranslator(extension.getUrl());
        return attributeTranslator.isPresent();
    }

    private Optional<DocumentReferenceAttributeTranslator> getAttributeTranslator(String extensionUrl) {
        return Optional.ofNullable(attributeTranslators.stream()
                .filter(translator -> translator.supports(extensionUrl))
                .findFirst().orElseGet(() -> {
                    if (defaultAttributeTranslator.supports(extensionUrl)) {
                        return defaultAttributeTranslator;
                    } else {
                        return null;
                    }
                }));
    }

    @Override
    public FhirDocumentReferenceAttribute toOpenmrsType(Extension extension) {
        List<FhirDocumentReferenceAttribute> fhirDocumentReferenceAttributes = this.toOpenmrsType(extension.getUrl(), Collections.singletonList(extension));
        return !fhirDocumentReferenceAttributes.isEmpty() ? fhirDocumentReferenceAttributes.get(0) : null;
    }



    @Override
    public List<FhirDocumentReferenceAttribute> toOpenmrsType(String extUrl, List<Extension> extensions) {
        Optional<DocumentReferenceAttributeTranslator> attributeTranslator
                = attributeTranslators.stream().filter(translator -> translator.supports(extUrl)).findFirst();
        if (attributeTranslator.isPresent()) {
            return attributeTranslator.get().toOpenmrsType(extUrl, extensions);
        }
        if (defaultAttributeTranslator.supports(extUrl)) {
            return defaultAttributeTranslator.toOpenmrsType(extUrl, extensions);
        }
        return Collections.emptyList();
    }

    @Override
    public void registerAttributeTranslator(DocumentReferenceAttributeTranslator translator) {
        if (attributeTranslators != null) {
            attributeTranslators.add(translator);
        }
    }
}
