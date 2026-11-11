package org.bahmni.module.fhir2addlextension.api.service;

import org.bahmni.module.fhir2addlextension.api.model.DispositionInfo;
import org.openmrs.Concept;
import org.openmrs.OrderType;

public interface DispositionOrderTypeResolver {
	
	/**
	 * @return the ADT OrderType for a disposition concept, or null if the concept is not a
	 *         disposition
	 * @throws ca.uhn.fhir.rest.server.exceptions.InvalidRequestException if the concept is a
	 *             disposition but the configured OrderType does not exist
	 */
	OrderType resolve(Concept concept);
	
	/**
	 * @return the disposition (emrapi code, localised label and configured rules) for the concept,
	 *         or null if it is not a disposition concept. A concept is a disposition when it
	 *         carries an emrapi code that is either configured in the disposition rules or is an
	 *         answer of the emrapi Disposition concept (the one that fills the disposition
	 *         dropdown).
	 */
	DispositionInfo getDisposition(Concept concept);
	
	String getAdtOrderTypeName();
}
