package org.bahmni.module.fhir2addlextension.api.service;

import org.openmrs.Concept;
import org.openmrs.OrderType;

public interface DispositionOrderTypeResolver {
	
	OrderType resolve(Concept concept);
}
