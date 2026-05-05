package org.bahmni.module.fhir2addlextension.api.translator.impl;

import org.bahmni.module.fhir2addlextension.api.TestUtils;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Order;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.bahmni.module.fhir2addlextension.api.TestUtils.minusDays;
import static org.bahmni.module.fhir2addlextension.api.TestUtils.plusDays;

public class ServiceRequestStatusTranslatorImplTest {
	
	/**
	 * should return entered-in-error because the order is voided
	 */
	@Test
	public void shouldValidateOrderStatusAsEnteredInErrorWhenVoided() {
		Order order = new Order();
		order.setVoided(true);
		ServiceRequestStatusTranslatorImpl serviceRequestStatusTranslator = new ServiceRequestStatusTranslatorImpl();
		ServiceRequest.ServiceRequestStatus requestStatus = serviceRequestStatusTranslator.toFhirResource(order);
		Assert.assertEquals(ServiceRequest.ServiceRequestStatus.ENTEREDINERROR, requestStatus);
	}
	
	//*********************** Service Status when order fulfiller status is available *******************
	
	/**
	 * order: urgency routine, action new, date of activation = yesterday, expiry date undefined,
	 * fulfiller status = received order action and urgency do not matter in this case
	 */
	@Test
	public void shouldValidateOrderStatusAsActiveWhenFulfillerStatusIsReceived() {
		Order order = new Order();
		order.setUrgency(Order.Urgency.ROUTINE);
		order.setFulfillerStatus(Order.FulfillerStatus.RECEIVED);
		order.setAction(Order.Action.NEW);
		Date currentDate = new Date();
		Date dateOfActivation = minusDays(currentDate, 1);
		order.setDateActivated(dateOfActivation);
		ServiceRequestStatusTranslatorImpl serviceRequestStatusTranslator = new ServiceRequestStatusTranslatorImpl();
		ServiceRequest.ServiceRequestStatus requestStatus = serviceRequestStatusTranslator.toFhirResource(order);
		Assert.assertEquals(ServiceRequest.ServiceRequestStatus.ACTIVE, requestStatus);
	}
	
	/**
	 * order: urgency routine, action new, date of activation = yesterday, expiry date undefined,
	 * fulfiller status = in-progress
	 */
	@Test
	public void shouldValidateOrderStatusAsActiveWhenFulfillerStatusIsInProgress() {
		Order order = new Order();
		order.setUrgency(Order.Urgency.ROUTINE);
		order.setFulfillerStatus(Order.FulfillerStatus.IN_PROGRESS);
		order.setAction(Order.Action.NEW);
		Date currentDate = new Date();
		Date dateOfActivation = minusDays(currentDate, 1);
		order.setDateActivated(dateOfActivation);
		ServiceRequestStatusTranslatorImpl serviceRequestStatusTranslator = new ServiceRequestStatusTranslatorImpl();
		ServiceRequest.ServiceRequestStatus requestStatus = serviceRequestStatusTranslator.toFhirResource(order);
		Assert.assertEquals(ServiceRequest.ServiceRequestStatus.ACTIVE, requestStatus);
	}
	
	/**
	 * order: urgency routine, action new, date of activation = yesterday, expiry date undefined,
	 * fulfiller status = completed
	 */
	@Test
	public void shouldValidateOrderStatusAsCompletedWhenFulfillerStatusIsCompleted() {
		Order order = new Order();
		order.setUrgency(Order.Urgency.ROUTINE);
		order.setFulfillerStatus(Order.FulfillerStatus.COMPLETED);
		order.setAction(Order.Action.NEW);
		Date currentDate = new Date();
		Date dateOfActivation = minusDays(currentDate, 1);
		order.setDateActivated(dateOfActivation);
		ServiceRequestStatusTranslatorImpl serviceRequestStatusTranslator = new ServiceRequestStatusTranslatorImpl();
		ServiceRequest.ServiceRequestStatus requestStatus = serviceRequestStatusTranslator.toFhirResource(order);
		Assert.assertEquals(ServiceRequest.ServiceRequestStatus.COMPLETED, requestStatus);
	}
	
