package org.bahmni.module.fhir2addlextension.api.service.impl;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import lombok.AccessLevel;
import lombok.Setter;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirMedicationRequestService;
import org.bahmni.module.fhir2addlextension.api.utils.MedicationRequestDateUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.api.EncounterService;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.api.impl.FhirMedicationRequestServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.Date;

@Component
@Primary
public class BahmniFhirMedicationRequestServiceImpl extends FhirMedicationRequestServiceImpl implements BahmniFhirMedicationRequestService {
	
	@Setter(onMethod_ = @Autowired, value = AccessLevel.PACKAGE)
	private OrderService orderService;
	
	@Setter(onMethod_ = @Autowired, value = AccessLevel.PACKAGE)
	private EncounterService encounterService;
	
	@Override
	public MedicationRequest stopMedicationRequest(String medicationRequestUuid, @Nullable CodeableConcept reason,
	        @Nullable Date effectiveDate, @Nullable String note) {
		
		Order existingOrder = orderService.getOrderByUuid(medicationRequestUuid);
		if (existingOrder == null) {
			throw new ResourceNotFoundException("MedicationRequest not found with id: " + medicationRequestUuid);
		}
		if (!(existingOrder instanceof DrugOrder)) {
			throw new UnprocessableEntityException("Order with id " + medicationRequestUuid + " is not a DrugOrder");
		}
		
		DrugOrder drugOrder = (DrugOrder) existingOrder;
		Date stopDate = effectiveDate != null ? effectiveDate : new Date();
		String reasonText = extractReasonText(reason, note);
		
		try {
			Date now = new Date();
			if (MedicationRequestDateUtils.isFutureDate(stopDate, now)) {
				drugOrder.setAutoExpireDate(stopDate);
				orderService.discontinueOrder(drugOrder, reasonText, now, drugOrder.getOrderer(), drugOrder.getEncounter());
			} else {
				orderService.discontinueOrder(drugOrder, reasonText, stopDate, drugOrder.getOrderer(),
				    drugOrder.getEncounter());
			}
		}
		catch (UnprocessableEntityException e) {
			throw e;
		}
		catch (Exception e) {
			throw new UnprocessableEntityException("Failed to stop medication: " + e.getMessage());
		}
		
		return get(medicationRequestUuid);
	}
	
	private String extractReasonText(@Nullable CodeableConcept reason, @Nullable String note) {
		String reasonText = null;
		if (reason != null) {
			if (reason.hasText()) {
				reasonText = reason.getText();
			} else if (reason.hasCoding() && reason.getCodingFirstRep().hasDisplay()) {
				reasonText = reason.getCodingFirstRep().getDisplay();
			}
		}
		if (note != null && !note.isEmpty()) {
			reasonText = reasonText != null ? reasonText + " - " + note : note;
		}
		return reasonText;
	}
}
