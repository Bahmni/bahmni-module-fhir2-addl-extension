package org.bahmni.module.fhir2AddlExtension.api.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.bahmni.module.fhir2AddlExtension.api.search.param.BahmniDocumentReferenceSearchParams;
import org.hl7.fhir.r4.model.DocumentReference;
import org.openmrs.module.fhir2.api.FhirService;

public interface BahmniFhirDocumentReferenceService extends FhirService<DocumentReference> {
	
	IBundleProvider searchDocumentReferences(BahmniDocumentReferenceSearchParams searchParams);
	
}
