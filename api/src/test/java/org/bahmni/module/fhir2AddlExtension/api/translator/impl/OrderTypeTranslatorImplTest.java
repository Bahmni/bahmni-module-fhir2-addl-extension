package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.OrderType;
import org.openmrs.api.OrderService;
import org.springframework.test.util.ReflectionTestUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OrderTypeTranslatorImplTest {
	
	private static final String ORDER_TYPE_UUID = "test-uuid";
	
	private static final String ORDER_TYPE_NAME = "Test Order Type";
	
	@Mock
	private OrderService orderService;
	
	@InjectMocks
	private OrderTypeTranslatorImpl translator;
	
	private OrderType orderType;
	
	@Before
	public void setup() {
		// Create a sample OrderType for testing
		orderType = new OrderType();
		orderType.setUuid(ORDER_TYPE_UUID);
		orderType.setName(ORDER_TYPE_NAME);
	}
	
	@Test
	public void shouldTranslateOrderTypeToCodeableConcept() {
		// Call method under test
		CodeableConcept result = translator.toFhirResource(orderType);
		
		// Verify result
		assertThat(result, notNullValue());
		assertThat(result.getCoding(), notNullValue());
		assertThat(result.getCoding(), hasSize(1));
		
		Coding coding = result.getCodingFirstRep();
		assertThat(coding.getSystem(), equalTo(BahmniFhirConstants.ORDER_TYPE_SYSTEM_URI));
		assertThat(coding.getCode(), equalTo(ORDER_TYPE_UUID));
		assertThat(coding.getDisplay(), equalTo(ORDER_TYPE_NAME));
		
		assertThat(result.getText(), equalTo(ORDER_TYPE_NAME));
	}
	
	@Test(expected = NullPointerException.class)
	public void shouldThrowExceptionWhenTranslatingNullOrderType() {
		translator.toFhirResource(null);
	}
	
	@Test
	public void shouldTranslateCodeableConceptToOrderType() {
		// Create a sample CodeableConcept
		CodeableConcept codeableConcept = new CodeableConcept();
		Coding coding = codeableConcept.addCoding();
		coding.setSystem(BahmniFhirConstants.ORDER_TYPE_SYSTEM_URI);
		coding.setCode(ORDER_TYPE_UUID);
		coding.setDisplay(ORDER_TYPE_NAME);
		
		// Mock OrderService behavior
		when(orderService.getOrderTypeByUuid(ORDER_TYPE_UUID)).thenReturn(orderType);
		
		// Call method under test
		OrderType result = translator.toOpenmrsType(codeableConcept);
		
		// Verify result
		assertThat(result, notNullValue());
		assertThat(result, equalTo(orderType));
		
		// Verify OrderService was called with correct UUID
		verify(orderService).getOrderTypeByUuid(ORDER_TYPE_UUID);
	}
	
	@Test
	public void shouldReturnNullWhenNoMatchingCodingFound() {
		// Create a CodeableConcept with a coding that has a different system
		CodeableConcept codeableConcept = new CodeableConcept();
		Coding coding = codeableConcept.addCoding();
		coding.setSystem("http://some-other-system.org");
		coding.setCode(ORDER_TYPE_UUID);
		
		// Call method under test
		OrderType result = translator.toOpenmrsType(codeableConcept);
		
		// Verify result is null
		assertThat(result, nullValue());
		
		// Verify OrderService was not called
		verifyNoInteractions(orderService);
	}
	
	@Test
	public void shouldReturnNullWhenOrderServiceReturnsNull() {
		// Create a sample CodeableConcept
		CodeableConcept codeableConcept = new CodeableConcept();
		Coding coding = codeableConcept.addCoding();
		coding.setSystem(BahmniFhirConstants.ORDER_TYPE_SYSTEM_URI);
		coding.setCode(ORDER_TYPE_UUID);
		
		// Mock OrderService to return null
		when(orderService.getOrderTypeByUuid(ORDER_TYPE_UUID)).thenReturn(null);
		
		// Call method under test
		OrderType result = translator.toOpenmrsType(codeableConcept);
		
		// Verify result is null
		assertThat(result, nullValue());
		
		// Verify OrderService was called with correct UUID
		verify(orderService).getOrderTypeByUuid(ORDER_TYPE_UUID);
	}
	
	@Test
	public void shouldReturnNullWhenCodingHasNoSystem() {
		// Create a CodeableConcept with a coding that has no system
		CodeableConcept codeableConcept = new CodeableConcept();
		Coding coding = codeableConcept.addCoding();
		coding.setCode(ORDER_TYPE_UUID);
		
		// Call method under test
		OrderType result = translator.toOpenmrsType(codeableConcept);
		
		// Verify result is null
		assertThat(result, nullValue());
		
		// Verify OrderService was not called
		verifyNoInteractions(orderService);
	}
	
	@Test(expected = NullPointerException.class)
	public void shouldThrowExceptionWhenTranslatingNullCodeableConcept() {
		translator.toOpenmrsType(null);
	}
}
