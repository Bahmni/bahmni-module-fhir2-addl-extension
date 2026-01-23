package org.bahmni.module.fhir2AddlExtension.api.utils;

import org.hl7.fhir.r4.model.Bundle;
import org.openmrs.module.fhir2.api.util.FhirUtils;

import java.util.Optional;

public class BahmniFhirUtils {
	
	public static <T> Optional<T> findResourceOfTypeInBundle(Bundle bundle, String resourceType, Class<T> type) {
        return bundle.getEntry().stream()
                .filter(entry -> entry.getResource().getResourceType().name().equals(resourceType))
                .findFirst().map(entry -> (T) entry.getResource());
    }
	
	public static <T> Optional<T> findResourceInBundle(Bundle bundle, String resourceId, Class<T> targetClass) {
        return bundle.getEntry().stream()
                .map(entry -> entry.getResource())
                .filter(resource -> {
                    String idPart = resource.getIdElement().getIdPart();
                    String extractIdPart = extractId(idPart);
                    return resourceId.equals(extractIdPart) && targetClass.isInstance(resource);
                })
                .map(resource -> targetClass.cast(resource))
                .findFirst();
    }
	
	public static Optional<String> referenceToId(String reference) {
		if (reference.startsWith("urn:uuid:")) {
			String idPart = reference.substring(reference.lastIndexOf(":") + 1);
			return idPart.trim().isEmpty() ? Optional.empty() : Optional.of(idPart.trim());
		}
		return FhirUtils.referenceToId(reference);
	}
	
	public static String extractId(String reference) {
		if (reference.startsWith("urn:uuid:")) {
			String idStr = reference.substring(reference.lastIndexOf(":") + 1);
			return idStr.trim().isEmpty() ? null : idStr.trim();
		}
		if (reference.startsWith("#")) {
			String idStr = reference.substring(reference.lastIndexOf("#") + 1);
			return idStr.trim().isEmpty() ? null : idStr.trim();
		}
		int separatorIndex = reference.indexOf("/");
		if (separatorIndex == -1) {
			return reference;
		}
		return FhirUtils.referenceToId(reference).orElse(null);
	}
	
}
