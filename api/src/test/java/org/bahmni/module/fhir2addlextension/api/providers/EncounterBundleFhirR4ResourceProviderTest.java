package org.bahmni.module.fhir2addlextension.api.providers;

import ca.uhn.fhir.rest.api.MethodOutcome;
import org.bahmni.module.fhir2addlextension.api.domain.EncounterBundle;
import org.bahmni.module.fhir2addlextension.api.service.EncounterBundleService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.fhir2.api.FhirEncounterService;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EncounterBundleFhirR4ResourceProviderTest {
	
	@Mock
	private FhirEncounterService fhirEncounterService;
	
	@Mock
	private EncounterBundleService encounterBundleService;
	
	private EncounterBundleFhirR4ResourceProvider provider;
	
	@Before
	public void setup() {
		provider = new EncounterBundleFhirR4ResourceProvider(fhirEncounterService, encounterBundleService);
	}
	
	@Test
	public void getResourceTypeShouldReturnEncounterBundleClass() {
		assertEquals(EncounterBundle.class, provider.getResourceType());
	}
	
	@Test
	public void createEncounterBundleShouldDelegateToServiceAndReturnOutcome() {
		EncounterBundle bundle = new EncounterBundle();
		Bundle responseBundle = new Bundle();
		when(encounterBundleService.create(bundle)).thenReturn(responseBundle);
		
		MethodOutcome result = provider.createEncounterBundle(bundle);
		
		assertNotNull(result);
		verify(encounterBundleService).create(bundle);
	}
	
	@Test
	public void getEncounterBundleByUuidShouldReturnNull() {
		assertNull(provider.getEncounterBundleByUuid(new IdType("test-uuid")));
	}
}
