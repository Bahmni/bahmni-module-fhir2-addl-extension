package org.bahmni.module.fhir2AddlExtension.api.validators.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2AddlExtension.api.validators.ConsultationBundleValidator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ResourceType;
import org.springframework.stereotype.Component;

@Component
public class ConsultationBundleValidatorImpl implements ConsultationBundleValidator {
	
	@Override
	public void validateBundleType(Bundle bundle) {
		if (bundle.getType() != Bundle.BundleType.TRANSACTION) {
			throw new InvalidRequestException("Bundle type must be transaction");
		}
	}
	
	@Override
	public void validateBundleEntries(Bundle bundle) {
		int encounterEntryCount = 0;
		for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
			validateBundleEntry(entry);
			if (entry.getResource().getResourceType() == ResourceType.Encounter) {
				encounterEntryCount++;
			}
		}
		if (encounterEntryCount != 1) {
			throw new InvalidRequestException("Consultation bundle should contain only one Encounter entry. Found "
			        + encounterEntryCount + " instead.");
		}
	}
	
	private void validateBundleEntry(Bundle.BundleEntryComponent entryComponent) throws InvalidRequestException {
		boolean hasMandatoryFields = entryComponent.hasResource() && entryComponent.hasRequest()
		        && entryComponent.hasFullUrl();
		if (!hasMandatoryFields) {
			throw new InvalidRequestException("Bundle entries must contain fullUrl, resource and request fields");
		}
		ResourceType resourceType = entryComponent.getResource().getResourceType();
		boolean isResourceSupported = BahmniFhirConstants.CONSULTATION_BUNDLE_SUPPORTED_RESOURCES.contains(resourceType);
		if (!isResourceSupported) {
			throw new InvalidRequestException(String.format(
			    "Entry of resource type %s is not supported as part of Consultation Bundle", resourceType));
		}
	}
}
