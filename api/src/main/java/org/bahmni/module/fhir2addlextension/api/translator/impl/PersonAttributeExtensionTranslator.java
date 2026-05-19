package org.bahmni.module.fhir2addlextension.api.translator.impl;

import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2addlextension.api.utils.ModuleUtils;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PersonAttributeExtensionTranslator {

	private static final String BOOLEAN_FORMAT = "org.openmrs.customdatatype.datatype.BooleanDatatype";

	private final PersonService personService;

	@Autowired
	public PersonAttributeExtensionTranslator(@Qualifier("personService") PersonService personService) {
		this.personService = personService;
	}

	public Extension toFhirResource(PersonAttribute attribute) {
		String value = attribute.getValue();
		if (value == null) {
			return null;
		}

		String slug = ModuleUtils.toSlugCase(attribute.getAttributeType().getName());
		Extension ext = new Extension(BahmniFhirConstants.FHIR_EXT_PATIENT_ATTRIBUTE_PREFIX + slug);

		if (BOOLEAN_FORMAT.equals(attribute.getAttributeType().getFormat())) {
			ext.setValue(new BooleanType(Boolean.parseBoolean(value)));
		} else {
			ext.setValue(new StringType(value));
		}

		return ext;
	}

	public PersonAttributeType resolveType(String extensionUrl) {
		if (extensionUrl == null || !extensionUrl.startsWith(BahmniFhirConstants.FHIR_EXT_PATIENT_ATTRIBUTE_PREFIX)) {
			return null;
		}

		String slug = extensionUrl.substring(BahmniFhirConstants.FHIR_EXT_PATIENT_ATTRIBUTE_PREFIX.length());
		if (slug.isEmpty()) {
			return null;
		}

		return buildSlugToTypeMap().get(slug);
	}

	private Map<String, PersonAttributeType> buildSlugToTypeMap() {
		return personService.getAllPersonAttributeTypes(false).stream()
				.collect(Collectors.toMap(
						t -> ModuleUtils.toSlugCase(t.getName()),
						Function.identity()
				));
	}
}
