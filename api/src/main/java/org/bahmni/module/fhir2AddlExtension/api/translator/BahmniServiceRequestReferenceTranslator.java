package org.bahmni.module.fhir2AddlExtension.api.translator;

import org.hl7.fhir.r4.model.Reference;
import org.openmrs.Order;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;

import javax.annotation.Nonnull;

/**
 * TODO: to be replaced in future by https://openmrs.atlassian.net/browse/FM2-675 pending PR merge
 * of https://github.com/openmrs/openmrs-module-fhir2/pull/583/files and upgrade to fhir2 module
 * where the above PR is reflected
 */
public interface BahmniServiceRequestReferenceTranslator extends OpenmrsFhirTranslator<Order, Reference> {
	
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
