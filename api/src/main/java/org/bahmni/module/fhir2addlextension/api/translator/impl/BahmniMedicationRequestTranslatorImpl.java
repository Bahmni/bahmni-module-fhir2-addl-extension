package org.bahmni.module.fhir2addlextension.api.translator.impl;

import lombok.AccessLevel;
import lombok.Setter;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Timing;
import org.openmrs.CareSetting;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.api.translators.impl.MedicationRequestTranslatorImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;

@Component
@Primary
public class BahmniMedicationRequestTranslatorImpl extends MedicationRequestTranslatorImpl {
	
	@Autowired
	@Setter(value = AccessLevel.PACKAGE)
	private OrderService orderService;
	
	@Override
	public DrugOrder toOpenmrsType(@Nonnull DrugOrder existingDrugOrder, @Nonnull MedicationRequest medicationRequest) {
		DrugOrder drugOrder = super.toOpenmrsType(existingDrugOrder, medicationRequest);
		
		//TODO: This should be translated based on an extension of MedicationRequest to set correct CareSetting
		drugOrder.setCareSetting(orderService.getCareSettingByName(CareSetting.CareSettingType.OUTPATIENT.name()));
		
		translatePriorPrescription(drugOrder, medicationRequest);
		
		if (drugOrder.getUrgency() != null && drugOrder.getUrgency().equals(Order.Urgency.STAT)) {
			drugOrder.setScheduledDate(null);
			// MedicationRequestTimingRepeatComponentTranslatorImpl silently drops boundsPeriod.
			// Read it directly here to set autoExpireDate so OpenMRS knows when the STAT order
			// expires and won't block a new order for the same drug.
			if (medicationRequest.hasDosageInstruction()) {
				Timing timing = medicationRequest.getDosageInstructionFirstRep().getTiming();
				if (timing != null && timing.getRepeat() != null && timing.getRepeat().hasBoundsPeriod()) {
					Period boundsPeriod = timing.getRepeat().getBoundsPeriod();
					if (boundsPeriod.hasEnd()) {
						drugOrder.setAutoExpireDate(boundsPeriod.getEnd());
					}
				}
			}
		} else if (drugOrder.getScheduledDate() != null) {
			drugOrder.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		}
		return drugOrder;
	}
	
	private void translatePriorPrescription(@Nonnull DrugOrder drugOrder, @Nonnull MedicationRequest medicationRequest) {
		if (!medicationRequest.hasPriorPrescription()) {
			return;
		}
		
		try {
			String priorPrescriptionReference = medicationRequest.getPriorPrescription().getReference();
			if (!StringUtils.hasText(priorPrescriptionReference)) {
				return;
			}
			
			String priorUuid = extractUuidFromReference(priorPrescriptionReference);
			if (!StringUtils.hasText(priorUuid)) {
				return;
			}
			
			Order priorOrder = orderService.getOrderByUuid(priorUuid);
			if (priorOrder == null) {
				return;
			}
			
			if (!(priorOrder instanceof DrugOrder)) {
				return;
			}
			
			drugOrder.setAction(Order.Action.REVISE);
			drugOrder.setPreviousOrder(priorOrder);
		}
		catch (Exception e) {
			// Don't throw - allow the order to be created as NEW if translation fails
		}
	}
	
	private String extractUuidFromReference(String reference) {
		if (!StringUtils.hasText(reference)) {
			return "";
		}
		
		if (reference.contains("/")) {
			String[] parts = reference.split("/");
			return parts[parts.length - 1];
		}
		
		if (reference.startsWith("urn:uuid:")) {
			return reference.substring("urn:uuid:".length());
		}
		
		return reference;
	}
}
