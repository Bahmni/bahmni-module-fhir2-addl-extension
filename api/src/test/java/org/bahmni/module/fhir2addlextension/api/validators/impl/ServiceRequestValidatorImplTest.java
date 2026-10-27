package org.bahmni.module.fhir2addlextension.api.validators.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2addlextension.api.service.DispositionOrderTypeResolver;
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
import org.openmrs.EncounterType;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Visit;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServiceRequestValidatorImplTest {
	
	@Mock
	private ConceptTranslator conceptTranslator;
	
	@Mock
	private OrderService orderService;
	
	@Mock
	private DispositionOrderTypeResolver dispositionOrderTypeResolver;
	
	@Mock
	private EncounterReferenceTranslator<Encounter> encounterReferenceTranslator;
	
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
		validatorImpl.setDispositionOrderTypeResolver(dispositionOrderTypeResolver);
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
	
	private Order admitOrder(OrderType adtOrderType, Concept admitConcept, Date dateActivated) {
		Order order = new Order();
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
	public void shouldAllowAdmitOrderWhenThePreviousAdmitOrderWasAlreadyFulfilledByAnAdmissionEncounter() {
		Concept admitConcept = conceptWithEmrapiMapping("ADMIT");
		orderType.setName("ADT Order");
		CodeableConcept admitCodeableConcept = new CodeableConcept();
		admitCodeableConcept.addCoding(new Coding().setCode("admit-uuid"));
		serviceRequest.setCode(admitCodeableConcept);
		serviceRequest.setEncounter(new Reference("Encounter/encounter-uuid"));

		Visit visit = new Visit();
		Date orderDate = new Date(System.currentTimeMillis() - 100000);
		Order fulfilledAdmitOrder = admitOrder(orderType, admitConcept, orderDate);
		Encounter orderEncounter = encounterWithOrders(visit, fulfilledAdmitOrder);

		EncounterType admissionType = new EncounterType();
		admissionType.setName("ADMISSION");
		Encounter admissionEncounter = new Encounter();
		admissionEncounter.setVisit(visit);
		admissionEncounter.setEncounterType(admissionType);
		admissionEncounter.setEncounterDatetime(new Date());

		visit.setEncounters(new HashSet<>(java.util.Arrays.asList(orderEncounter, admissionEncounter)));

		Encounter newEncounter = new Encounter();
		newEncounter.setVisit(visit);

		when(conceptTranslator.toOpenmrsType(admitCodeableConcept)).thenReturn(admitConcept);
		when(orderService.getOrderTypeByConcept(admitConcept)).thenReturn(orderType);
		when(encounterReferenceTranslator.toOpenmrsType(serviceRequest.getEncounter())).thenReturn(newEncounter);

		validator.validate(serviceRequest);
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
	public void shouldThrowInvalidRequestExceptionWhenActiveTransferOrderAlreadyExistsForTheVisit() {
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

		Encounter newEncounter = new Encounter();
		newEncounter.setVisit(visit);

		when(conceptTranslator.toOpenmrsType(transferCodeableConcept)).thenReturn(transferConcept);
		when(orderService.getOrderTypeByConcept(transferConcept)).thenReturn(orderType);
		when(encounterReferenceTranslator.toOpenmrsType(serviceRequest.getEncounter())).thenReturn(newEncounter);

		InvalidRequestException exception = assertThrows(InvalidRequestException.class,
			() -> validator.validate(serviceRequest));
		assertEquals("An active Transfer order already exists for this visit", exception.getMessage());
	}
	
	@Test
	public void shouldAllowTransferOrderWhenThePreviousTransferOrderWasAlreadyFulfilledByADischargeEncounter() {
		Concept transferConcept = conceptWithEmrapiMapping("TRANSFER");
		orderType.setName("ADT Order");
		CodeableConcept transferCodeableConcept = new CodeableConcept();
		transferCodeableConcept.addCoding(new Coding().setCode("transfer-uuid"));
		serviceRequest.setCode(transferCodeableConcept);
		serviceRequest.setEncounter(new Reference("Encounter/encounter-uuid"));

		Visit visit = new Visit();
		Date orderDate = new Date(System.currentTimeMillis() - 100000);
		Order fulfilledTransferOrder = admitOrder(orderType, transferConcept, orderDate);
		Encounter orderEncounter = encounterWithOrders(visit, fulfilledTransferOrder);

		EncounterType dischargeType = new EncounterType();
		dischargeType.setName("DISCHARGE");
		Encounter dischargeEncounter = new Encounter();
		dischargeEncounter.setVisit(visit);
		dischargeEncounter.setEncounterType(dischargeType);
		dischargeEncounter.setEncounterDatetime(new Date());

		visit.setEncounters(new HashSet<>(java.util.Arrays.asList(orderEncounter, dischargeEncounter)));

		Encounter newEncounter = new Encounter();
		newEncounter.setVisit(visit);

		when(conceptTranslator.toOpenmrsType(transferCodeableConcept)).thenReturn(transferConcept);
		when(orderService.getOrderTypeByConcept(transferConcept)).thenReturn(orderType);
		when(encounterReferenceTranslator.toOpenmrsType(serviceRequest.getEncounter())).thenReturn(newEncounter);

		validator.validate(serviceRequest);
	}
}
