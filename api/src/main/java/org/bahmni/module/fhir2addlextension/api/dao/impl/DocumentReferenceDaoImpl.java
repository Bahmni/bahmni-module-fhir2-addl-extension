package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2addlextension.api.dao.DocumentReferenceDao;
import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReference;
import org.hibernate.Criteria;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class DocumentReferenceDaoImpl extends BaseFhirDao<FhirDocumentReference> implements DocumentReferenceDao {
	
	@Override
	public void voidDocumentReference(String uuid, String voidReason) {
		FhirDocumentReference documentReference = get(uuid);
		if (documentReference == null) {
			throw new InvalidRequestException("DocumentReference not found with uuid: " + uuid);
		}
		User authenticatedUser = Context.getAuthenticatedUser();
		documentReference.setVoided(true);
		documentReference.setDocStatus(FhirDocumentReference.FhirDocumentReferenceDocStatus.ENTEREDINERROR);
		documentReference.setVoidReason(voidReason);
		documentReference.setDateVoided(new Date());
		documentReference.setVoidedBy(authenticatedUser);
		createOrUpdate(documentReference);
		log.info("Successfully voided DocumentReference with uuid: {} for reason: {}", uuid, voidReason);
	}
	
	@Override
    protected void setupSearchParams(Criteria criteria, SearchParameterMap theParams) {
        super.setupSearchParams(criteria, theParams);
        theParams.getParameters().forEach(param -> {
            switch (param.getKey()) {
                case FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER :
                    param.getValue().forEach(patientReference -> handlePatientReference(criteria,
                            (ReferenceAndListParam) patientReference.getParam(), "subject"));
                    break;
                case FhirConstants.COMMON_SEARCH_HANDLER:
                    handleCommonSearchParameters(param.getValue()).ifPresent(criteria::add);
                    break;
            }
        });
    }
}
