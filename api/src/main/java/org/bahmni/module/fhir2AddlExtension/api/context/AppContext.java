package org.bahmni.module.fhir2AddlExtension.api.context;

import org.openmrs.EncounterRole;
import org.openmrs.EncounterType;
import org.openmrs.User;
import org.openmrs.api.EncounterService;

import java.util.Map;

public interface AppContext {
	
	User getCurrentUser();
	
	Map<String, String> getOrderTypeToLocationAttributeNameMap();
	
	EncounterType getEncounterType(String name);
	
	EncounterRole getLabEncounterRole();
}
