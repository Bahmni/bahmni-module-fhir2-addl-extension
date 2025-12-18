package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2AddlExtension.api.service.ServiceRequestLocationReferenceResolver;
import org.bahmni.module.fhir2AddlExtension.api.translator.OrderTypeTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.ServiceRequestPriorityTranslator;
import org.bahmni.module.fhir2AddlExtension.api.validators.ServiceRequestValidator;
import org.hl7.fhir.r4.model.*;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.Provider;
import org.openmrs.CareSetting;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.api.translators.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.apache.commons.lang3.Validate.notNull;
import static org.openmrs.module.fhir2.api.translators.impl.FhirTranslatorUtils.getLastUpdated;
import static org.openmrs.module.fhir2.api.translators.impl.FhirTranslatorUtils.getVersionId;
import static org.openmrs.module.fhir2.api.translators.impl.ReferenceHandlingTranslator.createOrderReference;

@Primary
@Component
@Setter(AccessLevel.PACKAGE)
@Slf4j
public class BahmniServiceRequestTranslatorImpl implements ServiceRequestTranslator<Order> {
	
	@Autowired
	private ConceptTranslator conceptTranslator;
	
	@Autowired
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Autowired
	private EncounterReferenceTranslator<Encounter> encounterReferenceTranslator;
	
	@Autowired
	private PractitionerReferenceTranslator<Provider> providerReferenceTranslator;
	
	@Autowired
	private OrderIdentifierTranslator orderIdentifierTranslator;
	
	@Autowired
	private OrderTypeTranslator orderTypeTranslator;
	
	@Autowired
	private ServiceRequestPriorityTranslator serviceRequestPriorityTranslator;
	
	@Autowired
	private OrderService orderService;
	
	@Autowired
	private ServiceRequestValidator serviceRequestValidator;
	
	@Autowired
	private ServiceRequestLocationReferenceResolver locationReferenceResolver;
	
	@Override
	public ServiceRequest toFhirResource(@Nonnull Order order) {
		notNull(order, "The TestOrder object should not be null");
		
		ServiceRequest serviceRequest = new ServiceRequest();
		
		serviceRequest.setId(order.getUuid());
		
		serviceRequest.setStatus(determineServiceRequestStatus(order));
		
		serviceRequest.setCode(conceptTranslator.toFhirResource(order.getConcept()));
		
		serviceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
		
		serviceRequest.setPriority(serviceRequestPriorityTranslator.toFhirResource(order.getUrgency()));
		
		serviceRequest.setSubject(patientReferenceTranslator.toFhirResource(order.getPatient()));
		
		serviceRequest.setEncounter(encounterReferenceTranslator.toFhirResource(order.getEncounter()));
		
		serviceRequest.setRequester(providerReferenceTranslator.toFhirResource(order.getOrderer()));
		
		serviceRequest.setOccurrence(new Period().setStart(order.getEffectiveStartDate()).setEnd(
		    order.getEffectiveStopDate()));
		
		if (order.getPreviousOrder() != null
		        && (order.getAction() == Order.Action.DISCONTINUE || order.getAction() == Order.Action.REVISE)) {
			serviceRequest.setReplaces((Collections.singletonList(createOrderReferenceInternal(order.getPreviousOrder())
			        .setIdentifier(orderIdentifierTranslator.toFhirResource(order.getPreviousOrder())))));
		} else if (order.getPreviousOrder() != null && order.getAction() == Order.Action.RENEW) {
			serviceRequest.setBasedOn(Collections.singletonList(createOrderReferenceInternal(order.getPreviousOrder())
			        .setIdentifier(orderIdentifierTranslator.toFhirResource(order.getPreviousOrder()))));
		}

		Optional.ofNullable(locationReferenceResolver.getRequestedLocationReferenceForOrder(order)).ifPresent(reference -> {
			serviceRequest.setLocationReference(Collections.singletonList(reference));
		});
		
		serviceRequest.getMeta().setLastUpdated(getLastUpdated(order));
		serviceRequest.getMeta().setVersionId(getVersionId(order));
		
		serviceRequest.setCategory(Collections.singletonList(orderTypeTranslator.toFhirResource(order.getOrderType())));
		
		Extension extension = determineLabOrderConceptTypeExtension(order);
		if (extension != null) {
			serviceRequest.addExtension(extension);
		}
		
		return serviceRequest;
	}
	
