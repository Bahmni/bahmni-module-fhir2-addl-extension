package org.bahmni.module.fhir2addlextension.api.translator;

import org.hl7.fhir.r4.model.Identifier;
import org.openmrs.Provider;

import java.util.List;

public interface PractitionerIdentifierTranslator {
	
	List<Identifier> getAttributeDerivedIdentifiers(Provider provider);
}
