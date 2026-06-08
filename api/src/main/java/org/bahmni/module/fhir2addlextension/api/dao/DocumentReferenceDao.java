package org.bahmni.module.fhir2addlextension.api.dao;

import org.bahmni.module.fhir2addlextension.api.PrivilegeConstants;
import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReference;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

public interface DocumentReferenceDao extends FhirDao<FhirDocumentReference> {
	
	@Authorized({ PrivilegeConstants.DELETE_DOCUMENT_REFERENCE })
	void voidDocumentReference(@Nonnull FhirDocumentReference documentReference, @Nonnull String voidReason);
	
	@Authorized({ PrivilegeConstants.GET_DOCUMENT_REFERENCE })
	FhirDocumentReference get(@Nonnull String uuid);
	
	@Authorized({ PrivilegeConstants.GET_DOCUMENT_REFERENCE })
	List<FhirDocumentReference> get(@Nonnull Collection<String> uuids);
	
	@Authorized({ PrivilegeConstants.ADD_DOCUMENT_REFERENCE, PrivilegeConstants.EDIT_DOCUMENT_REFERENCE })
	FhirDocumentReference createOrUpdate(@Nonnull FhirDocumentReference newEntry);
	
	@Authorized({ PrivilegeConstants.DELETE_DOCUMENT_REFERENCE })
	FhirDocumentReference delete(@Nonnull String uuid);
	
	@Authorized({ PrivilegeConstants.GET_DOCUMENT_REFERENCE })
	List<FhirDocumentReference> getSearchResults(@Nonnull SearchParameterMap theParams);
}
