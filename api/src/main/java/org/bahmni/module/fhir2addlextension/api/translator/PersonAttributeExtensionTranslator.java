package org.bahmni.module.fhir2addlextension.api.translator;

import org.hl7.fhir.r4.model.Extension;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;

import java.util.Map;

public interface PersonAttributeExtensionTranslator {

	Extension toFhirResource(PersonAttribute attribute);

	PersonAttributeType resolveType(String extensionUrl, Map<String, PersonAttributeType> slugToTypeMap);

	Map<String, PersonAttributeType> buildSlugToTypeMap();
}
