package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReference;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReferenceAttribute;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReferenceContent;
import org.bahmni.module.fhir2AddlExtension.api.translator.DocumentReferenceAttributeExtensionTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.DocumentReferenceTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.DocumentReferenceStatusTranslator;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Period;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.Validate.notNull;

@Component
public class DocumentReferenceTranslatorImpl implements DocumentReferenceTranslator {
	
	public static final String DOCUMENT_CONTENT_FORMAT_MUST_BE_SPECIFIED_AND_FROM_ACCEPTED_LIST = "Document content format must be specified and from accepted list";
	
	public static final String NEW_RESOURCE_REQUEST_CAN_NOT_HAVE_STATUS_WITH_ENTERED_IN_ERROR = "New resource request can not have status with entered-in-error";
	
	public static final String ENTERED_IN_ERROR_VOID_REASON = "entered-in-error";
	
	public static final String CAN_NOT_FIND_REFERENCE_TO_DOCUMENT_TYPE = "Can not find reference to document type";
	
	public static final String CAN_NOT_FIND_REFERENCE_TO_DOCUMENT_SECURITY_LABEL = "Can not find reference to document security label";
	
	public static final String DOCUMENT_REFERENCE_OBJECT_MUST_HAVE_A_SUBJECT_REFERENCE_TO_PATIENT = "DocumentReference object must have a subject reference to patient";
	
	public static final String PRACTITIONER_REFERENCE_IS_NOT_VALID = "Practitioner Reference is not valid.";
	
	private final PatientReferenceTranslator patientReferenceTranslator;
	
	private final ConceptTranslator conceptTranslator;
	
	private final DocumentReferenceStatusTranslator statusTranslator;
	
	private final EncounterReferenceTranslator<Encounter> encounterReferenceTranslator;
	
	private final PractitionerReferenceTranslator<Provider> providerReferenceTranslator;
	
	private final DocumentReferenceAttributeExtensionTranslator attributeExtensionTranslator;
	
	@Autowired
	public DocumentReferenceTranslatorImpl(PatientReferenceTranslator patientReferenceTranslator,
										   ConceptTranslator conceptTranslator, DocumentReferenceStatusTranslator statusTranslator,
										   EncounterReferenceTranslator<Encounter> encounterReferenceTranslator,
										   PractitionerReferenceTranslator<Provider> providerReferenceTranslator,
										   DocumentReferenceAttributeExtensionTranslator attributeExtensionTranslator) {
		this.patientReferenceTranslator = patientReferenceTranslator;
		this.conceptTranslator = conceptTranslator;
		this.statusTranslator = statusTranslator;
		this.encounterReferenceTranslator = encounterReferenceTranslator;
		this.providerReferenceTranslator = providerReferenceTranslator;
		this.attributeExtensionTranslator = attributeExtensionTranslator;
	}
	
	@Override
    public DocumentReference toFhirResource(@Nonnull FhirDocumentReference docRef) {
        DocumentReference documentReference = new DocumentReference();
        documentReference.setId(docRef.getUuid());
        Identifier masterIdentifier = new Identifier();
        masterIdentifier.setValue(docRef.getMasterIdentifier());
        documentReference.setMasterIdentifier(masterIdentifier);
		documentReference.setSubject(patientReferenceTranslator.toFhirResource(docRef.getSubject()));
        documentReference.setType(conceptTranslator.toFhirResource(docRef.getDocType()));
        CodeableConcept securityConcept = conceptTranslator.toFhirResource(docRef.getSecurityLabel());
        Optional.of(securityConcept).ifPresent(codeableConcept -> documentReference.setSecurityLabel(Collections.singletonList(codeableConcept)));
        documentReference.setStatus(statusTranslator.toFhirType(docRef.getStatus()));
        documentReference.setDocStatus(statusTranslator.toFhirType(docRef.getDocStatus()));
        mapContextToFhirDocument(documentReference, docRef);
        mapContentToFhirDocument(documentReference, docRef);
        mapProviderToFhirDocument(documentReference, docRef);
        return documentReference;
    }
	
	@Override
	public FhirDocumentReference toOpenmrsType(@Nonnull DocumentReference resource) {
		notNull(resource.getSubject(), DOCUMENT_REFERENCE_OBJECT_MUST_HAVE_A_SUBJECT_REFERENCE_TO_PATIENT);
		FhirDocumentReference newDoc = new FhirDocumentReference();
		return this.toOpenmrsType(newDoc, resource);
	}
	
