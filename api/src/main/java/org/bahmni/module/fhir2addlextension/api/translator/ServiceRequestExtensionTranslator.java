package org.bahmni.module.fhir2addlextension.api.translator;

import org.hl7.fhir.r4.model.Extension;
import org.openmrs.OrderAttribute;

import java.util.Optional;

public interface ServiceRequestExtensionTranslator {
	
	boolean hasAttributeTranslator(Extension extension);
	
	Optional<ServiceRequestAttributeTranslator> getAttributeTranslator(String extensionUrl);
	
	Optional<ServiceRequestAttributeTranslator> getAttributeTranslator(OrderAttribute attribute);
	
	void registerAttributeTranslator(ServiceRequestAttributeTranslator translator);
}
