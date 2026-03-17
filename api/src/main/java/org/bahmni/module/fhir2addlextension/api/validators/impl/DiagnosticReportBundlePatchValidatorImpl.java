package org.bahmni.module.fhir2addlextension.api.validators.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException;
import org.bahmni.module.fhir2addlextension.api.validators.DiagnosticReportBundlePatchValidator;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportStatus;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of DiagnosticReportBundlePatchValidator Enforces immutability rules for PATCH
 * operations on DiagnosticReportBundle
 */
@Component
public class DiagnosticReportBundlePatchValidatorImpl implements DiagnosticReportBundlePatchValidator {
	
	private static final EnumSet<DiagnosticReportStatus> TERMINAL_STATES = EnumSet.of(DiagnosticReportStatus.CANCELLED,
	    DiagnosticReportStatus.ENTEREDINERROR);
	
	static final String TERMINAL_STATE_ERROR = "Cannot patch DiagnosticReport in terminal state";
	
	static final String PATIENT_CHANGED_ERROR = "Patient reference cannot be changed";
	
	static final String ENCOUNTER_CHANGED_ERROR = "Encounter reference cannot be changed";
	
	static final String BASEDON_RETRACTED_ERROR = "BasedOn references cannot be retracted";
	
	static final String RESULTS_RETRACTED_ERROR = "Result observations cannot be retracted";
	
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
	public void validatePatchChanges(DiagnosticReport original, DiagnosticReport patched) {
		if (original == null || patched == null) {
			throw new InvalidRequestException("Original and patched reports must not be null");
		}
		
		validatePatientReferenceNotChanged(original, patched);
		validateEncounterReferenceNotChanged(original, patched);
		validateBasedOnReferencesNotRetracted(original, patched);
		validateResultReferencesNotRetracted(original, patched);
	}
	
	private void validatePatientReferenceNotChanged(DiagnosticReport original, DiagnosticReport patched) {
		if (!original.hasSubject() || !patched.hasSubject()) {
			if (original.hasSubject() != patched.hasSubject()) {
				throw new InvalidRequestException(PATIENT_CHANGED_ERROR);
			}
			return;
		}
		
		String originalPatient = original.getSubject().getReference();
		String patchedPatient = patched.getSubject().getReference();
		
		if (originalPatient == null || patchedPatient == null) {
			if (originalPatient != patchedPatient) {
				throw new InvalidRequestException(PATIENT_CHANGED_ERROR);
			}
			return;
		}
		
		if (!originalPatient.equals(patchedPatient)) {
			throw new InvalidRequestException(PATIENT_CHANGED_ERROR);
		}
	}
	
	private void validateEncounterReferenceNotChanged(DiagnosticReport original, DiagnosticReport patched) {
		boolean originalHasEncounter = original.hasEncounter();
		boolean patchedHasEncounter = patched.hasEncounter();
		
		if (!originalHasEncounter && !patchedHasEncounter) {
			return; // Both null, OK
		}
		
		if (originalHasEncounter != patchedHasEncounter) {
			throw new InvalidRequestException(ENCOUNTER_CHANGED_ERROR);
		}
		
		String originalEnc = original.getEncounter().getReference();
		String patchedEnc = patched.getEncounter().getReference();
		
		if (originalEnc == null || patchedEnc == null) {
			if (originalEnc != patchedEnc) {
				throw new InvalidRequestException(ENCOUNTER_CHANGED_ERROR);
			}
			return;
		}
		
		if (!originalEnc.equals(patchedEnc)) {
			throw new InvalidRequestException(ENCOUNTER_CHANGED_ERROR);
		}
	}
	
	private void validateBasedOnReferencesNotRetracted(DiagnosticReport original, DiagnosticReport patched) {
		Set<String> originalBasedOn = original.getBasedOn().stream()
				.map(Reference::getReference)
				.filter(ref -> ref != null)
				.collect(Collectors.toSet());
		
		Set<String> patchedBasedOn = patched.getBasedOn().stream()
				.map(Reference::getReference)
				.filter(ref -> ref != null)
				.collect(Collectors.toSet());
		
		// All original basedOn must exist in patched
		if (!patchedBasedOn.containsAll(originalBasedOn)) {
			throw new InvalidRequestException(BASEDON_RETRACTED_ERROR);
		}
	}
	
	private void validateResultReferencesNotRetracted(DiagnosticReport original, DiagnosticReport patched) {
		Set<String> originalResults = original.getResult().stream()
				.map(Reference::getReference)
				.filter(ref -> ref != null)
				.collect(Collectors.toSet());
		
		Set<String> patchedResults = patched.getResult().stream()
				.map(Reference::getReference)
				.filter(ref -> ref != null)
				.collect(Collectors.toSet());
		
		// All original results must exist in patched
		if (!patchedResults.containsAll(originalResults)) {
			throw new InvalidRequestException(RESULTS_RETRACTED_ERROR);
		}
	}
}
