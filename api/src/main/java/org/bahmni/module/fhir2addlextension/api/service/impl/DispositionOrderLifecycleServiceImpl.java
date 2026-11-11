package org.bahmni.module.fhir2addlextension.api.service.impl;

import org.bahmni.module.fhir2addlextension.api.model.DispositionInfo;
import org.bahmni.module.fhir2addlextension.api.service.DispositionOrderLifecycleService;
import org.bahmni.module.fhir2addlextension.api.service.DispositionOrderTypeResolver;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.EncounterService;
import org.openmrs.api.VisitService;
import org.openmrs.parameter.EncounterSearchCriteriaBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.apache.commons.collections.CollectionUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DispositionOrderLifecycleServiceImpl implements DispositionOrderLifecycleService {
	
	@Autowired
	private VisitService visitService;
	
	@Autowired
	private EncounterService encounterService;
	
	@Autowired
	private DispositionOrderTypeResolver dispositionOrderTypeResolver;
	
	@Override
	public boolean hasActiveOrder(Encounter requestEncounter, DispositionInfo disposition) {
		String emrapiCode = disposition.getCode();
		String adtOrderTypeName = dispositionOrderTypeResolver.getAdtOrderTypeName();
		final Visit visit = resolveVisit(requestEncounter);
		if (visit == null) {
			return false;
		}
		final List<Encounter> visitEncounters = collectVisitEncounters(visit, requestEncounter);
		List<Order> adtOrders = visitEncounters.stream()
		        .filter(e -> !e.getVoided()).flatMap(e -> e.getOrders().stream()).filter(order -> !order.getVoided())
		        .filter(order -> order.getAction() != Order.Action.DISCONTINUE && !order.isDiscontinuedRightNow() && !order.isExpired())
		        .filter(order -> order.getOrderType() != null && adtOrderTypeName.equals(order.getOrderType().getName()))
		        .collect(Collectors.toList());

		return adtOrders.stream()
		        .filter(order -> emrapiCode.equals(codeOf(order)))
		        .anyMatch(order -> !isSuperseded(adtOrders, order, disposition) && !isFulfilled(visitEncounters, order, disposition));
	}
	
	private Visit resolveVisit(Encounter encounter) {
		if (encounter.getVisit() != null || encounter.getPatient() == null) {
			return encounter.getVisit();
		}
		List<Visit> activeVisits = visitService.getActiveVisitsByPatient(encounter.getPatient());
		return activeVisits.isEmpty() ? null : activeVisits.get(0);
	}
	
	/**
	 * Encounters of the visit (voided ones included, as voiding is how Undo Discharge is
	 * fulfilled), plus the patient's visit-less encounters that fall within the visit's time window
	 * (clients that omit Encounter.partOf save with no visit).
	 */
	private List<Encounter> collectVisitEncounters(Visit visit, Encounter requestEncounter) {
		Set<Encounter> result = new HashSet<>();
		result.addAll(visit.getEncounters());
		Patient patient = requestEncounter.getPatient() != null ? requestEncounter.getPatient() : visit.getPatient();
		if (patient != null && visit.getStartDatetime() != null) {
			Date windowEnd = visit.getStopDatetime() != null ? visit.getStopDatetime() : Date.from(Instant.now());
			List<Encounter> patientEncounters = encounterService.getEncounters(new EncounterSearchCriteriaBuilder()
				        .setPatient(patient).setFromDate(visit.getStartDatetime()).setToDate(windowEnd)
				        .setIncludeVoided(true).createEncounterSearchCriteria());
			patientEncounters.stream().filter(e -> e.getVisit() == null)
			        .filter(e -> !e.getEncounterDatetime().before(visit.getStartDatetime())
			                && !e.getEncounterDatetime().after(windowEnd))
			        .forEach(result::add);
		}
		return new ArrayList<>(result);
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
		if (CollectionUtils.isEmpty(closingCodes)) {
			return false;
		}
		Date orderDate = effectiveDate(order);
		inProgress.add(order.getUuid());
		try {
			return adtOrders.stream().filter(other -> !inProgress.contains(other.getUuid())).anyMatch(other -> {
				if (effectiveDate(other).before(orderDate) || !closingCodes.contains(codeOf(other))) {
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
		if (CollectionUtils.isEmpty(fulfillingEncounterTypes)) {
			return false;
		}
		Date orderDate = effectiveDate(order);
		boolean byVoiding = disposition.getRule().isFulfilledByVoiding();
		return visitEncounters.stream().filter(e -> byVoiding == Boolean.TRUE.equals(e.getVoided()))
		        .filter(e -> e.getEncounterType() != null && fulfillingEncounterTypes.contains(e.getEncounterType().getName()))
		        .anyMatch(e -> {
			        Date happened = byVoiding ? e.getDateVoided() : e.getEncounterDatetime();
			        return happened.after(orderDate);
		        });
	}
	
	private Date effectiveDate(Order order) {
		return order.getDateActivated() != null ? order.getDateActivated() : order.getDateCreated();
	}
	
	private String codeOf(Order order) {
		DispositionInfo info = dispositionOrderTypeResolver.getDisposition(order.getConcept());
		return info == null ? null : info.getCode();
	}
}
