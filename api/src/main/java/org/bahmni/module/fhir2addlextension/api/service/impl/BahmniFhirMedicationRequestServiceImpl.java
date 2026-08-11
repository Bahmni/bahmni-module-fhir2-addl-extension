package org.bahmni.module.fhir2addlextension.api.service.impl;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import lombok.AccessLevel;
import lombok.Setter;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirMedicationRequestService;
import org.bahmni.module.fhir2addlextension.api.utils.MedicationRequestDateUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.openmrs.Concept;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.api.EncounterService;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.api.impl.FhirMedicationRequestServiceImpl;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
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
	
	@Setter(onMethod_ = @Autowired, value = AccessLevel.PACKAGE)
	private ConceptTranslator conceptTranslator;
	
	@Override
	public MedicationRequest stopMedicationRequest(String medicationRequestUuid, @Nullable CodeableConcept reason,
	        @Nullable Date effectiveDate, @Nullable String note, @Nullable String encounterUuid) {
		
		Order existingOrder = orderService.getOrderByUuid(medicationRequestUuid);
		if (existingOrder == null) {
			throw new ResourceNotFoundException("MedicationRequest not found with id: " + medicationRequestUuid);
		}
		if (!(existingOrder instanceof DrugOrder)) {
			throw new UnprocessableEntityException("Order with id " + medicationRequestUuid + " is not a DrugOrder");
		}
		
		DrugOrder drugOrder = (DrugOrder) existingOrder;
		Date stopDate = effectiveDate != null ? effectiveDate : new Date();
		String reasonText = extractReasonText(reason);
		Concept reasonConcept = (reason != null && reason.hasCoding()) ? conceptTranslator.toOpenmrsType(reason) : null;
		Encounter encounter = (encounterUuid != null) ? encounterService.getEncounterByUuid(encounterUuid) : null;
		
		try {
			Date now = new Date();
			if (MedicationRequestDateUtils.isFutureDate(stopDate, now)) {
				stopDate = now;
			}
			
			DrugOrder discontinuationOrder = (DrugOrder) drugOrder.cloneForDiscontinuing();
			if (encounter != null) {
				discontinuationOrder.setEncounter(encounter);
			}
			discontinuationOrder.setOrderer(drugOrder.getOrderer());
			discontinuationOrder.setDateActivated(now);
			discontinuationOrder.setOrderReason(reasonConcept);
			discontinuationOrder.setOrderReasonNonCoded(reasonText);
			discontinuationOrder.setCommentToFulfiller(note);
			
			orderService.saveOrder(discontinuationOrder, null);
		}
		catch (UnprocessableEntityException e) {
			throw e;
		}
		catch (Exception e) {
			throw new UnprocessableEntityException("Failed to stop medication: " + e.getMessage());
		}
		
		return get(medicationRequestUuid);
	}
	
	private String extractReasonText(@Nullable CodeableConcept reason) {
		if (reason == null)
			return null;
		if (reason.hasText())
			return reason.getText();
		if (reason.hasCoding() && reason.getCodingFirstRep().hasDisplay())
			return reason.getCodingFirstRep().getDisplay();
		return null;
	}
}
