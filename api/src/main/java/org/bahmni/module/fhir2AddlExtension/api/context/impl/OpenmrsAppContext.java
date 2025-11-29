package org.bahmni.module.fhir2AddlExtension.api.context.impl;

import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2AddlExtension.api.context.AppContext;
import org.openmrs.User;
import org.openmrs.api.AdministrationService;
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
	
	public static final String PROP_ORDER_TYPE_TO_LOCATION_ATTR_NAME_MAP = "fhir2Extension.orderTypeToReferralLocationAttributeMap";
	
	@Autowired
	public OpenmrsAppContext(@Qualifier("adminService") AdministrationService administrationService) {
		this.administrationService = administrationService;
	}
	
	@Override
	public User getCurrentUser() {
		return Context.getUserContext().getAuthenticatedUser();
	}
	
	@Override
	@Cacheable(value = "fhir2extensionOrderTypeToLocationAttributeMap")
	public Map<String, String> getOrderTypeToLocationAttributeNameMap() {
		String propertyValue = administrationService.getGlobalProperty(PROP_ORDER_TYPE_TO_LOCATION_ATTR_NAME_MAP, "");
		return parseStringToMap(propertyValue);
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
