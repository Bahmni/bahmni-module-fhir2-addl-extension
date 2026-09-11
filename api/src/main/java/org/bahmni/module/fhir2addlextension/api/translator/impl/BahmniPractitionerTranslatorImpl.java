package org.bahmni.module.fhir2addlextension.api.translator.impl;

import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2addlextension.api.translator.PractitionerIdentifierTranslator;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Practitioner;
import org.openmrs.Provider;
import org.openmrs.module.fhir2.api.translators.impl.PractitionerTranslatorProviderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Primary
@Component
@Slf4j
public class BahmniPractitionerTranslatorImpl extends PractitionerTranslatorProviderImpl {
	
	@Autowired
	private PractitionerIdentifierTranslator practitionerIdentifierTranslator;
	
	void setPractitionerIdentifierTranslator(PractitionerIdentifierTranslator translator) {
		this.practitionerIdentifierTranslator = translator;
	}
	
	@Override
	public Practitioner toFhirResource(@Nonnull Provider provider) {
		Practitioner practitioner = super.toFhirResource(provider);
		for (Identifier identifier : practitionerIdentifierTranslator.getAttributeDerivedIdentifiers(provider)) {
			practitioner.addIdentifier(identifier);
		}
		return practitioner;
	}
}
