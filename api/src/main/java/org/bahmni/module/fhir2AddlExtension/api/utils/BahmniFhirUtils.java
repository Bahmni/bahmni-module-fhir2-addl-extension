package org.bahmni.module.fhir2AddlExtension.api.utils;

import org.hl7.fhir.r4.model.Bundle;

import java.util.Optional;

public class BahmniFhirUtils {
	
	public static <T> Optional<T> findResourceOfTypeInBundle(Bundle bundle, String resourceType, Class<T> type) {
        return bundle.getEntry().stream()
                .filter(entry -> entry.getResource().getResourceType().name().equals(resourceType))
                .findFirst().map(entry -> (T) entry.getResource());
    }
}
