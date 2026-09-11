package org.bahmni.module.fhir2addlextension;

import org.openmrs.GlobalProperty;
import org.openmrs.api.GlobalPropertyListener;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.util.FhirGlobalPropertyHolder;

/**
 * Clears the FHIR2 global property cache whenever a fhir2 setting is changed or deleted.
 * <p>
 * FHIR2 serves every {@code fhir2*} and {@code allergy*} setting out of
 * {@link FhirGlobalPropertyHolder}, a static cache. The holder implements
 * {@link GlobalPropertyListener} itself, but FhirActivator only registers it when its static holder
 * field is null, while willStop() removes the listener without clearing that field. Any
 * willStop/contextRefreshed cycle with no intervening stopped() therefore leaves the holder
 * deregistered permanently, and its cache stale until the container is restarted. Bahmni loads
 * several modules after fhir2, so that cycle happens on every startup.
 * <p>
 * .
 * <p>
 * This listener re-clears the cache on our behalf. Drop it once the upstream module registers its
 * holder reliably;
 */
public class FhirGlobalPropertyCacheInvalidator implements GlobalPropertyListener {
	
	private static final String ALLERGY_PROPERTY_PREFIX = "allergy";
	
	@Override
	public boolean supportsPropertyName(String globalProperty) {
		return globalProperty != null && (globalProperty.startsWith(FhirConstants.FHIR2_MODULE_ID));
	}
	
	@Override
	public void globalPropertyChanged(GlobalProperty globalProperty) {
		FhirGlobalPropertyHolder.reset();
	}
	
	@Override
	public void globalPropertyDeleted(String globalProperty) {
		FhirGlobalPropertyHolder.reset();
	}
}
