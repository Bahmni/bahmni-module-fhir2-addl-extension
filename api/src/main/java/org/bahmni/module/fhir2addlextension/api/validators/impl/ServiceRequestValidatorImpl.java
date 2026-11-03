package org.bahmni.module.fhir2addlextension.api.validators.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.AccessLevel;
import lombok.Setter;
import org.bahmni.module.fhir2addlextension.api.service.DispositionOrderTypeResolver;
import org.bahmni.module.fhir2addlextension.api.validators.ServiceRequestValidator;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Visit;
import org.openmrs.Patient;
import org.openmrs.api.EncounterService;
import org.openmrs.api.OrderService;
import org.openmrs.api.VisitService;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Setter(value = AccessLevel.PACKAGE)
public class ServiceRequestValidatorImpl implements ServiceRequestValidator {

	private static final Set<String> DUPLICATE_CHECKED_CODES = new HashSet<>(
	        Arrays.asList("ADMIT", "DISCHARGE", "UNDO_DISCHARGE"));

	private static final Map<String, String> DISPOSITION_LABEL_BY_CODE = new HashMap<>();
	static {
		DISPOSITION_LABEL_BY_CODE.put("ADMIT", "Admit");
		DISPOSITION_LABEL_BY_CODE.put("DISCHARGE", "Discharge");
		DISPOSITION_LABEL_BY_CODE.put("TRANSFER", "Transfer");
		DISPOSITION_LABEL_BY_CODE.put("UNDO_DISCHARGE", "Undo Discharge");
	}

	// Real Bed Management encounter types that mark an ADT Order as fulfilled, matching the
	// gating used by the emrapi.sqlSearch.patientsTo* worklist queries.
	private static final Map<String, Set<String>> FULFILLING_ENCOUNTER_TYPES_BY_CODE = new HashMap<>();
	static {
		FULFILLING_ENCOUNTER_TYPES_BY_CODE.put("ADMIT", new HashSet<>(Arrays.asList("ADMISSION", "DISCHARGE")));
		FULFILLING_ENCOUNTER_TYPES_BY_CODE.put("UNDO_DISCHARGE", new HashSet<>(Arrays.asList("ADMISSION", "DISCHARGE")));
		FULFILLING_ENCOUNTER_TYPES_BY_CODE.put("DISCHARGE", new HashSet<>(Collections.singletonList("DISCHARGE")));
	}
	
	@Autowired
	private ConceptTranslator conceptTranslator;
	
	@Autowired
	private OrderService orderService;
	
	@Autowired
	private VisitService visitService;
	
