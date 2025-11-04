package org.bahmni.module.fhir2AddlExtension.api.providers;

import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * OpenMRS Fhir2 SearchQueryIncludeImpl calls getResources(0, -1), which causes the hapi
 * SimpleBundleProvider to throw error. To resolve that, here getResources() returns all resources
 * when theToIndex is negative
 */
@Slf4j
public class BahmniSimpleBundleProvider extends SimpleBundleProvider {
	
	public BahmniSimpleBundleProvider(List<? extends IBaseResource> theList) {
		super(theList);
	}
	
	@Nonnull
	@Override
	public List<IBaseResource> getResources(int theFromIndex, int theToIndex) {
		if (theToIndex < 0) {
			log.info("Requested with negative toIndex from a resource list. Returning entire list");
			return (List<IBaseResource>) getList();
		}
		return super.getResources(theFromIndex, theToIndex);
	}
}