	@Override
	public FhirDocumentReference toOpenmrsType(@Nonnull FhirDocumentReference newOrExistingDoc,
	        @Nonnull DocumentReference resource) {
		User authenticatedUser = Context.getUserContext().getAuthenticatedUser();
		if (resource.hasSubject() && newOrExistingDoc.getSubject() == null) {
			//TODO: safety check - ideally should check if the resource subject and db subject are same
			//for now, assigning only in case of create request
			newOrExistingDoc.setSubject(patientReferenceTranslator.toOpenmrsType(resource.getSubject()));
		}
		
		//not supporting client assigned id, resource.date is auto assigned
		if (newOrExistingDoc.getDocumentReferenceId() == null || newOrExistingDoc.getDocumentReferenceId() == 0) {
			newOrExistingDoc.setCreator(authenticatedUser);
			newOrExistingDoc.setDateCreated(new Date());
		} else {
			newOrExistingDoc.setChangedBy(authenticatedUser);
			newOrExistingDoc.setDateChanged(new Date());
		}
		
		if (resource.hasType()) {
			Concept docType = conceptTranslator.toOpenmrsType(resource.getType());
			if (docType == null) {
				throw new InvalidRequestException(CAN_NOT_FIND_REFERENCE_TO_DOCUMENT_TYPE);
			}
			newOrExistingDoc.setDocType(docType);
		}
		
		if (resource.hasSecurityLabel()) {
			Concept securityLabel = conceptTranslator.toOpenmrsType(resource.getSecurityLabelFirstRep());
			if (securityLabel == null) {
				throw new InvalidRequestException(CAN_NOT_FIND_REFERENCE_TO_DOCUMENT_SECURITY_LABEL);
			}
			newOrExistingDoc.setSecurityLabel(securityLabel);
		}
		
		if (resource.hasStatus()) {
			newOrExistingDoc.setStatus(statusTranslator.toOpenmrsType(resource.getStatus()));
		}
		
		if (resource.hasDocStatus()) {
			newOrExistingDoc.setDocStatus(statusTranslator.toOpenmrsType(resource.getDocStatus()));
		}
		
		if (resource.hasMasterIdentifier()) {
			//for now, just storing the value and not taking system in context.
			newOrExistingDoc.setMasterIdentifier(resource.getMasterIdentifier().getValue());
		}
		
		checkAndVoidOnStatus(newOrExistingDoc, resource);
		
		if (resource.hasDescription()) {
			newOrExistingDoc.setDescription(resource.getDescription());
		}
		
		mapProviderAndLocationFromFhirDocument(newOrExistingDoc, resource);
		mapContextFromFhirDocument(newOrExistingDoc, resource);
		mapContentsFromFhirDocument(newOrExistingDoc, resource, authenticatedUser);
		mapExtensionsToDocumentAttributes(newOrExistingDoc, resource, authenticatedUser);
		
		return newOrExistingDoc;
	}

	private void mapExtensionsToDocumentAttributes(FhirDocumentReference newOrExistingDoc, DocumentReference resource, User authenticatedUser) {
		if (!resource.hasExtension()) {
			return;
		}
		Map<String, List<Extension>> extnListMap = resource.getExtension().stream()
				.filter(extension -> attributeExtensionTranslator.supports(extension))
				.collect(Collectors.groupingBy(Extension::getUrl));

		extnListMap.forEach((extUrl, extensions) -> {
			List<FhirDocumentReferenceAttribute> attributes = attributeExtensionTranslator.toOpenmrsType(extUrl, extensions);
			if (newOrExistingDoc.getActiveAttributes().isEmpty()) {
				attributes.forEach(attr -> {
					attr.setCreator(authenticatedUser);
					attr.setDateCreated(new Date());
					newOrExistingDoc.addAttribute(attr);
				});
			} else {
				//not empty - merge
				//TODO
			}

		});

	}

	private void mapProviderToFhirDocument(DocumentReference documentReference, FhirDocumentReference docRef) {
        Optional.ofNullable(docRef.getProvider())
            .ifPresent(provider -> {
                documentReference.getAuthor().add(providerReferenceTranslator.toFhirResource(provider));
            });
    }
	
	private void mapProviderAndLocationFromFhirDocument(FhirDocumentReference newOrExistingDoc, DocumentReference resource) {
        if (!resource.hasAuthor()) {
            return;
        }
        resource.getAuthor().stream()
            .filter(reference -> reference.getReference().startsWith(FhirConstants.PRACTITIONER))
            .findFirst()
            .map(reference -> {
                Provider provider = providerReferenceTranslator.toOpenmrsType(reference);
                if (provider == null) {
                    throw new InvalidRequestException(PRACTITIONER_REFERENCE_IS_NOT_VALID);
                }
                return provider;
            }).ifPresent(provider -> newOrExistingDoc.setProvider(provider));
    }
	
	private void mapContentToFhirDocument(DocumentReference documentReference, FhirDocumentReference docRef) {
        //assumption is unless you have content for a document, you would not create just metadata.
        //TODO: add validation at service for checking content presence
        docRef.getContents().forEach(content -> {
            DocumentReference.DocumentReferenceContentComponent fhirContent = new DocumentReference.DocumentReferenceContentComponent();
            fhirContent.setId(content.getUuid());
            Attachment attachment = new Attachment().setContentType(content.getContentType()).setUrl(content.getContentUrl());
            documentReference.addContent(fhirContent.setAttachment(attachment));
        });
    }
	
