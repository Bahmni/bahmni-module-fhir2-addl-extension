package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2AddlExtension.api.dao.DocumentReferenceDao;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReference;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReferenceContent;
import org.bahmni.module.fhir2AddlExtension.api.search.param.BahmniDocumentReferenceSearchParams;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirDocumentReferenceService;
import org.bahmni.module.fhir2AddlExtension.api.translator.DocumentReferenceTranslator;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Enumerations;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.dao.FhirPractitionerDao;
import org.openmrs.module.fhir2.api.impl.BaseFhirService;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;
import org.openmrs.module.fhir2.api.util.FhirUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Primary
@Transactional
@Slf4j
public class BahmniFhirDocumentReferenceServiceImpl extends BaseFhirService<DocumentReference, FhirDocumentReference> implements BahmniFhirDocumentReferenceService {
	
	private static final String MISSING_PATIENT_IDENTIFIER_OR_RES_ID = "Missing patient identifier/id or resource id";
	
	private static final String PATIENT_IDENTIFIER_OR_REFERENCE_OR_RES_ID_MUST_BE_SPECIFIED = "You must specify patient identifier or reference or resource _id!";
	
	private static final String DOCUMENT_CONTENT_FORMAT_MUST_BE_SPECIFIED_AND_FROM_ACCEPTED_LIST = "Document content format must be specified and from accepted list";
	
	private static final String VOIDED_DUE_TO_USER_ACTION = "User Action";
	
	private static final String ENTERED_IN_ERROR_VOID_REASON = "entered-in-error";
	
	private static final String NEW_RESOURCE_REQUEST_CAN_NOT_HAVE_STATUS_WITH_ENTERED_IN_ERROR = "New resource request can not have status with entered-in-error";
	
	private static final String INVALID_OPERATION_OVERRIDE_OF_PATIENT_REFERENCE = "Invalid Operation: Override of Patient Reference for a Document to another";
	
	private static final String ERROR_INVALID_PATIENT_REFERENCE = "Invalid Patient Reference";
	
	private DocumentReferenceDao documentReferenceDao;
	
	private DocumentReferenceTranslator documentReferenceTranslator;
	
	private final SearchQueryInclude<DocumentReference> searchQueryInclude;
	
	private final SearchQuery<FhirDocumentReference, DocumentReference, DocumentReferenceDao, DocumentReferenceTranslator, SearchQueryInclude<DocumentReference>> searchQuery;
	
	@Autowired
	public BahmniFhirDocumentReferenceServiceImpl(
	    DocumentReferenceTranslator documentReferenceTranslator,
	    DocumentReferenceDao documentReferenceDao,
	    SearchQueryInclude<DocumentReference> searchQueryInclude,
	    SearchQuery<FhirDocumentReference, DocumentReference, DocumentReferenceDao, DocumentReferenceTranslator, SearchQueryInclude<DocumentReference>> searchQuery) {
		this.documentReferenceTranslator = documentReferenceTranslator;
		this.documentReferenceDao = documentReferenceDao;
		this.searchQueryInclude = searchQueryInclude;
		this.searchQuery = searchQuery;
	}
	
	@Override
	public IBundleProvider searchDocumentReferences(BahmniDocumentReferenceSearchParams searchParams) {
		if (!searchParams.hasPatientReference() && !searchParams.hasId()) {
			logAndThrowUnsupportedExceptionForMissingPatientOrResourceId();
		}
		return searchQuery.getQueryResults(searchParams.toSearchParameterMap(), documentReferenceDao,
		    documentReferenceTranslator, searchQueryInclude);
		
	}
	
	private void logAndThrowUnsupportedExceptionForMissingPatientOrResourceId() {
		log.error(MISSING_PATIENT_IDENTIFIER_OR_RES_ID);
		throw new UnsupportedOperationException(PATIENT_IDENTIFIER_OR_REFERENCE_OR_RES_ID_MUST_BE_SPECIFIED);
	}
	
	@Override
	protected FhirDao<FhirDocumentReference> getDao() {
		return documentReferenceDao;
	}
	
