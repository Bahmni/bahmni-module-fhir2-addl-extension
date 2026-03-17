package org.bahmni.module.fhir2addlextension.api.context;

import org.openmrs.EncounterRole;
import org.openmrs.EncounterType;
import org.openmrs.User;

import java.util.Map;

public interface AppContext {
	
	User getCurrentUser();
	
	Map<String, String> getOrderTypeToLocationAttributeNameMap();
	
	EncounterType getEncounterType(String name);
	
	EncounterRole getLabEncounterRole();
}
