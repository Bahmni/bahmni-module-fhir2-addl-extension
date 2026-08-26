package org.bahmni.module.fhir2addlextension.api.validators.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2addlextension.api.helper.EncounterBundleEntriesHelper;
import org.bahmni.module.fhir2addlextension.api.validators.EncounterBundleValidator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EncounterBundleValidatorImpl implements EncounterBundleValidator {
	
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
		if (encounterEntryCount > 1) {
			throw new InvalidRequestException("Encounter bundle should contain at most one Encounter entry. Found "
			        + encounterEntryCount + " instead.");
		}
		if (encounterEntryCount == 0) {
			validateEveryEntryReferencesAnExistingEncounter(bundle);
		}
	}
	
	/**
	 * A bundle may omit the Encounter entry, but only when every other entry already names an encounter
	 * that exists server-side. That keeps this endpoint encounter-scoped while letting a caller attach
	 * resources to an existing encounter without re-sending — and so overwriting — the encounter itself.
	 */
	private void validateEveryEntryReferencesAnExistingEncounter(Bundle bundle) {
		for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
			// DELETE entries are identified by resource id alone and carry no encounter context.
			if (entry.getRequest().getMethod() == Bundle.HTTPVerb.DELETE) {
				continue;
			}
			List<Reference> encounterReferences = EncounterBundleEntriesHelper.encounterReferencesOf(entry
			        .getResource());
			boolean namesAnExistingEncounter = !encounterReferences.isEmpty()
			        && encounterReferences.stream().allMatch(EncounterBundleEntriesHelper::isConcreteReference);
			if (!namesAnExistingEncounter) {
				throw new InvalidRequestException(String.format(
				    "Bundle has no Encounter entry, so entry [%s] must reference an existing encounter",
				    entry.getFullUrl()));
			}
		}
	}
	
	private void validateBundleEntry(Bundle.BundleEntryComponent entryComponent) throws InvalidRequestException {
		boolean hasMandatoryFields = entryComponent.hasResource() && entryComponent.hasRequest()
		        && entryComponent.hasFullUrl();
		if (!hasMandatoryFields) {
			throw new InvalidRequestException("Bundle entries must contain fullUrl, resource and request fields");
		}
		ResourceType resourceType = entryComponent.getResource().getResourceType();
		boolean isResourceSupported = BahmniFhirConstants.ENCOUNTER_BUNDLE_SUPPORTED_RESOURCES.contains(resourceType);
		if (!isResourceSupported) {
			throw new InvalidRequestException(String.format(
			    "Entry of resource type %s is not supported as part of Encounter Bundle", resourceType));
		}
	}
}
