package org.bahmni.module.fhir2AddlExtension.api.translator;

import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReference;
import org.hl7.fhir.r4.model.DocumentReference;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirUpdatableTranslator;

public interface DocumentReferenceTranslator extends OpenmrsFhirTranslator<FhirDocumentReference, DocumentReference>, OpenmrsFhirUpdatableTranslator<FhirDocumentReference, DocumentReference> {}
