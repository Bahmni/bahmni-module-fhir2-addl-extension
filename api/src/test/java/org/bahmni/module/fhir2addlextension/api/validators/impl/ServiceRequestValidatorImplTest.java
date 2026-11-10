package org.bahmni.module.fhir2addlextension.api.validators.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2addlextension.api.context.AppContext;
import org.bahmni.module.fhir2addlextension.api.model.DispositionConfig;
import org.bahmni.module.fhir2addlextension.api.model.DispositionInfo;
import org.bahmni.module.fhir2addlextension.api.service.DispositionOrderTypeResolver;
import org.bahmni.module.fhir2addlextension.api.service.impl.DispositionOrderTypeResolverImpl;
import org.bahmni.module.fhir2addlextension.api.validators.ServiceRequestValidator;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.ConceptSource;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Visit;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServiceRequestValidatorImplTest {
	
	@Mock
	private ConceptTranslator conceptTranslator;
	
	@Mock
	private OrderService orderService;
	
	@Mock
	private org.openmrs.api.VisitService visitService;
	
	@Mock
	private org.openmrs.api.EncounterService encounterService;
	
	@Mock
	private DispositionOrderTypeResolver dispositionOrderTypeResolver;
	
	@Mock
	private EncounterReferenceTranslator<Encounter> encounterReferenceTranslator;
	
	@Mock
	private AppContext appContext;
	
	private static DispositionConfig.Rule rule(boolean duplicateChecked, String closedBy, String... encounterTypes) {
		DispositionConfig.Rule r = new DispositionConfig.Rule();
		r.setDuplicateChecked(duplicateChecked);
		r.setClosedBy(new HashSet<>(Arrays.asList(closedBy.split(","))));
		r.setFulfilledByEncounterTypes(new HashSet<>(Arrays.asList(encounterTypes)));
		return r;
	}
	
	private static final String EMRAPI_SOURCE_NAME = "org.openmrs.module.emrapi";
	
	private ServiceRequestValidator validator;
	
	private ServiceRequest serviceRequest;
	
	private CodeableConcept codeableConcept;
	
	private Concept concept;
	
	private OrderType orderType;
	
	@Before
	public void setup() {
		ServiceRequestValidatorImpl validatorImpl = new ServiceRequestValidatorImpl();
		validatorImpl.setConceptTranslator(conceptTranslator);
		validatorImpl.setOrderService(orderService);
		validatorImpl.setVisitService(visitService);
		validatorImpl.setEncounterService(encounterService);
		validatorImpl.setDispositionOrderTypeResolver(dispositionOrderTypeResolver);
		Map<String, DispositionConfig.Rule> rules = new HashMap<>();
		rules.put("ADMIT", rule(true, "DISCHARGE", "ADMISSION", "DISCHARGE"));
		rules.put("DISCHARGE", rule(true, "ADMIT,UNDO_DISCHARGE", "DISCHARGE"));
		DispositionConfig.Rule undoRule = rule(true, "DISCHARGE", "DISCHARGE");
		undoRule.setFulfilledByVoiding(true);
		rules.put("UNDO_DISCHARGE", undoRule);
		rules.put("TRANSFER", new DispositionConfig.Rule());
		when(appContext.getDispositionConfig()).thenReturn(new DispositionConfig("ADT Order", rules));
		DispositionOrderTypeResolverImpl realResolver = new DispositionOrderTypeResolverImpl();
		org.springframework.test.util.ReflectionTestUtils.setField(realResolver, "appContext", appContext);
		Map<String, String> labels = new HashMap<>();
		labels.put("ADMIT", "Admit");
		labels.put("DISCHARGE", "Discharge");
		labels.put("TRANSFER", "Transfer");
		labels.put("UNDO_DISCHARGE", "Undo Discharge");
		lenient().when(dispositionOrderTypeResolver.getAdtOrderTypeName()).thenReturn("ADT Order");
		lenient().when(dispositionOrderTypeResolver.getDisposition(any())).thenAnswer(invocation -> {
			DispositionInfo info = realResolver.getDisposition(invocation.getArgument(0));
			return info == null ? null : new DispositionInfo(info.getCode(), labels.get(info.getCode()), info.getRule());
		});
		validatorImpl.setEncounterReferenceTranslator(encounterReferenceTranslator);
		validator = validatorImpl;
		
		serviceRequest = new ServiceRequest();
		codeableConcept = new CodeableConcept();
		Coding coding = new Coding();
		coding.setCode("12345");
		codeableConcept.addCoding(coding);
		serviceRequest.setCode(codeableConcept);
		
		concept = new Concept();
		concept.setUuid("12345");
		
		orderType = new OrderType();
		orderType.setUuid("order-type-uuid-456");
		orderType.setName("Lab Order");
	}
	
	@Test
	public void shouldValidateSuccessfullyWhenConceptAndOrderTypeAreValid() {
		// Given
		when(conceptTranslator.toOpenmrsType(codeableConcept)).thenReturn(concept);
		when(orderService.getOrderTypeByConcept(concept)).thenReturn(orderType);
		
		// When & Then - should not throw any exception
		validator.validate(serviceRequest);
	}
	
	@Test
	public void shouldThrowInvalidRequestExceptionWhenConceptIsNull() {
		// Given
		when(conceptTranslator.toOpenmrsType(codeableConcept)).thenReturn(null);
		
		// When & Then
		InvalidRequestException exception = assertThrows(InvalidRequestException.class, 
			() -> validator.validate(serviceRequest));
		assertEquals("Invalid ServiceRequest code", exception.getMessage());
	}
	
	@Test
	public void shouldThrowInvalidRequestExceptionWhenOrderTypeIsNull() {
		// Given
		when(conceptTranslator.toOpenmrsType(codeableConcept)).thenReturn(concept);
		when(orderService.getOrderTypeByConcept(concept)).thenReturn(null);
		
		// When & Then
		InvalidRequestException exception = assertThrows(InvalidRequestException.class,
			() -> validator.validate(serviceRequest));
		assertEquals("Unable to determine order type for ServiceRequest", exception.getMessage());
	}
	
	@Test
	public void shouldValidateSuccessfullyWhenDispositionResolverFallbackSucceeds() {
		// Given
		when(conceptTranslator.toOpenmrsType(codeableConcept)).thenReturn(concept);
		when(orderService.getOrderTypeByConcept(concept)).thenReturn(null);
		when(dispositionOrderTypeResolver.resolve(concept)).thenReturn(orderType);
		
		// When & Then - should not throw any exception
		validator.validate(serviceRequest);
	}
	
	@Test
	public void shouldThrowInvalidRequestExceptionWhenBothOrderTypeLookupAndDispositionResolverReturnNull() {
		// Given
		when(conceptTranslator.toOpenmrsType(codeableConcept)).thenReturn(concept);
		when(orderService.getOrderTypeByConcept(concept)).thenReturn(null);
		when(dispositionOrderTypeResolver.resolve(concept)).thenReturn(null);

		// When & Then
		InvalidRequestException exception = assertThrows(InvalidRequestException.class,
			() -> validator.validate(serviceRequest));
		assertEquals("Unable to determine order type for ServiceRequest", exception.getMessage());
	}
	
	@Test
	public void shouldThrowInvalidRequestExceptionWhenServiceRequestCodeIsNull() {
		// Given
		serviceRequest.setCode(null);
		
		// When & Then
		InvalidRequestException exception = assertThrows(InvalidRequestException.class,
			() -> validator.validate(serviceRequest));
		assertEquals("Invalid ServiceRequest code", exception.getMessage());
	}
	
	@Test
	public void shouldThrowInvalidRequestExceptionWhenConceptTranslatorReturnsNullForValidCode() {
		// Given - valid code but translator returns null (concept not found in system)
		when(conceptTranslator.toOpenmrsType(codeableConcept)).thenReturn(null);
		
		// When & Then
		InvalidRequestException exception = assertThrows(InvalidRequestException.class,
			() -> validator.validate(serviceRequest));
		assertEquals("Invalid ServiceRequest code", exception.getMessage());
	}
	
	private Concept conceptWithEmrapiMapping(String code) {
		Concept concept = new Concept();
		ConceptSource source = new ConceptSource();
		source.setName(EMRAPI_SOURCE_NAME);
		ConceptReferenceTerm term = new ConceptReferenceTerm();
		term.setConceptSource(source);
		term.setCode(code);
		ConceptMap conceptMap = new ConceptMap();
		conceptMap.setConceptReferenceTerm(term);
		Set<ConceptMap> mappings = new HashSet<>();
		mappings.add(conceptMap);
		concept.setConceptMappings(mappings);
		return concept;
	}
	
	private int orderUuidCounter = 0;
	
	private Order admitOrder(OrderType adtOrderType, Concept admitConcept, Date dateActivated) {
		Order order = new Order();
		order.setUuid("order-uuid-" + (orderUuidCounter++));
		order.setOrderType(adtOrderType);
		order.setConcept(admitConcept);
		order.setDateActivated(dateActivated);
		return order;
	}
	
	private Encounter encounterWithOrders(Visit visit, Order... orders) {
		Encounter encounter = new Encounter();
		encounter.setVisit(visit);
		for (Order order : orders) {
			encounter.addOrder(order);
		}
		return encounter;
	}
	
	@Test
	public void shouldThrowInvalidRequestExceptionWhenActiveAdmitOrderAlreadyExistsForTheVisit() {
		Concept admitConcept = conceptWithEmrapiMapping("ADMIT");
		orderType.setName("ADT Order");
		CodeableConcept admitCodeableConcept = new CodeableConcept();
		admitCodeableConcept.addCoding(new Coding().setCode("admit-uuid"));
		serviceRequest.setCode(admitCodeableConcept);
		serviceRequest.setEncounter(new Reference("Encounter/encounter-uuid"));

		Visit visit = new Visit();
		Order existingAdmitOrder = admitOrder(orderType, admitConcept, new Date());
		Encounter existingEncounter = encounterWithOrders(visit, existingAdmitOrder);
		visit.setEncounters(new HashSet<>(Collections.singletonList(existingEncounter)));

		Encounter newEncounter = new Encounter();
		newEncounter.setVisit(visit);

		when(conceptTranslator.toOpenmrsType(admitCodeableConcept)).thenReturn(admitConcept);
		when(orderService.getOrderTypeByConcept(admitConcept)).thenReturn(orderType);
		when(encounterReferenceTranslator.toOpenmrsType(serviceRequest.getEncounter())).thenReturn(newEncounter);

		InvalidRequestException exception = assertThrows(InvalidRequestException.class,
			() -> validator.validate(serviceRequest));
		assertEquals("An active Admit order already exists for this visit", exception.getMessage());
	}
	
	@Test
	public void shouldAllowAdmitOrderWhenThePreviousAdmitOrderWasAlreadySupersededByALaterOrder() {
		Concept admitConcept = conceptWithEmrapiMapping("ADMIT");
		Concept dischargeConcept = conceptWithEmrapiMapping("DISCHARGE");
		orderType.setName("ADT Order");
		CodeableConcept admitCodeableConcept = new CodeableConcept();
		admitCodeableConcept.addCoding(new Coding().setCode("admit-uuid"));
		serviceRequest.setCode(admitCodeableConcept);
		serviceRequest.setEncounter(new Reference("Encounter/encounter-uuid"));

		Visit visit = new Visit();
		Date orderDate = new Date(System.currentTimeMillis() - 100000);
		Order fulfilledAdmitOrder = admitOrder(orderType, admitConcept, orderDate);
		Order laterDischargeOrder = admitOrder(orderType, dischargeConcept, new Date());
		Encounter orderEncounter = encounterWithOrders(visit, fulfilledAdmitOrder, laterDischargeOrder);

		visit.setEncounters(new HashSet<>(Collections.singletonList(orderEncounter)));

		Encounter newEncounter = new Encounter();
		newEncounter.setVisit(visit);

		when(conceptTranslator.toOpenmrsType(admitCodeableConcept)).thenReturn(admitConcept);
		when(orderService.getOrderTypeByConcept(admitConcept)).thenReturn(orderType);
		when(encounterReferenceTranslator.toOpenmrsType(serviceRequest.getEncounter())).thenReturn(newEncounter);

		validator.validate(serviceRequest);
	}
	
	@Test
	public void shouldAllowDischargeOrderAfterThePreviousDischargeWasSupersededByAnUndoDischargeOrder() {
		Concept dischargeConcept = conceptWithEmrapiMapping("DISCHARGE");
		Concept undoDischargeConcept = conceptWithEmrapiMapping("UNDO_DISCHARGE");
		orderType.setName("ADT Order");
		CodeableConcept dischargeCodeableConcept = new CodeableConcept();
		dischargeCodeableConcept.addCoding(new Coding().setCode("discharge-uuid"));
		serviceRequest.setCode(dischargeCodeableConcept);
		serviceRequest.setEncounter(new Reference("Encounter/encounter-uuid"));

		Visit visit = new Visit();
		Date firstDischargeDate = new Date(System.currentTimeMillis() - 200000);
		Date undoDischargeDate = new Date(System.currentTimeMillis() - 100000);
		Order firstDischargeOrder = admitOrder(orderType, dischargeConcept, firstDischargeDate);
		Order undoDischargeOrder = admitOrder(orderType, undoDischargeConcept, undoDischargeDate);
		Encounter orderEncounter = encounterWithOrders(visit, firstDischargeOrder, undoDischargeOrder);

		visit.setEncounters(new HashSet<>(Collections.singletonList(orderEncounter)));

		Encounter newEncounter = new Encounter();
		newEncounter.setVisit(visit);

		when(conceptTranslator.toOpenmrsType(dischargeCodeableConcept)).thenReturn(dischargeConcept);
		when(orderService.getOrderTypeByConcept(dischargeConcept)).thenReturn(orderType);
		when(encounterReferenceTranslator.toOpenmrsType(serviceRequest.getEncounter())).thenReturn(newEncounter);

		validator.validate(serviceRequest);
	}
	
	private void validateAdmitAgainst(Order... existingOrders) {
		Visit visit = new Visit();
		orderType.setName("ADT Order");
		Encounter orderEncounter = encounterWithOrders(visit, existingOrders);
		visit.setEncounters(new HashSet<>(Collections.singletonList(orderEncounter)));
		stubAdmitRequestInVisit(visit);
		validator.validate(serviceRequest);
	}
	
	@Test
	public void shouldRejectAdmitOrderWhenAnAdmitWasFollowedByATransferButNeverDischarged() {
		Order admit = admitOrder(orderType, conceptWithEmrapiMapping("ADMIT"), new Date(System.currentTimeMillis() - 200000));
		Order transfer = admitOrder(orderType, conceptWithEmrapiMapping("TRANSFER"), new Date(System.currentTimeMillis() - 100000));
		
		assertThrows(InvalidRequestException.class, () -> validateAdmitAgainst(admit, transfer));
	}
	
	@Test
	public void shouldAllowAdmitOrderWhenTheAdmitWasClosedByADischargeAtTheSameInstant() {
		Date sameTime = new Date();
		Order admit = admitOrder(orderType, conceptWithEmrapiMapping("ADMIT"), sameTime);
		Order discharge = admitOrder(orderType, conceptWithEmrapiMapping("DISCHARGE"), sameTime);
		
		validateAdmitAgainst(admit, discharge);
	}
	
	@Test
	public void shouldAllowAdmitOrderWhenThePreviousAdmitOrderWasDiscontinued() {
		Order admit = admitOrder(orderType, conceptWithEmrapiMapping("ADMIT"), new Date(System.currentTimeMillis() - 200000));
		Order discontinuedAdmit = org.mockito.Mockito.spy(admit);
		org.mockito.Mockito.doReturn(true).when(discontinuedAdmit).isDiscontinuedRightNow();
		
		validateAdmitAgainst(discontinuedAdmit);
	}
	
	@Test
	public void shouldAllowAdmitOrderWhenThePreviousAdmitOrderHasExpired() {
		Order admit = admitOrder(orderType, conceptWithEmrapiMapping("ADMIT"), new Date(System.currentTimeMillis() - 200000));
		admit.setAutoExpireDate(new Date(System.currentTimeMillis() - 100000));
		
		validateAdmitAgainst(admit);
	}
	
	@Test
	public void shouldIgnoreDiscontinueActionOrdersWhenCheckingForAnActiveAdmit() {
		Order admit = admitOrder(orderType, conceptWithEmrapiMapping("ADMIT"), new Date());
		admit.setAction(Order.Action.DISCONTINUE);
		
		validateAdmitAgainst(admit);
	}
	
	@Test
	public void shouldAllowAdmitOrderWhenNoEncounterReferenceIsPresent() {
		Concept admitConcept = conceptWithEmrapiMapping("ADMIT");
		orderType.setName("ADT Order");
		CodeableConcept admitCodeableConcept = new CodeableConcept();
		admitCodeableConcept.addCoding(new Coding().setCode("admit-uuid"));
		serviceRequest.setCode(admitCodeableConcept);
		
		when(conceptTranslator.toOpenmrsType(admitCodeableConcept)).thenReturn(admitConcept);
		when(orderService.getOrderTypeByConcept(admitConcept)).thenReturn(orderType);
		
		validator.validate(serviceRequest);
	}
	
	@Test
	public void shouldThrowInvalidRequestExceptionWhenActiveDischargeOrderAlreadyExistsForTheVisit() {
		Concept dischargeConcept = conceptWithEmrapiMapping("DISCHARGE");
		orderType.setName("ADT Order");
		CodeableConcept dischargeCodeableConcept = new CodeableConcept();
		dischargeCodeableConcept.addCoding(new Coding().setCode("discharge-uuid"));
		serviceRequest.setCode(dischargeCodeableConcept);
		serviceRequest.setEncounter(new Reference("Encounter/encounter-uuid"));

		Visit visit = new Visit();
		Order existingDischargeOrder = admitOrder(orderType, dischargeConcept, new Date());
		Encounter existingEncounter = encounterWithOrders(visit, existingDischargeOrder);
		visit.setEncounters(new HashSet<>(Collections.singletonList(existingEncounter)));

		Encounter newEncounter = new Encounter();
		newEncounter.setVisit(visit);

		when(conceptTranslator.toOpenmrsType(dischargeCodeableConcept)).thenReturn(dischargeConcept);
		when(orderService.getOrderTypeByConcept(dischargeConcept)).thenReturn(orderType);
		when(encounterReferenceTranslator.toOpenmrsType(serviceRequest.getEncounter())).thenReturn(newEncounter);

		InvalidRequestException exception = assertThrows(InvalidRequestException.class,
			() -> validator.validate(serviceRequest));
		assertEquals("An active Discharge order already exists for this visit", exception.getMessage());
	}
	
	@Test
	public void shouldThrowInvalidRequestExceptionWhenActiveUndoDischargeOrderAlreadyExistsForTheVisit() {
		Concept undoDischargeConcept = conceptWithEmrapiMapping("UNDO_DISCHARGE");
		orderType.setName("ADT Order");
		CodeableConcept undoDischargeCodeableConcept = new CodeableConcept();
		undoDischargeCodeableConcept.addCoding(new Coding().setCode("undo-discharge-uuid"));
		serviceRequest.setCode(undoDischargeCodeableConcept);
		serviceRequest.setEncounter(new Reference("Encounter/encounter-uuid"));

		Visit visit = new Visit();
		Order existingUndoDischargeOrder = admitOrder(orderType, undoDischargeConcept, new Date());
		Encounter existingEncounter = encounterWithOrders(visit, existingUndoDischargeOrder);
		visit.setEncounters(new HashSet<>(Collections.singletonList(existingEncounter)));

		Encounter newEncounter = new Encounter();
		newEncounter.setVisit(visit);

		when(conceptTranslator.toOpenmrsType(undoDischargeCodeableConcept)).thenReturn(undoDischargeConcept);
		when(orderService.getOrderTypeByConcept(undoDischargeConcept)).thenReturn(orderType);
		when(encounterReferenceTranslator.toOpenmrsType(serviceRequest.getEncounter())).thenReturn(newEncounter);

		InvalidRequestException exception = assertThrows(InvalidRequestException.class,
			() -> validator.validate(serviceRequest));
		assertEquals("An active Undo Discharge order already exists for this visit", exception.getMessage());
	}
	
	@Test
	public void shouldAllowTransferOrderWhenAnActiveTransferOrderAlreadyExistsForTheVisit() {
		Concept transferConcept = conceptWithEmrapiMapping("TRANSFER");
		orderType.setName("ADT Order");
		CodeableConcept transferCodeableConcept = new CodeableConcept();
		transferCodeableConcept.addCoding(new Coding().setCode("transfer-uuid"));
		serviceRequest.setCode(transferCodeableConcept);
		serviceRequest.setEncounter(new Reference("Encounter/encounter-uuid"));

		Visit visit = new Visit();
		Order existingTransferOrder = admitOrder(orderType, transferConcept, new Date());
		Encounter existingEncounter = encounterWithOrders(visit, existingTransferOrder);
		visit.setEncounters(new HashSet<>(Collections.singletonList(existingEncounter)));

		when(conceptTranslator.toOpenmrsType(transferCodeableConcept)).thenReturn(transferConcept);
		when(orderService.getOrderTypeByConcept(transferConcept)).thenReturn(orderType);

		validator.validate(serviceRequest);
	}
	
	private void stubAdmitRequestInVisit(Visit visit, Encounter... visitEncounters) {
		Concept admitConcept = conceptWithEmrapiMapping("ADMIT");
		orderType.setName("ADT Order");
		CodeableConcept admitCodeableConcept = new CodeableConcept();
		admitCodeableConcept.addCoding(new Coding().setCode("admit-uuid"));
		serviceRequest.setCode(admitCodeableConcept);
		serviceRequest.setEncounter(new Reference("Encounter/encounter-uuid"));
		
		Encounter newEncounter = new Encounter();
		newEncounter.setVisit(visit);
		when(conceptTranslator.toOpenmrsType(admitCodeableConcept)).thenReturn(admitConcept);
		when(orderService.getOrderTypeByConcept(admitConcept)).thenReturn(orderType);
		when(encounterReferenceTranslator.toOpenmrsType(serviceRequest.getEncounter())).thenReturn(newEncounter);
	}
	
	private Encounter encounterOfType(String typeName, Date datetime) {
		Encounter encounter = new Encounter();
		org.openmrs.EncounterType type = new org.openmrs.EncounterType();
		type.setName(typeName);
		encounter.setEncounterType(type);
		encounter.setEncounterDatetime(datetime);
		return encounter;
	}
	
	@Test
	public void shouldAllowAdmitOrderWhenThePreviousAdmitOrderWasFulfilledByALaterAdmissionEncounter() {
		Visit visit = new Visit();
		Date orderDate = new Date(System.currentTimeMillis() - 3600_000L);
		Order existingAdmitOrder = admitOrder(orderType, conceptWithEmrapiMapping("ADMIT"), orderDate);
		orderType.setName("ADT Order");
		Encounter orderEncounter = encounterWithOrders(visit, existingAdmitOrder);
		Encounter admission = encounterOfType("ADMISSION", new Date());
		visit.setEncounters(new HashSet<>(java.util.Arrays.asList(orderEncounter, admission)));
		stubAdmitRequestInVisit(visit);
		
		validator.validate(serviceRequest);
	}
	
	@Test
	public void shouldRejectAdmitOrderWhenOnlyAnEarlierAdmissionEncounterExists() {
		Visit visit = new Visit();
		Date orderDate = new Date();
		Order existingAdmitOrder = admitOrder(orderType, conceptWithEmrapiMapping("ADMIT"), orderDate);
		orderType.setName("ADT Order");
		Encounter orderEncounter = encounterWithOrders(visit, existingAdmitOrder);
		Encounter earlierAdmission = encounterOfType("ADMISSION", new Date(orderDate.getTime() - 3600_000L));
		visit.setEncounters(new HashSet<>(java.util.Arrays.asList(orderEncounter, earlierAdmission)));
		stubAdmitRequestInVisit(visit);
		
		assertThrows(InvalidRequestException.class, () -> validator.validate(serviceRequest));
	}
	
	@Test
	public void shouldSkipDuplicateCheckWhenThereIsNoVisitAndNoActiveVisit() {
		stubAdmitRequestInVisit(null);
		when(encounterReferenceTranslator.toOpenmrsType(serviceRequest.getEncounter())).thenReturn(
		    encounterWithPatient(null));
		
		validator.validate(serviceRequest);
	}
	
	@Test
	public void shouldRejectDuplicateAdmitUsingTheActiveVisitWhenTheEncounterHasNoVisit() {
		Visit visit = new Visit();
		Order existingAdmitOrder = admitOrder(orderType, conceptWithEmrapiMapping("ADMIT"), new Date());
		orderType.setName("ADT Order");
		visit.setEncounters(new HashSet<>(java.util.Arrays.asList(encounterWithOrders(visit, existingAdmitOrder))));
		stubAdmitRequestInVisit(null);
		org.openmrs.Patient patient = new org.openmrs.Patient();
		when(encounterReferenceTranslator.toOpenmrsType(serviceRequest.getEncounter())).thenReturn(encounterWithPatient(patient));
		when(visitService.getActiveVisitsByPatient(patient)).thenReturn(java.util.Collections.singletonList(visit));
		
		assertThrows(InvalidRequestException.class, () -> validator.validate(serviceRequest));
	}
	
	@Test
	public void shouldRejectDuplicateAdmitFoundInAVisitLessEncounterWithinTheActiveVisitWindow() {
		Visit visit = new Visit();
		visit.setStartDatetime(new Date(System.currentTimeMillis() - 86400_000L));
		Order existingAdmitOrder = admitOrder(orderType, conceptWithEmrapiMapping("ADMIT"), new Date());
		orderType.setName("ADT Order");
		Encounter visitLess = encounterWithOrders(null, existingAdmitOrder);
		visitLess.setEncounterDatetime(new Date());
		stubAdmitRequestInVisit(null);
		org.openmrs.Patient patient = new org.openmrs.Patient();
		when(encounterReferenceTranslator.toOpenmrsType(serviceRequest.getEncounter())).thenReturn(encounterWithPatient(patient));
		when(visitService.getActiveVisitsByPatient(patient)).thenReturn(java.util.Collections.singletonList(visit));
		when(encounterService.getEncounters(org.mockito.ArgumentMatchers.any(org.openmrs.parameter.EncounterSearchCriteria.class))).thenReturn(java.util.Collections.singletonList(visitLess));
		
		assertThrows(InvalidRequestException.class, () -> validator.validate(serviceRequest));
	}
	
	@Test
	public void shouldIgnoreVisitLessEncountersOutsideTheActiveVisitWindow() {
		Visit visit = new Visit();
		visit.setStartDatetime(new Date(System.currentTimeMillis() - 3600_000L));
		Order oldAdmitOrder = admitOrder(orderType, conceptWithEmrapiMapping("ADMIT"), new Date());
		orderType.setName("ADT Order");
		Encounter visitLess = encounterWithOrders(null, oldAdmitOrder);
		visitLess.setEncounterDatetime(new Date(System.currentTimeMillis() - 86400_000L));
		stubAdmitRequestInVisit(null);
		org.openmrs.Patient patient = new org.openmrs.Patient();
		when(encounterReferenceTranslator.toOpenmrsType(serviceRequest.getEncounter())).thenReturn(encounterWithPatient(patient));
		when(visitService.getActiveVisitsByPatient(patient)).thenReturn(java.util.Collections.singletonList(visit));
		when(encounterService.getEncounters(org.mockito.ArgumentMatchers.any(org.openmrs.parameter.EncounterSearchCriteria.class))).thenReturn(java.util.Collections.singletonList(visitLess));
		
		validator.validate(serviceRequest);
	}
	
	private Date hoursAgo(int hours) {
		return new Date(System.currentTimeMillis() - hours * 3600_000L);
	}
	
	@Test
	public void shouldRejectAdmitOrderAfterAnUndoDischargeReopenedTheAdmission() {
		validateAdmitAgainstAndExpectRejection(admitOrder(orderType, conceptWithEmrapiMapping("ADMIT"), hoursAgo(3)),
		    admitOrder(orderType, conceptWithEmrapiMapping("DISCHARGE"), hoursAgo(2)),
		    admitOrder(orderType, conceptWithEmrapiMapping("UNDO_DISCHARGE"), hoursAgo(1)));
	}
	
	@Test
	public void shouldAllowAdmitOrderAfterADischargeThatWasNotUndone() {
		validateAdmitAgainst(admitOrder(orderType, conceptWithEmrapiMapping("ADMIT"), hoursAgo(3)),
		    admitOrder(orderType, conceptWithEmrapiMapping("DISCHARGE"), hoursAgo(2)));
	}
	
	private void validateAdmitAgainstAndExpectRejection(Order... existingOrders) {
		assertThrows(InvalidRequestException.class, () -> validateAdmitAgainst(existingOrders));
	}
	
	private void validateUndoDischargeAgainst(Encounter... extraEncounters) {
		Concept undoConcept = conceptWithEmrapiMapping("UNDO_DISCHARGE");
		orderType.setName("ADT Order");
		CodeableConcept code = new CodeableConcept();
		code.addCoding(new Coding().setCode("undo-uuid"));
		serviceRequest.setCode(code);
		serviceRequest.setEncounter(new Reference("Encounter/encounter-uuid"));
		Visit visit = new Visit();
		Encounter orderEncounter = encounterWithOrders(visit, admitOrder(orderType, undoConcept, hoursAgo(2)));
		java.util.Set<Encounter> encounters = new HashSet<>(Collections.singletonList(orderEncounter));
		encounters.addAll(Arrays.asList(extraEncounters));
		visit.setEncounters(encounters);
		Encounter newEncounter = new Encounter();
		newEncounter.setVisit(visit);
		when(conceptTranslator.toOpenmrsType(code)).thenReturn(undoConcept);
		when(orderService.getOrderTypeByConcept(undoConcept)).thenReturn(orderType);
		when(encounterReferenceTranslator.toOpenmrsType(serviceRequest.getEncounter())).thenReturn(newEncounter);
		validator.validate(serviceRequest);
	}
	
	@Test
	public void shouldTreatUndoDischargeAsFulfilledOnceTheDischargeEncounterIsVoided() {
		Encounter voidedDischarge = encounterOfType("DISCHARGE", hoursAgo(5));
		voidedDischarge.setVoided(true);
		voidedDischarge.setDateVoided(hoursAgo(1));
		validateUndoDischargeAgainst(voidedDischarge);
	}
	
	@Test
	public void shouldKeepUndoDischargeActiveWhileTheDischargeEncounterIsNotVoided() {
		Encounter discharge = encounterOfType("DISCHARGE", hoursAgo(5));
		assertThrows(InvalidRequestException.class, () -> validateUndoDischargeAgainst(discharge));
	}
	
	private Encounter encounterWithPatient(org.openmrs.Patient patient) {
		Encounter encounter = new Encounter();
		encounter.setPatient(patient);
		return encounter;
	}
}
