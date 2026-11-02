package org.bahmni.module.fhir2addlextension.api.service;

import org.openmrs.Concept;
import org.openmrs.OrderType;

public interface DispositionOrderTypeResolver {
	
	String ADT_ORDER_TYPE_NAME = "ADT Order";
	
	OrderType resolve(Concept concept);
	
	/**
	 * @return the ADMIT/DISCHARGE/TRANSFER/UNDO_DISCHARGE emrapi code of the concept, or null if it
	 *         is not a disposition concept
	 */
	String getDispositionCode(Concept concept);
}
