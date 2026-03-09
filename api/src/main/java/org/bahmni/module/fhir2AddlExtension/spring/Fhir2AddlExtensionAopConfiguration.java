package org.bahmni.module.fhir2AddlExtension.spring;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.bahmni.module.fhir2AddlExtension.advice.FhirEncounterSaveAdvice;
import org.openmrs.module.fhir2.api.FhirEncounterService;
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
		final List<String> SUPPORTED_METHODS = Arrays.asList("create", "update");
		return new StaticMethodMatcherPointcutAdvisor(
		                                              fhirEncounterSaveAdvice) {
			
			@Override
			public boolean matches(Method method, Class<?> targetClass) {
				return FhirEncounterService.class.isAssignableFrom(targetClass)
				        && SUPPORTED_METHODS.contains(method.getName());
			}
		};
	}
}
