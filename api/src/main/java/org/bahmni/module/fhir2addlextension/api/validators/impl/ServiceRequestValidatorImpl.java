package org.bahmni.module.fhir2addlextension.api.validators.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.AccessLevel;
import lombok.Setter;
import org.bahmni.module.fhir2addlextension.api.model.DispositionInfo;
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
import org.openmrs.parameter.EncounterSearchCriteriaBuilder;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Setter(value = AccessLevel.PACKAGE)
public class ServiceRequestValidatorImpl implements ServiceRequestValidator {
	
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
		DispositionInfo disposition = dispositionOrderTypeResolver.getDisposition(conceptBeingOrdered);
		if (disposition != null && disposition.getRule() != null && disposition.getRule().isDuplicateChecked()
		        && dispositionOrderTypeResolver.getAdtOrderTypeName().equals(orderType.getName())) {
			rejectIfActiveOrderExists(serviceRequest, disposition);
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
	 * Encounters of the visit (voided ones included, as voiding is how Undo Discharge is
	 * fulfilled), plus the patient's visit-less encounters that fall within the visit's time window
	 * (clients that omit Encounter.partOf save with no visit).
	 */
	private List<Encounter> collectVisitEncounters(Visit visit, Encounter requestEncounter) {
		Set<Encounter> result = new HashSet<>();
		if (visit.getEncounters() != null) {
			result.addAll(visit.getEncounters());
		}
		Patient patient = requestEncounter.getPatient() != null ? requestEncounter.getPatient() : visit.getPatient();
		if (patient != null && visit.getStartDatetime() != null) {
			Date windowEnd = visit.getStopDatetime() != null ? visit.getStopDatetime() : new Date();
			List<Encounter> patientEncounters = encounterService.getEncounters(new EncounterSearchCriteriaBuilder()
				        .setPatient(patient).setFromDate(visit.getStartDatetime()).setToDate(windowEnd)
				        .setIncludeVoided(true).createEncounterSearchCriteria());
			if (patientEncounters != null) {
				patientEncounters.stream().filter(e -> e.getVisit() == null && e.getEncounterDatetime() != null)
				        .filter(e -> !e.getEncounterDatetime().before(visit.getStartDatetime())
				                && !e.getEncounterDatetime().after(windowEnd))
				        .forEach(result::add);
			}
		}
		return new ArrayList<>(result);
	}
	
	private void rejectIfActiveOrderExists(ServiceRequest serviceRequest, DispositionInfo disposition) {
		String emrapiCode = disposition.getCode();
		String adtOrderTypeName = dispositionOrderTypeResolver.getAdtOrderTypeName();
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
		        .filter(e -> !e.getVoided()).flatMap(e -> e.getOrders().stream()).filter(order -> !order.getVoided())
		        .filter(order -> order.getAction() != Order.Action.DISCONTINUE && !order.isDiscontinuedRightNow() && !order.isExpired())
		        .filter(order -> order.getOrderType() != null && adtOrderTypeName.equals(order.getOrderType().getName()))
		        .collect(Collectors.toList());

		boolean activeOrderExists = adtOrders.stream()
		        .filter(order -> emrapiCode.equals(codeOf(order)))
		        .anyMatch(order -> !isSuperseded(adtOrders, order, disposition) && !isFulfilled(visitEncounters, order, disposition));

		if (activeOrderExists) {
			throw new InvalidRequestException(
			        "An active " + disposition.getLabel() + " order already exists for this visit");
		}
	}
	
	/**
	 * An ADT Order is superseded once a later order of a closing kind is placed in the same visit
	 * (Discharge closes Admit; Admit or Undo Discharge closes Discharge). A closing order that has
	 * itself been superseded no longer closes anything, so Admit, Discharge, Undo Discharge leaves
	 * the Admit open again. Other kinds, such as Transfer, do not close it. An order at the same
	 * instant counts as later.
	 */
	private boolean isSuperseded(List<Order> adtOrders, Order order, DispositionInfo disposition) {
		return isSuperseded(adtOrders, order, disposition, new HashSet<>());
	}
	
	private boolean isSuperseded(List<Order> adtOrders, Order order, DispositionInfo disposition, Set<String> inProgress) {
		Set<String> closingCodes = disposition.getRule().getClosedBy();
		if (closingCodes == null || closingCodes.isEmpty()) {
			return false;
		}
		Date orderDate = order.getDateActivated() != null ? order.getDateActivated() : order.getDateCreated();
		inProgress.add(order.getUuid());
		try {
			return adtOrders.stream().filter(other -> !inProgress.contains(other.getUuid())).anyMatch(other -> {
				Date otherDate = other.getDateActivated() != null ? other.getDateActivated() : other.getDateCreated();
				if (otherDate == null || orderDate == null || otherDate.before(orderDate)
				        || !closingCodes.contains(codeOf(other))) {
					return false;
				}
				DispositionInfo otherInfo = dispositionOrderTypeResolver.getDisposition(other.getConcept());
				return otherInfo.getRule() == null || !isSuperseded(adtOrders, other, otherInfo, inProgress);
			});
		}
		finally {
			inProgress.remove(order.getUuid());
		}
	}
	
	/**
	 * An ADT Order is also considered done once the fulfiller (real Bed Management action) has
	 * actually carried it out - e.g. a Transfer order is fulfilled once a real TRANSFER encounter
	 * is recorded after it, so a new Transfer order can be placed again. Mirrors the encounter-type
	 * gating used by the emrapi.sqlSearch.patientsTo* worklist queries.
	 */
	private boolean isFulfilled(List<Encounter> visitEncounters, Order order, DispositionInfo disposition) {
		Set<String> fulfillingEncounterTypes = disposition.getRule().getFulfilledByEncounterTypes();
		if (fulfillingEncounterTypes == null || fulfillingEncounterTypes.isEmpty()) {
			return false;
		}
		Date orderDate = order.getDateActivated() != null ? order.getDateActivated() : order.getDateCreated();
		if (orderDate == null) {
			return false;
		}
		boolean byVoiding = disposition.getRule().isFulfilledByVoiding();
		return visitEncounters.stream().filter(e -> byVoiding == Boolean.TRUE.equals(e.getVoided()))
		        .filter(e -> e.getEncounterType() != null && fulfillingEncounterTypes.contains(e.getEncounterType().getName()))
		        .anyMatch(e -> {
			        Date happened = byVoiding ? e.getDateVoided() : e.getEncounterDatetime();
			        return happened != null && happened.after(orderDate);
		        });
	}
	
	private String codeOf(Order order) {
		DispositionInfo info = dispositionOrderTypeResolver.getDisposition(order.getConcept());
		return info == null ? null : info.getCode();
	}
}
