package org.bahmni.module.fhir2addlextension.interceptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.ModuleActivator;
import org.openmrs.module.ModuleFactory;
import org.openmrs.module.fhir2.FhirActivator;
import org.openmrs.module.fhir2.api.spi.ModuleLifecycleListener;
import org.openmrs.module.fhir2.web.servlet.FhirRestServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.List;

@Component
@Order(Integer.MAX_VALUE)
public class FhirInterceptorRegistrar implements ModuleLifecycleListener {
	
	private static final Log log = LogFactory.getLog(FhirInterceptorRegistrar.class);
	
	@Autowired
	private FhirAuthExceptionHandlingInterceptor exceptionInterceptor;
	
	private boolean lifecycleListenerRegistered = false;
	
	@EventListener
	public void onApplicationContextRefreshed(ContextRefreshedEvent event) {
		if (!lifecycleListenerRegistered) {
			FhirActivator fhirActivator = getFhirActivator();
			if (fhirActivator != null) {
				fhirActivator.addModuleLifecycleListener(this);
				lifecycleListenerRegistered = true;
			}
		}
		registerInterceptorWithServlet();
	}
	
	@Override
	public void refreshed() {
		registerInterceptorWithServlet();
	}
	
	private void registerInterceptorWithServlet() {
		try {
			FhirRestServlet servlet = findFhirRestServlet();
			if (servlet == null) {
				log.warn("FhirRestServlet not found in fhir2 lifecycle listeners; interceptor not registered");
				return;
			}
			servlet.registerInterceptor(exceptionInterceptor);
			log.info("Successfully registered FhirAuthExceptionHandlingInterceptor with FhirRestServlet");
		}
		catch (Exception e) {
			log.warn("Failed to register FhirAuthExceptionHandlingInterceptor", e);
		}
	}
	
	private FhirRestServlet findFhirRestServlet() throws Exception {
		FhirActivator activator = getFhirActivator();
		if (activator == null) {
			return null;
		}
		Field listenersField = FhirActivator.class.getDeclaredField("lifecycleListeners");
		listenersField.setAccessible(true);
		@SuppressWarnings("unchecked")
		List<ModuleLifecycleListener> listeners = (List<ModuleLifecycleListener>) listenersField.get(activator);
		for (ModuleLifecycleListener listener : listeners) {
			if (listener instanceof FhirRestServlet) {
				return (FhirRestServlet) listener;
			}
		}
		return null;
	}
	
	private FhirActivator getFhirActivator() {
		org.openmrs.module.Module module = ModuleFactory.getModuleById("fhir2");
		if (module == null) {
			log.warn("fhir2 module not loaded; cannot register FHIR interceptor");
			return null;
		}
		ModuleActivator activator = module.getModuleActivator();
		if (activator instanceof FhirActivator) {
			return (FhirActivator) activator;
		}
		return null;
	}
}
