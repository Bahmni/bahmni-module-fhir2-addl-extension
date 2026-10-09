package org.bahmni.module.fhir2addlextension.api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * One entry of the {@code fhir2Extension.telecomAttributeTypeMap} global property: declares that a
 * person attribute type (identified by uuid) should appear in FHIR {@code Patient.telecom} with the
 * given system/use/rank. {@code system}/{@code use} are stored as the raw FHIR enum constant name
 * (e.g. {@code "PHONE"}) rather than the {@code org.hl7.fhir.r4} enum types themselves, so this
 * package (otherwise OpenMRS/Hibernate types only) has no compile-time dependency on the FHIR
 * layer; callers convert to/from the FHIR enum where needed.
 */
@Getter
@AllArgsConstructor
public class TelecomAttributeTypeMapping {
	
	private final String attributeTypeUuid;
	
	private final String system;
	
	private final String use;
	
	private final Integer rank;
}
