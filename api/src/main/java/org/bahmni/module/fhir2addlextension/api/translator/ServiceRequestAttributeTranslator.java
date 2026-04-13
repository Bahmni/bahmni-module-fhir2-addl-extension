package org.bahmni.module.fhir2addlextension.api.translator;

import org.hl7.fhir.r4.model.Extension;
import org.openmrs.OrderAttribute;
import org.openmrs.OrderAttributeType;

import java.util.List;
import java.util.Optional;

public interface ServiceRequestAttributeTranslator {
	
	boolean supports(OrderAttribute attribute);
	
	List<OrderAttribute> toOpenmrsType(String extUrl, List<Extension> extensions);
	
	Extension toFhirResource(OrderAttribute attribute);
	
	Optional<OrderAttributeType> getAttributeType(String extUrl);
	
}
