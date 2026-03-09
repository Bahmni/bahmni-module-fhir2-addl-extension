package org.bahmni.module.fhir2AddlExtension.api.dao;

import org.openmrs.Obs;

import java.util.Set;

public interface BahmniObsDao {
	
	void updateObsMember(Obs obsGroup, Set<Obs> groupMembers);
}
