package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Order;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@RunWith(MockitoJUnitRunner.class)
public class ServiceRequestPriorityTranslatorImplTest {
	
	@InjectMocks
	private ServiceRequestPriorityTranslatorImpl translator;
	
	@Test
	public void shouldTranslateStatUrgencyToStatPriority() {
		ServiceRequest.ServiceRequestPriority result = translator.toFhirResource(Order.Urgency.STAT);
		
		assertThat(result, notNullValue());
		assertThat(result, equalTo(ServiceRequest.ServiceRequestPriority.STAT));
	}
	
	@Test
	public void shouldTranslateRoutineUrgencyToRoutinePriority() {
		ServiceRequest.ServiceRequestPriority result = translator.toFhirResource(Order.Urgency.ROUTINE);
		
		assertThat(result, notNullValue());
		assertThat(result, equalTo(ServiceRequest.ServiceRequestPriority.ROUTINE));
	}
	
	@Test
	public void shouldTranslateOnScheduledDateUrgencyToRoutinePriority() {
		ServiceRequest.ServiceRequestPriority result = translator.toFhirResource(Order.Urgency.ON_SCHEDULED_DATE);
		
		assertThat(result, notNullValue());
		assertThat(result, equalTo(ServiceRequest.ServiceRequestPriority.ROUTINE));
	}
	
	@Test
	public void shouldReturnRoutinePriorityWhenUrgencyIsNull() {
		ServiceRequest.ServiceRequestPriority result = translator.toFhirResource(null);
		
		assertThat(result, notNullValue());
		assertThat(result, equalTo(ServiceRequest.ServiceRequestPriority.ROUTINE));
	}
	
	// Tests for toOpenmrsType method
	
	@Test
	public void shouldTranslateStatPriorityToStatUrgency() {
		Order.Urgency result = translator.toOpenmrsType(ServiceRequest.ServiceRequestPriority.STAT);
		
		assertThat(result, notNullValue());
		assertThat(result, equalTo(Order.Urgency.STAT));
	}
	
	@Test
	public void shouldTranslateRoutinePriorityToRoutineUrgency() {
		Order.Urgency result = translator.toOpenmrsType(ServiceRequest.ServiceRequestPriority.ROUTINE);
		
		assertThat(result, notNullValue());
		assertThat(result, equalTo(Order.Urgency.ROUTINE));
	}
	
	@Test
	public void shouldTranslateAsapPriorityToRoutineUrgency() {
		Order.Urgency result = translator.toOpenmrsType(ServiceRequest.ServiceRequestPriority.ASAP);
		
		assertThat(result, notNullValue());
		assertThat(result, equalTo(Order.Urgency.ROUTINE));
	}
	
	@Test
	public void shouldTranslateUrgentPriorityToRoutineUrgency() {
		Order.Urgency result = translator.toOpenmrsType(ServiceRequest.ServiceRequestPriority.URGENT);
		
		assertThat(result, notNullValue());
		assertThat(result, equalTo(Order.Urgency.ROUTINE));
	}
	
	@Test
	public void shouldReturnRoutineUrgencyWhenPriorityIsNull() {
		Order.Urgency result = translator.toOpenmrsType(null);
		
		assertThat(result, notNullValue());
		assertThat(result, equalTo(Order.Urgency.ROUTINE));
	}
}
