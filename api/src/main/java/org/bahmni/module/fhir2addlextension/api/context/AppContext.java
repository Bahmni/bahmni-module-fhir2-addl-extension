package org.bahmni.module.fhir2addlextension.api.context;

import org.bahmni.module.fhir2addlextension.api.model.TelecomAttributeTypeMapping;
import org.openmrs.EncounterRole;
import org.openmrs.EncounterType;
import org.openmrs.User;

import java.util.List;
import java.util.Map;

public interface AppContext {
	
	User getCurrentUser();
	
	Map<String, String> getOrderTypeToLocationAttributeNameMap();
	
	EncounterType getEncounterType(String name);
	
	EncounterRole getLabEncounterRole();
	
	List<TelecomAttributeTypeMapping> getTelecomAttributeTypeMappings();
}
