package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import org.bahmni.module.fhir2AddlExtension.api.context.AppContext;
import org.bahmni.module.fhir2AddlExtension.api.dao.OrderAttributeTypeDao;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.LocationAttribute;
import org.openmrs.LocationAttributeType;
import org.openmrs.Order;
import org.openmrs.OrderAttribute;
import org.openmrs.OrderAttributeType;
import org.openmrs.OrderType;
import org.openmrs.module.fhir2.api.translators.LocationReferenceTranslator;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServiceRequestLocationReferenceResolverImplTest {
	
	ServiceRequestLocationReferenceResolverImpl serviceRequestLocationReferenceResolver;
	
	@Mock
	OrderAttributeTypeDao orderAttributeTypeDao;
	@Mock
	LocationReferenceTranslator locationReferenceTranslator;
	@Mock
	AppContext appContext;

	private Map<String, String> orderTypeToLocationAttributeNameMap = Stream.of(new Object[][] {
			{ "RADIOLOGY ORDER", "REFERRAL_RADIOLOGY_CENTER" },
			{ "TEST ORDER", "REFERRAL_LABORATORY_CENTER" },
			{ "LAB ORDER", "REFERRAL_LABORATORY_CENTER" }
	}).collect(Collectors.toMap(
			data -> (String) data[0],
			data -> (String) data[1]
	));
	
	@Before
	public void setUp() {
		serviceRequestLocationReferenceResolver = new ServiceRequestLocationReferenceResolverImpl(
		        locationReferenceTranslator, orderAttributeTypeDao, appContext);
		when(appContext.getOrderTypeToLocationAttributeNameMap()).thenReturn(orderTypeToLocationAttributeNameMap);
	}
	
	@Test
	public void shouldReturnLocationReferenceFromOrderAttributeForRequestedLocation() {
		OrderAttributeType requestedLocationAttributeType = new OrderAttributeType();
		requestedLocationAttributeType.setName(ServiceRequestLocationReferenceResolverImpl.REQUESTED_LOCATION_FOR_ORDER);
		requestedLocationAttributeType.setDatatypeClassname(ServiceRequestLocationReferenceResolverImpl.LOCATION_DATA_TYPE);
		
		Location encounterLocation = new Location();
		String locationUuid = encounterLocation.getUuid();
		
		OrderAttribute attribute = new OrderAttribute();
		attribute.setValue(encounterLocation);
		attribute.setAttributeType(requestedLocationAttributeType);
		
		Order order = new Order();
		order.addAttribute(attribute);
		
		Reference requestedLocationReferenceForOrder = serviceRequestLocationReferenceResolver
		        .getRequestedLocationReferenceForOrder(order);
		Assert.assertEquals("Location/" + locationUuid, requestedLocationReferenceForOrder.getReference());
	}
	
	@Test
	public void shouldNotReturnLocationReferenceIfAttributeNameDoNotMatch() {
		OrderAttributeType requestedLocationAttributeType = new OrderAttributeType();
		requestedLocationAttributeType.setName("Random");
		requestedLocationAttributeType.setDatatypeClassname(ServiceRequestLocationReferenceResolverImpl.LOCATION_DATA_TYPE);
		
		Location encounterLocation = new Location();
		
		OrderAttribute attribute = new OrderAttribute();
		attribute.setValue(encounterLocation);
		attribute.setAttributeType(requestedLocationAttributeType);
		
		Order order = new Order();
		order.addAttribute(attribute);
		
		Reference requestedLocationReferenceForOrder = serviceRequestLocationReferenceResolver
		        .getRequestedLocationReferenceForOrder(order);
		Assert.assertTrue(requestedLocationReferenceForOrder == null);
	}
	
	@Test
	public void shouldNotReturnLocationReferenceIfAttributeDataTypeDoNotMatch() {
		OrderAttributeType requestedLocationAttributeType = new OrderAttributeType();
		requestedLocationAttributeType.setName(ServiceRequestLocationReferenceResolverImpl.REQUESTED_LOCATION_FOR_ORDER);
		requestedLocationAttributeType.setDatatypeClassname(ServiceRequestLocationReferenceResolverImpl.LOCATION_DATA_TYPE
		        + "-random");
		
		Location encounterLocation = new Location();
		
		OrderAttribute attribute = new OrderAttribute();
		attribute.setValue(encounterLocation);
		attribute.setAttributeType(requestedLocationAttributeType);
		
		Order order = new Order();
		order.addAttribute(attribute);
		
		Reference requestedLocationReferenceForOrder = serviceRequestLocationReferenceResolver
		        .getRequestedLocationReferenceForOrder(order);
		Assert.assertTrue(requestedLocationReferenceForOrder == null);
	}
	
	@Test
	public void shouldNotFindAnyPreferredLocationForOrder() {
		Encounter encounter = new Encounter();
		Location encLocation = new Location();
		encounter.setLocation(encLocation);
		
		OrderType orderType = new OrderType();
		orderType.setName("Radiology Order");
		Order order = new Order();
		order.setOrderType(orderType);
		order.setEncounter(encounter);
		Location locationForOrder = serviceRequestLocationReferenceResolver.getPreferredLocation(order);
		Assert.assertNull(locationForOrder);
	}
	
	@Test
	public void shouldFindPreferredLocationForOrder() {
		Location pathLab = new Location();
		
		LocationAttributeType locationAttributeType = new LocationAttributeType();
		locationAttributeType.setName("REFERRAL_LABORATORY_CENTER");
		locationAttributeType.setDatatypeClassname(ServiceRequestLocationReferenceResolverImpl.LOCATION_DATA_TYPE);
		
		Location clinic = new Location();
		LocationAttribute referLocation = new LocationAttribute();
		referLocation.setAttributeType(locationAttributeType);
		referLocation.setValue(pathLab);
		clinic.addAttribute(referLocation);
		
		Encounter encounter = new Encounter();
		encounter.setLocation(clinic);
		
		OrderType orderType = new OrderType();
		orderType.setName("LAB ORDER");
		Order order = new Order();
		order.setOrderType(orderType);
		order.setEncounter(encounter);
		Location locationForOrder = serviceRequestLocationReferenceResolver.getPreferredLocation(order);
		
		Assert.assertEquals(pathLab, locationForOrder);
	}
	
	@Test
	public void shouldFindRequestedLocationOnOrder() {
		Location pathLab = new Location();
		
		Encounter encounter = new Encounter();
		
		OrderType orderType = new OrderType();
		orderType.setName("LAB ORDER");
		Order order = new Order();
		order.setOrderType(orderType);
		order.setEncounter(encounter);
		
		OrderAttributeType orderLocationType = new OrderAttributeType();
		orderLocationType.setName(ServiceRequestLocationReferenceResolverImpl.REQUESTED_LOCATION_FOR_ORDER);
		orderLocationType.setDatatypeClassname(ServiceRequestLocationReferenceResolverImpl.LOCATION_DATA_TYPE);
		
		OrderAttribute orderLocation = new OrderAttribute();
		orderLocation.setAttributeType(orderLocationType);
		orderLocation.setValue(pathLab);
		
		order.addAttribute(orderLocation);
		
		Assert.assertEquals(true, serviceRequestLocationReferenceResolver.hasRequestedLocation(order));
		Optional<OrderAttribute> requestedLocationAttribute = serviceRequestLocationReferenceResolver
		        .getRequestedLocationAttribute(order);
		Assert.assertEquals(pathLab, requestedLocationAttribute.get().getValue());
	}
	
}
