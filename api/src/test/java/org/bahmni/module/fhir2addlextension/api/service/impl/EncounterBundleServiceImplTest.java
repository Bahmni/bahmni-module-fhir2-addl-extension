package org.bahmni.module.fhir2addlextension.api.service.impl;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2addlextension.api.service.FhirResourceHandler;
import org.bahmni.module.fhir2addlextension.api.validators.EncounterBundleValidator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EncounterBundleServiceImplTest {
	
	@Mock
	private FhirResourceHandler resourceHandler;
	
	@Mock
	private EncounterBundleValidator encounterBundleValidator;
	
	@Mock
	private IResourceProvider resourceProvider;
	
	private EncounterBundleServiceImpl encounterBundleService;
	
	private Bundle validBundle;
	
	@Before
	public void setup() {
		encounterBundleService = new EncounterBundleServiceImpl(resourceHandler, encounterBundleValidator);
		
		Encounter encounter = new Encounter();
		encounter.setId("enc-123");
		encounter.setStatus(Encounter.EncounterStatus.INPROGRESS);
		encounter.setSubject(new Reference("Patient/patient-123"));
		
		Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
		entry.setFullUrl("urn:uuid:enc-123");
		entry.setResource(encounter);
		entry.setRequest(new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.POST).setUrl("Encounter"));
		
		validBundle = new Bundle();
		validBundle.setType(Bundle.BundleType.TRANSACTION);
		validBundle.addEntry(entry);
	}
	
	@Test
	public void shouldCreateBundleSuccessfullyAndReturn201ForPostRequest() {
		Encounter responseEncounter = new Encounter();
		responseEncounter.setId("created-enc-123");
		MethodOutcome outcome = new MethodOutcome();
		outcome.setResource(responseEncounter);
		
		when(resourceHandler.getResourceProvider(any())).thenReturn(Optional.of(resourceProvider));
		when(resourceHandler.invokeResourceProvider(eq(Bundle.HTTPVerb.POST), any())).thenReturn(Optional.of(outcome));
		
		Bundle result = encounterBundleService.create(validBundle);
		
		assertNotNull(result);
		assertEquals(1, result.getEntry().size());
		assertEquals("201", result.getEntry().get(0).getResponse().getStatus());
		verify(encounterBundleValidator).validateBundleType(validBundle);
		verify(encounterBundleValidator).validateBundleEntries(validBundle);
	}
	
	@Test
	public void shouldReturn200ForPutRequest() {
		validBundle.getEntry().get(0).getRequest().setMethod(Bundle.HTTPVerb.PUT);
		
		Encounter responseEncounter = new Encounter();
		responseEncounter.setId("updated-enc-123");
		MethodOutcome outcome = new MethodOutcome();
		outcome.setResource(responseEncounter);
		
		when(resourceHandler.getResourceProvider(any())).thenReturn(Optional.of(resourceProvider));
		when(resourceHandler.invokeResourceProvider(eq(Bundle.HTTPVerb.PUT), any())).thenReturn(Optional.of(outcome));
		
		Bundle result = encounterBundleService.create(validBundle);
		
		assertEquals("200", result.getEntry().get(0).getResponse().getStatus());
	}
	
	@Test
	public void shouldThrowInvalidRequestExceptionWhenResourceProviderNotFound() {
		when(resourceHandler.getResourceProvider(any())).thenReturn(Optional.empty());

		assertThrows(InvalidRequestException.class, () -> encounterBundleService.create(validBundle));
	}
	
	@Test
	public void shouldThrowInvalidRequestExceptionWhenMethodOutcomeNotReturned() {
		when(resourceHandler.getResourceProvider(any())).thenReturn(Optional.of(resourceProvider));
		when(resourceHandler.invokeResourceProvider(any(), any())).thenReturn(Optional.empty());

		assertThrows(InvalidRequestException.class, () -> encounterBundleService.create(validBundle));
	}
	
	@Test
	public void shouldThrowInvalidRequestExceptionOnUndeclaredThrowableException() {
		Exception rootCause = new Exception("root cause message");
		Exception undeclaredThrowable = new Exception("undeclared", rootCause);
		UndeclaredThrowableException exception = new UndeclaredThrowableException(undeclaredThrowable);

		when(resourceHandler.getResourceProvider(any())).thenThrow(exception);

		InvalidRequestException result = assertThrows(InvalidRequestException.class,
		    () -> encounterBundleService.create(validBundle));
		assertTrue(result.getMessage().contains("Error occurred while processing bundle entry"));
	}
	
	@Test
	public void shouldThrowInvalidRequestExceptionOnGenericException() {
		when(resourceHandler.getResourceProvider(any())).thenThrow(new RuntimeException("something went wrong"));

		InvalidRequestException result = assertThrows(InvalidRequestException.class,
		    () -> encounterBundleService.create(validBundle));
		assertTrue(result.getMessage().contains("Error occurred while processing bundle entry"));
	}
}
