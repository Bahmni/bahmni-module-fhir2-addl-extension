package org.bahmni.module.fhir2addlextension;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.openmrs.api.AdministrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Fhir2AddlExtensionModuleConfiguration {
	
	@Autowired
	@Qualifier("adminService")
	private AdministrationService administrationService;
	
	@Bean
	public IParser getFhirJsonParser() {
		return FhirContext.forR4().newJsonParser();
	}
}
