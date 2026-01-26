package org.bahmni.module.fhir2AddlExtension.api.utils;

import org.hl7.fhir.r4.model.Bundle;
import org.openmrs.module.fhir2.api.util.FhirUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BahmniFhirUtils {
	
	public static <T> List<T> findResourcesOfTypeInBundle(Bundle bundle, Class<T> targetClass) {
		return bundle.getEntry().stream()
				.filter(entry -> targetClass.isInstance (entry.getResource()))
				.map(entry -> targetClass.cast(entry.getResource()))
				.collect(Collectors.toList());
	}
	
	public static <T> Optional<T> findResourceInBundle(Bundle bundle, String idParam, Class<T> targetClass) {
        return bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> {
					String extractIdPart = extractId(resource.getIdElement().getIdPart());
                    return idParam.equals(extractIdPart) && targetClass.isInstance(resource);
                })
                .map(targetClass::cast)
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
