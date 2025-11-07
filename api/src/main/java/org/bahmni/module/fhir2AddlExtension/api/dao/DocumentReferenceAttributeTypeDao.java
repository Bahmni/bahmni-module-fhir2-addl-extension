package org.bahmni.module.fhir2AddlExtension.api.dao;

import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReferenceAttributeType;

import java.util.List;

public interface DocumentReferenceAttributeTypeDao {
	
	List<FhirDocumentReferenceAttributeType> getAttributeTypes(boolean includeRetired);
}
