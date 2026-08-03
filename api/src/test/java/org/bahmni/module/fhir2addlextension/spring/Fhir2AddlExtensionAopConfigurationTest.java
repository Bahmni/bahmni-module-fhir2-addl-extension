package org.bahmni.module.fhir2addlextension.spring;

import ca.uhn.fhir.rest.api.PatchTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.bahmni.module.fhir2addlextension.advice.FhirEncounterSaveAdvice;
import org.bahmni.module.fhir2addlextension.advice.FhirPatientSaveAdvice;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.fhir2.api.FhirEncounterService;
import org.openmrs.module.fhir2.api.FhirPatientService;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;

import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Fhir2AddlExtensionAopConfigurationTest {
	
	private StaticMethodMatcherPointcutAdvisor patientAdvisor;
	
	@Before
	public void setUp() {
		Fhir2AddlExtensionAopConfiguration configuration = new Fhir2AddlExtensionAopConfiguration();
		patientAdvisor = (StaticMethodMatcherPointcutAdvisor) configuration
		        .createFhirPatientSaveAdvisor(new FhirPatientSaveAdvice());
	}
	
	@Test
	public void shouldMatchCreateUpdateAndPatchOnFhirPatientService() throws Exception {
		assertTrue(patientAdvisor.matches(createMethod(), FhirPatientService.class));
		assertTrue(patientAdvisor.matches(updateMethod(), FhirPatientService.class));
		assertTrue(patientAdvisor.matches(patchMethod(), FhirPatientService.class));
	}
	
	@Test
	public void shouldNotMatchDeleteOrGetOnFhirPatientService() throws Exception {
		assertFalse(patientAdvisor.matches(deleteMethod(), FhirPatientService.class));
		assertFalse(patientAdvisor.matches(getMethod(), FhirPatientService.class));
	}
	
	@Test
	public void shouldNotMatchCreateOrUpdateOnAnUnrelatedFhirService() throws Exception {
		assertFalse(patientAdvisor.matches(createMethod(), FhirEncounterService.class));
		assertFalse(patientAdvisor.matches(updateMethod(), FhirEncounterService.class));
	}
	
	@Test
	public void encounterAdvisorShouldOnlyMatchCreateAndUpdateOnFhirEncounterService() throws Exception {
		Fhir2AddlExtensionAopConfiguration configuration = new Fhir2AddlExtensionAopConfiguration();
		StaticMethodMatcherPointcutAdvisor encounterAdvisor = (StaticMethodMatcherPointcutAdvisor) configuration
		        .createFhirEncounterSaveAdvisor(new FhirEncounterSaveAdvice());
		
		assertTrue(encounterAdvisor.matches(createMethod(), FhirEncounterService.class));
		assertTrue(encounterAdvisor.matches(updateMethod(), FhirEncounterService.class));
		assertFalse(encounterAdvisor.matches(patchMethod(), FhirEncounterService.class));
		assertFalse(encounterAdvisor.matches(createMethod(), FhirPatientService.class));
	}
	
	private Method createMethod() throws NoSuchMethodException {
		return org.openmrs.module.fhir2.api.FhirService.class.getMethod("create", IAnyResource.class);
	}
	
	private Method updateMethod() throws NoSuchMethodException {
		return org.openmrs.module.fhir2.api.FhirService.class.getMethod("update", String.class, IAnyResource.class);
	}
	
	private Method patchMethod() throws NoSuchMethodException {
		return org.openmrs.module.fhir2.api.FhirService.class.getMethod("patch", String.class, PatchTypeEnum.class,
		    String.class, RequestDetails.class);
	}
	
	private Method deleteMethod() throws NoSuchMethodException {
		return org.openmrs.module.fhir2.api.FhirService.class.getMethod("delete", String.class);
	}
	
	private Method getMethod() throws NoSuchMethodException {
		return org.openmrs.module.fhir2.api.FhirService.class.getMethod("get", String.class);
	}
}
