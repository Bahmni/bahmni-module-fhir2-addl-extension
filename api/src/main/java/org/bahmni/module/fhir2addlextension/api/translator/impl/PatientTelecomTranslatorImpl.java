package org.bahmni.module.fhir2addlextension.api.translator.impl;

import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.hl7.fhir.r4.model.ContactPoint;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class PatientTelecomTranslatorImpl implements org.bahmni.module.fhir2addlextension.api.translator.PatientTelecomTranslator {
	
	private static final Logger log = LoggerFactory.getLogger(PatientTelecomTranslatorImpl.class);
	
	private final PersonService personService;
	
	private final AdministrationService administrationService;
	
	@Autowired
	public PatientTelecomTranslatorImpl(@Qualifier("personService") PersonService personService,
	    @Qualifier("adminService") AdministrationService administrationService) {
		this.personService = personService;
		this.administrationService = administrationService;
	}
	
	/**
	 * Parses the global property into an ordered map: attributeTypeName -> ContactPointSystem.
	 * Skips blank, malformed, or invalid entries (logs a warning for invalid system values).
	 */
	LinkedHashMap<String, ContactPoint.ContactPointSystem> parseMapping() {
		String gpValue = administrationService.getGlobalProperty(BahmniFhirConstants.GP_PATIENT_CONTACT_TELECOM_MAP);
		LinkedHashMap<String, ContactPoint.ContactPointSystem> result = new LinkedHashMap<>();
		if (gpValue == null || gpValue.trim().isEmpty()) {
			return result;
		}
		for (String pair : gpValue.split(";")) {
			String trimmed = pair.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			int colonIdx = trimmed.indexOf(':');
			if (colonIdx <= 0 || colonIdx == trimmed.length() - 1) {
				log.warn("Skipping malformed telecom mapping entry: '{}'", trimmed);
				continue;
			}
			String attrTypeName = trimmed.substring(0, colonIdx).trim();
			String systemCode = trimmed.substring(colonIdx + 1).trim();
			if (attrTypeName.isEmpty() || systemCode.isEmpty()) {
				log.warn("Skipping malformed telecom mapping entry: '{}'", trimmed);
				continue;
			}
			ContactPoint.ContactPointSystem system;
			try {
				system = ContactPoint.ContactPointSystem.fromCode(systemCode);
			}
			catch (Exception e) {
				log.warn("Skipping telecom mapping entry '{}': invalid system code '{}'", trimmed, systemCode);
				continue;
			}
			if (system == null || system == ContactPoint.ContactPointSystem.NULL) {
				log.warn("Skipping telecom mapping entry '{}': invalid system code '{}'", trimmed, systemCode);
				continue;
			}
			result.put(attrTypeName, system);
		}
		return result;
	}
	
	@Override
	public List<ContactPoint> getContactPoints(org.openmrs.Patient patient) {
		LinkedHashMap<String, ContactPoint.ContactPointSystem> mapping = parseMapping();
		if (mapping.isEmpty()) {
			return Collections.emptyList();
		}
		List<ContactPoint> contactPoints = new ArrayList<>();
		for (Map.Entry<String, ContactPoint.ContactPointSystem> entry : mapping.entrySet()) {
			String attrTypeName = entry.getKey();
			ContactPoint.ContactPointSystem system = entry.getValue();
			for (PersonAttribute attr : patient.getActiveAttributes()) {
				if (attr.getAttributeType() == null) {
					continue;
				}
				if (!attrTypeName.equals(attr.getAttributeType().getName())) {
					continue;
				}
				String value = attr.getValue();
				if (value == null || value.isEmpty()) {
					continue;
				}
				ContactPoint cp = new ContactPoint();
				cp.setSystem(system);
				cp.setValue(value);
				contactPoints.add(cp);
			}
		}
		return contactPoints;
	}
	
	@Override
	public void updateAttributes(org.openmrs.Patient patient, List<ContactPoint> telecom) {
		if (telecom == null || telecom.isEmpty()) {
			return;
		}
		LinkedHashMap<String, ContactPoint.ContactPointSystem> mapping = parseMapping();
		if (mapping.isEmpty()) {
			return;
		}
		// Build inverse: system -> attrTypeName (first mapping wins)
		Map<ContactPoint.ContactPointSystem, String> inverseMap = new LinkedHashMap<>();
		for (Map.Entry<String, ContactPoint.ContactPointSystem> entry : mapping.entrySet()) {
			inverseMap.putIfAbsent(entry.getValue(), entry.getKey());
		}
		for (ContactPoint cp : telecom) {
			ContactPoint.ContactPointSystem system = cp.getSystem();
			if (system == null) {
				continue;
			}
			String attrTypeName = inverseMap.get(system);
			if (attrTypeName == null) {
				continue;
			}
			PersonAttributeType attrType = personService.getPersonAttributeTypeByName(attrTypeName);
			if (attrType == null) {
				log.warn("Telecom write: person attribute type not found for name '{}'", attrTypeName);
				continue;
			}
			// Void existing active attributes of this type
			for (PersonAttribute existing : patient.getActiveAttributes()) {
				if (existing.getAttributeType() != null && existing.getAttributeType().equals(attrType)) {
					existing.setVoided(true);
					existing.setVoidReason("Updated via FHIR");
					existing.setVoidedBy(Context.getAuthenticatedUser());
					existing.setDateVoided(new Date());
				}
			}
			// Add new attribute if value is present
			String value = cp.getValue();
			if (value != null && !value.isEmpty()) {
				PersonAttribute attr = new PersonAttribute();
				attr.setAttributeType(attrType);
				attr.setValue(value);
				patient.addAttribute(attr);
			}
		}
	}
	
	@Override
	public Set<String> getMappedAttributeTypeNames() {
		return new LinkedHashSet<>(parseMapping().keySet());
	}
}
