package org.bahmni.module.fhir2AddlExtension.api.context;

import org.openmrs.User;

public interface AppContext {
	
	User getCurrentUser();
}
