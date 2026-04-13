package org.bahmni.module.fhir2addlextension.api.translator.impl;

import org.bahmni.module.fhir2addlextension.api.translator.ServiceRequestAttributeTranslator;
import org.bahmni.module.fhir2addlextension.api.translator.ServiceRequestExtensionTranslator;
import org.hl7.fhir.r4.model.Extension;
import org.openmrs.OrderAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
public class ServiceRequestExtensionTranslatorImpl implements ServiceRequestExtensionTranslator {
	
	private Set<ServiceRequestAttributeTranslator> attributeTranslators = new HashSet<>();
	private final ServiceRequestAttributeTranslatorImpl defaultAttributeTranslator;
	
	@Autowired
	public ServiceRequestExtensionTranslatorImpl(ServiceRequestAttributeTranslatorImpl defaultAttributeTranslator) {
		this.defaultAttributeTranslator = defaultAttributeTranslator;
	}
	
	@Override
	public boolean hasAttributeTranslator(Extension extension) {
		return getAttributeTranslator(extension.getUrl()).isPresent();
	}
	
	@Override
	public Optional<ServiceRequestAttributeTranslator> getAttributeTranslator(String extensionUrl) {
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
	public Optional<ServiceRequestAttributeTranslator> getAttributeTranslator(OrderAttribute attribute) {
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
	public void registerAttributeTranslator(ServiceRequestAttributeTranslator translator) {
		if (attributeTranslators != null) {
			attributeTranslators.add(translator);
		}
	}
}
