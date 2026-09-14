package org.bahmni.module.fhir2addlextension.api.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniRelatedPersonSearchParams;
import org.hl7.fhir.r4.model.RelatedPerson;
import org.openmrs.module.fhir2.api.FhirService;

public interface BahmniFhirRelatedPersonService extends FhirService<RelatedPerson> {
	
	IBundleProvider searchByPatient(BahmniRelatedPersonSearchParams searchParams);
}
