package org.bahmni.module.fhir2AddlExtension.api.context.impl;

import org.bahmni.module.fhir2AddlExtension.api.context.AppContext;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.springframework.stereotype.Component;

@Component
public class OpenmrsAppContext implements AppContext {
	
	@Override
	public User getCurrentUser() {
		return Context.getUserContext().getAuthenticatedUser();
	}
}
