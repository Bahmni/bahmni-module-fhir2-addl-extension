package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2AddlExtension.api.context.AppContext;
import org.bahmni.module.fhir2AddlExtension.api.dao.OrderAttributeTypeDao;
import org.bahmni.module.fhir2AddlExtension.api.service.impl.ServiceRequestLocationReferenceResolverImpl;
import org.bahmni.module.fhir2AddlExtension.api.translator.OrderTypeTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.ServiceRequestPriorityTranslator;
import org.bahmni.module.fhir2AddlExtension.api.validators.ServiceRequestValidator;
import org.hl7.fhir.r4.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.*;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.LocationReferenceTranslator;
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
import static org.mockito.Mockito.*;

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
	
	@Mock
	private ServiceRequestPriorityTranslator serviceRequestPriorityTranslator;
	
	@Mock
	private ServiceRequestValidator serviceRequestValidator;
	
	@Mock
	private OrderService orderService;
	
	@Mock
	OrderAttributeTypeDao orderAttributeTypeDao;
	
	@Mock
	LocationReferenceTranslator locationReferenceTranslator;
	
	@Mock
	AppContext appContext;
	
	private Order discontinuedOrder;
	
	private Order order;
	
	private Concept orderConcept;
	
	private ServiceRequest serviceRequest;
	
	private Concept testConcept;
	
	private Patient testPatient;
	
	private Encounter testEncounter;
	
	private Provider testProvider;
	
	private CareSetting testCareSetting;
	
	private OrderType testOrderType;
	
	@Before
	public void setup() throws Exception {
		translator = new BahmniServiceRequestTranslatorImpl();
		translator.setConceptTranslator(conceptTranslator);
		translator.setPatientReferenceTranslator(patientReferenceTranslator);
		translator.setEncounterReferenceTranslator(encounterReferenceTranslator);
		translator.setProviderReferenceTranslator(practitionerReferenceTranslator);
		translator.setOrderIdentifierTranslator(new OrderIdentifierTranslatorImpl());
		translator.setOrderTypeTranslator(orderTypeTranslator);
		translator.setServiceRequestPriorityTranslator(serviceRequestPriorityTranslator);
		translator.setServiceRequestValidator(serviceRequestValidator);
		translator.setOrderService(orderService);
		
		ServiceRequestLocationReferenceResolverImpl orderLocationReferenceResolver = new ServiceRequestLocationReferenceResolverImpl(
		        locationReferenceTranslator, orderAttributeTypeDao, appContext);
		
		translator.setLocationReferenceResolver(orderLocationReferenceResolver);
		
		orderConcept = new Concept();
		ConceptClass cc = new ConceptClass();
		cc.setName("Test");
		orderConcept.setConceptClass(cc);
		
		order = new Order();
		order.setUuid(SERVICE_REQUEST_UUID);
		order.setConcept(orderConcept);
		setOrderNumberByReflection(order, ORDER_NUMBER);
		
		OrderType ordertype = new OrderType();
		ordertype.setUuid(ORDER_TYPE_UUID);
		order.setOrderType(ordertype);
		
		discontinuedOrder = new Order();
		discontinuedOrder.setUuid(DISCONTINUED_ORDER_UUID);
		discontinuedOrder.setConcept(orderConcept);
		setOrderNumberByReflection(discontinuedOrder, DISCONTINUED_ORDER_NUMBER);
		discontinuedOrder.setPreviousOrder(order);
		
		// Setup test data for toOpenmrsType tests
		setupToOpenmrsTypeTestData();
	}
	
	private void setupToOpenmrsTypeTestData() {
		// Create test ServiceRequest
		serviceRequest = new ServiceRequest();
		serviceRequest.setId("test-service-request-id");
		serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);
		serviceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
		serviceRequest.setPriority(ServiceRequest.ServiceRequestPriority.ROUTINE);
		
		// Create test code
		CodeableConcept code = new CodeableConcept();
		Coding coding = new Coding();
		coding.setSystem("http://loinc.org");
		coding.setCode("12345-6");
		coding.setDisplay("Test Lab Order");
		code.addCoding(coding);
		serviceRequest.setCode(code);
		
		// Create test subject reference
		Reference subjectRef = new Reference();
		subjectRef.setReference("Patient/test-patient-uuid");
		serviceRequest.setSubject(subjectRef);
		
		// Create test encounter reference
		Reference encounterRef = new Reference();
		encounterRef.setReference("Encounter/test-encounter-uuid");
		serviceRequest.setEncounter(encounterRef);
		
		// Create test requester reference
		Reference requesterRef = new Reference();
		requesterRef.setReference("Practitioner/test-provider-uuid");
		serviceRequest.setRequester(requesterRef);
		
		// Create test OpenMRS objects
		testConcept = new Concept();
		testConcept.setUuid("test-concept-uuid");
		ConceptClass conceptClass = new ConceptClass();
		conceptClass.setName("Test");
		testConcept.setConceptClass(conceptClass);
		
		testPatient = new Patient();
		testPatient.setUuid("test-patient-uuid");
		
		testEncounter = new Encounter();
		testEncounter.setUuid("test-encounter-uuid");
		
		testProvider = new Provider();
		testProvider.setUuid("test-provider-uuid");
		
		testCareSetting = new CareSetting();
		testCareSetting.setUuid("test-care-setting-uuid");
		testCareSetting.setCareSettingType(CareSetting.CareSettingType.OUTPATIENT);
		
		testOrderType = new OrderType();
		testOrderType.setUuid("test-order-type-uuid");
		testOrderType.setName("Lab Order");
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
		
		ServiceRequest result = translator.toFhirResource(order);
		
		assertThat(result, notNullValue());
		assertThat(result.getIntent(), equalTo(ServiceRequest.ServiceRequestIntent.ORDER));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOrderFromOnlyDateActivatedToActiveServiceRequest() {
		
		Calendar activationDate = Calendar.getInstance();
		activationDate.set(2000, Calendar.APRIL, 16);
		order.setDateActivated(activationDate.getTime());
		
		ServiceRequest result = translator.toFhirResource(order);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOrderFromAutoExpireToCompleteServiceRequest() throws Exception {
		
		Calendar date = Calendar.getInstance();
		date.set(2000, Calendar.APRIL, 16);
		order.setDateActivated(date.getTime());
		date.set(2070, Calendar.APRIL, 16);
		order.setAutoExpireDate(date.getTime());
		date.set(2010, Calendar.APRIL, 16);
		OrderUtilTest.setDateStopped(order, date.getTime());
		
		ServiceRequest result = translator.toFhirResource(order);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.COMPLETED));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOrderToActiveServiceRequest() throws Exception {
		
		Calendar date = Calendar.getInstance();
		date.set(2000, Calendar.APRIL, 16);
		order.setDateActivated(date.getTime());
		date.set(2070, Calendar.APRIL, 16);
		order.setAutoExpireDate(date.getTime());
		date.set(2069, Calendar.APRIL, 16);
		OrderUtilTest.setDateStopped(order, date.getTime());
		
		ServiceRequest result = translator.toFhirResource(order);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOrderToCompletedServiceRequest() throws Exception {
		
		Calendar date = Calendar.getInstance();
		date.set(2000, Calendar.APRIL, 16);
		order.setDateActivated(date.getTime());
		date.set(2011, Calendar.APRIL, 16);
		order.setAutoExpireDate(date.getTime());
		date.set(2010, Calendar.APRIL, 16);
		OrderUtilTest.setDateStopped(order, date.getTime());
		
		ServiceRequest result = translator.toFhirResource(order);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.COMPLETED));
	}
	
	@Test
	public void toFhirResource_shouldTranslateWrongOrderFromActiveToUnknownServiceRequest() throws Exception {
		
		Calendar date = Calendar.getInstance();
		date.set(2000, Calendar.APRIL, 16);
		order.setDateActivated(date.getTime());
		date.set(2015, Calendar.APRIL, 16);
		order.setAutoExpireDate(date.getTime());
		date.set(2010, Calendar.APRIL, 16);
		order.setAction(Order.Action.DISCONTINUE);
		OrderUtilTest.setDateStopped(order, date.getTime());
		
		ServiceRequest result = translator.toFhirResource(order);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.UNKNOWN));
	}
	
	@Test
	public void toFhirResource_shouldTranslateWrongOrderFromCompleteToUnknownServiceRequest() throws Exception {
		
		Calendar date = Calendar.getInstance();
		date.set(2000, Calendar.APRIL, 16);
		order.setDateActivated(date.getTime());
		date.set(2070, Calendar.APRIL, 16);
		order.setAutoExpireDate(date.getTime());
		date.set(2069, Calendar.APRIL, 16);
		order.setAction(Order.Action.DISCONTINUE);
		OrderUtilTest.setDateStopped(order, date.getTime());
		
		ServiceRequest result = translator.toFhirResource(order);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.REVOKED));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOrderFromOnlyAutoExpireToCompleteServiceRequest() throws Exception {
		
		Calendar date = Calendar.getInstance();
		date.set(2000, Calendar.APRIL, 16);
		order.setDateActivated(date.getTime());
		date.set(2015, Calendar.APRIL, 16);
		order.setAutoExpireDate(date.getTime());
		
		ServiceRequest result = translator.toFhirResource(order);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.COMPLETED));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOrderFromOnlyDateStoppedToCompleteServiceRequest() throws Exception {
		
		Calendar date = Calendar.getInstance();
		date.set(2000, Calendar.APRIL, 16);
		order.setDateActivated(date.getTime());
		date.set(2015, Calendar.APRIL, 16);
		OrderUtilTest.setDateStopped(order, date.getTime());
		
		ServiceRequest result = translator.toFhirResource(order);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.COMPLETED));
	}
	
	@Test
	public void toFhirResource_shouldTranslateFromNoDataToActiveServiceRequest() {
		
		ServiceRequest result = translator.toFhirResource(order);
		
		assertThat(result, notNullValue());
		assertThat(result.getStatus(), equalTo(ServiceRequest.ServiceRequestStatus.ACTIVE));
	}
	
	@Test
	public void toFhirResource_shouldTranslateCode() {
		Concept openmrsConcept = new Concept();
		ConceptClass cc = new ConceptClass();
		cc.setName("Test");
		openmrsConcept.setConceptClass(cc);
		
		order.setConcept(openmrsConcept);
		
		CodeableConcept codeableConcept = new CodeableConcept();
		
		Coding loincCoding = codeableConcept.addCoding();
		loincCoding.setSystem(LOINC_SYSTEM_URL);
		loincCoding.setCode(LOINC_CODE);
		
		when(conceptTranslator.toFhirResource(openmrsConcept)).thenReturn(codeableConcept);
		
		CodeableConcept result = translator.toFhirResource(order).getCode();
		
		assertThat(result, notNullValue());
		assertThat(result.getCoding(), notNullValue());
		assertThat(result.getCoding(), hasItem(hasProperty("system", equalTo(LOINC_SYSTEM_URL))));
		assertThat(result.getCoding(), hasItem(hasProperty("code", equalTo(LOINC_CODE))));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOccurrence() {
		Date fromDate = new Date();
		Date toDate = new Date();
		
		order.setDateActivated(fromDate);
		order.setAutoExpireDate(toDate);
		
		Period result = translator.toFhirResource(order).getOccurrencePeriod();
		
		assertThat(result, notNullValue());
		assertThat(result.getStart(), equalTo(fromDate));
		assertThat(result.getEnd(), equalTo(toDate));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOccurrenceWithMissingEffectiveStart() {
		Date toDate = new Date();
		
		order.setAutoExpireDate(toDate);
		
		Period result = translator.toFhirResource(order).getOccurrencePeriod();
		
		assertThat(result, notNullValue());
		assertThat(result.getStart(), nullValue());
		assertThat(result.getEnd(), equalTo(toDate));
	}
	
	@Test
	public void toFhirResource_shouldTranslateOccurrenceWithMissingEffectiveEnd() {
		Date fromDate = new Date();
		
		order.setDateActivated(fromDate);
		
		Period result = translator.toFhirResource(order).getOccurrencePeriod();
		
		assertThat(result, notNullValue());
		assertThat(result.getStart(), equalTo(fromDate));
		assertThat(result.getEnd(), nullValue());
	}
	
	@Test
	public void toFhirResource_shouldTranslateOccurrenceFromScheduled() {
		Date fromDate = new Date();
		Date toDate = new Date();
		
		order.setUrgency(Order.Urgency.ON_SCHEDULED_DATE);
		order.setScheduledDate(fromDate);
		order.setAutoExpireDate(toDate);
		
		Period result = translator.toFhirResource(order).getOccurrencePeriod();
		
		assertThat(result, notNullValue());
		assertThat(result.getStart(), equalTo(fromDate));
		assertThat(result.getEnd(), equalTo(toDate));
	}
	
	@Test
	public void toFhirResource_shouldTranslateSubject() {
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
		order.setDateChanged(new Date());
		
		ServiceRequest result = translator.toFhirResource(order);
		assertThat(result, notNullValue());
		//assertThat(result.getMeta().getLastUpdated(), DateMatchers.sameDay(new Date()));
	}
	
	@Test
	public void shouldTranslateOpenMrsDateChangedToVersionId() {
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
	public void toFhirResource_shouldSetPriorityUsingPriorityTranslator() {
		order.setUrgency(Order.Urgency.STAT);
		
		when(serviceRequestPriorityTranslator.toFhirResource(Order.Urgency.STAT)).thenReturn(
		    ServiceRequest.ServiceRequestPriority.STAT);
		
		ServiceRequest result = translator.toFhirResource(order);
		
		assertThat(result, notNullValue());
		assertThat(result.getPriority(), equalTo(ServiceRequest.ServiceRequestPriority.STAT));
		verify(serviceRequestPriorityTranslator).toFhirResource(Order.Urgency.STAT);
	}
	
	@Test
	public void toFhirResource_shouldAddLabTestConceptTypeExtensionWhenConceptClassIsLabTest() {
		Concept concept = new Concept();
		ConceptClass conceptClass = new ConceptClass();
		conceptClass.setName("LabTest");
		concept.setConceptClass(conceptClass);
		order.setConcept(concept);
		
		ServiceRequest result = translator.toFhirResource(order);
		
		assertThat(result, notNullValue());
		assertThat(result.getExtension(), notNullValue());
		assertThat(result.getExtension().size(), equalTo(1));
		assertThat(result.getExtension().get(0).getUrl(), equalTo(BahmniFhirConstants.LAB_ORDER_CONCEPT_TYPE_EXTENSION_URL));
		assertThat(result.getExtension().get(0).getValue(), notNullValue());
		assertThat(((StringType) result.getExtension().get(0).getValue()).getValue(), equalTo("Test"));
	}
	
	@Test
	public void toFhirResource_shouldAddTestConceptTypeExtensionWhenConceptClassIsTest() {
		Concept concept = new Concept();
		ConceptClass conceptClass = new ConceptClass();
		conceptClass.setName("Test");
		concept.setConceptClass(conceptClass);
		order.setConcept(concept);
		
		ServiceRequest result = translator.toFhirResource(order);
		
		assertThat(result, notNullValue());
		assertThat(result.getExtension(), notNullValue());
		assertThat(result.getExtension().size(), equalTo(1));
		assertThat(result.getExtension().get(0).getUrl(), equalTo(BahmniFhirConstants.LAB_ORDER_CONCEPT_TYPE_EXTENSION_URL));
		assertThat(result.getExtension().get(0).getValue(), notNullValue());
		assertThat(((StringType) result.getExtension().get(0).getValue()).getValue(), equalTo("Test"));
	}
	
	@Test
	public void toFhirResource_shouldAddLabSetConceptTypeExtensionWhenConceptClassIsLabSet() {
		Concept concept = new Concept();
		ConceptClass conceptClass = new ConceptClass();
		conceptClass.setName("LabSet");
		concept.setConceptClass(conceptClass);
		order.setConcept(concept);
		
		ServiceRequest result = translator.toFhirResource(order);
		
		assertThat(result, notNullValue());
		assertThat(result.getExtension(), notNullValue());
		assertThat(result.getExtension().size(), equalTo(1));
		assertThat(result.getExtension().get(0).getUrl(), equalTo(BahmniFhirConstants.LAB_ORDER_CONCEPT_TYPE_EXTENSION_URL));
		assertThat(result.getExtension().get(0).getValue(), notNullValue());
		assertThat(((StringType) result.getExtension().get(0).getValue()).getValue(), equalTo("Panel"));
	}
	
	@Test
	public void toFhirResource_shouldNotAddExtensionWhenConceptClassIsNotLabTestOrTestOrLabSet() {
		Concept concept = new Concept();
		ConceptClass conceptClass = new ConceptClass();
		conceptClass.setName("Other");
		concept.setConceptClass(conceptClass);
		order.setConcept(concept);
		
		ServiceRequest result = translator.toFhirResource(order);
		
		assertThat(result, notNullValue());
		assertThat(result.getExtension(), empty());
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
	
	@Test
	public void toOpenmrsType_shouldTranslateCompleteServiceRequestToOrder() {
		// Setup mocks
		when(conceptTranslator.toOpenmrsType(serviceRequest.getCode())).thenReturn(testConcept);
		when(patientReferenceTranslator.toOpenmrsType(serviceRequest.getSubject())).thenReturn(testPatient);
		when(encounterReferenceTranslator.toOpenmrsType(serviceRequest.getEncounter())).thenReturn(testEncounter);
		when(practitionerReferenceTranslator.toOpenmrsType(serviceRequest.getRequester())).thenReturn(testProvider);
		when(serviceRequestPriorityTranslator.toOpenmrsType(serviceRequest.getPriority())).thenReturn(Order.Urgency.ROUTINE);
		when(orderService.getCareSettingByName(CareSetting.CareSettingType.OUTPATIENT.toString())).thenReturn(
		    testCareSetting);
		when(orderService.getOrderTypeByConcept(testConcept)).thenReturn(testOrderType);
		
		// Execute
		Order result = translator.toOpenmrsType(serviceRequest);
		
		// Verify result
		assertThat(result, notNullValue());
		assertThat(result.getAction(), equalTo(Order.Action.NEW));
		assertThat(result.getCareSetting(), equalTo(testCareSetting));
		assertThat(result.getOrderType(), equalTo(testOrderType));
		assertThat(result.getConcept(), equalTo(testConcept));
		assertThat(result.getPatient(), equalTo(testPatient));
		assertThat(result.getEncounter(), equalTo(testEncounter));
		assertThat(result.getOrderer(), equalTo(testProvider));
		assertThat(result.getUrgency(), equalTo(Order.Urgency.ROUTINE));
		
		// Verify mock interactions
		verify(serviceRequestValidator).validate(serviceRequest);
		verify(conceptTranslator).toOpenmrsType(serviceRequest.getCode());
		verify(patientReferenceTranslator).toOpenmrsType(serviceRequest.getSubject());
		verify(encounterReferenceTranslator).toOpenmrsType(serviceRequest.getEncounter());
		verify(practitionerReferenceTranslator).toOpenmrsType(serviceRequest.getRequester());
		verify(serviceRequestPriorityTranslator).toOpenmrsType(serviceRequest.getPriority());
		verify(orderService).getCareSettingByName(CareSetting.CareSettingType.OUTPATIENT.toString());
		verify(orderService).getOrderTypeByConcept(testConcept);
	}
	
	@Test
	public void toOpenmrsType_shouldTranslateMinimalServiceRequestToOrder() {
		// Create minimal ServiceRequest with only required fields
		ServiceRequest minimalRequest = new ServiceRequest();
		CodeableConcept code = new CodeableConcept();
		code.addCoding().setCode("minimal-test");
		minimalRequest.setCode(code);
		
		Reference subjectRef = new Reference();
		subjectRef.setReference("Patient/minimal-patient");
		minimalRequest.setSubject(subjectRef);
		
		// Setup mocks
		when(conceptTranslator.toOpenmrsType(minimalRequest.getCode())).thenReturn(testConcept);
		when(patientReferenceTranslator.toOpenmrsType(minimalRequest.getSubject())).thenReturn(testPatient);
		when(serviceRequestPriorityTranslator.toOpenmrsType(null)).thenReturn(null);
		when(orderService.getCareSettingByName(CareSetting.CareSettingType.OUTPATIENT.toString())).thenReturn(
		    testCareSetting);
		when(orderService.getOrderTypeByConcept(testConcept)).thenReturn(testOrderType);
		
		// Execute
		Order result = translator.toOpenmrsType(minimalRequest);
		
		// Verify result
		assertThat(result, notNullValue());
		assertThat(result.getAction(), equalTo(Order.Action.NEW));
		assertThat(result.getCareSetting(), equalTo(testCareSetting));
		assertThat(result.getOrderType(), equalTo(testOrderType));
		assertThat(result.getConcept(), equalTo(testConcept));
		assertThat(result.getPatient(), equalTo(testPatient));
		assertThat(result.getEncounter(), nullValue());
		assertThat(result.getOrderer(), nullValue());
		assertThat(result.getUrgency(), nullValue());
		
		// Verify validator was called
		verify(serviceRequestValidator).validate(minimalRequest);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void toOpenmrsType_shouldThrowExceptionWhenValidationFails() {
		// Setup validator to throw exception
		doThrow(new InvalidRequestException("Invalid ServiceRequest")).when(serviceRequestValidator)
		        .validate(serviceRequest);
		
		// Execute - should throw exception
		translator.toOpenmrsType(serviceRequest);
	}
	
	@Test
	public void toOpenmrsType_shouldSetConstantsCorrectly() {
		// Setup mocks
		when(conceptTranslator.toOpenmrsType(serviceRequest.getCode())).thenReturn(testConcept);
		when(patientReferenceTranslator.toOpenmrsType(serviceRequest.getSubject())).thenReturn(testPatient);
		when(encounterReferenceTranslator.toOpenmrsType(serviceRequest.getEncounter())).thenReturn(testEncounter);
		when(practitionerReferenceTranslator.toOpenmrsType(serviceRequest.getRequester())).thenReturn(testProvider);
		when(serviceRequestPriorityTranslator.toOpenmrsType(serviceRequest.getPriority())).thenReturn(Order.Urgency.ROUTINE);
		when(orderService.getCareSettingByName(CareSetting.CareSettingType.OUTPATIENT.toString())).thenReturn(
		    testCareSetting);
		when(orderService.getOrderTypeByConcept(testConcept)).thenReturn(testOrderType);
		
		// Execute
		Order result = translator.toOpenmrsType(serviceRequest);
		
		// Verify constants are set correctly
		assertThat(result.getAction(), equalTo(Order.Action.NEW));
		assertThat(result.getCareSetting(), equalTo(testCareSetting));
		assertThat(result.getOrderType(), equalTo(testOrderType));
		
		// Verify OrderService was called with correct parameters
		verify(orderService).getCareSettingByName(CareSetting.CareSettingType.OUTPATIENT.toString());
		verify(orderService).getOrderTypeByConcept(testConcept);
	}
	
	@Test
	public void toOpenmrsType_shouldHandleNullOptionalFields() {
		// Create ServiceRequest with null optional fields
		ServiceRequest requestWithNulls = new ServiceRequest();
		requestWithNulls.setCode(serviceRequest.getCode());
		requestWithNulls.setSubject(serviceRequest.getSubject());
		requestWithNulls.setEncounter(null);
		requestWithNulls.setRequester(null);
		requestWithNulls.setPriority(null);
		
		// Setup mocks
		when(conceptTranslator.toOpenmrsType(requestWithNulls.getCode())).thenReturn(testConcept);
		when(patientReferenceTranslator.toOpenmrsType(requestWithNulls.getSubject())).thenReturn(testPatient);
		when(serviceRequestPriorityTranslator.toOpenmrsType(null)).thenReturn(null);
		when(orderService.getCareSettingByName(CareSetting.CareSettingType.OUTPATIENT.toString())).thenReturn(
		    testCareSetting);
		when(orderService.getOrderTypeByConcept(testConcept)).thenReturn(testOrderType);
		
		// Execute
		Order result = translator.toOpenmrsType(requestWithNulls);
		
		// Verify null fields are handled gracefully
		assertThat(result, notNullValue());
		assertThat(result.getEncounter(), nullValue());
		assertThat(result.getOrderer(), nullValue());
		assertThat(result.getUrgency(), nullValue());
		
		// Verify translators were called with null values
		verify(encounterReferenceTranslator).toOpenmrsType(any());
		verify(practitionerReferenceTranslator).toOpenmrsType(any());
		verify(serviceRequestPriorityTranslator).toOpenmrsType(any());
	}
	
	@Test
	public void toOpenmrsType_shouldHandleTranslatorsReturningNull() {
		// Setup translators to return null
		when(conceptTranslator.toOpenmrsType(serviceRequest.getCode())).thenReturn(null);
		when(patientReferenceTranslator.toOpenmrsType(serviceRequest.getSubject())).thenReturn(null);
		when(encounterReferenceTranslator.toOpenmrsType(serviceRequest.getEncounter())).thenReturn(null);
		when(practitionerReferenceTranslator.toOpenmrsType(serviceRequest.getRequester())).thenReturn(null);
		when(serviceRequestPriorityTranslator.toOpenmrsType(serviceRequest.getPriority())).thenReturn(null);
		when(orderService.getCareSettingByName(CareSetting.CareSettingType.OUTPATIENT.toString())).thenReturn(
		    testCareSetting);
		when(orderService.getOrderTypeByConcept(null)).thenReturn(testOrderType);
		
		// Execute
		Order result = translator.toOpenmrsType(serviceRequest);
		
		// Verify order is still created with null values
		assertThat(result, notNullValue());
		assertThat(result.getConcept(), nullValue());
		assertThat(result.getPatient(), nullValue());
		assertThat(result.getEncounter(), nullValue());
		assertThat(result.getOrderer(), nullValue());
		assertThat(result.getUrgency(), nullValue());
		
		// Verify constants are still set
		assertThat(result.getAction(), equalTo(Order.Action.NEW));
		assertThat(result.getCareSetting(), equalTo(testCareSetting));
		assertThat(result.getOrderType(), equalTo(testOrderType));
	}
	
	@Test
	public void toOpenmrsType_shouldCallPriorityTranslatorWithCorrectParameter() {
		serviceRequest.setPriority(ServiceRequest.ServiceRequestPriority.STAT);
		
		// Setup mocks
		when(conceptTranslator.toOpenmrsType(serviceRequest.getCode())).thenReturn(testConcept);
		when(patientReferenceTranslator.toOpenmrsType(serviceRequest.getSubject())).thenReturn(testPatient);
		when(encounterReferenceTranslator.toOpenmrsType(serviceRequest.getEncounter())).thenReturn(testEncounter);
		when(practitionerReferenceTranslator.toOpenmrsType(serviceRequest.getRequester())).thenReturn(testProvider);
		when(serviceRequestPriorityTranslator.toOpenmrsType(ServiceRequest.ServiceRequestPriority.STAT)).thenReturn(
		    Order.Urgency.STAT);
		when(orderService.getCareSettingByName(CareSetting.CareSettingType.OUTPATIENT.toString())).thenReturn(
		    testCareSetting);
		when(orderService.getOrderTypeByConcept(testConcept)).thenReturn(testOrderType);
		
		// Execute
		translator.toOpenmrsType(serviceRequest);
		
		// Verify priority translator is called with correct parameter
		verify(serviceRequestPriorityTranslator).toOpenmrsType(ServiceRequest.ServiceRequestPriority.STAT);
	}
	
	@Test
	public void toOpenmrsType_shouldVerifyAllTranslatorsAreCalled() {
		// Setup mocks
		when(conceptTranslator.toOpenmrsType(serviceRequest.getCode())).thenReturn(testConcept);
		when(patientReferenceTranslator.toOpenmrsType(serviceRequest.getSubject())).thenReturn(testPatient);
		when(encounterReferenceTranslator.toOpenmrsType(serviceRequest.getEncounter())).thenReturn(testEncounter);
		when(practitionerReferenceTranslator.toOpenmrsType(serviceRequest.getRequester())).thenReturn(testProvider);
		when(serviceRequestPriorityTranslator.toOpenmrsType(serviceRequest.getPriority())).thenReturn(Order.Urgency.ROUTINE);
		when(orderService.getCareSettingByName(CareSetting.CareSettingType.OUTPATIENT.toString())).thenReturn(
		    testCareSetting);
		when(orderService.getOrderTypeByConcept(testConcept)).thenReturn(testOrderType);
		
		// Execute
		translator.toOpenmrsType(serviceRequest);
		
		// Verify all translators and services are called exactly once
		verify(serviceRequestValidator, times(1)).validate(serviceRequest);
		verify(conceptTranslator, times(1)).toOpenmrsType(serviceRequest.getCode());
		verify(patientReferenceTranslator, times(1)).toOpenmrsType(serviceRequest.getSubject());
		verify(encounterReferenceTranslator, times(1)).toOpenmrsType(serviceRequest.getEncounter());
		verify(practitionerReferenceTranslator, times(1)).toOpenmrsType(serviceRequest.getRequester());
		verify(serviceRequestPriorityTranslator, times(1)).toOpenmrsType(serviceRequest.getPriority());
		verify(orderService, times(1)).getCareSettingByName(CareSetting.CareSettingType.OUTPATIENT.toString());
		verify(orderService, times(1)).getOrderTypeByConcept(testConcept);
	}
	
	@Test
	public void toOpenmrsType_shouldHandleComplexCodeableConcept() {
		// Create complex CodeableConcept with multiple codings
		CodeableConcept complexCode = new CodeableConcept();
		complexCode.addCoding().setSystem("http://loinc.org").setCode("12345-6").setDisplay("Primary Test");
		complexCode.addCoding().setSystem("http://snomed.info/sct").setCode("67890").setDisplay("Secondary Test");
		complexCode.setText("Complex Lab Test");
		
		serviceRequest.setCode(complexCode);
		
		// Setup mocks
		when(conceptTranslator.toOpenmrsType(complexCode)).thenReturn(testConcept);
		when(patientReferenceTranslator.toOpenmrsType(serviceRequest.getSubject())).thenReturn(testPatient);
		when(encounterReferenceTranslator.toOpenmrsType(serviceRequest.getEncounter())).thenReturn(testEncounter);
		when(practitionerReferenceTranslator.toOpenmrsType(serviceRequest.getRequester())).thenReturn(testProvider);
		when(serviceRequestPriorityTranslator.toOpenmrsType(serviceRequest.getPriority())).thenReturn(Order.Urgency.ROUTINE);
		when(orderService.getCareSettingByName(CareSetting.CareSettingType.OUTPATIENT.toString())).thenReturn(
		    testCareSetting);
		when(orderService.getOrderTypeByConcept(testConcept)).thenReturn(testOrderType);
		
		// Execute
		Order result = translator.toOpenmrsType(serviceRequest);
		
		// Verify concept is properly translated
		assertThat(result.getConcept(), equalTo(testConcept));
		verify(conceptTranslator).toOpenmrsType(complexCode);
	}
	
	@Test
	public void toOpenmrsType_shouldSetOrderTypeBasedOnConcept() {
		// Create different concept for different order type
		Concept labConcept = new Concept();
		labConcept.setUuid("lab-concept-uuid");
		ConceptClass labConceptClass = new ConceptClass();
		labConceptClass.setName("LabTest");
		labConcept.setConceptClass(labConceptClass);
		
		OrderType labOrderType = new OrderType();
		labOrderType.setUuid("lab-order-type-uuid");
		labOrderType.setName("Lab Order Type");
		
		// Setup mocks
		when(conceptTranslator.toOpenmrsType(serviceRequest.getCode())).thenReturn(labConcept);
		when(patientReferenceTranslator.toOpenmrsType(serviceRequest.getSubject())).thenReturn(testPatient);
		when(encounterReferenceTranslator.toOpenmrsType(serviceRequest.getEncounter())).thenReturn(testEncounter);
		when(practitionerReferenceTranslator.toOpenmrsType(serviceRequest.getRequester())).thenReturn(testProvider);
		when(serviceRequestPriorityTranslator.toOpenmrsType(serviceRequest.getPriority())).thenReturn(Order.Urgency.ROUTINE);
		when(orderService.getCareSettingByName(CareSetting.CareSettingType.OUTPATIENT.toString())).thenReturn(
		    testCareSetting);
		when(orderService.getOrderTypeByConcept(labConcept)).thenReturn(labOrderType);
		
		// Execute
		Order result = translator.toOpenmrsType(serviceRequest);
		
		// Verify order type is set based on concept
		assertThat(result.getOrderType(), equalTo(labOrderType));
		verify(orderService).getOrderTypeByConcept(labConcept);
	}
}
