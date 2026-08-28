package org.bahmni.module.fhir2addlextension.api.context.impl;

import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2addlextension.api.context.AppContext;
import org.bahmni.module.fhir2addlextension.api.model.TelecomAttributeTypeMapping;
import org.hl7.fhir.r4.model.ContactPoint;
import org.openmrs.EncounterRole;
import org.openmrs.EncounterType;
import org.openmrs.User;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class OpenmrsAppContext implements AppContext {
	
	private final AdministrationService administrationService;
	
	private final EncounterService encounterService;
	
	public static final String PROP_ORDER_TYPE_TO_LOCATION_ATTR_NAME_MAP = "fhir2Extension.orderTypeToReferralLocationAttributeMap";
	
	public static final String PROP_TELECOM_ATTRIBUTE_TYPE_MAP = "fhir2Extension.telecomAttributeTypeMap";
	
	public static final String LAB_RESULTS_ENCOUNTER_ROLE = "Supporting services";
	
	@Autowired
	public OpenmrsAppContext(@Qualifier("adminService") AdministrationService administrationService,
	    EncounterService encounterService) {
		this.administrationService = administrationService;
		this.encounterService = encounterService;
	}
	
	@Override
	public User getCurrentUser() {
		return Context.getUserContext().getAuthenticatedUser();
	}
	
	@Override
	@Cacheable(value = "fhir2addlextensionOrderTypeToLocationAttributeMap")
	public Map<String, String> getOrderTypeToLocationAttributeNameMap() {
		String propertyValue = administrationService.getGlobalProperty(PROP_ORDER_TYPE_TO_LOCATION_ATTR_NAME_MAP, "");
		return parseStringToMap(propertyValue);
	}
	
	@Override
	@Cacheable(value = "fhir2addlextensionEncounterTypeByName")
	public EncounterType getEncounterType(String typeName) {
		return encounterService.getEncounterType(typeName);
	}
	
	@Override
	@Cacheable(value = "fhir2addlextensionLabEncounterRole")
	public EncounterRole getLabEncounterRole() {
		EncounterRole role = encounterService.getEncounterRoleByName(LAB_RESULTS_ENCOUNTER_ROLE);
		if (role != null) {
			return role;
		}
		return encounterService.getEncounterRoleByUuid(EncounterRole.UNKNOWN_ENCOUNTER_ROLE_UUID);
	}
	
	@Override
	@Cacheable(value = "fhir2addlextensionTelecomAttributeTypeMappings")
	public List<TelecomAttributeTypeMapping> getTelecomAttributeTypeMappings() {
		String propertyValue = administrationService.getGlobalProperty(PROP_TELECOM_ATTRIBUTE_TYPE_MAP, "");
		return parseTelecomAttributeTypeMappings(propertyValue);
	}
	
	/**
	 * Parses entries of the form <code>attributeTypeUuid:SYSTEM:USE:RANK</code>, separated by
	 * <code>;</code>. <code>USE</code> and <code>RANK</code> are optional (e.g.
	 * <code>uuid:EMAIL</code> , or <code>uuid:PHONE::2</code> to set rank without a use).
	 * <code>SYSTEM</code>/<code>USE</code> are matched case-insensitively against the FHIR enum
	 * constants. Malformed entries, and entries with an unknown system, are skipped (logged, not
	 * thrown) so one bad entry doesn't prevent the rest from being read.
	 */
	private List<TelecomAttributeTypeMapping> parseTelecomAttributeTypeMappings(String input) {
		List<TelecomAttributeTypeMapping> mappings = new ArrayList<>();
		if (input == null || input.trim().isEmpty()) {
			return mappings;
		}

		for (String entry : input.split(";")) {
			String trimmedEntry = entry.trim();
			if (trimmedEntry.isEmpty()) {
				continue;
			}

			String[] parts = trimmedEntry.split(":", -1);
			if (parts.length < 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
				log.warn("Skipping malformed telecom attribute type mapping entry: '{}'", trimmedEntry);
				continue;
			}

			String attributeTypeUuid = parts[0].trim();
			ContactPoint.ContactPointSystem system;
			try {
				system = ContactPoint.ContactPointSystem.valueOf(parts[1].trim().toUpperCase());
			}
			catch (IllegalArgumentException e) {
				log.warn("Skipping telecom attribute type mapping for '{}' - unknown system '{}'", attributeTypeUuid,
				    parts[1]);
				continue;
			}

			ContactPoint.ContactPointUse use = null;
			if (parts.length > 2 && !parts[2].trim().isEmpty()) {
				try {
					use = ContactPoint.ContactPointUse.valueOf(parts[2].trim().toUpperCase());
				}
				catch (IllegalArgumentException e) {
					log.warn("Ignoring unknown contact point use '{}' for attribute type '{}'", parts[2],
					    attributeTypeUuid);
				}
			}

			Integer rank = null;
			if (parts.length > 3 && !parts[3].trim().isEmpty()) {
				try {
					rank = Integer.valueOf(parts[3].trim());
				}
				catch (NumberFormatException e) {
					log.warn("Ignoring non-numeric rank '{}' for attribute type '{}'", parts[3], attributeTypeUuid);
				}
			}

			mappings.add(new TelecomAttributeTypeMapping(attributeTypeUuid, system, use, rank));
		}
		return mappings;
	}
	
	private Map<String, String> parseStringToMap(String input) {
		Map<String, String> resultMap = new HashMap<>();
		if (input == null || input.trim().isEmpty()) {
			return resultMap;
		}
		String[] pairs = input.split(";");
		for (String pair : pairs) {
			String trimmedPair = pair.trim();
			if (trimmedPair.isEmpty()) {
				continue;
			}
			int firstColonIndex = trimmedPair.indexOf(':');
			if (firstColonIndex > 0) {
				String key = trimmedPair.substring(0, firstColonIndex).trim();
				String value = trimmedPair.substring(firstColonIndex + 1).trim();
				if (!key.isEmpty() && !"".equals(value)) {
					resultMap.put(key, value);
				}
			}
			// If firstColonIndex is -1 (no colon) or 0 (key starts with colon), the pair is skipped
		}
		return resultMap;
	}
}
