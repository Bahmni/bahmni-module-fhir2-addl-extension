package org.bahmni.module.fhir2addlextension.api.context.impl;

import org.bahmni.module.fhir2addlextension.api.model.TelecomAttributeTypeMapping;
import org.hl7.fhir.r4.model.ContactPoint;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.EncounterService;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
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
	public void shouldParseMultipleTelecomAttributeTypeMappings() {
		when(adminService.getGlobalProperty(OpenmrsAppContext.PROP_TELECOM_ATTRIBUTE_TYPE_MAP, "")).thenReturn(
		    "uuid1:PHONE::1;uuid2:EMAIL;uuid3:PHONE:HOME:2");
		
		List<TelecomAttributeTypeMapping> mappings = new OpenmrsAppContext(adminService, encounterService)
		        .getTelecomAttributeTypeMappings();
		
		assertThat(mappings.size(), equalTo(3));
		assertThat(mappings.get(0).getAttributeTypeUuid(), equalTo("uuid1"));
		assertThat(mappings.get(0).getSystem(), equalTo(ContactPoint.ContactPointSystem.PHONE));
		assertThat(mappings.get(0).getUse(), nullValue());
		assertThat(mappings.get(0).getRank(), equalTo(1));
		
		assertThat(mappings.get(1).getSystem(), equalTo(ContactPoint.ContactPointSystem.EMAIL));
		assertThat(mappings.get(1).getRank(), nullValue());
		
		assertThat(mappings.get(2).getUse(), equalTo(ContactPoint.ContactPointUse.HOME));
		assertThat(mappings.get(2).getRank(), equalTo(2));
	}
	
	@Test
	public void shouldBeCaseInsensitiveForSystemAndUseInTelecomAttributeTypeMap() {
		when(adminService.getGlobalProperty(OpenmrsAppContext.PROP_TELECOM_ATTRIBUTE_TYPE_MAP, "")).thenReturn(
		    "uuid1:phone:Home:1");
		
		List<TelecomAttributeTypeMapping> mappings = new OpenmrsAppContext(adminService, encounterService)
		        .getTelecomAttributeTypeMappings();
		
		assertThat(mappings.get(0).getSystem(), equalTo(ContactPoint.ContactPointSystem.PHONE));
		assertThat(mappings.get(0).getUse(), equalTo(ContactPoint.ContactPointUse.HOME));
	}
	
	@Test
	public void shouldSkipMalformedOrUnknownEntriesInTelecomAttributeTypeMap() {
		when(adminService.getGlobalProperty(OpenmrsAppContext.PROP_TELECOM_ATTRIBUTE_TYPE_MAP, "")).thenReturn(
		    "uuid1;uuid2:NOT_A_SYSTEM;uuid3:EMAIL");
		
		List<TelecomAttributeTypeMapping> mappings = new OpenmrsAppContext(adminService, encounterService)
		        .getTelecomAttributeTypeMappings();
		
		assertThat(mappings.size(), equalTo(1));
		assertThat(mappings.get(0).getAttributeTypeUuid(), equalTo("uuid3"));
	}
	
	@Test
	public void shouldReturnEmptyListForBlankTelecomAttributeTypeMap() {
		when(adminService.getGlobalProperty(OpenmrsAppContext.PROP_TELECOM_ATTRIBUTE_TYPE_MAP, "")).thenReturn("");
		
		List<TelecomAttributeTypeMapping> mappings = new OpenmrsAppContext(adminService, encounterService)
		        .getTelecomAttributeTypeMappings();
		
		assertThat(mappings, is(java.util.Collections.emptyList()));
	}
}
