package org.bahmni.module.fhir2addlextension.api.providers;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.StringType;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.api.EncounterService;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.api.FhirMedicationRequestService;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.providers.r4.MedicationRequestFhirResourceProvider;
import org.openmrs.module.fhir2.providers.util.FhirProviderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@R4Provider
public class BahmniMedicationRequestFhirR4Provider extends MedicationRequestFhirResourceProvider {
	
	@Autowired
	private FhirMedicationRequestService fhirMedicationRequestService;
	
	@Autowired
	private OrderService orderService;
	
	@Autowired
	private EncounterService encounterService;
	
	@Create
	public MethodOutcome createMedicationRequest(@ResourceParam MedicationRequest medicationRequest) {
		return FhirProviderUtils.buildCreate(fhirMedicationRequestService.create(medicationRequest));
	}
	
	/**
	 * FHIR $stop operation for MedicationRequest. Discontinues a DrugOrder in OpenMRS and returns
	 * the updated MedicationRequest. POST /openmrs/ws/fhir2/R4/MedicationRequest/{id}/$stop
	 * 
	 * @param theId The MedicationRequest (DrugOrder) UUID to stop
	 * @param reason The reason for stopping (mapped to orderReasonNonCoded)
	 * @param effectiveDate The date the medication should be stopped (defaults to now)
	 * @param note Optional note about the stop action
	 * @return The updated MedicationRequest reflecting the stopped status
	 */
	@Operation(name = "$stop", idempotent = false)
	public MedicationRequest stopMedicationRequest(@IdParam IdType theId,
	        @OperationParam(name = "reason") StringType reason,
	        @OperationParam(name = "effectiveDate") DateType effectiveDate, @OperationParam(name = "note") StringType note) {
		
		String uuid = theId.getIdPart();
		
		Order existingOrder = orderService.getOrderByUuid(uuid);
		if (existingOrder == null) {
			throw new ResourceNotFoundException("MedicationRequest not found with id: " + uuid);
		}
		
		if (!(existingOrder instanceof DrugOrder)) {
			throw new ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException("Order with id " + uuid
			        + " is not a DrugOrder");
		}
		
		DrugOrder drugOrder = (DrugOrder) existingOrder;
		
		// Determine stop date — default to now
		Date stopDate = effectiveDate != null ? effectiveDate.getValue() : new Date();
		
		// Build reason text
		String reasonText = reason != null ? reason.getValue() : null;
		if (note != null && note.getValue() != null) {
			reasonText = reasonText != null ? reasonText + " - " + note.getValue() : note.getValue();
		}
		
		Encounter encounter = drugOrder.getEncounter();
		
		try {
			// OpenMRS discontinueOrder rejects future dates with IllegalArgumentException.
			// For future stop dates: discontinue immediately (with today) and set autoExpireDate
			// so OpenMRS knows when the order should expire.
			Date now = new Date();
			boolean isFutureDate = stopDate.after(now) && !isSameDay(stopDate, now);
			
			if (isFutureDate) {
				drugOrder.setAutoExpireDate(stopDate);
				orderService.discontinueOrder(drugOrder, reasonText, now, drugOrder.getOrderer(), encounter);
			} else {
				orderService.discontinueOrder(drugOrder, reasonText, stopDate, drugOrder.getOrderer(), encounter);
			}
		}
		catch (Exception e) {
			throw new ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException("Failed to stop medication: "
			        + e.getMessage());
		}
		
		// Return the updated MedicationRequest
		return fhirMedicationRequestService.get(uuid);
	}
	
	private boolean isSameDay(Date date1, Date date2) {
		java.util.Calendar cal1 = java.util.Calendar.getInstance();
		cal1.setTime(date1);
		java.util.Calendar cal2 = java.util.Calendar.getInstance();
		cal2.setTime(date2);
		return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR)
		        && cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR);
	}
}