	@Autowired
	private EncounterService encounterService;
	
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
		String emrapiCode = dispositionOrderTypeResolver.getDispositionCode(conceptBeingOrdered);
		if (DispositionOrderTypeResolver.ADT_ORDER_TYPE_NAME.equals(orderType.getName()) && DUPLICATE_CHECKED_CODES.contains(emrapiCode)) {
			rejectIfActiveOrderExists(serviceRequest, emrapiCode);
		}
	}

	private Visit resolveVisit(Encounter encounter) {
		if (encounter.getVisit() != null || encounter.getPatient() == null) {
			return encounter.getVisit();
		}
		List<Visit> activeVisits = visitService.getActiveVisitsByPatient(encounter.getPatient());
		return activeVisits == null || activeVisits.isEmpty() ? null : activeVisits.get(0);
	}
	
	/**
	 * Non-voided encounters of the visit, plus the patient's visit-less encounters that fall
	 * within the visit's time window (clients that omit Encounter.partOf save with no visit).
	 */
	private List<Encounter> collectVisitEncounters(Visit visit, Encounter requestEncounter) {
		Set<Encounter> result = new HashSet<>();
		if (visit.getEncounters() != null) {
			result.addAll(visit.getEncounters());
		}
		Patient patient = requestEncounter.getPatient() != null ? requestEncounter.getPatient() : visit.getPatient();
		if (patient != null && visit.getStartDatetime() != null) {
			Date windowEnd = visit.getStopDatetime() != null ? visit.getStopDatetime() : new Date();
			List<Encounter> patientEncounters = encounterService.getEncountersByPatient(patient);
			if (patientEncounters != null) {
				patientEncounters.stream().filter(e -> e.getVisit() == null && e.getEncounterDatetime() != null)
				        .filter(e -> !e.getEncounterDatetime().before(visit.getStartDatetime())
				                && !e.getEncounterDatetime().after(windowEnd))
				        .forEach(result::add);
			}
		}
		return result.stream().filter(e -> !e.getVoided()).collect(Collectors.toList());
	}
	
	private void rejectIfActiveOrderExists(ServiceRequest serviceRequest, String emrapiCode) {
		if (serviceRequest.getEncounter() == null) {
			return;
		}
		Encounter encounter = encounterReferenceTranslator.toOpenmrsType(serviceRequest.getEncounter());
		if (encounter == null) {
			return;
		}
		final Visit visit = resolveVisit(encounter);
		if (visit == null) {
			return;
		}
		final List<Encounter> visitEncounters = collectVisitEncounters(visit, encounter);
		List<Order> adtOrders = visitEncounters.stream()
		        .flatMap(e -> e.getOrders().stream()).filter(order -> !order.getVoided())
		        .filter(order -> order.getOrderType() != null && DispositionOrderTypeResolver.ADT_ORDER_TYPE_NAME.equals(order.getOrderType().getName()))
		        .collect(Collectors.toList());

		boolean activeOrderExists = adtOrders.stream()
		        .filter(order -> emrapiCode.equals(dispositionOrderTypeResolver.getDispositionCode(order.getConcept())))
		        .anyMatch(order -> !isSuperseded(adtOrders, order) && !isFulfilled(visitEncounters, order, emrapiCode));

		if (activeOrderExists) {
			throw new InvalidRequestException(
			        "An active " + DISPOSITION_LABEL_BY_CODE.get(emrapiCode) + " order already exists for this visit");
		}
	}

	/**
	 * An ADT Order is superseded once any other ADT Order (of any disposition kind) is
	 * placed after it in the same visit - the new ServiceRequest-driven flow has no
	 * reliable ADMISSION/DISCHARGE/TRANSFER encounter-type flip to check against, so the
	 * order timeline itself is the source of truth for "has this been moved past".
	 */
	private boolean isSuperseded(List<Order> adtOrders, Order order) {
		Date orderDate = order.getDateActivated() != null ? order.getDateActivated() : order.getDateCreated();
		return adtOrders.stream().filter(other -> !other.getUuid().equals(order.getUuid())).anyMatch(other -> {
			Date otherDate = other.getDateActivated() != null ? other.getDateActivated() : other.getDateCreated();
			return otherDate != null && orderDate != null && otherDate.after(orderDate);
		});
	}

	/**
	 * An ADT Order is also considered done once the fulfiller (real Bed Management action)
	 * has actually carried it out - e.g. a Transfer order is fulfilled once a real TRANSFER
	 * encounter is recorded after it, so a new Transfer order can be placed again. Mirrors
	 * the encounter-type gating used by the emrapi.sqlSearch.patientsTo* worklist queries.
	 */
	private boolean isFulfilled(List<Encounter> visitEncounters, Order order, String emrapiCode) {
		Set<String> fulfillingEncounterTypes = FULFILLING_ENCOUNTER_TYPES_BY_CODE.get(emrapiCode);
		if (fulfillingEncounterTypes == null || fulfillingEncounterTypes.isEmpty()) {
			return false;
		}
		Date orderDate = order.getDateActivated() != null ? order.getDateActivated() : order.getDateCreated();
		if (orderDate == null) {
			return false;
		}
		return visitEncounters.stream()
		        .filter(e -> e.getEncounterType() != null && fulfillingEncounterTypes.contains(e.getEncounterType().getName()))
		        .anyMatch(e -> e.getEncounterDatetime() != null && e.getEncounterDatetime().after(orderDate));
	}
}
