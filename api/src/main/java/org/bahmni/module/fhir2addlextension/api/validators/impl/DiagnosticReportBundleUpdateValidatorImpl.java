package org.bahmni.module.fhir2addlextension.api.validators.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException;
import org.bahmni.module.fhir2addlextension.api.validators.DiagnosticReportBundleUpdateValidator;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

/**
 * Implementation of DiagnosticReportBundleUpdateValidator Enforces immutability rules for PUT
 * operations on DiagnosticReportBundle PUT operations allow complete replacement of mutable fields
 * but enforce immutability of patient and encounter references
 */
@Component
public class DiagnosticReportBundleUpdateValidatorImpl implements DiagnosticReportBundleUpdateValidator {
	
	private static final EnumSet<DiagnosticReportStatus> TERMINAL_STATES = EnumSet.of(DiagnosticReportStatus.CANCELLED,
	    DiagnosticReportStatus.ENTEREDINERROR);
	
	static final String TERMINAL_STATE_ERROR = "Cannot update DiagnosticReport in terminal state";
	
	static final String PATIENT_CHANGED_ERROR = "Patient reference cannot be changed in update operation";
	
	static final String ENCOUNTER_CHANGED_ERROR = "Encounter reference cannot be changed in update operation";
	
	@Override
	public void validateNotInTerminalState(DiagnosticReport report) {
		if (report == null || !report.hasStatus()) {
			return;
		}
		
		if (TERMINAL_STATES.contains(report.getStatus())) {
			throw new MethodNotAllowedException(TERMINAL_STATE_ERROR + ": " + report.getStatus().toCode());
		}
	}
	
	@Override
	public void validateUpdateChanges(DiagnosticReport original, DiagnosticReport updated) {
		if (original == null || updated == null) {
			throw new InvalidRequestException("Original and updated reports must not be null");
		}
		
		// Only validate immutable fields (patient and encounter)
		// All other fields (basedOn, results, performers, attachments) can be replaced
		validatePatientReferenceNotChanged(original, updated);
		validateEncounterReferenceNotChanged(original, updated);
	}
	
	private void validatePatientReferenceNotChanged(DiagnosticReport original, DiagnosticReport updated) {
		if (!original.hasSubject() || !updated.hasSubject()) {
			throw new InvalidRequestException(PATIENT_CHANGED_ERROR);
		}
		
		String originalPatient = original.getSubject().getReference();
		String updatedPatient = updated.getSubject().getReference();
		
		if (!originalPatient.equals(updatedPatient)) {
			throw new InvalidRequestException(PATIENT_CHANGED_ERROR);
		}
	}
	
	private void validateEncounterReferenceNotChanged(DiagnosticReport original, DiagnosticReport updated) {
		if (!updated.hasEncounter()) {
			// No encounter reference, we use the existing encounter
			return;
		}
		
		String originalEnc = original.getEncounter().getReference();
		String updatedEnc = updated.getEncounter().getReference();
		
		if (!originalEnc.equals(updatedEnc)) {
			throw new InvalidRequestException(ENCOUNTER_CHANGED_ERROR);
		}
	}
}
