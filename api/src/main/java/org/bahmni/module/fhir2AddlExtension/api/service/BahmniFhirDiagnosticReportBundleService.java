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
	
	/**
	 * Updates a DiagnosticReportBundle using PUT semantics (complete replacement) All mutable
	 * fields are replaced with new values from the provided bundle Immutable fields (cannot be
	 * changed): - patient reference - encounter reference Mutable fields (completely replaced): -
	 * basedOn references - result observations (existing voided, new created) - performers -
	 * attachments (presentedForm) - status, conclusion, issued, and other metadata
	 * 
	 * @param uuid the UUID of the bundle to update
	 * @param bundle the complete replacement bundle
	 * @return the updated DiagnosticReportBundle
	 */
	DiagnosticReportBundle update(@Nonnull String uuid, @Nonnull DiagnosticReportBundle bundle);
}
