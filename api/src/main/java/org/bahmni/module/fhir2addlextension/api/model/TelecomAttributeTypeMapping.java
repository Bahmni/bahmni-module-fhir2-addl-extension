package org.bahmni.module.fhir2addlextension.api.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.ContactPoint;

/**
 * One entry of the {@code fhir2Extension.telecomAttributeTypeMap} global property: declares that a
 * person attribute type (identified by uuid) should appear in FHIR {@code Patient.telecom} with the
 * given system/use/rank.
 */
@Getter
@AllArgsConstructor
public class TelecomAttributeTypeMapping {
	
	private final String attributeTypeUuid;
	
	private final ContactPoint.ContactPointSystem system;
	
	private final ContactPoint.ContactPointUse use;
	
	private final Integer rank;
}
