package org.bahmni.module.fhir2addlextension.api.translator.impl;

import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2addlextension.api.utils.ModuleUtils;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.PersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PersonAttributeExtensionTranslatorImpl implements org.bahmni.module.fhir2addlextension.api.translator.PersonAttributeExtensionTranslator {

	private static final Logger log = LoggerFactory.getLogger(PersonAttributeExtensionTranslatorImpl.class);

	private static final String BOOLEAN_FORMAT = "org.openmrs.customdatatype.datatype.BooleanDatatype";

	private final PersonService personService;

	@Autowired
	public PersonAttributeExtensionTranslatorImpl(@Qualifier("personService") PersonService personService) {
		this.personService = personService;
	}

	public Extension toFhirResource(PersonAttribute attribute) {
		String value = attribute.getValue();
		if (value == null) {
			return null;
		}

		String name = attribute.getAttributeType().getName();
		if (name == null) {
			return null;
		}

		String slug = ModuleUtils.toSlugCase(name);
		Extension ext = new Extension(BahmniFhirConstants.FHIR_EXT_PATIENT_ATTRIBUTE_PREFIX + slug);

		if (BOOLEAN_FORMAT.equals(attribute.getAttributeType().getFormat())) {
			ext.setValue(new BooleanType(Boolean.parseBoolean(value)));
		} else {
			ext.setValue(new StringType(value));
		}

		return ext;
	}

	public PersonAttributeType resolveType(String extensionUrl, Map<String, PersonAttributeType> slugToTypeMap) {
		if (extensionUrl == null || !extensionUrl.startsWith(BahmniFhirConstants.FHIR_EXT_PATIENT_ATTRIBUTE_PREFIX)) {
			return null;
		}

		String slug = extensionUrl.substring(BahmniFhirConstants.FHIR_EXT_PATIENT_ATTRIBUTE_PREFIX.length());
		if (slug.isEmpty()) {
			return null;
		}

		return slugToTypeMap.get(slug);
	}

	public Map<String, PersonAttributeType> buildSlugToTypeMap() {
		List<PersonAttributeType> types = personService.getAllPersonAttributeTypes(false);
		return types.stream()
				.filter(t -> t.getName() != null)
				.collect(Collectors.toMap(
						t -> ModuleUtils.toSlugCase(t.getName()),
						Function.identity(),
						(existing, duplicate) -> {
							log.warn("Duplicate slug for attribute types '{}' and '{}'",
									existing.getName(), duplicate.getName());
							return existing;
						}
				));
	}
}
