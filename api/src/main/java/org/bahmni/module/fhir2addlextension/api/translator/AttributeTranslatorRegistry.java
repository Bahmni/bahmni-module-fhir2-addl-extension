package org.bahmni.module.fhir2addlextension.api.translator;

import org.hl7.fhir.r4.model.Extension;

import java.util.Optional;

public interface AttributeTranslatorRegistry<A, U, T extends AttributeTranslator<A, U>> {
	
	boolean hasAttributeTranslator(Extension extension);
	
	Optional<T> getAttributeTranslator(String extensionUrl);
	
	Optional<T> getAttributeTranslator(A attribute);
	
	void registerAttributeTranslator(T translator);
}
