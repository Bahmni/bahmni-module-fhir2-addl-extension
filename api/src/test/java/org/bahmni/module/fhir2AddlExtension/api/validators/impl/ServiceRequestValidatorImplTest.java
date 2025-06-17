package org.bahmni.module.fhir2AddlExtension.api.validators.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2AddlExtension.api.validators.ServiceRequestValidator;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.OrderType;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServiceRequestValidatorImplTest {
	
	@Mock
	private ConceptTranslator conceptTranslator;
	
	@Mock
	private OrderService orderService;
	
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
}
