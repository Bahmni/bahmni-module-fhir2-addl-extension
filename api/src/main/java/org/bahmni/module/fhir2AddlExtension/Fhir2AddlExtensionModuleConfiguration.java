package org.bahmni.module.fhir2AddlExtension;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Fhir2AddlExtensionModuleConfiguration {
	
	@Bean
	public IParser getFhirJsonParser() {
		return FhirContext.forR4().newJsonParser();
	}
}
