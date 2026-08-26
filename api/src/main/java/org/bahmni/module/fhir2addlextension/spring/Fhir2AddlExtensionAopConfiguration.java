package org.bahmni.module.fhir2addlextension.spring;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.bahmni.module.fhir2addlextension.advice.FhirEncounterSaveAdvice;
import org.bahmni.module.fhir2addlextension.advice.FhirPatientSaveAdvice;
import org.aopalliance.aop.Advice;
import org.openmrs.module.fhir2.api.FhirEncounterService;
import org.openmrs.module.fhir2.api.FhirPatientService;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Fhir2AddlExtensionAopConfiguration {
	
	@Bean
	public FhirEncounterSaveAdvice fhirEncounterSaveAdvice() {
		return new FhirEncounterSaveAdvice();
	}
	
	@Bean
	public Advisor createFhirEncounterSaveAdvisor(@Autowired FhirEncounterSaveAdvice fhirEncounterSaveAdvice) {
		return createAdvisor(fhirEncounterSaveAdvice, FhirEncounterService.class, Arrays.asList("create", "update"));
	}
	
	@Bean
	public FhirPatientSaveAdvice fhirPatientSaveAdvice() {
		return new FhirPatientSaveAdvice();
	}
	
	@Bean
	public Advisor createFhirPatientSaveAdvisor(@Autowired FhirPatientSaveAdvice fhirPatientSaveAdvice) {
		return createAdvisor(fhirPatientSaveAdvice, FhirPatientService.class, Arrays.asList("create", "update"));
	}
	
	private Advisor createAdvisor(Advice advice, Class<?> serviceInterface, List<String> supportedMethods) {
		return new StaticMethodMatcherPointcutAdvisor(
		                                              advice) {
			
			@Override
			public boolean matches(Method method, Class<?> targetClass) {
				return serviceInterface.isAssignableFrom(targetClass) && supportedMethods.contains(method.getName());
			}
		};
	}
}
