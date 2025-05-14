package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import lombok.AccessLevel;
import lombok.Setter;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.Provider;
import org.openmrs.module.fhir2.api.translators.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Date;

import static org.apache.commons.lang3.Validate.notNull;
import static org.openmrs.module.fhir2.api.translators.impl.FhirTranslatorUtils.getLastUpdated;
import static org.openmrs.module.fhir2.api.translators.impl.FhirTranslatorUtils.getVersionId;
import static org.openmrs.module.fhir2.api.translators.impl.ReferenceHandlingTranslator.createOrderReference;

@Primary
@Component
@Setter(AccessLevel.PACKAGE)
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
	
	@Override
	public ServiceRequest toFhirResource(@Nonnull Order order) {
		notNull(order, "The TestOrder object should not be null");
		
		ServiceRequest serviceRequest = new ServiceRequest();
		
		serviceRequest.setId(order.getUuid());
		
		serviceRequest.setStatus(determineServiceRequestStatus(order));
		
		serviceRequest.setCode(conceptTranslator.toFhirResource(order.getConcept()));
		
		serviceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
		
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
		
		serviceRequest.getMeta().setLastUpdated(getLastUpdated(order));
		serviceRequest.getMeta().setVersionId(getVersionId(order));
		
		return serviceRequest;
	}
	
	@Override
	public Order toOpenmrsType(@Nonnull ServiceRequest resource) {
		throw new UnsupportedOperationException();
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
}
