package org.bahmni.module.fhir2addlextension.api.translator.impl;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2addlextension.api.translator.AttributeTranslator;
import org.bahmni.module.fhir2addlextension.api.utils.ModuleUtils;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.openmrs.attribute.BaseAttribute;
import org.openmrs.attribute.BaseAttributeType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public abstract class BaseAttributeTranslator<A extends BaseAttribute<U, ?>, U extends BaseAttributeType<?>> implements AttributeTranslator<A, U> {
	
	private static final String DATATYPE_BOOLEAN = "org.openmrs.customdatatype.datatype.BooleanDatatype";
	
	protected abstract String getExtensionUrlPrefix();
	
	protected abstract ResourceType getResourceType();
	
	protected abstract List<U> getActiveAttributeTypes();
	
	protected abstract A createAttribute();
	
	@Override
	public boolean supports(A attribute) {
		return true;
	}
	
	protected boolean supports(String extUrl) {
		return extUrl != null && extUrl.startsWith(getExtensionUrlPrefix());
	}
	
	@Override
	public List<A> toOpenmrsType(String extUrl, List<Extension> extensions) {
		Optional<U> extAttributeType = getAttributeType(extUrl);
		if (!extAttributeType.isPresent()) {
			return Collections.emptyList();
		}

		List<A> attributes = extensions.stream()
				.filter(extension -> extension.getValue() != null)
				.map(extension -> {
					A attribute = createAttribute();
					attribute.setAttributeType(extAttributeType.get());
					Type extensionValue = extension.getValue();
					if (extensionValue != null) {
						attribute.setValueReferenceInternal(extensionValue.primitiveValue());
					}
					return attribute;
				}).collect(Collectors.toList());

		Integer allowedNumber = Optional.ofNullable(extAttributeType.get().getMaxOccurs()).orElse(1);
		if (attributes.size() > allowedNumber) {
			log.error(String.format("Found more than allowed instances [%d] of %s Attribute [%s]",
					allowedNumber, getResourceType().name(), extAttributeType.get().getName()));
			throw new UnprocessableEntityException(
					getResourceType().name() + " extension for attributes is over the allowed limit");
		}
		return attributes;
	}
	
	@Override
	public Extension toFhirResource(A attribute) {
		Extension extension = new Extension();
		extension.setUrl(getExtensionUrlPrefix() + ModuleUtils.toSlugCase(attribute.getAttributeType().getName()));
		
		Type fhirValue = convertToFhirType(attribute);
		if (fhirValue != null) {
			extension.setValue(fhirValue);
		}
		
		return extension;
	}
	
	protected Type convertToFhirType(A attribute) {
		Object value = attribute.getValue();
		if (value == null) {
			return null;
		}
		
		String datatypeClassName = attribute.getAttributeType().getDatatypeClassname();
		if (DATATYPE_BOOLEAN.equals(datatypeClassName)) {
			return new BooleanType(Boolean.parseBoolean(value.toString()));
		}
		
		return new StringType(value.toString());
	}
	
	@Override
	public Optional<U> getAttributeType(String extUrl) {
		if (!supports(extUrl)) {
			return Optional.empty();
		}

		String extAttributeName = extUrl.substring(getExtensionUrlPrefix().length());
		if (extAttributeName.isEmpty()) {
			return Optional.empty();
		}

		List<U> definedAttributeTypes = getActiveAttributeTypes();
		return definedAttributeTypes.stream()
				.filter(attrType -> ModuleUtils.toSlugCase(attrType.getName()).equals(extAttributeName))
				.findFirst();
	}
}
