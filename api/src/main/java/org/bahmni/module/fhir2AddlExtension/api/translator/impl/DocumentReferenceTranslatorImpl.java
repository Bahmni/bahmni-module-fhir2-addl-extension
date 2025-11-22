package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReference;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReferenceAttribute;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReferenceAttributeType;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReferenceContent;
import org.bahmni.module.fhir2AddlExtension.api.translator.DocumentReferenceAttributeTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniServiceRequestReferenceTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.DocumentReferenceExtensionTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.DocumentReferenceTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.DocumentReferenceStatusTranslator;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Type;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Order;
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
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.Validate.notNull;

@Component
@Slf4j
public class DocumentReferenceTranslatorImpl implements DocumentReferenceTranslator {
	
	public static final String CAN_NOT_FIND_REFERENCE_TO_DOCUMENT_TYPE = "Can not find reference to document type";
	
	public static final String CAN_NOT_FIND_REFERENCE_TO_DOCUMENT_SECURITY_LABEL = "Can not find reference to document security label";
	
	public static final String DOCUMENT_REFERENCE_OBJECT_MUST_HAVE_A_SUBJECT_REFERENCE_TO_PATIENT = "DocumentReference object must have a subject reference to patient";
	
	public static final String PRACTITIONER_REFERENCE_IS_NOT_VALID = "Practitioner Reference is not valid.";
	
	public static final String INVALID_BASED_ON_REQUEST = "Invalid request. extension /document-reference/based-on-service-request must be a reference";
	
	public static final String INVALID_BASED_ON_SERVICE_REQUEST = "Invalid Document Reference attribute for based-on-service-request";
	
	private final PatientReferenceTranslator patientReferenceTranslator;
	
	private final ConceptTranslator conceptTranslator;
	
	private final DocumentReferenceStatusTranslator statusTranslator;
	
	private final EncounterReferenceTranslator<Encounter> encounterReferenceTranslator;
	
	private final PractitionerReferenceTranslator<Provider> providerReferenceTranslator;
	
	private final DocumentReferenceExtensionTranslator extensionTranslator;
	
	private final BahmniServiceRequestReferenceTranslator basedOnReferenceTranslator;
	
	@Autowired
	public DocumentReferenceTranslatorImpl(PatientReferenceTranslator patientReferenceTranslator,
	    ConceptTranslator conceptTranslator, DocumentReferenceStatusTranslator statusTranslator,
	    EncounterReferenceTranslator<Encounter> encounterReferenceTranslator,
	    PractitionerReferenceTranslator<Provider> providerReferenceTranslator,
	    DocumentReferenceExtensionTranslator extensionTranslator,
	    BahmniServiceRequestReferenceTranslator basedOnReferenceTranslator) {
		this.patientReferenceTranslator = patientReferenceTranslator;
		this.conceptTranslator = conceptTranslator;
		this.statusTranslator = statusTranslator;
		this.encounterReferenceTranslator = encounterReferenceTranslator;
		this.providerReferenceTranslator = providerReferenceTranslator;
		this.extensionTranslator = extensionTranslator;
		this.basedOnReferenceTranslator = basedOnReferenceTranslator;
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
        Optional.ofNullable(securityConcept).ifPresent(codeableConcept -> documentReference.setSecurityLabel(Collections.singletonList(codeableConcept)));
        documentReference.setStatus(statusTranslator.toFhirType(docRef.getStatus()));
        documentReference.setDocStatus(statusTranslator.toFhirType(docRef.getDocStatus()));
		if (docRef.getOrder() != null) {
			documentReference.addExtension(BahmniFhirConstants.FHIR_EXT_DOCUMENT_REFERENCE_BASED_ON, basedOnReferenceTranslator.toFhirResource(docRef.getOrder()));
		}
        mapContextToFhirDocument(documentReference, docRef);
        mapContentToFhirDocument(documentReference, docRef);
        mapProviderToFhirDocument(documentReference, docRef);
		mapAttributesToExtensions(documentReference, docRef);
        return documentReference;
    }
	
	@Override
	public FhirDocumentReference toOpenmrsType(@Nonnull DocumentReference resource) {
		notNull(resource.getSubject(), DOCUMENT_REFERENCE_OBJECT_MUST_HAVE_A_SUBJECT_REFERENCE_TO_PATIENT);
		FhirDocumentReference newDoc = new FhirDocumentReference();
		newDoc.setCreator(Context.getUserContext().getAuthenticatedUser());
		newDoc.setDateCreated(new Date());
		//not supporting client assigned id, resource.date is auto assigned
		return this.toOpenmrsType(newDoc, resource);
	}
	
