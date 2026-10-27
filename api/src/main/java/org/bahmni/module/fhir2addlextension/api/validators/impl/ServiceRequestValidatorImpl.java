package org.bahmni.module.fhir2addlextension.api.validators.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.AccessLevel;
import lombok.Setter;
import org.bahmni.module.fhir2addlextension.api.service.DispositionOrderTypeResolver;
import org.bahmni.module.fhir2addlextension.api.validators.ServiceRequestValidator;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.Concept;
import org.openmrs.ConceptMap;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Visit;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@Setter(value = AccessLevel.PACKAGE)
public class ServiceRequestValidatorImpl implements ServiceRequestValidator {

	private static final String ADT_ORDER_TYPE_NAME = "ADT Order";

	private static final String EMRAPI_CONCEPT_SOURCE_NAME = "org.openmrs.module.emrapi";

	// Maps a disposition's emrapi code to the encounter type(s) whose occurrence after
	// the order date means that order has already been fulfilled, and is therefore no
	// longer "active" (i.e. no longer blocks a new order of the same kind).
	private static final Map<String, Set<String>> SUPERSEDING_ENCOUNTER_TYPES_BY_CODE = new HashMap<>();
	static {
		SUPERSEDING_ENCOUNTER_TYPES_BY_CODE.put("ADMIT", new HashSet<>(Collections.singletonList("ADMISSION")));
		SUPERSEDING_ENCOUNTER_TYPES_BY_CODE.put("DISCHARGE", new HashSet<>(Collections.singletonList("DISCHARGE")));
		SUPERSEDING_ENCOUNTER_TYPES_BY_CODE.put("TRANSFER",
		    new HashSet<>(Arrays.asList("TRANSFER", "DISCHARGE")));
	}

	private static final Map<String, String> DISPOSITION_LABEL_BY_CODE = new HashMap<>();
	static {
		DISPOSITION_LABEL_BY_CODE.put("ADMIT", "Admit");
		DISPOSITION_LABEL_BY_CODE.put("DISCHARGE", "Discharge");
		DISPOSITION_LABEL_BY_CODE.put("TRANSFER", "Transfer");
	}
	
	@Autowired
	private ConceptTranslator conceptTranslator;
	
	@Autowired
	private OrderService orderService;
	
	@Autowired
	private DispositionOrderTypeResolver dispositionOrderTypeResolver;
	
	@Autowired
	private EncounterReferenceTranslator<Encounter> encounterReferenceTranslator;
	
	@Override
	public void validate(ServiceRequest serviceRequest) {
		Concept conceptBeingOrdered = conceptTranslator.toOpenmrsType(serviceRequest.getCode());
		if (conceptBeingOrdered == null) {
			throw new InvalidRequestException("Invalid ServiceRequest code");
		}
		OrderType orderType = orderService.getOrderTypeByConcept(conceptBeingOrdered);
		if (orderType == null) {
			orderType = dispositionOrderTypeResolver.resolve(conceptBeingOrdered);
		}
		if (orderType == null) {
			throw new InvalidRequestException("Unable to determine order type for ServiceRequest");
		}
		String emrapiCode = getEmrapiMappingCode(conceptBeingOrdered);
		if (ADT_ORDER_TYPE_NAME.equals(orderType.getName()) && SUPERSEDING_ENCOUNTER_TYPES_BY_CODE.containsKey(emrapiCode)) {
			rejectIfActiveOrderExists(serviceRequest, emrapiCode);
		}
	}

	private void rejectIfActiveOrderExists(ServiceRequest serviceRequest, String emrapiCode) {
		if (serviceRequest.getEncounter() == null) {
			return;
		}
		Encounter encounter = encounterReferenceTranslator.toOpenmrsType(serviceRequest.getEncounter());
		if (encounter == null || encounter.getVisit() == null) {
			return;
		}

		Visit visit = encounter.getVisit();
		boolean activeOrderExists = visit.getEncounters().stream().filter(e -> !e.getVoided())
		        .flatMap(e -> e.getOrders().stream()).filter(order -> !order.getVoided())
		        .filter(order -> order.getOrderType() != null && ADT_ORDER_TYPE_NAME.equals(order.getOrderType().getName()))
		        .filter(order -> emrapiCode.equals(getEmrapiMappingCode(order.getConcept())))
		        .anyMatch(order -> !isSuperseded(visit, order, emrapiCode));

		if (activeOrderExists) {
			throw new InvalidRequestException(
			        "An active " + DISPOSITION_LABEL_BY_CODE.get(emrapiCode) + " order already exists for this visit");
		}
	}

	private boolean isSuperseded(Visit visit, Order order, String emrapiCode) {
		Date orderDate = order.getDateActivated() != null ? order.getDateActivated() : order.getDateCreated();
		Set<String> supersedingEncounterTypes = SUPERSEDING_ENCOUNTER_TYPES_BY_CODE.get(emrapiCode);
		return visit.getEncounters().stream().filter(e -> !e.getVoided())
		        .filter(e -> e.getEncounterType() != null && supersedingEncounterTypes.contains(e.getEncounterType().getName()))
		        .anyMatch(e -> e.getEncounterDatetime() != null && e.getEncounterDatetime().after(orderDate));
	}
	
	private String getEmrapiMappingCode(Concept concept) {
		if (concept == null || concept.getConceptMappings() == null) {
			return null;
		}
		for (ConceptMap conceptMap : concept.getConceptMappings()) {
			if (conceptMap.getConceptReferenceTerm() == null
			        || conceptMap.getConceptReferenceTerm().getConceptSource() == null) {
				continue;
			}
			if (EMRAPI_CONCEPT_SOURCE_NAME.equals(conceptMap.getConceptReferenceTerm().getConceptSource().getName())) {
				return conceptMap.getConceptReferenceTerm().getCode();
			}
		}
		return null;
	}
}
