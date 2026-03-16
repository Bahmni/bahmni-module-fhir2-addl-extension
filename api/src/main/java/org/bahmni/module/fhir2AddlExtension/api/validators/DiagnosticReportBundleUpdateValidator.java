package org.bahmni.module.fhir2addlextension.api.validators;

import org.hl7.fhir.r4.model.DiagnosticReport;

/**
 * Validator for PUT operations on DiagnosticReportBundle Ensures immutability constraints for
 * complete replacement operations PUT operations replace all mutable fields: - basedOn references -
 * result observations - performers - attachments (presentedForm) - status and other metadata
 * Immutable fields that cannot change: - patient reference - encounter reference
 */
public interface DiagnosticReportBundleUpdateValidator {
	
	/**
	 * Validates that the diagnostic report is not in a terminal state Terminal states: CANCELLED,
	 * ENTERED_IN_ERROR
	 * 
	 * @param report the diagnostic report to validate
	 * @throws ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException if report is in terminal
	 *             state
	 */
	void validateNotInTerminalState(DiagnosticReport report);
	
	/**
	 * Validates that PUT changes comply with immutability rules Only patient and encounter
	 * references are immutable in PUT operations All other fields can be replaced
	 * 
	 * @param original the original diagnostic report before update
	 * @param updated the updated diagnostic report
	 * @throws ca.uhn.fhir.rest.server.exceptions.InvalidRequestException if validation fails
	 */
	void validateUpdateChanges(DiagnosticReport original, DiagnosticReport updated);
}
