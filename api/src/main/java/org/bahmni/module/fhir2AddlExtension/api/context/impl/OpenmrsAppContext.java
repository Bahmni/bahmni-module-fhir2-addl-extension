package org.bahmni.module.fhir2AddlExtension.api.context.impl;

import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2AddlExtension.api.context.AppContext;
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

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class OpenmrsAppContext implements AppContext {
	
	private final AdministrationService administrationService;
	
	private final EncounterService encounterService;
	
	public static final String PROP_ORDER_TYPE_TO_LOCATION_ATTR_NAME_MAP = "fhir2Extension.orderTypeToReferralLocationAttributeMap";
	
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
