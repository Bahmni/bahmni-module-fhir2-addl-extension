package org.bahmni.module.fhir2addlextension.api.service;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniRelatedPersonSearchParams;
import org.hl7.fhir.r4.model.RelatedPerson;

public interface BahmniFhirRelatedPersonService {
	
	IBundleProvider searchByPatient(BahmniRelatedPersonSearchParams searchParams);
	
	RelatedPerson get(String uuid);
	
	RelatedPerson create(RelatedPerson relatedPerson);
	
	RelatedPerson update(String uuid, RelatedPerson relatedPerson);
	
	void delete(String uuid);
}
