package org.bahmni.module.fhir2addlextension.api.dao;

import org.openmrs.Obs;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.util.PrivilegeConstants;

import java.util.Set;

public interface BahmniObsDao extends FhirDao<Obs> {
	
	@Authorized(PrivilegeConstants.EDIT_OBS)
	void updateObsMember(Obs obsGroup, Set<Obs> groupMembers);
}
