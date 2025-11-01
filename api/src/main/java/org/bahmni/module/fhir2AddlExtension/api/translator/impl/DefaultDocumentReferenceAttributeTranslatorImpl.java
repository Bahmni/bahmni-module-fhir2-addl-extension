package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2AddlExtension.api.dao.DocumentReferenceAttributeTypeDao;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReferenceAttribute;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReferenceAttributeType;
import org.bahmni.module.fhir2AddlExtension.api.translator.DocumentReferenceAttributeTranslator;
import org.bahmni.module.fhir2AddlExtension.api.utils.ModuleUtils;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DefaultDocumentReferenceAttributeTranslatorImpl implements DocumentReferenceAttributeTranslator {
	
	private static final String DOC_REF_ATTR_EXT_URL = BahmniFhirConstants.FHIR_EXT_DOCUMENT_REFERENCE_ATTRIBUTE + "#";
	
	private final DocumentReferenceAttributeTypeDao attributeTypeDao;
	
	@Autowired
	public DefaultDocumentReferenceAttributeTranslatorImpl(DocumentReferenceAttributeTypeDao attributeTypeDao) {
		this.attributeTypeDao = attributeTypeDao;
	}
	
	@Override
	public boolean supports(String extUrl) {
		return extUrl != null && extUrl.startsWith(DOC_REF_ATTR_EXT_URL);
	}
	
	@Override
    public List<FhirDocumentReferenceAttribute> toOpenmrsType(String extUrl, List<Extension> extensions) {
        if (!supports(extUrl)) {
            return Collections.emptyList();
        }
        String extAttributeName = extUrl.substring(DOC_REF_ATTR_EXT_URL.length());
        List<FhirDocumentReferenceAttributeType> attributeTypes = attributeTypeDao.getAttributeTypes(false);
        Optional<FhirDocumentReferenceAttributeType> attributeType
                = attributeTypes.stream().filter(attrType -> ModuleUtils.toSlugCase(attrType.getName()).equals(extAttributeName)).findFirst();
        if (!attributeType.isPresent()) {
            log.error("Can not identify any corresponding DocumentReference Attribute for extension:" + extUrl);
            throw new UnprocessableEntityException(String.format("Unknown extension [%] for Document Reference", extUrl));
        }
        //String datatypeClassname = attributeType.get().getDatatypeClassname();
        List<FhirDocumentReferenceAttribute> attributes = extensions.stream()
            .filter(extension -> extension.getValue() != null)
            .map(extension -> {
                FhirDocumentReferenceAttribute attribute = new FhirDocumentReferenceAttribute();
                attribute.setAttributeType(attributeType.get());
                Type extensionValue = extension.getValue();
                if (extensionValue != null) {
                    attribute.setValueReferenceInternal(extensionValue.primitiveValue());
                }
                return attribute;
            }).collect(Collectors.toList());
        //check whether multi values are allowed for the attribute
        Integer allowedNumber = Optional.ofNullable(attributeType.get().getMaxOccurs()).orElse(1);
        if (attributes.size() > allowedNumber) {
            log.error(String.format("Only %d is allowed for Document Reference Attribute: %s", allowedNumber, attributeType.get().getName()));
            throw new UnprocessableEntityException("Document Reference extension for attributes is over the allowed limit");
        }
        return attributes;
    }
}
