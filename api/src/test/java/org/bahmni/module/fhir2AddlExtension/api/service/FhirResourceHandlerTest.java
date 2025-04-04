package org.bahmni.module.fhir2AddlExtension.api.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.PatchTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.module.fhir2.api.FhirEncounterService;
import org.openmrs.module.fhir2.api.search.param.EncounterSearchParams;
import org.openmrs.module.fhir2.providers.r4.EncounterFhirResourceProvider;
import org.springframework.util.ReflectionUtils;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FhirResourceHandlerTest {
	
	@Test
	public void shouldCreateEncounter() {
		Encounter encounter = new Encounter();
		encounter.setId("tempEncounterId");
		encounter.setSubject(new Reference("Patient/123"));
		EncounterFhirResourceProvider resourceProvider = createEncounterResourceProvider(mockFhirEncounterService(encounter));
		FhirResourceHandler resourceHandler = new FhirResourceHandler(FhirContext.forR4());
		
		Optional<MethodOutcome> methodOutcome = resourceHandler.invokeResourceProvider(Bundle.HTTPVerb.POST, encounter,
		    resourceProvider);
		if (methodOutcome.isPresent()) {
			IBaseResource resource = methodOutcome.get().getResource();
			Assert.assertTrue(resource instanceof Encounter);
		} else {
			Assert.fail("No result was returned as outcome. Expected MethodOutcome with resource as Encounter");
		}
	}
	
	private EncounterFhirResourceProvider createEncounterResourceProvider(FhirEncounterService fhirEncounterService) {
		EncounterFhirResourceProvider encounterProvider = new EncounterFhirResourceProvider();
		Field encounterServiceField = ReflectionUtils.findField(EncounterFhirResourceProvider.class, "encounterService");
		encounterServiceField.setAccessible(true);
		ReflectionUtils.setField(encounterServiceField, encounterProvider, fhirEncounterService);
		return encounterProvider;
	}
	
	private FhirEncounterService mockFhirEncounterService(Encounter encounter) {
		return new FhirEncounterService() {
			
			private Encounter mockEncounter = encounter.copy();
			
			@Override
			public Encounter get(@Nonnull String s) {
				System.out.println("FhirEncounterService.get(). params: " + s);
				return mockEncounter;
			}
			
			@Override
			public List<Encounter> get(@Nonnull Collection<String> collection) {
				System.out.println("FhirEncounterService.get(). collection: "
				        + collection.stream().collect(Collectors.joining(",")));
				return Arrays.asList(mockEncounter);
			}
			
			@Override
			public Encounter create(@Nonnull Encounter encounter) {
				System.out.println("FhirEncounterService.create(). encounter= " + encounter);
				return mockEncounter;
			}
			
			@Override
			public Encounter update(@Nonnull String s, @Nonnull Encounter toUpdate) {
				System.out.println("FhirEncounterService.update(). encounter: " + toUpdate);
				return toUpdate;
			}
			
			@Override
			public Encounter patch(@Nonnull String s, @Nonnull PatchTypeEnum patchTypeEnum, @Nonnull String s1,
			        RequestDetails requestDetails) {
				System.out.println("FhirEncounterService.patch()");
				return null;
			}
			
			@Override
			public void delete(@Nonnull String s) {
				System.out.println("FhirEncounterService.delete()");
			}
			
			@Override
			public IBundleProvider searchForEncounters(EncounterSearchParams encounterSearchParams) {
				return null;
			}
			
			@Override
			public IBundleProvider getEncounterEverything(TokenParam tokenParam) {
				return null;
			}
		};
	}
}
