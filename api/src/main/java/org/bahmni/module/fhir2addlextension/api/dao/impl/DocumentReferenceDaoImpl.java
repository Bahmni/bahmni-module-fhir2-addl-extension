package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import org.bahmni.module.fhir2addlextension.api.dao.DocumentReferenceDao;
import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReference;
import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReferenceAttribute;
import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReferenceContent;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;
import org.openmrs.module.fhir2.api.dao.internals.OpenmrsFhirCriteriaContext;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.persistence.criteria.Join;
import java.util.Date;

@Component
public class DocumentReferenceDaoImpl extends BaseFhirDao<FhirDocumentReference> implements DocumentReferenceDao {
	
	private static final String DOC_TYPE_PROPERTY = "docType";
	
	private static final String DOC_TYPE_ALIAS = "dt";
	
	private static final String DOC_TYPE_CONCEPT_MAP_ALIAS = "dtcm";
	
	private static final String DOC_TYPE_CONCEPT_REFERENCE_TERM_ALIAS = "dtcrt";
	
	private static final String ENCOUNTER_ALIAS = "e";
	
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
		
		getSessionFactory().getCurrentSession().saveOrUpdate(documentReference);
	}
	
	@Override
	protected <U> void setupSearchParams(OpenmrsFhirCriteriaContext<FhirDocumentReference, U> ctx,
	        SearchParameterMap theParams) {
		super.setupSearchParams(ctx, theParams);
		theParams.getParameters().forEach(param -> {
			switch (param.getKey()) {
				case FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER:
					param.getValue().forEach(patientReference -> getSearchQueryHelper().handlePatientReference(ctx,
					    (ReferenceAndListParam) patientReference.getParam(), "subject"));
					break;
				case FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER:
					param.getValue().forEach(encounterReference -> getSearchQueryHelper().handleEncounterReference(ctx,
					    (ReferenceAndListParam) encounterReference.getParam(), ENCOUNTER_ALIAS));
					break;
				case FhirConstants.CODED_SEARCH_HANDLER:
					param.getValue().forEach(type -> handleDocType(ctx, (TokenAndListParam) type.getParam()));
					break;
				case FhirConstants.COMMON_SEARCH_HANDLER:
					handleCommonSearchParameters(ctx, param.getValue()).ifPresent(ctx::addPredicate);
					break;
			}
		});
	}
	
	private <U> void handleDocType(OpenmrsFhirCriteriaContext<FhirDocumentReference, U> ctx, TokenAndListParam type) {
		if (type == null) {
			return;
		}
		Join<?, ?> dtJoin = ctx.getJoin(DOC_TYPE_ALIAS).orElseGet(() -> ctx.addJoin(DOC_TYPE_PROPERTY, DOC_TYPE_ALIAS));
		getSearchQueryHelper()
		        .handleCodeableConcept(ctx, type, dtJoin, DOC_TYPE_CONCEPT_MAP_ALIAS, DOC_TYPE_CONCEPT_REFERENCE_TERM_ALIAS)
		        .ifPresent(ctx::addPredicate);
	}
}
