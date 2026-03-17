package org.bahmni.module.fhir2addlextension.api.dao;

import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReferenceAttributeType;

import java.util.List;

public interface DocumentReferenceAttributeTypeDao {
	
	List<FhirDocumentReferenceAttributeType> getAttributeTypes(boolean includeRetired);
}