	/**
	 * order: urgency routine, action new, date of activation = yesterday, expiry date undefined,
	 * fulfiller status = exception In this case, since the order is not stopped (Date stopped is
	 * null), the order status is unknown.
	 */
	@Test
	public void shouldValidateOrderStatusAsUnknownWhenFulfillerStatusIsException() {
		Order order = new Order();
		order.setUrgency(Order.Urgency.ROUTINE);
		order.setFulfillerStatus(Order.FulfillerStatus.EXCEPTION);
		order.setAction(Order.Action.NEW);
		Date currentDate = new Date();
		Date dateOfActivation = minusDays(currentDate, 1);
		order.setDateActivated(dateOfActivation);
		ServiceRequestStatusTranslatorImpl serviceRequestStatusTranslator = new ServiceRequestStatusTranslatorImpl();
		ServiceRequest.ServiceRequestStatus requestStatus = serviceRequestStatusTranslator.toFhirResource(order);
		Assert.assertEquals(ServiceRequest.ServiceRequestStatus.UNKNOWN, requestStatus);
	}
	
	/**
	 * order: urgency routine, action new, date of activation = yesterday, stop date defined,
	 * fulfiller status = exception
	 */
	@Test
	public void shouldValidateOrderStatusAsRevokedWhenFulfillerStatusIsExceptionWithDateStopped()
	        throws NoSuchFieldException, IllegalAccessException {
		Order order = new Order();
		order.setUrgency(Order.Urgency.ROUTINE);
		order.setFulfillerStatus(Order.FulfillerStatus.EXCEPTION);
		order.setAction(Order.Action.NEW);
		Date currentDate = new Date();
		Date dateOfActivation = minusDays(currentDate, 1);
		order.setDateActivated(dateOfActivation);
		TestUtils.setPropertyOnObject(order, "dateStopped", dateOfActivation);
		ServiceRequestStatusTranslatorImpl serviceRequestStatusTranslator = new ServiceRequestStatusTranslatorImpl();
		ServiceRequest.ServiceRequestStatus requestStatus = serviceRequestStatusTranslator.toFhirResource(order);
		Assert.assertEquals(ServiceRequest.ServiceRequestStatus.REVOKED, requestStatus);
	}
	
	//*********************** Service Status when order fulfiller status is available *******************
	
	/**
	 * order: urgency routine, action new, date of activation = yesterday, expiry date undefined
	 */
	@Test
	public void shouldValidateOrderStatusAsActiveWhenDateOfActivationIsYesterdayAndStopDateIsNotSet() {
		Order order = new Order();
		order.setUrgency(Order.Urgency.ROUTINE);
		order.setAction(Order.Action.NEW);
		Date currentDate = new Date();
		Date dateOfActivation = minusDays(currentDate, 1);
		order.setDateActivated(dateOfActivation);
		ServiceRequestStatusTranslatorImpl serviceRequestStatusTranslator = new ServiceRequestStatusTranslatorImpl();
		ServiceRequest.ServiceRequestStatus requestStatus = serviceRequestStatusTranslator.toFhirResource(order);
		Assert.assertEquals(ServiceRequest.ServiceRequestStatus.ACTIVE, requestStatus);
	}
	
	/**
	 * order: urgency routine, action new, date of activation = yesterday, date stopped is yesterday
	 */
	@Test
	public void shouldValidateOrderStatusAsRevokedWhenDateOfActivationIsYesterdayAndStopDateIsSet()
	        throws NoSuchFieldException, IllegalAccessException {
		Order order = new Order();
		order.setUrgency(Order.Urgency.ROUTINE);
		order.setAction(Order.Action.NEW);
		Date currentDate = new Date();
		Date dateOfActivation = minusDays(currentDate, 1);
		order.setDateActivated(dateOfActivation);
		TestUtils.setPropertyOnObject(order, "dateStopped", dateOfActivation);
		ServiceRequestStatusTranslatorImpl serviceRequestStatusTranslator = new ServiceRequestStatusTranslatorImpl();
		ServiceRequest.ServiceRequestStatus requestStatus = serviceRequestStatusTranslator.toFhirResource(order);
		Assert.assertEquals(ServiceRequest.ServiceRequestStatus.REVOKED, requestStatus);
	}
	
