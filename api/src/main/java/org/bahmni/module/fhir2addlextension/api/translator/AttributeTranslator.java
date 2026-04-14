package org.bahmni.module.fhir2addlextension.api.translator;

import org.hl7.fhir.r4.model.Extension;

import java.util.List;
import java.util.Optional;

public interface AttributeTranslator<A, U> {
	
	boolean supports(A attribute);
	
	List<A> toOpenmrsType(String extUrl, List<Extension> extensions);
	
	Extension toFhirResource(A attribute);
	
	Optional<U> getAttributeType(String extUrl);
}
