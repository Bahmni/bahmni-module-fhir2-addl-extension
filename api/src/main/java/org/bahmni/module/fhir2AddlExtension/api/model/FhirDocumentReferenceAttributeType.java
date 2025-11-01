package org.bahmni.module.fhir2AddlExtension.api.model;

import org.openmrs.attribute.AttributeType;
import org.openmrs.attribute.BaseAttributeType;

public class FhirDocumentReferenceAttributeType extends BaseAttributeType<FhirDocumentReference> implements AttributeType<FhirDocumentReference> {
	
	private Integer documentReferenceAttributeTypeId;
	
	@Override
	public Integer getId() {
		return getDocumentReferenceAttributeTypeId();
	}
	
	@Override
	public void setId(Integer id) {
		setDocumentReferenceAttributeTypeId(id);
	}
	
	public Integer getDocumentReferenceAttributeTypeId() {
		return documentReferenceAttributeTypeId;
	}
	
	public void setDocumentReferenceAttributeTypeId(Integer episodeAttributeTypeId) {
		this.documentReferenceAttributeTypeId = episodeAttributeTypeId;
	}
}