	/**
	 * order: urgency routine, action discontinue, date of activation = yesterday, date stopped is
	 * yesterday the serviceRequest is the discontinuation order, this order is active. the
	 * serviceRequest.replaces is the original order, which is revoked
	 * 
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 */
	@Test
	public void shouldValidateOrderStatusAsCompletedForOrderActionDiscontinue() throws NoSuchFieldException,
	        IllegalAccessException {
		Order order = new Order();
		order.setUrgency(Order.Urgency.ROUTINE);
		order.setAction(Order.Action.DISCONTINUE);
		Date currentDate = new Date();
		Date dateOfActivation = minusDays(currentDate, 1);
		order.setDateActivated(dateOfActivation);
		ServiceRequestStatusTranslatorImpl serviceRequestStatusTranslator = new ServiceRequestStatusTranslatorImpl();
		ServiceRequest.ServiceRequestStatus requestStatus = serviceRequestStatusTranslator.toFhirResource(order);
		Assert.assertEquals(ServiceRequest.ServiceRequestStatus.COMPLETED, requestStatus);
	}
	
	/**
	 * order: urgency routine, action new, date of activation = yesterday, date stopped is tomorrow
	 * order.action - REVISE, RENEW is not applicable for Service orders like Lab,Radiology or
	 * Procedure. It is expected/assumed for service requests like above, there is no revision or
	 * renewal like Medication Requests
	 * 
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 */
	@Test
	public void shouldValidateOrderStatusAsUnknownForOrderActionNotNewOrDiscontinue() throws NoSuchFieldException,
	        IllegalAccessException {
		Order order = new Order();
		order.setUrgency(Order.Urgency.ROUTINE);
		order.setAction(Order.Action.REVISE);
		Date currentDate = new Date();
		Date dateOfActivation = minusDays(currentDate, 1);
		order.setDateActivated(dateOfActivation);
		order.setAutoExpireDate(plusDays(currentDate, 1));
		TestUtils.setPropertyOnObject(order, "dateStopped", dateOfActivation);
		ServiceRequestStatusTranslatorImpl serviceRequestStatusTranslator = new ServiceRequestStatusTranslatorImpl();
		ServiceRequest.ServiceRequestStatus requestStatus = serviceRequestStatusTranslator.toFhirResource(order);
		Assert.assertEquals(ServiceRequest.ServiceRequestStatus.UNKNOWN, requestStatus);
	}
	
	/**
	 * order: urgency routine, action new, date of activation = yesterday, expiry date in future
	 */
	@Test
	public void shouldValidateOrderStatusAsActiveWhenDateOfActivationIsYesterdayAndStopDateIsFuture() {
		Order order = new Order();
		order.setUrgency(Order.Urgency.ROUTINE);
		order.setAction(Order.Action.NEW);
		Date currentDate = new Date();
		Date dateOfActivation = minusDays(currentDate, 1);
		order.setDateActivated(dateOfActivation);
		order.setAutoExpireDate(plusDays(currentDate, 2));
		ServiceRequestStatusTranslatorImpl serviceRequestStatusTranslator = new ServiceRequestStatusTranslatorImpl();
		ServiceRequest.ServiceRequestStatus requestStatus = serviceRequestStatusTranslator.toFhirResource(order);
		Assert.assertEquals(ServiceRequest.ServiceRequestStatus.ACTIVE, requestStatus);
	}
	
	/**
	 * order: urgency ON_SCHEDULED_DATE, action new, date of activation = yesterday, expiry date in
	 * future
	 */
	@Test
	public void shouldValidateScheduledOrderStatusAsActiveWhenDateOfActivationIsYesterdayAndStopDateIsFuture() {
		Order order = new Order();
		order.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		order.setAction(Order.Action.NEW);
		Date currentDate = new Date();
		Date scheduledDate = minusDays(currentDate, 1);
		order.setScheduledDate(scheduledDate);
		order.setAutoExpireDate(plusDays(currentDate, 2));
		ServiceRequestStatusTranslatorImpl serviceRequestStatusTranslator = new ServiceRequestStatusTranslatorImpl();
		ServiceRequest.ServiceRequestStatus requestStatus = serviceRequestStatusTranslator.toFhirResource(order);
		Assert.assertEquals(ServiceRequest.ServiceRequestStatus.ACTIVE, requestStatus);
	}
	
