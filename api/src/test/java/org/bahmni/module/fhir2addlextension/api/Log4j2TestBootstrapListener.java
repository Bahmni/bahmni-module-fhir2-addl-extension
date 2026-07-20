package org.bahmni.module.fhir2addlextension.api;

import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.junit.runner.notification.RunListener;

/**
 * Registered as a Surefire JUnit RunListener (see api/pom.xml) so it is instantiated, and its
 * static initializer runs, before Surefire loads any test class in this module. Pre-configuring
 * Log4j2 here - directly, bypassing ConfigurationFactory discovery - stops OpenMRS's
 * OpenmrsConfigurationFactory (which reenters Context.getRuntimeProperties() before Context's own
 * <clinit> has finished) from ever running, regardless of which test class happens to touch Context
 * first.
 */
public class Log4j2TestBootstrapListener extends RunListener {
	
	static {
		Configurator.initialize(new DefaultConfiguration());
	}
}
