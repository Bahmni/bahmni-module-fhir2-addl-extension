package org.bahmni.module.fhir2addlextension.api.translator.impl;

import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2addlextension.api.dao.DocumentReferenceAttributeTypeDao;
import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReferenceAttribute;
import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReferenceAttributeType;
import org.bahmni.module.fhir2addlextension.api.translator.DocumentReferenceAttributeTranslator;
import org.hl7.fhir.r4.model.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DefaultDocumentReferenceAttributeTranslatorImpl extends BaseAttributeTranslator<FhirDocumentReferenceAttribute, FhirDocumentReferenceAttributeType> implements DocumentReferenceAttributeTranslator {
	
	private static final String DOC_REF_ATTR_EXT_URL = BahmniFhirConstants.FHIR_EXT_DOCUMENT_REFERENCE_ATTRIBUTE + "#";
	
	private final DocumentReferenceAttributeTypeDao attributeTypeDao;
	
	@Autowired
	public DefaultDocumentReferenceAttributeTranslatorImpl(DocumentReferenceAttributeTypeDao attributeTypeDao) {
		this.attributeTypeDao = attributeTypeDao;
	}
	
	@Override
	protected String getExtensionUrlPrefix() {
		return DOC_REF_ATTR_EXT_URL;
	}
	
	@Override
	protected ResourceType getResourceType() {
		return ResourceType.DocumentReference;
	}
	
	@Override
	protected List<FhirDocumentReferenceAttributeType> getActiveAttributeTypes() {
		return attributeTypeDao.getAttributeTypes(false);
	}
	
	@Override
	protected FhirDocumentReferenceAttribute createAttribute() {
		return new FhirDocumentReferenceAttribute();
	}
}
