package org.bahmni.module.fhir2addlextension.api.context.impl;

import org.bahmni.module.fhir2addlextension.api.model.DispositionConfig;
import org.bahmni.module.fhir2addlextension.api.model.TelecomAttributeTypeMapping;
import org.hl7.fhir.r4.model.ContactPoint;
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.EncounterType;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.EncounterService;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
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

	@Test
	public void shouldParseMultipleTelecomAttributeTypeMappings() {
		when(adminService.getGlobalProperty(OpenmrsAppContext.PROP_TELECOM_ATTRIBUTE_TYPE_MAP, "")).thenReturn(
		    "uuid1:PHONE::1;uuid2:EMAIL;uuid3:PHONE:HOME:2");
		
		List<TelecomAttributeTypeMapping> mappings = new OpenmrsAppContext(adminService, encounterService)
		        .getTelecomAttributeTypeMappings();
		
		assertThat(mappings.size(), equalTo(3));
		assertThat(mappings.get(0).getAttributeTypeUuid(), equalTo("uuid1"));
		assertThat(mappings.get(0).getSystem(), equalTo(ContactPoint.ContactPointSystem.PHONE.name()));
		assertThat(mappings.get(0).getUse(), nullValue());
		assertThat(mappings.get(0).getRank(), equalTo(1));
		
		assertThat(mappings.get(1).getSystem(), equalTo(ContactPoint.ContactPointSystem.EMAIL.name()));
		assertThat(mappings.get(1).getRank(), nullValue());
		
		assertThat(mappings.get(2).getUse(), equalTo(ContactPoint.ContactPointUse.HOME.name()));
		assertThat(mappings.get(2).getRank(), equalTo(2));
	}
	
	@Test
	public void shouldBeCaseInsensitiveForSystemAndUseInTelecomAttributeTypeMap() {
		when(adminService.getGlobalProperty(OpenmrsAppContext.PROP_TELECOM_ATTRIBUTE_TYPE_MAP, "")).thenReturn(
		    "uuid1:phone:Home:1");
		
		List<TelecomAttributeTypeMapping> mappings = new OpenmrsAppContext(adminService, encounterService)
		        .getTelecomAttributeTypeMappings();
		
		assertThat(mappings.get(0).getSystem(), equalTo(ContactPoint.ContactPointSystem.PHONE.name()));
		assertThat(mappings.get(0).getUse(), equalTo(ContactPoint.ContactPointUse.HOME.name()));
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
	
	@Test
	public void shouldGetOrderTypeToCategoryMap() {
		when(adminService.getGlobalProperty(OpenmrsAppContext.PROP_ORDER_TYPE_TO_CATEGORY_MAP, "")).thenReturn(
		    "Lab Order:laboratory; Imaging Order : imaging");
		Map<String, String> orderTypeToCategoryMap = new OpenmrsAppContext(adminService, encounterService)
		        .getOrderTypeToCategoryMap();
		Assert.assertEquals("laboratory", orderTypeToCategoryMap.get("Lab Order"));
		Assert.assertEquals("imaging", orderTypeToCategoryMap.get("Imaging Order"));
		Assert.assertEquals(2, orderTypeToCategoryMap.size());
	}
	
	@Test
	public void shouldReturnEmptyMapForBlankOrderTypeToCategoryMap() {
		when(adminService.getGlobalProperty(OpenmrsAppContext.PROP_ORDER_TYPE_TO_CATEGORY_MAP, "")).thenReturn("");
		Map<String, String> orderTypeToCategoryMap = new OpenmrsAppContext(adminService, encounterService)
		        .getOrderTypeToCategoryMap();
		assertThat(orderTypeToCategoryMap.size(), equalTo(0));
	}
	
	@Test
	public void shouldParseTheShippedDispositionRules() {
		when(adminService.getGlobalProperty(OpenmrsAppContext.PROP_ADT_ORDER_TYPE_NAME)).thenReturn("ADT Order");
		when(adminService.getGlobalProperty(OpenmrsAppContext.PROP_ADT_DISPOSITION_RULES)).thenReturn(
		    "{\"ADMIT\":{\"duplicateChecked\":true,\"closedBy\":[\"DISCHARGE\"],\"fulfilledByEncounterTypes\":[\"ADMISSION\"]},\"DISCHARGE\":{\"duplicateChecked\":true,\"closedBy\":[\"ADMIT\",\"UNDO_DISCHARGE\"],\"fulfilledByEncounterTypes\":[\"DISCHARGE\"]},\"UNDO_DISCHARGE\":{\"duplicateChecked\":true,\"closedBy\":[\"DISCHARGE\"],\"fulfilledByEncounterTypes\":[\"DISCHARGE\"],\"fulfilledByVoiding\":true},\"TRANSFER\":{\"duplicateChecked\":true,\"closedBy\":[\"DISCHARGE\"],\"fulfilledByEncounterTypes\":[\"TRANSFER\"]}}");
		
		when(encounterService.getEncounterType(org.mockito.ArgumentMatchers.anyString())).thenReturn(new EncounterType());
		
		DispositionConfig config = new OpenmrsAppContext(adminService, encounterService).getDispositionConfig();
		
		assertTrue(config.getRule("UNDO_DISCHARGE").isFulfilledByVoiding());
		assertFalse(config.getRule("ADMIT").isFulfilledByVoiding());
		assertThat(config.getRule("ADMIT").getFulfilledByEncounterTypes(), equalTo(Collections.singleton("ADMISSION")));
		assertThat(config.getAdtOrderTypeName(), equalTo("ADT Order"));
		assertTrue(config.getRule("ADMIT").isDuplicateChecked());
		assertThat(config.getRule("ADMIT").getClosedBy(), equalTo(Collections.singleton("DISCHARGE")));
		assertThat(config.getRule("DISCHARGE").getClosedBy(),
		    equalTo(new HashSet<>(Arrays.asList("ADMIT", "UNDO_DISCHARGE"))));
		assertThat(config.getRule("DISCHARGE").getFulfilledByEncounterTypes(), equalTo(Collections.singleton("DISCHARGE")));
		assertTrue(config.getRule("TRANSFER").isDuplicateChecked());
		assertThat(config.getRule("TRANSFER").getClosedBy(), equalTo(Collections.singleton("DISCHARGE")));
		assertThat(config.getRule("TRANSFER").getFulfilledByEncounterTypes(), equalTo(Collections.singleton("TRANSFER")));
		assertThat(config.getRule("UNKNOWN"), nullValue());
	}
	
	@Test
	public void shouldReadDispositionConfigFromGlobalProperties() {
		when(adminService.getGlobalProperty(OpenmrsAppContext.PROP_ADT_ORDER_TYPE_NAME)).thenReturn("Bed Order");
		when(adminService.getGlobalProperty(OpenmrsAppContext.PROP_ADT_DISPOSITION_RULES)).thenReturn(
		    "{\"ADMIT\":{\"duplicateChecked\":true,\"closedBy\":[\"LEAVE\"],\"fulfilledByEncounterTypes\":[\"IN\"]}}");
		
		when(encounterService.getEncounterType("IN")).thenReturn(new EncounterType());
		
		DispositionConfig config = new OpenmrsAppContext(adminService, encounterService).getDispositionConfig();
		
		assertThat(config.getAdtOrderTypeName(), equalTo("Bed Order"));
		assertThat(config.getRule("ADMIT").getClosedBy(), equalTo(Collections.singleton("LEAVE")));
		assertThat(config.getRule("ADMIT").getFulfilledByEncounterTypes(), equalTo(Collections.singleton("IN")));
		assertThat(config.getRule("DISCHARGE"), nullValue());
	}
	
	@Test
	public void shouldFailWhenARuleRefersToAnEncounterTypeThatDoesNotExist() {
		when(adminService.getGlobalProperty(OpenmrsAppContext.PROP_ADT_ORDER_TYPE_NAME)).thenReturn("ADT Order");
		when(adminService.getGlobalProperty(OpenmrsAppContext.PROP_ADT_DISPOSITION_RULES)).thenReturn(
		    "{\"ADMIT\":{\"fulfilledByEncounterTypes\":[\"NOPE\"]}}");
		
		IllegalStateException e = assertThrows(IllegalStateException.class,
		    () -> new OpenmrsAppContext(adminService, encounterService).getDispositionConfig());
		assertTrue(e.getMessage().contains("NOPE"));
	}
	
	@Test
	public void shouldFailWithClearMessageWhenDispositionRulesJsonIsInvalid() {
		when(adminService.getGlobalProperty(OpenmrsAppContext.PROP_ADT_ORDER_TYPE_NAME)).thenReturn("ADT Order");
		when(adminService.getGlobalProperty(OpenmrsAppContext.PROP_ADT_DISPOSITION_RULES)).thenReturn("not json");
		
		IllegalStateException e = assertThrows(IllegalStateException.class,
		    () -> new OpenmrsAppContext(adminService, encounterService).getDispositionConfig());
		assertTrue(e.getMessage().contains(OpenmrsAppContext.PROP_ADT_DISPOSITION_RULES));
	}
	
	@Test
	public void shouldFailWhenDispositionGlobalPropertiesAreNotConfigured() {
		when(adminService.getGlobalProperty(OpenmrsAppContext.PROP_ADT_ORDER_TYPE_NAME)).thenReturn(null);
		
		IllegalStateException e = assertThrows(IllegalStateException.class,
		    () -> new OpenmrsAppContext(adminService, encounterService).getDispositionConfig());
		assertTrue(e.getMessage().contains(OpenmrsAppContext.PROP_ADT_ORDER_TYPE_NAME));
	}
}
