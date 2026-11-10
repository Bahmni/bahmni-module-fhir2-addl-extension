package org.bahmni.module.fhir2addlextension.api.service.impl;

import org.bahmni.module.fhir2addlextension.api.model.DispositionConfig;
import org.bahmni.module.fhir2addlextension.api.model.DispositionInfo;
import org.bahmni.module.fhir2addlextension.api.service.DispositionOrderTypeResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.EncounterService;
import org.openmrs.api.VisitService;
import org.openmrs.parameter.EncounterSearchCriteria;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@RunWith(MockitoJUnitRunner.class)
public class DispositionOrderLifecycleServiceImplTest {

	private static final String ADT_ORDER_TYPE = "ADT Order";

	private static final long HOUR = 3600_000L;

	@Mock
	private VisitService visitService;

	@Mock
	private EncounterService encounterService;

	@Mock
	private DispositionOrderTypeResolver resolver;

	@InjectMocks
	private DispositionOrderLifecycleServiceImpl service;

	private final Map<String, Concept> conceptByCode = new HashMap<>();

	private final Map<String, DispositionInfo> infoByCode = new HashMap<>();

	private final OrderType adtOrderType = new OrderType();

	private final Patient patient = new Patient();

	private Visit visit;

	private Encounter requestEncounter;

	private long base;

	@Before
	public void setUp() {
		base = System.currentTimeMillis() - 100 * HOUR;
		adtOrderType.setName(ADT_ORDER_TYPE);
		lenient().when(resolver.getAdtOrderTypeName()).thenReturn(ADT_ORDER_TYPE);
		lenient().when(resolver.getDisposition(any(Concept.class))).thenAnswer(
		    invocation -> infoByCode.values().stream()
		            .filter(info -> conceptByCode.get(info.getCode()) == invocation.getArgument(0)).findFirst().orElse(null));

		define("ADMIT", rule(true, set("DISCHARGE"), set(), false));
		define("DISCHARGE", rule(true, set("ADMIT", "UNDO_DISCHARGE"), set(), false));
		define("TRANSFER", rule(false, set(), set("TRANSFER"), false));
		define("UNDO_DISCHARGE", rule(true, set(), set("DISCHARGE"), true));

		visit = new Visit();
		visit.setPatient(patient);
		visit.setStartDatetime(at(0));
		requestEncounter = new Encounter();
		requestEncounter.setPatient(patient);
		requestEncounter.setVisit(visit);
		lenient().when(encounterService.getEncounters(any(EncounterSearchCriteria.class))).thenReturn(Collections.emptyList());
	}

	@Test
	public void shouldReturnFalseWhenNoVisitCanBeResolved() {
		requestEncounter.setVisit(null);
		lenient().when(visitService.getActiveVisitsByPatient(patient)).thenReturn(Collections.emptyList());

		assertFalse(service.hasActiveOrder(requestEncounter, info("ADMIT")));
		verify(encounterService, never()).getEncounters(any(EncounterSearchCriteria.class));
	}

	@Test
	public void shouldUseActiveVisitWhenEncounterHasNoVisit() {
		requestEncounter.setVisit(null);
		lenient().when(visitService.getActiveVisitsByPatient(patient)).thenReturn(Collections.singletonList(visit));
		addOrder("ADMIT", at(1));

		assertTrue(service.hasActiveOrder(requestEncounter, info("ADMIT")));
	}

	@Test
	public void shouldBlockWhenAnActiveOrderOfTheSameKindExists() {
		addOrder("ADMIT", at(1));

		assertTrue(service.hasActiveOrder(requestEncounter, info("ADMIT")));
	}

	@Test
	public void shouldNotBlockWhenNoOrderOfTheSameKindExists() {
		addOrder("ADMIT", at(1));

		assertFalse(service.hasActiveOrder(requestEncounter, info("DISCHARGE")));
	}

