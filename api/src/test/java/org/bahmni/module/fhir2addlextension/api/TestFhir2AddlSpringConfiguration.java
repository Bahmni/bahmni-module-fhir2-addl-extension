package org.bahmni.module.fhir2addlextension.api;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * Loads the OpenMRS service context plus every module's moduleApplicationContext.xml (fhir2 + this
 * module) and registers this module's hibernate mappings (normally done through config.xml
 * mappingFiles) so DAOs can be exercised against a real in-memory H2 database.
 */
/**
 * DAO beans live outside the org.openmrs package scanned by applicationContext-service.xml, so they
 * are scanned explicitly here (the runtime scans the module package declared in config.xml).
 */
@Configuration
@ComponentScan("org.bahmni.module.fhir2addlextension.api")
@ImportResource({ "classpath:applicationContext-service.xml", "classpath*:moduleApplicationContext.xml" })
public class TestFhir2AddlSpringConfiguration {
	
	@Bean
	public static BeanFactoryPostProcessor fhir2AddlMappingRegistrar() {
		return beanFactory -> beanFactory.getBeanDefinition("sessionFactory").getPropertyValues().add("mappingResources",
		    new String[] { "DocumentReference.hbm.xml", "DocumentReferenceContent.hbm.xml",
		            "DocumentReferenceAttribute.hbm.xml", "DocumentReferenceAttributeType.hbm.xml", "ImagingStudy.hbm.xml",
		            "ImagingStudyNotes.hbm.xml", "DocumentAttachment.hbm.xml", "FhirDiagnosticReportExt.hbm.xml",
		            // provided-scope Bahmni modules (episodes-api / appointments-api jars)
		            "Episode.hbm.xml", "EpisodeAttribute.hbm.xml", "EpisodeAttributeType.hbm.xml",
		            "EpisodeReason.hbm.xml", "EpisodeStatusHistory.hbm.xml", "Appointment.hbm.xml",
		            "AppointmentAudit.hbm.xml", "AppointmentProvider.hbm.xml", "AppointmentReason.hbm.xml",
		            "AppointmentRecurringPattern.hbm.xml", "AppointmentServiceAttribute.hbm.xml",
		            "AppointmentServiceAttributeType.hbm.xml", "AppointmentServiceDefinition.hbm.xml",
		            "AppointmentServiceType.hbm.xml", "ServiceWeeklyAvailability.hbm.xml", "Speciality.hbm.xml" });
	}
}
