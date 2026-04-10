package org.bahmni.module.fhir2addlextension.api.dao;

import org.openmrs.Obs;
import org.openmrs.annotation.Authorized;
import org.openmrs.util.PrivilegeConstants;

import java.util.Set;

public interface BahmniObsDao {
	
	@Authorized(PrivilegeConstants.EDIT_OBS)
	void updateObsMember(Obs obsGroup, Set<Obs> groupMembers);
}
