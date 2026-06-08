package org.bahmni.module.fhir2addlextension.api.translator.impl;

import org.bahmni.module.fhir2addlextension.api.translator.AttributeTranslator;
import org.bahmni.module.fhir2addlextension.api.translator.AttributeTranslatorRegistry;
import org.hl7.fhir.r4.model.Extension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public abstract class BaseAttributeTranslatorRegistry<A, U, T extends AttributeTranslator<A, U>>
		implements AttributeTranslatorRegistry<A, U, T> {

	private final Set<T> attributeTranslators = new HashSet<>();

	private final T defaultAttributeTranslator;

	protected BaseAttributeTranslatorRegistry(T defaultAttributeTranslator) {
		this.defaultAttributeTranslator = defaultAttributeTranslator;
	}

	@Override
	public boolean hasAttributeTranslator(Extension extension) {
		return getAttributeTranslator(extension.getUrl()).isPresent();
	}

	@Override
	public Optional<T> getAttributeTranslator(String extensionUrl) {
		return Optional.ofNullable(attributeTranslators.stream()
				.filter(translator -> translator.getAttributeType(extensionUrl).isPresent())
				.findFirst()
				.orElseGet(() -> {
					if (defaultAttributeTranslator.getAttributeType(extensionUrl).isPresent()) {
						return defaultAttributeTranslator;
					} else {
						return null;
					}
				}));
	}

	@Override
	public Optional<T> getAttributeTranslator(A attribute) {
		return Optional.ofNullable(attributeTranslators.stream()
				.filter(translator -> translator.supports(attribute))
				.findFirst()
				.orElseGet(() -> {
					if (defaultAttributeTranslator.supports(attribute)) {
						return defaultAttributeTranslator;
					} else {
						return null;
					}
				}));
	}

	@Override
	public void registerAttributeTranslator(T translator) {
		attributeTranslators.add(translator);
	}
}