	@Override
	public Order toOpenmrsType(@Nonnull ServiceRequest resource) {
		serviceRequestValidator.validate(resource);
		
		Concept conceptBeingOrdered = conceptTranslator.toOpenmrsType(resource.getCode());
		Order order = new Order();
		//Set Constants
		order.setAction(Order.Action.NEW);
		order.setCareSetting(orderService.getCareSettingByName(CareSetting.CareSettingType.OUTPATIENT.toString()));
		order.setOrderType(orderService.getOrderTypeByConcept(conceptBeingOrdered));
		
		order.setConcept(conceptBeingOrdered);
		order.setPatient(patientReferenceTranslator.toOpenmrsType(resource.getSubject()));
		order.setEncounter(encounterReferenceTranslator.toOpenmrsType(resource.getEncounter()));
		order.setOrderer(providerReferenceTranslator.toOpenmrsType(resource.getRequester()));
		order.setUrgency(serviceRequestPriorityTranslator.toOpenmrsType(resource.getPriority()));
		
		if (resource.hasNote() && !resource.getNote().isEmpty()) {
			Annotation firstNote = resource.getNote().get(0);
			if (firstNote.hasText()) {
				order.setCommentToFulfiller(firstNote.getText());
			}
		}
		
		return order;
	}
	
	private ServiceRequest.ServiceRequestStatus determineServiceRequestStatus(Order order) {
		
		Date currentDate = new Date();
		
		boolean isCompeted = order.isActivated()
		        && ((order.getDateStopped() != null && currentDate.after(order.getDateStopped())) || (order
		                .getAutoExpireDate() != null && currentDate.after(order.getAutoExpireDate())));
		boolean isDiscontinued = order.isActivated() && order.getAction() == Order.Action.DISCONTINUE;
		
		if ((isCompeted && isDiscontinued)) {
			return ServiceRequest.ServiceRequestStatus.UNKNOWN;
		} else if (isDiscontinued) {
			return ServiceRequest.ServiceRequestStatus.REVOKED;
		} else if (isCompeted) {
			return ServiceRequest.ServiceRequestStatus.COMPLETED;
		} else {
			return ServiceRequest.ServiceRequestStatus.ACTIVE;
		}
	}
	
	private Reference createOrderReferenceInternal(Order order) {
		Reference reference = createOrderReference(order);
		if (reference == null) {
			reference = new Reference().setReference("ServiceRequest/" + order.getUuid()).setType("ServiceRequest");
		}
		return reference;
	}
	
	private Extension determineLabOrderConceptTypeExtension(Order order) {
		Extension labOrderConceptTypeExtension = new Extension();
		labOrderConceptTypeExtension.setUrl(BahmniFhirConstants.LAB_ORDER_CONCEPT_TYPE_EXTENSION_URL);
		Concept concept = order.getConcept();
		String orderConceptClassName = concept.getConceptClass().getName();
		if (orderConceptClassName.equals(BahmniFhirConstants.LAB_TEST_CONCEPT_CLASS)
		        || orderConceptClassName.equals(BahmniFhirConstants.TEST_CONCEPT_CLASS)) {
			labOrderConceptTypeExtension.setValue(new StringType("Test"));
			return labOrderConceptTypeExtension;
		} else if (orderConceptClassName.equals(BahmniFhirConstants.LABSET_CONCEPT_CLASS)) {
			labOrderConceptTypeExtension.setValue(new StringType("Panel"));
			return labOrderConceptTypeExtension;
		} else
			return null;
	}
}
