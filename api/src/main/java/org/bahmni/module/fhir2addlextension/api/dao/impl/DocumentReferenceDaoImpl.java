package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2addlextension.api.dao.DocumentReferenceDao;
import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReference;
import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReferenceAttribute;
import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReferenceContent;
import org.hibernate.Criteria;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Date;

@Component
@Slf4j
public class DocumentReferenceDaoImpl extends BaseFhirDao<FhirDocumentReference> implements DocumentReferenceDao {
	
	@Override
	public void voidDocumentReference(@Nonnull FhirDocumentReference documentReference, @Nonnull String voidReason) {
		User authenticatedUser = Context.getAuthenticatedUser();
		Date voidedDate = new Date();
		
		documentReference.setVoided(true);
		documentReference.setDocStatus(FhirDocumentReference.FhirDocumentReferenceDocStatus.ENTEREDINERROR);
		documentReference.setVoidReason(voidReason);
		documentReference.setDateVoided(voidedDate);
		documentReference.setVoidedBy(authenticatedUser);
		
		if (documentReference.getContents() != null) {
			for (FhirDocumentReferenceContent content : documentReference.getContents()) {
				if (!content.getVoided()) {
					content.setVoided(true);
					content.setVoidReason(voidReason);
					content.setDateVoided(voidedDate);
					content.setVoidedBy(authenticatedUser);
				}
			}
		}
		
		if (documentReference.getAttributes() != null) {
			for (FhirDocumentReferenceAttribute attribute : documentReference.getAttributes()) {
				if (!attribute.getVoided()) {
					attribute.setVoided(true);
					attribute.setVoidReason(voidReason);
					attribute.setDateVoided(voidedDate);
					attribute.setVoidedBy(authenticatedUser);
				}
			}
		}
		
		createOrUpdate(documentReference);
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