	/**
	 * order: urgency routine, action new, date of activation in past, expiry date in past
	 */
	@Test
	public void shouldValidateOrderStatusAsCompletedWhenDateOfActivationAndStopDateInPast() {
		Order order = new Order();
		order.setUrgency(Order.Urgency.ROUTINE);
		order.setAction(Order.Action.NEW);
		Date currentDate = new Date();
		Date dateOfActivation = minusDays(currentDate, 5);
		order.setDateActivated(dateOfActivation);
		order.setAutoExpireDate(minusDays(currentDate, 2));
		ServiceRequestStatusTranslatorImpl serviceRequestStatusTranslator = new ServiceRequestStatusTranslatorImpl();
		ServiceRequest.ServiceRequestStatus requestStatus = serviceRequestStatusTranslator.toFhirResource(order);
		Assert.assertEquals(ServiceRequest.ServiceRequestStatus.COMPLETED, requestStatus);
	}
	
	/**
	 * order: urgency ON_SCHEDULED_DATE, action new, date of activation in past, expiry date in past
	 */
	@Test
	public void shouldValidateScheduledOrderStatusAsCompletedWhenDateOfActivationAndStopDateInPast() {
		Order order = new Order();
		order.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		order.setAction(Order.Action.NEW);
		Date currentDate = new Date();
		Date scheduledDate = minusDays(currentDate, 5);
		order.setScheduledDate(scheduledDate);
		order.setAutoExpireDate(minusDays(currentDate, 2));
		ServiceRequestStatusTranslatorImpl serviceRequestStatusTranslator = new ServiceRequestStatusTranslatorImpl();
		ServiceRequest.ServiceRequestStatus requestStatus = serviceRequestStatusTranslator.toFhirResource(order);
		Assert.assertEquals(ServiceRequest.ServiceRequestStatus.COMPLETED, requestStatus);
	}
	
	/**
	 * order: urgency routine, action new, date of activation = today, expiry date undefined
	 */
	@Test
	public void shouldValidateOrderStatusAsActiveWhenDateOfActivationIsTodayAndAutoExpireDateIsNotSet() {
		Order order = new Order();
		order.setUrgency(Order.Urgency.ROUTINE);
		order.setAction(Order.Action.NEW);
		Date currentDate = new Date();
		order.setDateActivated(currentDate);
		ServiceRequestStatusTranslatorImpl serviceRequestStatusTranslator = new ServiceRequestStatusTranslatorImpl();
		ServiceRequest.ServiceRequestStatus requestStatus = serviceRequestStatusTranslator.toFhirResource(order);
		Assert.assertEquals(ServiceRequest.ServiceRequestStatus.ACTIVE, requestStatus);
	}
	
	/**
	 * order: urgency routine, action new, date of activation = today, expiry date in future
	 */
	@Test
	public void shouldValidateOrderStatusAsActiveWhenDateOfActivationIsFutureAndAutoExpireDateIsNotSet() {
		Order order = new Order();
		order.setUrgency(Order.Urgency.ROUTINE);
		order.setAction(Order.Action.NEW);
		Date currentDate = new Date();
		Instant instant = currentDate.toInstant();
		Instant resultInstant = instant.plus(1, ChronoUnit.DAYS);
		Date dateOfActivation = Date.from(resultInstant);
		order.setDateActivated(dateOfActivation);
		ServiceRequestStatusTranslatorImpl serviceRequestStatusTranslator = new ServiceRequestStatusTranslatorImpl();
		ServiceRequest.ServiceRequestStatus requestStatus = serviceRequestStatusTranslator.toFhirResource(order);
		Assert.assertEquals(ServiceRequest.ServiceRequestStatus.ACTIVE, requestStatus);
	}
	
}