	@Override
	protected OpenmrsFhirTranslator<FhirDocumentReference, DocumentReference> getTranslator() {
		return documentReferenceTranslator;
	}
	
	@Override
	public DocumentReference create(@Nonnull DocumentReference newResource) {
		if (newResource.hasStatus()) {
			if (newResource.getStatus().equals(Enumerations.DocumentReferenceStatus.ENTEREDINERROR)) {
				throw new InvalidRequestException(NEW_RESOURCE_REQUEST_CAN_NOT_HAVE_STATUS_WITH_ENTERED_IN_ERROR);
			}
		}
		return super.create(newResource);
	}
	
	@Override
	protected DocumentReference applyUpdate(FhirDocumentReference existingObject, DocumentReference updatedResource) {
		ensureSubjectReference(existingObject, updatedResource);
		if (voidWhenEnteredInError(existingObject, updatedResource)) {
			return super.applyUpdate(existingObject, updatedResource);
		}
		User authenticatedUser = Context.getUserContext().getAuthenticatedUser();
		Set<String> existingContentIds = existingObject.getContents().stream().map(c -> c.getUuid()).collect(Collectors.toSet());
		Set<String> submittedContentIds = updatedResource.hasContent()
				? updatedResource.getContent().stream().map(c -> c.getId()).collect(Collectors.toSet())
				: Collections.emptySet();
		Set<String> removedContentIds = existingContentIds.stream()
				.filter(existing -> !submittedContentIds.contains(existing)).collect(Collectors.toSet());
		if (!removedContentIds.isEmpty()) {
			//voiding any missing content
			existingObject.getContents().forEach(existing -> {
				if (removedContentIds.contains(existing.getUuid())) {
					voidExistingContent(existing, authenticatedUser);
				}
			});
		}
		updatedResource.getContent().forEach(contentComponent -> {
			if (!hasValidAttachment(contentComponent)) {
				throw new InvalidRequestException(DOCUMENT_CONTENT_FORMAT_MUST_BE_SPECIFIED_AND_FROM_ACCEPTED_LIST);
			}
		});
		return super.applyUpdate(existingObject, updatedResource);
	}
	
	private void ensureSubjectReference(FhirDocumentReference existingObject, DocumentReference updatedResource) {
		if (updatedResource.hasSubject()) {
			Optional.ofNullable(existingObject.getSubject())
				.ifPresent(patient -> {
					if (!patient.getUuid().equals(FhirUtils.referenceToId(updatedResource.getSubject().getReference()).get())) {
						log.error(INVALID_OPERATION_OVERRIDE_OF_PATIENT_REFERENCE);
						throw new InvalidRequestException(ERROR_INVALID_PATIENT_REFERENCE);
					}
				});
		}
	}
	
	private boolean voidWhenEnteredInError(FhirDocumentReference existingObject, DocumentReference resource) {
		if (!resource.getStatus().equals(Enumerations.DocumentReferenceStatus.ENTEREDINERROR)) {
			return false;
		}
		existingObject.setVoided(true);
		existingObject.setVoidReason(ENTERED_IN_ERROR_VOID_REASON);
		existingObject.setDocStatus(FhirDocumentReference.FhirDocumentReferenceDocStatus.ENTEREDINERROR);
		return true;
	}
	
	private void voidExistingContent(FhirDocumentReferenceContent existingContent, User authenticatedUser) {
		existingContent.setVoided(true);
		existingContent.setDateVoided(new Date());
		existingContent.setVoidedBy(authenticatedUser);
		existingContent.setVoidReason(VOIDED_DUE_TO_USER_ACTION);
	}
	
	private boolean hasValidAttachment(DocumentReference.DocumentReferenceContentComponent contentComponent) {
		if (!contentComponent.hasAttachment())
			return false;
		if (StringUtils.isEmpty(contentComponent.getAttachment().getContentType()))
			return false;
		if (StringUtils.isEmpty(contentComponent.getAttachment().getUrl()))
			return false;
		return true;
	}
}
