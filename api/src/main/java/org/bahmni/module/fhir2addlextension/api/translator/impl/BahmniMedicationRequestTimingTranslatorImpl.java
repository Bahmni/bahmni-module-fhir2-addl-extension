package org.bahmni.module.fhir2addlextension.api.translator.impl;

import java.util.Date;

import javax.annotation.Nonnull;

import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Timing;
import org.openmrs.DrugOrder;
import org.openmrs.module.fhir2.api.translators.impl.MedicationRequestTimingTranslatorImpl;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Upstream MedicationRequestTimingTranslatorImpl only reads/writes timing.event and ignores
 * timing.repeat.boundsPeriod entirely (the repeat translator drops it). This override teaches both
 * directions to honour boundsPeriod alongside the existing timing.event behaviour:
 * <ul>
 * <li>toOpenmrsType: if scheduledDate / autoExpireDate are still null after super, fall back to
 * boundsPeriod.start / boundsPeriod.end.</li>
 * <li>toFhirResource: in addition to timing.event, emit timing.repeat.boundsPeriod so consumers
 * reading boundsPeriod (per FHIR semantics for ranged validity) see the same data.</li>
 * </ul>
 */
@Component
@Primary
public class BahmniMedicationRequestTimingTranslatorImpl extends MedicationRequestTimingTranslatorImpl {
	
	@Override
	public Timing toFhirResource(@Nonnull DrugOrder drugOrder) {
		Timing timing = super.toFhirResource(drugOrder);
		if (timing == null) {
			return null;
		}
		
		Date start = drugOrder.getScheduledDate();
		Date end = drugOrder.getAutoExpireDate();
		if (start == null && end == null) {
			return timing;
		}
		
		Timing.TimingRepeatComponent repeat = timing.getRepeat();
		if (repeat == null) {
			repeat = new Timing.TimingRepeatComponent();
			timing.setRepeat(repeat);
		}
		Period boundsPeriod = new Period();
		if (start != null) {
			boundsPeriod.setStart(start);
		}
		if (end != null) {
			boundsPeriod.setEnd(end);
		}
		repeat.setBounds(boundsPeriod);
		return timing;
	}
	
	@Override
	public DrugOrder toOpenmrsType(@Nonnull DrugOrder drugOrder, @Nonnull Timing timing) {
		super.toOpenmrsType(drugOrder, timing);
		
		if (timing.getRepeat() != null && timing.getRepeat().hasBoundsPeriod()) {
			Period boundsPeriod = timing.getRepeat().getBoundsPeriod();
			if (drugOrder.getScheduledDate() == null && boundsPeriod.hasStart()) {
				drugOrder.setScheduledDate(boundsPeriod.getStart());
			}
			if (drugOrder.getAutoExpireDate() == null && boundsPeriod.hasEnd()) {
				drugOrder.setAutoExpireDate(boundsPeriod.getEnd());
			}
		}
		return drugOrder;
	}
}
