package org.bahmni.module.fhir2AddlExtension.api.context;

import org.openmrs.User;

import java.util.Map;

public interface AppContext {
	
	User getCurrentUser();
	
	Map<String, String> getOrderTypeToLocationAttributeNameMap();
}
