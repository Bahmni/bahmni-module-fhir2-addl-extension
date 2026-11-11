package org.bahmni.module.fhir2addlextension.api.service;

import org.bahmni.module.fhir2addlextension.api.model.DispositionInfo;
import org.openmrs.Encounter;

public interface DispositionOrderLifecycleService {
	
	/**
	 * @return true if the visit of the given encounter already has an active order of the same
	 *         disposition kind, i.e. one that has been neither superseded by a closing order nor
	 *         fulfilled by the fulfiller (see the disposition rules global property)
	 */
	boolean hasActiveOrder(Encounter requestEncounter, DispositionInfo disposition);
}