	@Test
	public void shouldIgnoreVoidedDiscontinueAndExpiredOrders() {
		Order voided = addOrder("ADMIT", at(1));
		voided.setVoided(true);
		Order discontinue = addOrder("ADMIT", at(2));
		discontinue.setAction(Order.Action.DISCONTINUE);
		Order expired = addOrder("ADMIT", at(3));
		expired.setAutoExpireDate(at(4));

		assertFalse(service.hasActiveOrder(requestEncounter, info("ADMIT")));
	}

	@Test
	public void shouldIgnoreOrdersOfOtherOrderTypes() {
		Order lab = addOrder("ADMIT", at(1));
		OrderType labType = new OrderType();
		labType.setName("Lab Order");
		lab.setOrderType(labType);

		assertFalse(service.hasActiveOrder(requestEncounter, info("ADMIT")));
	}

	@Test
	public void shouldIgnoreOrdersOnVoidedEncounters() {
		Encounter voidedEncounter = encounter("CONSULTATION", at(1));
		voidedEncounter.setVoided(true);
		voidedEncounter.addOrder(order("ADMIT", at(1)));

		assertFalse(service.hasActiveOrder(requestEncounter, info("ADMIT")));
	}

	@Test
	public void shouldReleaseAdmitOnceALaterDischargeIsPlaced() {
		addOrder("ADMIT", at(1));
		addOrder("DISCHARGE", at(2));

		assertFalse(service.hasActiveOrder(requestEncounter, info("ADMIT")));
	}

	@Test
	public void shouldTreatAnOrderAtTheSameInstantAsLater() {
		addOrder("ADMIT", at(1));
		addOrder("DISCHARGE", at(1));

		assertFalse(service.hasActiveOrder(requestEncounter, info("ADMIT")));
	}

	@Test
	public void shouldNotReleaseAdmitWhenAnEarlierDischargeExists() {
		addOrder("DISCHARGE", at(1));
		addOrder("ADMIT", at(2));

		assertTrue(service.hasActiveOrder(requestEncounter, info("ADMIT")));
	}

	@Test
	public void shouldNotReleaseAdmitWhenOnlyATransferFollows() {
		addOrder("ADMIT", at(1));
		addOrder("TRANSFER", at(2));

		assertTrue(service.hasActiveOrder(requestEncounter, info("ADMIT")));
	}

	@Test
	public void shouldReopenAdmitWhenTheDischargeThatClosedItWasItselfUndone() {
		addOrder("ADMIT", at(1));
		addOrder("DISCHARGE", at(2));
		addOrder("UNDO_DISCHARGE", at(3));

		assertTrue(service.hasActiveOrder(requestEncounter, info("ADMIT")));
	}

	@Test
	public void shouldReleaseTransferOnceATransferEncounterFollows() {
		addOrder("TRANSFER", at(1));
		encounter("TRANSFER", at(2));

		assertFalse(service.hasActiveOrder(requestEncounter, info("TRANSFER")));
	}

	@Test
	public void shouldNotReleaseTransferWhenTheTransferEncounterPredatesTheOrder() {
		encounter("TRANSFER", at(1));
		addOrder("TRANSFER", at(2));

		assertTrue(service.hasActiveOrder(requestEncounter, info("TRANSFER")));
	}

	@Test
	public void shouldNotReleaseTransferOnAnUnrelatedEncounterType() {
		addOrder("TRANSFER", at(1));
		encounter("CONSULTATION", at(2));

		assertTrue(service.hasActiveOrder(requestEncounter, info("TRANSFER")));
	}

	@Test
	public void shouldReleaseUndoDischargeOnceTheDischargeEncounterIsVoidedAfterIt() {
		addOrder("UNDO_DISCHARGE", at(2));
		Encounter discharge = encounter("DISCHARGE", at(1));
		discharge.setVoided(true);
		discharge.setDateVoided(at(3));

		assertFalse(service.hasActiveOrder(requestEncounter, info("UNDO_DISCHARGE")));
	}

