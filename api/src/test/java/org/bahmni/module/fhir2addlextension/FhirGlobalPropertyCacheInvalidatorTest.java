package org.bahmni.module.fhir2addlextension;

import org.junit.Test;
import org.openmrs.GlobalProperty;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FhirGlobalPropertyCacheInvalidatorTest {
	
	private final FhirGlobalPropertyCacheInvalidator invalidator = new FhirGlobalPropertyCacheInvalidator();
	
	@Test
	public void shouldSupportEveryPropertyTheFhirHolderCaches() {
		assertTrue(invalidator.supportsPropertyName("fhir2.personContactPointAttributeTypeUuid"));
		assertTrue(invalidator.supportsPropertyName("fhir2.locationContactPointAttributeTypeUuid"));
		assertTrue(invalidator.supportsPropertyName("fhir2.supportedLocationHierarchySearchDepth"));
	}
	
	@Test
	public void shouldIgnorePropertiesTheFhirHolderDoesNotCache() {
		assertFalse(invalidator.supportsPropertyName("bahmni.encounterSessionDuration"));
		assertFalse(invalidator.supportsPropertyName("emrapi.sqlSearch.activePatients"));
	}
	
	@Test
	public void shouldIgnoreNullPropertyNameRatherThanThrow() {
		assertFalse(invalidator.supportsPropertyName(null));
	}
	
	@Test
	public void shouldClearTheCacheOnChangeWithoutInspectingTheNewValue() {
		// reset() only clears static collections, so this needs no OpenMRS context
		invalidator.globalPropertyChanged(new GlobalProperty("fhir2.personContactPointAttributeTypeUuid", "some-uuid"));
	}
	
	@Test
	public void shouldClearTheCacheOnDelete() {
		invalidator.globalPropertyDeleted("fhir2.personContactPointAttributeTypeUuid");
	}
}
