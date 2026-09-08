package org.bahmni.module.fhir2addlextension.api.translator;

import org.hl7.fhir.r4.model.RelatedPerson;
import org.openmrs.Relationship;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirUpdatableTranslator;
import org.openmrs.module.fhir2.api.translators.RelatedPersonTranslator;

public interface BahmniRelatedPersonTranslator extends RelatedPersonTranslator, OpenmrsFhirUpdatableTranslator<Relationship, RelatedPerson> {
	
	RelatedPerson toFhirResource(Relationship relationship, String subjectPatientUuid);
}