	private void mapContextToFhirDocument(DocumentReference documentReference, FhirDocumentReference docRef) {
        Optional.ofNullable(docRef.getEncounter())
                .ifPresent(encounter -> {
                    DocumentReference.DocumentReferenceContextComponent contextComponent = new DocumentReference.DocumentReferenceContextComponent();
                    contextComponent.setEncounter(Collections.singletonList(encounterReferenceTranslator.toFhirResource(encounter)));
                    Optional<Period> contextPeriod = Optional.ofNullable(docRef.getDateStarted()).map(date -> {
                        Period period = new Period();
                        return period.setStart(docRef.getDateStarted());
                    }).map(period -> period.setEnd((docRef.getDateEnded())));
                    contextPeriod.ifPresent(value -> contextComponent.setPeriod(value));
                    documentReference.setContext(contextComponent);
                });
    }
	
	private void mapContentsFromFhirDocument(
            FhirDocumentReference newOrExistingDoc, DocumentReference fhirResource, User user) {
        if (!fhirResource.hasContent()) {
            //TODO should we assume such resources have been voided? or is it a patch?
            return;
        }

        fhirResource.getContent().forEach(contentComponent -> {
            if (!contentComponent.hasAttachment()) {
                return;
            }
            if (!contentComponent.getAttachment().hasContentType()) {
                throw new InvalidRequestException(DOCUMENT_CONTENT_FORMAT_MUST_BE_SPECIFIED_AND_FROM_ACCEPTED_LIST);
            }

            Optional<FhirDocumentReferenceContent> existingDocContent = newOrExistingDoc.getContents().stream()
                    .filter(docContent -> contentComponent.getAttachment().getContentType().equals(docContent.getContentType()))
                    .findFirst();

            if (!existingDocContent.isPresent()) {
                FhirDocumentReferenceContent documentReferenceContent = new FhirDocumentReferenceContent();
                if (contentComponent.hasFormat()) {
                    documentReferenceContent.setContentFormat(contentComponent.getFormat().getCode());
                }
                //Should we check against a set of mimetypes?
                documentReferenceContent.setContentType(contentComponent.getAttachment().getContentType());
                //TODO - not handling attachment.data as of now
                //would have to use some ways to create an interface, through which implementation
                //can either configure or plugin their own storage if data is present
                //examples include openmrs attachment omod, new storage service or simple volume storage or minio
                documentReferenceContent.setContentUrl(contentComponent.getAttachment().getUrl());
                documentReferenceContent.setCreator(user);
                documentReferenceContent.setDateCreated(new Date());
                newOrExistingDoc.addContent(documentReferenceContent);
            } else {
                if (contentComponent.hasFormat()) {
                    existingDocContent.get().setContentFormat(contentComponent.getFormat().getCode());
                }
                existingDocContent.get().setContentType(contentComponent.getAttachment().getContentType());
                existingDocContent.get().setContentUrl(contentComponent.getAttachment().getUrl());
                existingDocContent.get().setChangedBy(user);
                existingDocContent.get().setDateChanged(new Date());
            }
        });
    }
	
	private void checkAndVoidOnStatus(FhirDocumentReference newOrExistingDoc, DocumentReference resource) {
		if (resource.getStatus().equals(Enumerations.DocumentReferenceStatus.ENTEREDINERROR)) {
			if (newOrExistingDoc.getUuid() == null) {
				throw new InvalidRequestException(NEW_RESOURCE_REQUEST_CAN_NOT_HAVE_STATUS_WITH_ENTERED_IN_ERROR);
			}
			newOrExistingDoc.setVoided(true);
			newOrExistingDoc.setVoidReason(ENTERED_IN_ERROR_VOID_REASON);
			newOrExistingDoc.setDocStatus(FhirDocumentReference.FhirDocumentReferenceDocStatus.ENTEREDINERROR);
		}
	}
	
	private void mapContextFromFhirDocument(FhirDocumentReference newOrExistingDoc, DocumentReference resource) {
		if (!resource.hasContext()) {
			return;
		}
		DocumentReference.DocumentReferenceContextComponent documentContext = resource.getContext();
		if (documentContext.hasEncounter()) {
			newOrExistingDoc.setEncounter(encounterReferenceTranslator.toOpenmrsType(documentContext.getEncounter().get(0)));
		}
		
		if (documentContext.hasPeriod()) {
			newOrExistingDoc.setDateStarted(documentContext.getPeriod().getStart());
			newOrExistingDoc.setDateEnded(documentContext.getPeriod().getEnd());
		}
	}
	
}
