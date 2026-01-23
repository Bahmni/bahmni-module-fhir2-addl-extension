package org.bahmni.module.fhir2AddlExtension.api.translator;

import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Observation;
import org.openmrs.Concept;
import org.openmrs.Obs;

public interface ComplexObsDataTranslator {
	
	boolean supports(Concept concept);
	
	Extension toFhirResource(Obs obs);
	
	String toOpenmrsType(Observation observation);
}
