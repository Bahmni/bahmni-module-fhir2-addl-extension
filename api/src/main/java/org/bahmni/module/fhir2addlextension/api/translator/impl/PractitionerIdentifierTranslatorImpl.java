package org.bahmni.module.fhir2addlextension.api.translator.impl;

import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2addlextension.api.context.AppContext;
import org.bahmni.module.fhir2addlextension.api.translator.PractitionerIdentifierTranslator;
import org.hl7.fhir.r4.model.Identifier;
import org.openmrs.Provider;
import org.openmrs.ProviderAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class PractitionerIdentifierTranslatorImpl implements PractitionerIdentifierTranslator {
	
	@Autowired
	private AppContext appContext;
	
	@Override
	public List<Identifier> getAttributeDerivedIdentifiers(Provider provider) {
		List<Identifier> identifiers = new ArrayList<>();
		Map<String, String> attributeIdentifierSystemMap = appContext.getPractitionerAttributeIdentifierSystemMap();
		if (attributeIdentifierSystemMap.isEmpty() || provider.getActiveAttributes().isEmpty()) {
			return identifiers;
		}

		for (ProviderAttribute attribute : provider.getActiveAttributes()) {
			String system = attributeIdentifierSystemMap.get(attribute.getAttributeType().getName());
			if (system == null) {
				continue; // unmapped attribute type
			}
			String value;
			try {
				value = attribute.getValueReference();
			}
			catch (org.openmrs.customdatatype.NotYetPersistedException e) {
				value = null;
			}
			if (value == null || value.trim().isEmpty()) {
				continue; // mapped but null/blank value
			}
			identifiers.add(new Identifier().setSystem(system).setValue(value));
		}
		return identifiers;
	}
}
