package org.bahmni.module.fhir2AddlExtension.api;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReferenceAttributeType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.Provider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.UUID;

public class TestDataFactory {
	
	private static IParser r4ResourceParser = FhirContext.forR4().newJsonParser();
	
	public static FhirDocumentReferenceAttributeType exampleAttrTypeExternalOrganization() {
		FhirDocumentReferenceAttributeType attributeType = new FhirDocumentReferenceAttributeType();
		attributeType.setName("External Organization");
		attributeType.setDatatypeClassname("org.openmrs.customdatatype.datatype.FreeTextDatatype");
		attributeType.setMinOccurs(0);
		attributeType.setMaxOccurs(1);
		return attributeType;
	}
	
	public static FhirDocumentReferenceAttributeType exampleAttrTypeIsSelfSubmitted() {
		FhirDocumentReferenceAttributeType attributeType = new FhirDocumentReferenceAttributeType();
		attributeType.setName("Is self submitted");
		attributeType.setDatatypeClassname("org.openmrs.customdatatype.datatype.BooleanDatatype");
		attributeType.setMinOccurs(0);
		attributeType.setMaxOccurs(1);
		return attributeType;
	}
	
	public static Concept exampleConceptWithUuid(String name) {
		Concept concept = new Concept();
		ConceptName cn = new ConceptName();
		cn.setName(name);
		cn.setLocale(Locale.ENGLISH);
		concept.addName(cn);
		concept.setUuid(UUID.randomUUID().toString());
		return concept;
	}
	
	public static Concept exampleConceptWithUuid(String name, String uuid) {
		Concept concept = exampleConceptWithUuid(name);
		concept.setUuid(uuid);
		return concept;
	}
	
	public static Provider exampleProviderWithUuid(String name, String uuid) {
		Provider provider = new Provider();
		provider.setUuid(uuid);
		provider.setName(name);
		return provider;
	}
	
	public static IBaseResource loadResourceFromFile(String filename) throws IOException {
		InputStream inputStream = new TestDataFactory().getClass().getClassLoader().getResourceAsStream(filename);
		if (inputStream == null) {
			throw new IllegalArgumentException("File not found in test resources: " + filename);
		}
		// Read the content of the file
		IBaseResource resource;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			resource = r4ResourceParser.parseResource(reader);
		}
		return resource;
	}
}
