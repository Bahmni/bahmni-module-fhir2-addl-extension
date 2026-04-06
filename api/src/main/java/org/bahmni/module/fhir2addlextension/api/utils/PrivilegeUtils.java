package org.bahmni.module.fhir2addlextension.api.utils;

import org.openmrs.User;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;

public class PrivilegeUtils {
	
	private PrivilegeUtils() {
	}
	
	public static void requirePrivilege(String privilege) {
		User authenticatedUser = Context.getUserContext().getAuthenticatedUser();
		if (authenticatedUser == null) {
			throw new APIAuthenticationException("User must be authenticated");
		}
		if (!authenticatedUser.hasPrivilege(privilege)) {
			throw new APIAuthenticationException("User does not have required privilege: " + privilege);
		}
	}
}
