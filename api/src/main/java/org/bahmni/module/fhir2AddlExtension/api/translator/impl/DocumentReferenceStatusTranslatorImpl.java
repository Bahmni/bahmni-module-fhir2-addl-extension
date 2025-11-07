package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReference;
import org.bahmni.module.fhir2AddlExtension.api.translator.DocumentReferenceStatusTranslator;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Enumerations;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class DocumentReferenceStatusTranslatorImpl implements DocumentReferenceStatusTranslator {

    private final Map<FhirDocumentReference.FhirDocumentReferenceStatus, Enumerations.DocumentReferenceStatus> statusMap = new HashMap<>();
    private final Map<FhirDocumentReference.FhirDocumentReferenceDocStatus, DocumentReference.ReferredDocumentStatus> docStatusMap = new HashMap<>();

    @Override
    public FhirDocumentReference.FhirDocumentReferenceStatus toOpenmrsType(Enumerations.DocumentReferenceStatus status) {
        Optional<FhirDocumentReference.FhirDocumentReferenceStatus> key = statusMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(status))
                .map(Map.Entry::getKey)
                .findFirst();
        return key.orElse(FhirDocumentReference.FhirDocumentReferenceStatus.CURRENT);
    }

    @Override
    public FhirDocumentReference.FhirDocumentReferenceDocStatus toOpenmrsType(DocumentReference.ReferredDocumentStatus docStatus) {
        Optional<FhirDocumentReference.FhirDocumentReferenceDocStatus> key = docStatusMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(docStatus))
                .map(Map.Entry::getKey)
                .findFirst();
        return key.orElse(FhirDocumentReference.FhirDocumentReferenceDocStatus.PRELIMINARY);
    }

    @Override
    public Enumerations.DocumentReferenceStatus toFhirType(FhirDocumentReference.FhirDocumentReferenceStatus status) {
        return (status != null) ? statusMap.get(status) : null;
    }

    @Override
    public DocumentReference.ReferredDocumentStatus toFhirType(FhirDocumentReference.FhirDocumentReferenceDocStatus docStatus) {
        return (docStatus != null) ? docStatusMap.get(docStatus) : null;
    }


    @PostConstruct
    public void initialize() {
        statusMap.put(FhirDocumentReference.FhirDocumentReferenceStatus.CURRENT, Enumerations.DocumentReferenceStatus.CURRENT);
        statusMap.put(FhirDocumentReference.FhirDocumentReferenceStatus.SUPERSEDED, Enumerations.DocumentReferenceStatus.SUPERSEDED);
        statusMap.put(FhirDocumentReference.FhirDocumentReferenceStatus.ENTEREDINERROR, Enumerations.DocumentReferenceStatus.ENTEREDINERROR);

        docStatusMap.put(FhirDocumentReference.FhirDocumentReferenceDocStatus.PRELIMINARY, DocumentReference.ReferredDocumentStatus.PRELIMINARY);
        docStatusMap.put(FhirDocumentReference.FhirDocumentReferenceDocStatus.AMENDED, DocumentReference.ReferredDocumentStatus.AMENDED);
        docStatusMap.put(FhirDocumentReference.FhirDocumentReferenceDocStatus.FINAL, DocumentReference.ReferredDocumentStatus.FINAL);
        docStatusMap.put(FhirDocumentReference.FhirDocumentReferenceDocStatus.ENTEREDINERROR, DocumentReference.ReferredDocumentStatus.ENTEREDINERROR);
    }
}
