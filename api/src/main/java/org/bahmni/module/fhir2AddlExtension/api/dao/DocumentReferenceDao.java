package org.bahmni.module.fhir2AddlExtension.api.dao;

import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReference;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

public interface DocumentReferenceDao extends FhirDao<FhirDocumentReference> {
	
	@Authorized({ "Get DocumentReference" })
	FhirDocumentReference get(@Nonnull String uuid);
	
	@Authorized({ "Get DocumentReference" })
	List<FhirDocumentReference> get(@Nonnull Collection<String> uuids);
	
	@Authorized({ "Add DocumentReference" })
	FhirDocumentReference createOrUpdate(@Nonnull FhirDocumentReference newEntry);
	
	@Authorized({ "Delete DocumentReference" })
	FhirDocumentReference delete(@Nonnull String uuid);
	
	@Authorized({ "Get DocumentReference" })
	List<FhirDocumentReference> getSearchResults(@Nonnull SearchParameterMap theParams);
}
