package org.bahmni.module.fhir2addlextension.api.translator.impl;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2addlextension.api.dao.OrderAttributeTypeDao;
import org.bahmni.module.fhir2addlextension.api.translator.ServiceRequestAttributeTranslator;
import org.bahmni.module.fhir2addlextension.api.utils.ModuleUtils;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.openmrs.OrderAttribute;
import org.openmrs.OrderAttributeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ServiceRequestAttributeTranslatorImpl implements ServiceRequestAttributeTranslator {
	
	public static final String DATATYPE_BOOLEAN = "org.openmrs.customdatatype.datatype.BooleanDatatype";
	
	private final OrderAttributeTypeDao attributeTypeDao;
	
	@Autowired
	public ServiceRequestAttributeTranslatorImpl(OrderAttributeTypeDao attributeTypeDao) {
		this.attributeTypeDao = attributeTypeDao;
	}
	
	@Override
	public boolean supports(OrderAttribute attribute) {
		return true;
	}
	
	@Override
	public List<OrderAttribute> toOpenmrsType(String extUrl, List<Extension> extensions) {
		Optional<OrderAttributeType> extAttributeType = getAttributeType(extUrl);
		if (!extAttributeType.isPresent()) {
			return Collections.emptyList();
		}
		
		List<OrderAttribute> attributes = extensions.stream()
				.filter(extension -> extension.getValue() != null)
				.map(extension -> {
					OrderAttribute attribute = new OrderAttribute();
					attribute.setAttributeType(extAttributeType.get());
					Type extensionValue = extension.getValue();
					if (extensionValue != null) {
						attribute.setValueReferenceInternal(extensionValue.primitiveValue());
					}
					return attribute;
				}).collect(Collectors.toList());
		
		// Check whether multi values are allowed for the attribute
		Integer allowedNumber = Optional.ofNullable(extAttributeType.get().getMaxOccurs()).orElse(1);
		if (attributes.size() > allowedNumber) {
			log.error(String.format("Found more than allowed instances [%d] of Order Attribute [%s]", allowedNumber,
					extAttributeType.get().getName()));
			throw new UnprocessableEntityException("ServiceRequest extension for attributes is over the allowed limit");
		}
		return attributes;
	}
	
	@Override
	public Extension toFhirResource(OrderAttribute attribute) {
		Extension extension = new Extension();
		extension.setUrl(BahmniFhirConstants.FHIR_EXT_SERVICE_REQUEST_ATTRIBUTE_PREFIX
		        + ModuleUtils.toSlugCase(attribute.getAttributeType().getName()));
		
		Type fhirValue = convertToFhirType(attribute);
		if (fhirValue != null) {
			extension.setValue(fhirValue);
		}
		
		return extension;
	}
	
	private Type convertToFhirType(OrderAttribute attribute) {
		Object value = attribute.getValue();
		if (value == null) {
			return null;
		}
		
		String datatypeClassName = attribute.getAttributeType().getDatatypeClassname();
		if (isBooleanDatatype(datatypeClassName)) {
			return new BooleanType(Boolean.parseBoolean(value.toString()));
		}
		
		// Default to StringType for all other datatypes
		return new StringType(value.toString());
	}
	
	private boolean isBooleanDatatype(String datatypeClassname) {
		return datatypeClassname != null && datatypeClassname.equals(DATATYPE_BOOLEAN);
	}
	
	@Override
	public Optional<OrderAttributeType> getAttributeType(String extUrl) {
		if (extUrl == null || !extUrl.startsWith(BahmniFhirConstants.FHIR_EXT_SERVICE_REQUEST_ATTRIBUTE_PREFIX)) {
			return Optional.empty();
		}
		
		String extAttributeName = extUrl.substring(BahmniFhirConstants.FHIR_EXT_SERVICE_REQUEST_ATTRIBUTE_PREFIX.length());
		if (extAttributeName.isEmpty()) {
			return Optional.empty();
		}
		
		List<OrderAttributeType> definedAttributeTypes = attributeTypeDao.getOrderAttributeTypes(false);
		return definedAttributeTypes.stream()
				.filter(attrType -> ModuleUtils.toSlugCase(attrType.getName()).equals(extAttributeName))
				.findFirst();
	}
}