	@Override
	public FhirDocumentReference toOpenmrsType(@Nonnull FhirDocumentReference newOrExistingDoc,
	        @Nonnull DocumentReference resource) {
		User authenticatedUser = Context.getUserContext().getAuthenticatedUser();
		if (resource.hasSubject()) {
			newOrExistingDoc.setSubject(patientReferenceTranslator.toOpenmrsType(resource.getSubject()));
		}
		
		if (!isNewDocument(newOrExistingDoc)) {
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
		
		if (resource.hasDescription()) {
			newOrExistingDoc.setDescription(resource.getDescription());
		}
		
		mapProviderAndLocationFromFhirDocument(newOrExistingDoc, resource);
		mapContextFromFhirDocument(newOrExistingDoc, resource);
		mapContentsFromFhirDocument(newOrExistingDoc, resource, authenticatedUser);
		mapExtensionsToDocumentAttributes(newOrExistingDoc, resource, authenticatedUser);
		mapBasedOnServiceReqFromExtension(newOrExistingDoc, resource, authenticatedUser);
		
		return newOrExistingDoc;
	}
	
	private void mapBasedOnServiceReqFromExtension(FhirDocumentReference newOrExistingDoc, DocumentReference resource, User authenticatedUser) {
		if (!resource.hasExtension()) {
			return;
		}
		resource.getExtension().forEach(extension -> {
			String extUrl = Optional.ofNullable(extension.getUrl()).orElse("");
			if (!extUrl.startsWith(BahmniFhirConstants.FHIR_EXT_DOCUMENT_REFERENCE_BASED_ON)) {
				return;
			}
			Type serviceRequestReference = extension.getValue();
			if (serviceRequestReference instanceof Reference) {
				Order order = basedOnReferenceTranslator.toOpenmrsType((Reference) serviceRequestReference);
				ensureOrderForSamePatient(newOrExistingDoc, order);
				newOrExistingDoc.setOrder(order);
				return;
			} else {
				log.error(INVALID_BASED_ON_REQUEST);
				throw new UnprocessableEntityException(INVALID_BASED_ON_SERVICE_REQUEST);
			}
		});
	}
	
	private void ensureOrderForSamePatient(FhirDocumentReference newOrExistingDoc, Order order) {
		if (newOrExistingDoc.getSubject() == null) {
			return;
		}
		if (order != null && order.getPatient().getUuid().equals(newOrExistingDoc.getSubject().getUuid())) {
			log.error(INVALID_BASED_ON_SERVICE_REQUEST);
			throw new UnprocessableEntityException(INVALID_BASED_ON_SERVICE_REQUEST);
		}
	}
	
	private boolean isNewDocument(FhirDocumentReference newOrExistingDoc) {
		return newOrExistingDoc.getDocumentReferenceId() == null || newOrExistingDoc.getDocumentReferenceId() == 0;
	}
	
	private void mapAttributesToExtensions(DocumentReference resource, FhirDocumentReference docRef) {
		if (!docRef.getActiveAttributes().isEmpty()) {
			docRef.getActiveAttributes().forEach(attribute -> {
				extensionTranslator.getAttributeTranslator(attribute)
				.map(translator -> translator.toFhirResource(attribute))
				.ifPresent(extension -> resource.addExtension(extension));
			});
		}
	}
	
	private void mapExtensionsToDocumentAttributes(FhirDocumentReference newOrExistingDoc, DocumentReference resource, User authenticatedUser) {
		if (!resource.hasExtension()) {
			return;
		}
		Map<String, List<Extension>> attributeExtensions = resource.getExtension().stream().collect(Collectors.groupingBy(Extension::getUrl));
		List<FhirDocumentReferenceAttribute> allAttributesFromExt = new ArrayList<>();
		for (String extUrl : attributeExtensions.keySet()) {
			Optional<DocumentReferenceAttributeTranslator> attributeTranslator = extensionTranslator.getAttributeTranslator(extUrl);
			if (!attributeTranslator.isPresent()) {
				continue;
			}
			allAttributesFromExt.addAll(attributeTranslator.get().toOpenmrsType(extUrl, attributeExtensions.get(extUrl)));
		}

		if (newOrExistingDoc.getActiveAttributes().isEmpty()) { //add all new one
			allAttributesFromExt.forEach(attr -> {
				attr.setCreator(authenticatedUser);
				attr.setDateCreated(new Date());
				newOrExistingDoc.addAttribute(attr);
			});
			return;
		}

		List<FhirDocumentReferenceAttribute> newAttributes = new ArrayList<>();
		//group the allAttributesFromExt by type
		Map<FhirDocumentReferenceAttributeType, List<FhirDocumentReferenceAttribute>> groupedExtAttributes
				= allAttributesFromExt.stream().collect(Collectors.groupingBy(FhirDocumentReferenceAttribute::getAttributeType));
		for (FhirDocumentReferenceAttributeType attributeType : groupedExtAttributes.keySet()) {
			List<FhirDocumentReferenceAttribute> extAttributes = groupedExtAttributes.get(attributeType);
			List<FhirDocumentReferenceAttribute> existingAttributesForType
				= newOrExistingDoc.getActiveAttributes().stream().filter(existing -> existing.getAttributeType().equals(attributeType))
					.collect(Collectors.toList());

			//no existing attribute for type, so just submitted ones need to be added
			if (existingAttributesForType.isEmpty()) {
				newAttributes.addAll(extAttributes);
			}

			//there are existing attribute for type, so we need to merge
			if (!existingAttributesForType.isEmpty()) {
				if (attributeType.getMaxOccurs() == null || attributeType.getMaxOccurs().equals(1)) {
					existingAttributesForType.get(0).setValueReferenceInternal(extAttributes.get(0).getValueReference());
					existingAttributesForType.get(0).setDateChanged(new Date());
					existingAttributesForType.get(0).setChangedBy(authenticatedUser);
					return;
				} else {
					//there can be more than 1.
					//TODO diff and merge. add the new ones, update existing ones
				}
			}
		}

		newAttributes.forEach(attr -> {
			attr.setCreator(authenticatedUser);
			attr.setDateCreated(new Date());
			newOrExistingDoc.addAttribute(attr);
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
		ArrayList<FhirDocumentReferenceContent> sortedList
			= docRef.getContents().stream()
				.sorted(Comparator.comparingInt(FhirDocumentReferenceContent::getDocumentReferenceContentId))
				.collect(Collectors.toCollection(ArrayList::new));
		sortedList.forEach(content -> {
            DocumentReference.DocumentReferenceContentComponent fhirContent = new DocumentReference.DocumentReferenceContentComponent();
            fhirContent.setId(content.getUuid());
            Attachment attachment = new Attachment().setContentType(content.getContentType()).setUrl(content.getContentUrl());
            documentReference.addContent(fhirContent.setAttachment(attachment));
        });
    }
	
	private void mapContextToFhirDocument(DocumentReference resource, FhirDocumentReference docRef) {
        Optional.ofNullable(docRef.getEncounter())
			.ifPresent(encounter -> {
				DocumentReference.DocumentReferenceContextComponent contextComponent = new DocumentReference.DocumentReferenceContextComponent();
				contextComponent.setEncounter(Collections.singletonList(encounterReferenceTranslator.toFhirResource(encounter)));
				resource.setContext(contextComponent);
			});
		Optional<Period> contextPeriod = Optional.ofNullable(docRef.getDateStarted()).map(date -> {
			Period period = new Period();
			return period.setStart(docRef.getDateStarted());
		}).map(period -> period.setEnd((docRef.getDateEnded())));
		contextPeriod.ifPresent(value -> {
			if (!resource.hasContext()) {
				DocumentReference.DocumentReferenceContextComponent contextComponent = new DocumentReference.DocumentReferenceContextComponent();
				contextComponent.setPeriod(value);
			}
		});
    }
	
	private void mapContentsFromFhirDocument(FhirDocumentReference document, DocumentReference resource, User user) {
        if (!resource.hasContent()) {
            return;
        }
        resource.getContent().forEach(contentComponent -> {
            Optional<FhirDocumentReferenceContent> existingContent =
				isNewDocument(document)
				? Optional.empty()
				: document.getContents().stream().filter(docContent -> docContent.getUuid().equals(contentComponent.getId())).findFirst();

            if (!existingContent.isPresent()) {
				document.addContent(translateContentToOpenmrsType(contentComponent, user));
				return;
            } else if (!existingContent.get().isVoided()) {
				updateExistingContent(existingContent.get(), contentComponent, user);
            }
        });
    }
	
	private void updateExistingContent(FhirDocumentReferenceContent existingContent,
	        DocumentReference.DocumentReferenceContentComponent resourceContent, User user) {
		if (resourceContent.hasAttachment()) {
			existingContent.setContentType(resourceContent.getAttachment().getContentType());
			existingContent.setContentUrl(resourceContent.getAttachment().getUrl());
		}
		if (resourceContent.hasFormat()) {
			existingContent.setContentFormat(resourceContent.getFormat().getCode());
		}
		existingContent.setChangedBy(user);
		existingContent.setDateChanged(new Date());
	}
	
	private FhirDocumentReferenceContent translateContentToOpenmrsType(
	        DocumentReference.DocumentReferenceContentComponent contentComponent, User user) {
		if (!isValidAttachment(contentComponent)) {
			log.error("Submitted document does not have valid attachment");
			throw new UnprocessableEntityException(
			        "Invalid document attachment. Please ensure attachment has valid content-type and url");
		}
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
		return documentReferenceContent;
	}
	
	private boolean isValidAttachment(DocumentReference.DocumentReferenceContentComponent attachment) {
		if (!attachment.hasAttachment())
			return false;
		if (StringUtils.isEmpty(attachment.getAttachment().getContentType()))
			return false;
		if (StringUtils.isEmpty(attachment.getAttachment().getUrl()))
			return false;
		return true;
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
