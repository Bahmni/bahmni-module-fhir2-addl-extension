package org.bahmni.module.fhir2AddlExtension.api.translator;

import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReference;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Enumerations;

public interface DocumentReferenceStatusTranslator {
	
	FhirDocumentReference.FhirDocumentReferenceStatus toOpenmrsType(Enumerations.DocumentReferenceStatus status);
	
	FhirDocumentReference.FhirDocumentReferenceDocStatus toOpenmrsType(DocumentReference.ReferredDocumentStatus docStatus);
	
	Enumerations.DocumentReferenceStatus toFhirType(FhirDocumentReference.FhirDocumentReferenceStatus status);
	
	DocumentReference.ReferredDocumentStatus toFhirType(FhirDocumentReference.FhirDocumentReferenceDocStatus docStatus);
}
