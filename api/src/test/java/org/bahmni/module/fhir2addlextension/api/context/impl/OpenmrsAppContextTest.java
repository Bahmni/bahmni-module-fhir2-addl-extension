package org.bahmni.module.fhir2addlextension.api.context.impl;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.EncounterService;

import java.util.Map;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OpenmrsAppContextTest {
	
	@Mock
	private AdministrationService adminService;
	
	@Mock
	private EncounterService encounterService;
	
	@Test
	public void shouldGetReferralLocationAttributeNameForOrderType() {
		when(adminService.getGlobalProperty(OpenmrsAppContext.PROP_ORDER_TYPE_TO_LOCATION_ATTR_NAME_MAP, "")).thenReturn(
		    "Radiology Order:REFERRAL_RADIOLOGY_CENTER; Surgical Order : Referral Surgical Center");
		Map<String, String> orderTypeToLocationAttributeNameMap = new OpenmrsAppContext(adminService, encounterService)
		        .getOrderTypeToLocationAttributeNameMap();
		Assert.assertEquals("REFERRAL_RADIOLOGY_CENTER", orderTypeToLocationAttributeNameMap.get("Radiology Order"));
		Assert.assertEquals("Referral Surgical Center", orderTypeToLocationAttributeNameMap.get("Surgical Order"));
		Assert.assertEquals(2, orderTypeToLocationAttributeNameMap.size());
	}
	
	@Test
	public void shouldRedactErrorsInOrderTypeToAttributeMap() {
		when(adminService.getGlobalProperty(OpenmrsAppContext.PROP_ORDER_TYPE_TO_LOCATION_ATTR_NAME_MAP, "")).thenReturn(
		    "Radiology Order:REFERRAL_RADIOLOGY_CENTER;1: ; :LAB ORDER; Surgical Order : Referral Surgical Center");
		Map<String, String> orderTypeToLocationAttributeNameMap = new OpenmrsAppContext(adminService, encounterService)
		        .getOrderTypeToLocationAttributeNameMap();
		Assert.assertEquals(2, orderTypeToLocationAttributeNameMap.size());
	}
	
	@Test
	public void shouldReturnEmptyMapWhenPractitionerAttributeIdentifierSystemMapGpIsUnset() {
		when(adminService.getGlobalProperty(OpenmrsAppContext.PROP_PRACTITIONER_ATTRIBUTE_IDENTIFIER_SYSTEM_MAP, ""))
		        .thenReturn("");
		Map<String, String> map = new OpenmrsAppContext(adminService, encounterService)
		        .getPractitionerAttributeIdentifierSystemMap();
		Assert.assertTrue(map.isEmpty());
	}
	
	@Test
	public void shouldTrimWhitespaceWhenParsingPractitionerAttributeIdentifierSystemMap() {
		when(adminService.getGlobalProperty(OpenmrsAppContext.PROP_PRACTITIONER_ATTRIBUTE_IDENTIFIER_SYSTEM_MAP, ""))
		        .thenReturn(" identifier : http://example.org ; reg : http://other.org ");
		Map<String, String> map = new OpenmrsAppContext(adminService, encounterService)
		        .getPractitionerAttributeIdentifierSystemMap();
		Assert.assertEquals(2, map.size());
		Assert.assertEquals("http://example.org", map.get("identifier"));
		Assert.assertEquals("http://other.org", map.get("reg"));
	}
	
}
