package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.translator.OrderTypeTranslator;
import org.hl7.fhir.r4.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.*;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.impl.OrderIdentifierTranslatorImpl;
import org.openmrs.order.OrderUtilTest;

import java.lang.reflect.Field;
import java.util.*;

import static org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants.ORDER_TYPE_SYSTEM_URI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniServiceRequestTranslatorImplTest {
	
	public static final String ORDER_TYPE_UUID = "52a447d3-a64a-11e3-9aeb-50e549534c5e";
	
	public static final String ORDER_TYPE_NAME = "Lab Order";
	
	private static final String SERVICE_REQUEST_UUID = "4e4851c3-c265-400e-acc9-1f1b0ac7f9c4";
	
	private static final String DISCONTINUED_ORDER_UUID = "efca4077-493c-496b-8312-856ee5d1cc27";
	
	private static final String ORDER_NUMBER = "ORD-1";
	
	private static final String DISCONTINUED_ORDER_NUMBER = "ORD-2";
	
	private static final String PRIOR_SERVICE_REQUEST_REFERENCE = FhirConstants.SERVICE_REQUEST + "/" + SERVICE_REQUEST_UUID;
	
	private static final String LOINC_SYSTEM_URL = "http://loinc.org";
	
	private static final String LOINC_CODE = "1000-1";
	
	private static final String PATIENT_UUID = "14d4f066-15f5-102d-96e4-000c29c2a5d7";
	
	private static final String ENCOUNTER_UUID = "y403fafb-e5e4-42d0-9d11-4f52e89d123r";
	
	private static final String PRACTITIONER_UUID = "b156e76e-b87a-4458-964c-a48e64a20fbb";
	
	private BahmniServiceRequestTranslatorImpl translator;
	
	@Mock
	private ConceptTranslator conceptTranslator;
	
	@Mock
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Mock
	private EncounterReferenceTranslator<Encounter> encounterReferenceTranslator;
	
	@Mock
	private PractitionerReferenceTranslator<Provider> practitionerReferenceTranslator;
	
	@Mock
	private OrderTypeTranslator orderTypeTranslator;
	
	private Order discontinuedOrder;
	
	@Before
	public void setup() throws Exception {
		translator = new BahmniServiceRequestTranslatorImpl();
		translator.setConceptTranslator(conceptTranslator);
		translator.setPatientReferenceTranslator(patientReferenceTranslator);
		translator.setEncounterReferenceTranslator(encounterReferenceTranslator);
		translator.setProviderReferenceTranslator(practitionerReferenceTranslator);
		translator.setOrderIdentifierTranslator(new OrderIdentifierTranslatorImpl());
		translator.setOrderTypeTranslator(orderTypeTranslator);
		
		Order order = new Order();
		order.setUuid(SERVICE_REQUEST_UUID);
		setOrderNumberByReflection(order, ORDER_NUMBER);
		
		OrderType ordertype = new OrderType();
		ordertype.setUuid(ORDER_TYPE_UUID);
		order.setOrderType(ordertype);
		
		discontinuedOrder = new Order();
		discontinuedOrder.setUuid(DISCONTINUED_ORDER_UUID);
		setOrderNumberByReflection(discontinuedOrder, DISCONTINUED_ORDER_NUMBER);
		discontinuedOrder.setPreviousOrder(order);
	}
	
	@Test
	public void toFhirResource_shouldTranslateToFhirResourceWithReplacesFieldGivenDiscontinuedOrder() {
		discontinuedOrder.setAction(Order.Action.DISCONTINUE);
		
		ServiceRequest result = translator.toFhirResource(discontinuedOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getId(), notNullValue());
		assertThat(result.getId(), equalTo(DISCONTINUED_ORDER_UUID));
		assertThat(result.getReplaces().get(0).getReference(), equalTo(PRIOR_SERVICE_REQUEST_REFERENCE));
		assertThat(result.getReplaces().get(0).getIdentifier().getValue(), equalTo(ORDER_NUMBER));
	}
	
	@Test
	public void toFhirResource_shouldTranslateToFhirResourceWithReplacesFieldGivenRevisedOrder() {
		discontinuedOrder.setAction(Order.Action.REVISE);
		
		ServiceRequest result = translator.toFhirResource(discontinuedOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getId(), notNullValue());
		assertThat(result.getId(), equalTo(DISCONTINUED_ORDER_UUID));
		assertThat(result.getReplaces().get(0).getReference(), equalTo(PRIOR_SERVICE_REQUEST_REFERENCE));
		assertThat(result.getReplaces().get(0).getIdentifier().getValue(), equalTo(ORDER_NUMBER));
	}
	
	@Test
	public void toFhirResource_shouldTranslateToFhirResourceWithBasedOnFieldGivenRenewedOrder() {
		discontinuedOrder.setAction(Order.Action.RENEW);
		
		ServiceRequest result = translator.toFhirResource(discontinuedOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getId(), notNullValue());
		assertThat(result.getId(), equalTo(DISCONTINUED_ORDER_UUID));
		assertThat(result.getBasedOn().get(0).getReference(), equalTo(PRIOR_SERVICE_REQUEST_REFERENCE));
		assertThat(result.getBasedOn().get(0).getIdentifier().getValue(), equalTo(ORDER_NUMBER));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOpenmrsOrderToFhirServiceRequest() {
		Order order = new Order();
		
		ServiceRequest result = translator.toFhirResource(order);
		
		assertThat(result, notNullValue());
		assertThat(result.getIntent(), equalTo(ServiceRequest.ServiceRequestIntent.ORDER));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOrderFromOnlyDateActivatedToActiveServiceRequest() {
		Order newOrder = new Order();
		
		Calendar activationDate = Calendar.getInstance();
		activationDate.set(2000, Calendar.APRIL, 16);
		newOrder.setDateActivated(activationDate.getTime());
		
		ServiceRequest result = translator.toFhirResource(newOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOrderFromAutoExpireToCompleteServiceRequest() throws Exception {
		Order newOrder = new Order();
		
		Calendar date = Calendar.getInstance();
		date.set(2000, Calendar.APRIL, 16);
		newOrder.setDateActivated(date.getTime());
		date.set(2070, Calendar.APRIL, 16);
		newOrder.setAutoExpireDate(date.getTime());
		date.set(2010, Calendar.APRIL, 16);
		OrderUtilTest.setDateStopped(newOrder, date.getTime());
		
		ServiceRequest result = translator.toFhirResource(newOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.COMPLETED));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOrderToActiveServiceRequest() throws Exception {
		Order newOrder = new Order();
		
		Calendar date = Calendar.getInstance();
		date.set(2000, Calendar.APRIL, 16);
		newOrder.setDateActivated(date.getTime());
		date.set(2070, Calendar.APRIL, 16);
		newOrder.setAutoExpireDate(date.getTime());
		date.set(2069, Calendar.APRIL, 16);
		OrderUtilTest.setDateStopped(newOrder, date.getTime());
		
		ServiceRequest result = translator.toFhirResource(newOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOrderToCompletedServiceRequest() throws Exception {
		Order newOrder = new Order();
		
		Calendar date = Calendar.getInstance();
		date.set(2000, Calendar.APRIL, 16);
		newOrder.setDateActivated(date.getTime());
		date.set(2011, Calendar.APRIL, 16);
		newOrder.setAutoExpireDate(date.getTime());
		date.set(2010, Calendar.APRIL, 16);
		OrderUtilTest.setDateStopped(newOrder, date.getTime());
		
		ServiceRequest result = translator.toFhirResource(newOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.COMPLETED));
	}
	
	@Test
	public void toFhirResource_shouldTranslateWrongOrderFromActiveToUnknownServiceRequest() throws Exception {
		Order newOrder = new Order();
		
		Calendar date = Calendar.getInstance();
		date.set(2000, Calendar.APRIL, 16);
		newOrder.setDateActivated(date.getTime());
		date.set(2015, Calendar.APRIL, 16);
		newOrder.setAutoExpireDate(date.getTime());
		date.set(2010, Calendar.APRIL, 16);
		newOrder.setAction(Order.Action.DISCONTINUE);
		OrderUtilTest.setDateStopped(newOrder, date.getTime());
		
		ServiceRequest result = translator.toFhirResource(newOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.UNKNOWN));
	}
	
	@Test
	public void toFhirResource_shouldTranslateWrongOrderFromCompleteToUnknownServiceRequest() throws Exception {
		Order newOrder = new Order();
		
		Calendar date = Calendar.getInstance();
		date.set(2000, Calendar.APRIL, 16);
		newOrder.setDateActivated(date.getTime());
		date.set(2070, Calendar.APRIL, 16);
		newOrder.setAutoExpireDate(date.getTime());
		date.set(2069, Calendar.APRIL, 16);
		newOrder.setAction(Order.Action.DISCONTINUE);
		OrderUtilTest.setDateStopped(newOrder, date.getTime());
		
		ServiceRequest result = translator.toFhirResource(newOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.REVOKED));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOrderFromOnlyAutoExpireToCompleteServiceRequest() throws Exception {
		Order newOrder = new Order();
		
		Calendar date = Calendar.getInstance();
		date.set(2000, Calendar.APRIL, 16);
		newOrder.setDateActivated(date.getTime());
		date.set(2015, Calendar.APRIL, 16);
		newOrder.setAutoExpireDate(date.getTime());
		
		ServiceRequest result = translator.toFhirResource(newOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.COMPLETED));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOrderFromOnlyDateStoppedToCompleteServiceRequest() throws Exception {
		Order newOrder = new Order();
		
		Calendar date = Calendar.getInstance();
		date.set(2000, Calendar.APRIL, 16);
		newOrder.setDateActivated(date.getTime());
		date.set(2015, Calendar.APRIL, 16);
		OrderUtilTest.setDateStopped(newOrder, date.getTime());
		
		ServiceRequest result = translator.toFhirResource(newOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.COMPLETED));
	}
	
	@Test
	public void toFhirResource_shouldTranslateFromNoDataToActiveServiceRequest() {
		Order newOrder = new Order();
		
		ServiceRequest result = translator.toFhirResource(newOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldTranslateCode() {
		Concept openmrsConcept = new Concept();
		Order testOrder = new Order();
		
		testOrder.setConcept(openmrsConcept);
		
		CodeableConcept codeableConcept = new CodeableConcept();
		Coding loincCoding = codeableConcept.addCoding();
		loincCoding.setSystem(LOINC_SYSTEM_URL);
		loincCoding.setCode(LOINC_CODE);
		
		when(conceptTranslator.toFhirResource(openmrsConcept)).thenReturn(codeableConcept);
		
		CodeableConcept result = translator.toFhirResource(testOrder).getCode();
		
		assertThat(result, notNullValue());
		assertThat(result.getCoding(), notNullValue());
		assertThat(result.getCoding(), hasItem(hasProperty("system", equalTo(LOINC_SYSTEM_URL))));
		assertThat(result.getCoding(), hasItem(hasProperty("code", equalTo(LOINC_CODE))));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOccurrence() {
		Order testOrder = new Order();
		Date fromDate = new Date();
		Date toDate = new Date();
		
		testOrder.setDateActivated(fromDate);
		testOrder.setAutoExpireDate(toDate);
		
		Period result = translator.toFhirResource(testOrder).getOccurrencePeriod();
		
		assertThat(result, notNullValue());
		assertThat(result.getStart(), equalTo(fromDate));
		assertThat(result.getEnd(), equalTo(toDate));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOccurrenceWithMissingEffectiveStart() {
		Order testOrder = new Order();
		Date toDate = new Date();
		
		testOrder.setAutoExpireDate(toDate);
		
		Period result = translator.toFhirResource(testOrder).getOccurrencePeriod();
		
		assertThat(result, notNullValue());
		assertThat(result.getStart(), nullValue());
		assertThat(result.getEnd(), equalTo(toDate));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOccurrenceWithMissingEffectiveEnd() {
		Order testOrder = new Order();
		Date fromDate = new Date();
		
		testOrder.setDateActivated(fromDate);
		
		Period result = translator.toFhirResource(testOrder).getOccurrencePeriod();
		
		assertThat(result, notNullValue());
		assertThat(result.getStart(), equalTo(fromDate));
		assertThat(result.getEnd(), nullValue());
	}
	
	@Test
	public void toFhirResource_shouldTranslateOccurrenceFromScheduled() {
		Order testOrder = new Order();
		Date fromDate = new Date();
		Date toDate = new Date();
		
		testOrder.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		testOrder.setScheduledDate(fromDate);
		testOrder.setAutoExpireDate(toDate);
		
		Period result = translator.toFhirResource(testOrder).getOccurrencePeriod();
		
		assertThat(result, notNullValue());
		assertThat(result.getStart(), equalTo(fromDate));
		assertThat(result.getEnd(), equalTo(toDate));
	}
	
	@Test
	public void toFhirResource_shouldTranslateSubject() {
		Order order = new Order();
		Patient subject = new Patient();
		Reference subjectReference = new Reference();
		
		subject.setUuid(PATIENT_UUID);
		order.setUuid(SERVICE_REQUEST_UUID);
		order.setPatient(subject);
		subjectReference.setType(FhirConstants.PATIENT).setReference(FhirConstants.PATIENT + "/" + PATIENT_UUID);
		when(patientReferenceTranslator.toFhirResource(subject)).thenReturn(subjectReference);
		
		Reference result = translator.toFhirResource(order).getSubject();
		
		assertThat(result, notNullValue());
		assertThat(result.getReference(), containsString(PATIENT_UUID));
	}
	
	@Test
	public void toFhirResource_shouldTranslateEncounter() {
		Order order = new Order();
		Encounter encounter = new Encounter();
		Reference encounterReference = new Reference();
		
		encounter.setUuid(ENCOUNTER_UUID);
		order.setUuid(SERVICE_REQUEST_UUID);
		order.setEncounter(encounter);
		encounterReference.setType(FhirConstants.ENCOUNTER).setReference(FhirConstants.ENCOUNTER + "/" + ENCOUNTER_UUID);
		when(encounterReferenceTranslator.toFhirResource(encounter)).thenReturn(encounterReference);
		
		Reference result = translator.toFhirResource(order).getEncounter();
		
		assertThat(result, notNullValue());
		assertThat(result.getReference(), containsString(ENCOUNTER_UUID));
	}
	
	@Test
	public void toFhirResource_shouldTranslateRequester() {
		
		Order order = new Order();
		Provider requester = new Provider();
		Reference requesterReference = new Reference();
		
		requester.setUuid(PRACTITIONER_UUID);
		order.setUuid(SERVICE_REQUEST_UUID);
		order.setOrderer(requester);
		requesterReference.setType(FhirConstants.PRACTITIONER).setReference(
		    FhirConstants.PRACTITIONER + "/" + PRACTITIONER_UUID);
		when(practitionerReferenceTranslator.toFhirResource(requester)).thenReturn(requesterReference);
		
		Reference result = translator.toFhirResource(order).getRequester();
		
		assertThat(result, notNullValue());
		assertThat(result.getReference(), containsString(PRACTITIONER_UUID));
	}
	
	@Test
	public void shouldTranslateOpenMrsDateChangedToLastUpdatedDate() {
		Order order = new Order();
		order.setDateChanged(new Date());
		
		ServiceRequest result = translator.toFhirResource(order);
		assertThat(result, notNullValue());
		//assertThat(result.getMeta().getLastUpdated(), DateMatchers.sameDay(new Date()));
	}
	
	@Test
	public void shouldTranslateOpenMrsDateChangedToVersionId() {
		org.openmrs.Order order = new org.openmrs.Order();
		order.setDateChanged(new Date());
		
		org.hl7.fhir.r4.model.ServiceRequest result = translator.toFhirResource(order);
		
		assertThat(result, notNullValue());
		assertThat(result.getMeta().getVersionId(), notNullValue());
	}
	
	@Test
	public void shouldTranslateOpenMrsOrderTypeToCategory() {
		OrderType ordertype = new OrderType();
		ordertype.setUuid(ORDER_TYPE_UUID);
		ordertype.setName(ORDER_TYPE_NAME);
		Order order = new Order();
		order.setOrderType(ordertype);
		order.setOrderType(ordertype);
		
		CodeableConcept codeableConcept = new CodeableConcept();
		Coding coding = new Coding();
		coding.setSystem(ORDER_TYPE_SYSTEM_URI);
		coding.setCode(ORDER_TYPE_UUID);
		coding.setDisplay(ORDER_TYPE_NAME);
		codeableConcept.addCoding(coding);
		
		when(orderTypeTranslator.toFhirResource(ordertype)).thenReturn(codeableConcept);
		
		org.hl7.fhir.r4.model.ServiceRequest result = translator.toFhirResource(order);
		
		assertThat(result, notNullValue());
		assertThat(result.getCategory(), notNullValue());
		assertThat(result.getCategory(), equalTo(Collections.singletonList(codeableConcept)));
	}
	
	@Test
	public void toFhirResource_shouldTranslateRoutineUrgencyToRoutinePriority() {
		Order testOrder = new Order();
		testOrder.setUrgency(Order.Urgency.ROUTINE);
		
		ServiceRequest result = translator.toFhirResource(testOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getPriority(), equalTo(ServiceRequest.ServiceRequestPriority.ROUTINE));
	}
	
	@Test
	public void toFhirResource_shouldTranslateStatUrgencyToStatPriority() {
		Order testOrder = new Order();
		testOrder.setUrgency(Order.Urgency.STAT);
		
		ServiceRequest result = translator.toFhirResource(testOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getPriority(), equalTo(ServiceRequest.ServiceRequestPriority.STAT));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOnScheduledDateUrgencyToRoutinePriority() {
		Order testOrder = new Order();
		testOrder.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		
		ServiceRequest result = translator.toFhirResource(testOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getPriority(), equalTo(ServiceRequest.ServiceRequestPriority.ROUTINE));
	}
	
	@Test
	public void toFhirResource_shouldTranslateNullUrgencyToRoutinePriority() {
		Order testOrder = new Order();
		testOrder.setUrgency(null);
		
		ServiceRequest result = translator.toFhirResource(testOrder);
		
		assertThat(result, notNullValue());
		assertThat(result.getPriority(), equalTo(ServiceRequest.ServiceRequestPriority.ROUTINE));
	}
	
	private void setOrderNumberByReflection(Order order, String orderNumber) throws Exception {
		Class<? extends Order> clazz = order.getClass();
		Field orderNumberField = clazz.getDeclaredField("orderNumber");
		boolean isAccessible = orderNumberField.isAccessible();
		if (!isAccessible) {
			orderNumberField.setAccessible(true);
		}
		orderNumberField.set(((Order) order), orderNumber);
	}
}
