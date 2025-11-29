package org.bahmni.module.fhir2AddlExtension.api.context.impl;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.api.AdministrationService;

import java.util.Map;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OpenmrsAppContextTest {
	
	@Mock
	AdministrationService adminService;
	
	@Test
	public void shouldGetReferralLocationAttributeNameForOrderType() {
		when(adminService.getGlobalProperty(OpenmrsAppContext.PROP_ORDER_TYPE_TO_LOCATION_ATTR_NAME_MAP, "")).thenReturn(
		    "Radiology Order:REFERRAL_RADIOLOGY_CENTER; Surgical Order : Referral Surgical Center");
		Map<String, String> orderTypeToLocationAttributeNameMap = new OpenmrsAppContext(adminService)
		        .getOrderTypeToLocationAttributeNameMap();
		Assert.assertEquals("REFERRAL_RADIOLOGY_CENTER", orderTypeToLocationAttributeNameMap.get("Radiology Order"));
		Assert.assertEquals("Referral Surgical Center", orderTypeToLocationAttributeNameMap.get("Surgical Order"));
		Assert.assertEquals(2, orderTypeToLocationAttributeNameMap.size());
	}
	
	@Test
	public void shouldRedactErrorsInOrderTypeToAttributeMap() {
		when(adminService.getGlobalProperty(OpenmrsAppContext.PROP_ORDER_TYPE_TO_LOCATION_ATTR_NAME_MAP, "")).thenReturn(
		    "Radiology Order:REFERRAL_RADIOLOGY_CENTER;1: ; :LAB ORDER; Surgical Order : Referral Surgical Center");
		Map<String, String> orderTypeToLocationAttributeNameMap = new OpenmrsAppContext(adminService)
		        .getOrderTypeToLocationAttributeNameMap();
		Assert.assertEquals(2, orderTypeToLocationAttributeNameMap.size());
	}
	
}
