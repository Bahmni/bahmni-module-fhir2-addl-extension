package org.bahmni.module.fhir2AddlExtension.api.translator;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.openmrs.OrderType;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;

import javax.annotation.Nonnull;

public interface OrderTypeTranslator extends OpenmrsFhirTranslator<OrderType, CodeableConcept> {
	
	@Override
	CodeableConcept toFhirResource(@Nonnull OrderType visitType);
	
	@Override
	OrderType toOpenmrsType(@Nonnull CodeableConcept codeableConcept);
}
