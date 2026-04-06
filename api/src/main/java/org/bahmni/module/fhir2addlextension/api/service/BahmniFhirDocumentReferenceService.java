package org.bahmni.module.fhir2addlextension.api.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.bahmni.module.fhir2addlextension.api.PrivilegeConstants;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniDocumentReferenceSearchParams;
import org.hl7.fhir.r4.model.DocumentReference;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.fhir2.api.FhirService;

public interface BahmniFhirDocumentReferenceService extends FhirService<DocumentReference> {
	
	@Authorized({ PrivilegeConstants.GET_DOCUMENT_REFERENCE })
	IBundleProvider searchDocumentReferences(BahmniDocumentReferenceSearchParams searchParams);
	
}
