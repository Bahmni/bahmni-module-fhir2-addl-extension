package org.bahmni.module.fhir2AddlExtension.api.service;

import org.hl7.fhir.r4.model.Reference;
import org.openmrs.Order;
import org.openmrs.Patient;

import java.util.Optional;

public interface LabResultsEncounterService {
	
	Reference createLabResultEncounter(Patient omrsPatient, Optional<Order> order);
}
