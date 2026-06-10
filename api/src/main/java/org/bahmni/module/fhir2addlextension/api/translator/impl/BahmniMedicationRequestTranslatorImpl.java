package org.bahmni.module.fhir2addlextension.api.translator.impl;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2addlextension.api.utils.BahmniFhirUtils;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Extension;
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

@Slf4j
@Component
@Primary
public class BahmniMedicationRequestTranslatorImpl extends MedicationRequestTranslatorImpl {
	
	private static final String DATE_STOPPED_EXTENSION_URL = "http://fhir.bahmni.org/ext/medicationRequest/dateStopped";
	
	@Autowired
	@Setter(value = AccessLevel.PACKAGE)
	private OrderService orderService;
	
	@Override
	public MedicationRequest toFhirResource(@Nonnull DrugOrder drugOrder) {
		MedicationRequest medicationRequest = super.toFhirResource(drugOrder);
		
		// Map dateStopped → extension, and look up stop reason from discontinuation order
		if (drugOrder.getDateStopped() != null) {
			medicationRequest.addExtension(new Extension(DATE_STOPPED_EXTENSION_URL, new DateTimeType(drugOrder
			        .getDateStopped())));
			
			// The stop reason and note live on the discontinuation order, not the original.
			// Find orders that reference this order as previousOrder.
			try {
				java.util.List<Order> allOrders = orderService.getAllOrdersByPatient(drugOrder.getPatient());
				for (Order o : allOrders) {
					if (o.getPreviousOrder() != null && o.getPreviousOrder().equals(drugOrder)
					        && Order.Action.DISCONTINUE.equals(o.getAction())) {
						String reason = o.getOrderReasonNonCoded();
						if (reason != null && !reason.isEmpty()) {
							CodeableConcept statusReason = new CodeableConcept();
							statusReason.setText(reason);
							medicationRequest.setStatusReason(statusReason);
						}
						if (o.getCommentToFulfiller() != null && !o.getCommentToFulfiller().isEmpty()) {
							medicationRequest.addNote(new Annotation().setText(o.getCommentToFulfiller()));
						}
						break;
					}
				}
			}
			catch (Exception e) {
				log.warn("Failed to look up discontinuation order for {}: {}", drugOrder.getUuid(), e.getMessage());
			}
		}
		
		// Map orderReasonNonCoded → statusReason.text (for orders that have it directly)
		if (!medicationRequest.hasStatusReason() && drugOrder.getOrderReasonNonCoded() != null
		        && !drugOrder.getOrderReasonNonCoded().isEmpty()) {
			CodeableConcept statusReason = new CodeableConcept();
			statusReason.setText(drugOrder.getOrderReasonNonCoded());
			medicationRequest.setStatusReason(statusReason);
		}
		
		// Map commentToFulfiller → note (for orders that have it directly)
		if (drugOrder.getCommentToFulfiller() != null && !drugOrder.getCommentToFulfiller().isEmpty()) {
			medicationRequest.addNote(new Annotation().setText(drugOrder.getCommentToFulfiller()));
		}
		
		return medicationRequest;
	}
	
	@Override
	public DrugOrder toOpenmrsType(@Nonnull DrugOrder existingDrugOrder, @Nonnull MedicationRequest medicationRequest) {
		DrugOrder drugOrder = super.toOpenmrsType(existingDrugOrder, medicationRequest);
		
		//TODO: This should be translated based on an extension of MedicationRequest to set correct CareSetting
		drugOrder.setCareSetting(orderService.getCareSettingByName(CareSetting.CareSettingType.OUTPATIENT.name()));
		
		translatePriorPrescription(drugOrder, medicationRequest);
		
		readBoundsPeriod(drugOrder, medicationRequest);
		
		if (drugOrder.getUrgency() != null && drugOrder.getUrgency().equals(Order.Urgency.STAT)) {
			drugOrder.setScheduledDate(null);
		} else if (drugOrder.getScheduledDate() != null) {
			drugOrder.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		}
		return drugOrder;
	}
	
	private void readBoundsPeriod(DrugOrder drugOrder, MedicationRequest medicationRequest) {
		if (!medicationRequest.hasDosageInstruction()) {
			return;
		}
		Timing timing = medicationRequest.getDosageInstructionFirstRep().getTiming();
		if (timing == null || timing.getRepeat() == null || !timing.getRepeat().hasBoundsPeriod()) {
			return;
		}
		Period boundsPeriod = timing.getRepeat().getBoundsPeriod();
		if (drugOrder.getScheduledDate() == null && boundsPeriod.hasStart()) {
			drugOrder.setScheduledDate(boundsPeriod.getStart());
		}
		if (drugOrder.getAutoExpireDate() == null && boundsPeriod.hasEnd()) {
			drugOrder.setAutoExpireDate(boundsPeriod.getEnd());
		}
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
			
			String priorUuid = BahmniFhirUtils.extractId(priorPrescriptionReference);
			if (priorUuid == null || priorUuid.isEmpty()) {
				return;
			}
			
			Order priorOrder = orderService.getOrderByUuid(priorUuid);
			if (priorOrder == null) {
				return;
			}
			
			if (!(priorOrder instanceof DrugOrder)) {
				return;
			}
			
			if (MedicationRequest.MedicationRequestStatus.STOPPED.equals(medicationRequest.getStatus())) {
				drugOrder.setAction(Order.Action.DISCONTINUE);
				drugOrder.setPreviousOrder(priorOrder);
			} else if (MedicationRequest.MedicationRequestStatus.ACTIVE.equals(medicationRequest.getStatus())) {
				// Explicit REVISE for edit flow — when REFILL is added, it should be handled
				// as a separate condition rather than falling into this branch.
				drugOrder.setAction(Order.Action.REVISE);
				drugOrder.setPreviousOrder(priorOrder);
			}
		}
		catch (Exception e) {
			log.warn("Failed to translate priorPrescription reference '{}', order will be created as NEW: {}",
			    medicationRequest.getPriorPrescription().getReference(), e.getMessage());
		}
	}
}