	@Test
	public void shouldNotReleaseUndoDischargeWhenTheDischargeEncounterIsNotVoided() {
		addOrder("UNDO_DISCHARGE", at(2));
		encounter("DISCHARGE", at(3));

		assertTrue(service.hasActiveOrder(requestEncounter, info("UNDO_DISCHARGE")));
	}

	@Test
	public void shouldNotReleaseUndoDischargeWhenTheDischargeWasVoidedBeforeTheOrder() {
		addOrder("UNDO_DISCHARGE", at(2));
		Encounter discharge = encounter("DISCHARGE", at(0));
		discharge.setVoided(true);
		discharge.setDateVoided(at(1));

		assertTrue(service.hasActiveOrder(requestEncounter, info("UNDO_DISCHARGE")));
	}

	@Test
	public void shouldCountVisitlessEncountersInsideTheVisitWindow() {
		Encounter visitless = new Encounter();
		visitless.setPatient(patient);
		visitless.setEncounterDatetime(at(2));
		visitless.addOrder(order("ADMIT", at(2)));
		Encounter outside = new Encounter();
		outside.setPatient(patient);
		outside.setEncounterDatetime(new Date(base - 10 * HOUR));
		outside.addOrder(order("ADMIT", new Date(base - 10 * HOUR)));
		lenient().when(encounterService.getEncounters(any(EncounterSearchCriteria.class)))
		        .thenReturn(Arrays.asList(visitless, outside));

		assertTrue(service.hasActiveOrder(requestEncounter, info("ADMIT")));
	}

	@Test
	public void shouldIgnoreVisitlessEncountersOutsideTheVisitWindow() {
		Encounter outside = new Encounter();
		outside.setPatient(patient);
		outside.setEncounterDatetime(new Date(base - 10 * HOUR));
		outside.addOrder(order("ADMIT", new Date(base - 10 * HOUR)));
		lenient().when(encounterService.getEncounters(any(EncounterSearchCriteria.class)))
		        .thenReturn(Collections.singletonList(outside));

		assertFalse(service.hasActiveOrder(requestEncounter, info("ADMIT")));
	}

	private Date at(int hours) {
		return new Date(base + hours * HOUR);
	}

	private void define(String code, DispositionConfig.Rule rule) {
		conceptByCode.put(code, new Concept());
		infoByCode.put(code, new DispositionInfo(code, code, rule));
	}

	private DispositionInfo info(String code) {
		return infoByCode.get(code);
	}

	private static HashSet<String> set(String... values) {
		return new HashSet<>(Arrays.asList(values));
	}

	private static DispositionConfig.Rule rule(boolean duplicateChecked, HashSet<String> closedBy,
	        HashSet<String> fulfilledBy, boolean byVoiding) {
		DispositionConfig.Rule rule = new DispositionConfig.Rule();
		rule.setDuplicateChecked(duplicateChecked);
		rule.setClosedBy(closedBy);
		rule.setFulfilledByEncounterTypes(fulfilledBy);
		rule.setFulfilledByVoiding(byVoiding);
		return rule;
	}

	private Order order(String code, Date activated) {
		Order order = new Order();
		order.setConcept(conceptByCode.get(code));
		order.setOrderType(adtOrderType);
		order.setAction(Order.Action.NEW);
		order.setDateActivated(activated);
		return order;
	}

	private Order addOrder(String code, Date activated) {
		Order order = order(code, activated);
		requestEncounter.addOrder(order);
		visit.addEncounter(requestEncounter);
		return order;
	}

	private Encounter encounter(String typeName, Date when) {
		Encounter encounter = new Encounter();
		encounter.setPatient(patient);
		encounter.setVisit(visit);
		encounter.setEncounterDatetime(when);
		EncounterType type = new EncounterType();
		type.setName(typeName);
		encounter.setEncounterType(type);
		visit.addEncounter(encounter);
		return encounter;
	}
}
