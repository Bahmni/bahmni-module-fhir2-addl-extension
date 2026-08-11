package org.bahmni.module.fhir2addlextension.api.translator.impl;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2addlextension.api.utils.BahmniFhirUtils;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Timing;
import org.openmrs.Concept;
import org.openmrs.CareSetting;
import org.openmrs.DrugOrder;
import org.openmrs.Order;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.impl.MedicationRequestTranslatorImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;
import java.util.Date;

@Slf4j
@Component
@Primary
public class BahmniMedicationRequestTranslatorImpl extends MedicationRequestTranslatorImpl {
	
	@Autowired
	@Setter(value = AccessLevel.PACKAGE)
	private OrderService orderService;
	
	@Autowired
	@Setter(value = AccessLevel.PACKAGE)
	private ConceptTranslator conceptTranslator;
	
	@Override
	public MedicationRequest toFhirResource(@Nonnull DrugOrder drugOrder) {
		MedicationRequest medicationRequest = super.toFhirResource(drugOrder);

		// Tag all notes added by super (order notes from commentToFulfiller) as "order-note"
		for (Annotation note : medicationRequest.getNote()) {
			boolean alreadyTagged = note.getExtension().stream()
			        .anyMatch(e -> BahmniFhirConstants.FHIR_EXT_MEDICATION_REQUEST_NOTE_CATEGORY.equals(e.getUrl()));
			if (!alreadyTagged) {
				note.addExtension(new Extension(BahmniFhirConstants.FHIR_EXT_MEDICATION_REQUEST_NOTE_CATEGORY,
				        new CodeType("order-note")));
			}
		}

		if (drugOrder.getDateStopped() != null) {
			medicationRequest.addExtension(new Extension(BahmniFhirConstants.FHIR_EXT_MEDICATION_REQUEST_DATE_STOPPED,
			        new DateTimeType(drugOrder.getDateStopped())));

			try {
				Order discontinuationOrder = orderService.getDiscontinuationOrder(drugOrder);
				if (discontinuationOrder != null) {
					CodeableConcept statusReason = new CodeableConcept();
					if (discontinuationOrder.getOrderReason() != null) {
						CodeableConcept coded = conceptTranslator.toFhirResource(discontinuationOrder.getOrderReason());
						if (coded != null) {
							statusReason.setCoding(coded.getCoding());
						}
					}
					String reason = discontinuationOrder.getOrderReasonNonCoded();
					if (reason != null && !reason.isEmpty()) {
						statusReason.setText(reason);
					}
					if (!statusReason.isEmpty()) {
						medicationRequest.setStatusReason(statusReason);
					}
					if (discontinuationOrder.getCommentToFulfiller() != null
					        && !discontinuationOrder.getCommentToFulfiller().isEmpty()) {
						Annotation cancellationNote = new Annotation();
						cancellationNote.setText(discontinuationOrder.getCommentToFulfiller());
						cancellationNote.addExtension(new Extension(
						        BahmniFhirConstants.FHIR_EXT_MEDICATION_REQUEST_NOTE_CATEGORY,
						        new CodeType("cancellation-note")));
						medicationRequest.addNote(cancellationNote);
					}
				}
			}
			catch (Exception e) {
				log.warn("Failed to look up discontinuation order for {}: {}", drugOrder.getUuid(), e.getMessage());
			}
			if (!medicationRequest.hasStatusReason() && drugOrder.getOrderReasonNonCoded() != null
			        && !drugOrder.getOrderReasonNonCoded().isEmpty()) {
				medicationRequest.setStatusReason(new CodeableConcept().setText(drugOrder.getOrderReasonNonCoded()));
			}
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
	
	private void translateStopMedicationOrder(@Nonnull DrugOrder drugOrder, @Nonnull MedicationRequest medicationRequest) {
		if (medicationRequest.hasStatusReason()) {
			CodeableConcept statusReason = medicationRequest.getStatusReason();
			if (statusReason.hasCoding()) {
				Concept reasonConcept = conceptTranslator.toOpenmrsType(statusReason);
				if (reasonConcept != null) {
					drugOrder.setOrderReason(reasonConcept);
				}
			}
			if (statusReason.hasText() && !statusReason.getText().isEmpty()) {
				drugOrder.setOrderReasonNonCoded(statusReason.getText());
			}
		}

		medicationRequest.getNote().stream()
		        .filter(n -> "cancellation-note".equals(getNoteCategory(n)))
		        .findFirst()
		        .ifPresent(n -> {
			        if (n.hasText()) {
				        drugOrder.setCommentToFulfiller(n.getText());
			        }
		        });

		// Use now as dateActivated — stop date (effectiveDate from frontend) may be
		// midnight UTC which falls before the encounter datetime and fails validation.
		drugOrder.setDateActivated(new Date());

		if (drugOrder.getPreviousOrder() instanceof DrugOrder) {
			DrugOrder priorDrugOrder = (DrugOrder) drugOrder.getPreviousOrder();
			if (drugOrder.getOrderer() == null) {
				drugOrder.setOrderer(priorDrugOrder.getOrderer());
			}
			if (drugOrder.getConcept() == null) {
				drugOrder.setConcept(priorDrugOrder.getConcept());
				drugOrder.setDrug(priorDrugOrder.getDrug());
			}
			drugOrder.setAsNeeded(priorDrugOrder.getAsNeeded());
		}
	}
	
	private String getNoteCategory(Annotation note) {
		return note.getExtension().stream()
		        .filter(e -> BahmniFhirConstants.FHIR_EXT_MEDICATION_REQUEST_NOTE_CATEGORY.equals(e.getUrl()))
		        .map(e -> ((CodeType) e.getValue()).getValue())
		        .findFirst()
		        .orElse(null);
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
				translateStopMedicationOrder(drugOrder, medicationRequest);
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
