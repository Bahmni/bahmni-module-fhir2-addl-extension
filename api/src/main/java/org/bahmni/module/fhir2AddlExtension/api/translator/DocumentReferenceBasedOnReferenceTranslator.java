package org.bahmni.module.fhir2AddlExtension.api.translator;

import org.hl7.fhir.r4.model.Reference;
import org.openmrs.Order;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;

import javax.annotation.Nonnull;

public interface DocumentReferenceBasedOnReferenceTranslator extends OpenmrsFhirTranslator<Order, Reference> {
	
	/**
	 * Maps an {@link Order} to an {@link org.hl7.fhir.r4.model.Reference}
	 * 
	 * @param order the OpenMRS order element to translate
	 * @return a FHIR reference to the order which prompted this observation
	 */
	@Override
	Reference toFhirResource(@Nonnull Order order);
	
	/**
	 * Maps an {@link org.hl7.fhir.r4.model.Reference} to an {@link Order}
	 * 
	 * @param reference the resource to map
	 * @return the OpenMRS order matched by this reference
	 */
	@Override
	Order toOpenmrsType(@Nonnull Reference reference);
}
