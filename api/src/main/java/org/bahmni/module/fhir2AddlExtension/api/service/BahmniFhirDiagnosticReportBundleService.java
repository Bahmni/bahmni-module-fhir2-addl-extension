package org.bahmni.module.fhir2AddlExtension.api.service;

import ca.uhn.fhir.rest.api.PatchTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.bahmni.module.fhir2AddlExtension.api.domain.DiagnosticReportBundle;
import org.openmrs.module.fhir2.api.FhirService;

import javax.annotation.Nonnull;

public interface BahmniFhirDiagnosticReportBundleService extends FhirService<DiagnosticReportBundle> {
	
	/**
	 * Patches a DiagnosticReportBundle
	 * 
	 * @param uuid the UUID of the bundle to patch
	 * @param patchType the type of patch (JSON_PATCH supported)
	 * @param body the patch body
	 * @param requestDetails the request details
	 * @return the patched DiagnosticReportBundle
	 */
	DiagnosticReportBundle patch(@Nonnull String uuid, @Nonnull PatchTypeEnum patchType, @Nonnull String body,
	        RequestDetails requestDetails);
}
