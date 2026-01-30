package org.bahmni.module.fhir2AddlExtension.api.validators;

import org.hl7.fhir.r4.model.DiagnosticReport;

/**
 * Validator for PATCH operations on DiagnosticReportBundle Ensures immutability constraints and
 * business rules are enforced
 */
public interface DiagnosticReportBundlePatchValidator {
	
	/**
	 * Validates that the diagnostic report is not in a terminal state
	 * 
	 * @param report the diagnostic report to validate
	 * @throws ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException if report is in terminal
	 *             state
	 */
	void validateNotInTerminalState(DiagnosticReport report);
	
	/**
	 * Validates that PATCH changes comply with immutability rules
	 * 
	 * @param original the original diagnostic report before patch
	 * @param patched the patched diagnostic report
	 * @throws ca.uhn.fhir.rest.server.exceptions.InvalidRequestException if validation fails
	 */
	void validatePatchChanges(DiagnosticReport original, DiagnosticReport patched);
}
