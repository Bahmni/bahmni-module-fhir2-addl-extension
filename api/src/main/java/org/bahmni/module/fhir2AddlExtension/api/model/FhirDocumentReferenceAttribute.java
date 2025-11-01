package org.bahmni.module.fhir2AddlExtension.api.model;

import org.openmrs.attribute.Attribute;
import org.openmrs.attribute.BaseAttribute;

public class FhirDocumentReferenceAttribute extends BaseAttribute<FhirDocumentReferenceAttributeType, FhirDocumentReference> implements Attribute<FhirDocumentReferenceAttributeType, FhirDocumentReference> {
	
	private Integer documentReferenceAttributeId;
	
	@Override
	public Integer getId() {
		return getDocumentReferenceAttributeId();
	}
	
	@Override
	public void setId(Integer id) {
		setDocumentReferenceAttributeId(id);
	}
	
	public Integer getDocumentReferenceAttributeId() {
		return documentReferenceAttributeId;
	}
	
	public void setDocumentReferenceAttributeId(Integer documentReferenceAttributeId) {
		this.documentReferenceAttributeId = documentReferenceAttributeId;
	}
	
	public FhirDocumentReference getFhirDocumentReference() {
		return super.getOwner();
	}
	
	public void setFhirDocumentReference(FhirDocumentReference documentReference) {
		super.setOwner(documentReference);
	}
}
