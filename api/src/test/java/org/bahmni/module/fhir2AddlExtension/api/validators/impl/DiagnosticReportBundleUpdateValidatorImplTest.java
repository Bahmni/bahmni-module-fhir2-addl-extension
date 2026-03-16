package org.bahmni.module.fhir2addlextension.api.validators.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException;
import org.bahmni.module.fhir2addlextension.api.validators.DiagnosticReportBundleUpdateValidator;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DiagnosticReportBundleUpdateValidatorImplTest {
	
	private DiagnosticReportBundleUpdateValidator validator;
	
	@Before
	public void setUp() {
		validator = new DiagnosticReportBundleUpdateValidatorImpl();
	}
	
	// ========== validateNotInTerminalState Tests ==========
	
	@Test
	public void shouldAllowUpdateForRegisteredStatus() {
		DiagnosticReport report = new DiagnosticReport();
		report.setStatus(DiagnosticReport.DiagnosticReportStatus.REGISTERED);
		
		// Should not throw exception
		validator.validateNotInTerminalState(report);
	}
	
	@Test
	public void shouldRejectUpdateForCancelledStatus() {
		DiagnosticReport report = new DiagnosticReport();
		report.setStatus(DiagnosticReport.DiagnosticReportStatus.CANCELLED);
		
		MethodNotAllowedException exception = assertThrows(MethodNotAllowedException.class,
				() -> validator.validateNotInTerminalState(report));
		
		assertTrue(exception.getMessage().contains("Cannot update DiagnosticReport in terminal state"));
		assertTrue(exception.getMessage().contains("cancelled"));
	}
	
	@Test
	public void shouldRejectUpdateForEnteredInErrorStatus() {
		DiagnosticReport report = new DiagnosticReport();
		report.setStatus(DiagnosticReport.DiagnosticReportStatus.ENTEREDINERROR);
		
		MethodNotAllowedException exception = assertThrows(MethodNotAllowedException.class,
				() -> validator.validateNotInTerminalState(report));
		
		assertTrue(exception.getMessage().contains("Cannot update DiagnosticReport in terminal state"));
		assertTrue(exception.getMessage().contains("entered-in-error"));
	}
	
	@Test
	public void shouldHandleReportWithoutStatusGracefully() {
		DiagnosticReport report = new DiagnosticReport();
		// No status set
		
		// Should not throw exception
		validator.validateNotInTerminalState(report);
	}
	
	// ========== validateUpdateChanges Tests ==========
	
	@Test
	public void shouldAllowUpdateWhenPatientReferenceUnchanged() {
		DiagnosticReport original = new DiagnosticReport();
		original.setSubject(new Reference("Patient/patient-123"));
		
		DiagnosticReport updated = new DiagnosticReport();
		updated.setSubject(new Reference("Patient/patient-123"));
		
		// Should not throw exception
		validator.validateUpdateChanges(original, updated);
	}
	
	@Test
	public void shouldRejectUpdateWhenPatientReferenceChanged() {
		DiagnosticReport original = new DiagnosticReport();
		original.setSubject(new Reference("Patient/patient-123"));
		
		DiagnosticReport updated = new DiagnosticReport();
		updated.setSubject(new Reference("Patient/patient-456"));
		
		InvalidRequestException exception = assertThrows(InvalidRequestException.class,
				() -> validator.validateUpdateChanges(original, updated));
		
		assertEquals(DiagnosticReportBundleUpdateValidatorImpl.PATIENT_CHANGED_ERROR, exception.getMessage());
	}
	
	//TODO - not applicable
	@Test
	public void shouldRejectUpdateWhenPatientReferenceAdded() {
		DiagnosticReport original = new DiagnosticReport();
		// No subject
		
		DiagnosticReport updated = new DiagnosticReport();
		updated.setSubject(new Reference("Patient/patient-123"));
		
		InvalidRequestException exception = assertThrows(InvalidRequestException.class,
				() -> validator.validateUpdateChanges(original, updated));
		
		assertEquals(DiagnosticReportBundleUpdateValidatorImpl.PATIENT_CHANGED_ERROR, exception.getMessage());
	}
	
	@Test
	public void shouldRejectUpdateWhenPatientReferenceRemoved() {
		DiagnosticReport original = new DiagnosticReport();
		original.setSubject(new Reference("Patient/patient-123"));
		
		DiagnosticReport updated = new DiagnosticReport();
		// No subject
		
		InvalidRequestException exception = assertThrows(InvalidRequestException.class,
				() -> validator.validateUpdateChanges(original, updated));
		
		assertEquals(DiagnosticReportBundleUpdateValidatorImpl.PATIENT_CHANGED_ERROR, exception.getMessage());
	}
	
	@Test
	public void shouldAllowUpdateWhenEncounterReferenceUnchanged() {
		DiagnosticReport original = new DiagnosticReport();
		original.setSubject(new Reference("Patient/patient-123"));
		original.setEncounter(new Reference("Encounter/encounter-123"));
		
		DiagnosticReport updated = new DiagnosticReport();
		updated.setSubject(new Reference("Patient/patient-123"));
		updated.setEncounter(new Reference("Encounter/encounter-123"));
		
		// Should not throw exception
		validator.validateUpdateChanges(original, updated);
	}
	
	@Test
	public void shouldRejectUpdateWhenEncounterReferenceChanged() {
		DiagnosticReport original = new DiagnosticReport();
		original.setSubject(new Reference(("Patient/patient-123")));
		original.setEncounter(new Reference("Encounter/encounter-123"));

		DiagnosticReport updated = new DiagnosticReport();
		updated.setSubject(new Reference(("Patient/patient-123")));
		updated.setEncounter(new Reference("Encounter/encounter-456"));

		InvalidRequestException exception = assertThrows(InvalidRequestException.class,
				() -> validator.validateUpdateChanges(original, updated));
		
		assertEquals(DiagnosticReportBundleUpdateValidatorImpl.ENCOUNTER_CHANGED_ERROR, exception.getMessage());
	}
	
	@Test
	public void shouldAllowUpdateWhenBasedOnChanges() {
		// BasedOn can be replaced in UPDATE operations
		DiagnosticReport original = new DiagnosticReport();
		original.setSubject(new Reference("Patient/patient-123"));
		original.addBasedOn(new Reference("ServiceRequest/order-1"));
		
		DiagnosticReport updated = new DiagnosticReport();
		updated.setSubject(new Reference("Patient/patient-123"));
		updated.addBasedOn(new Reference("ServiceRequest/order-2"));
		
		// Should not throw exception - basedOn is mutable in UPDATE
		validator.validateUpdateChanges(original, updated);
	}
	
	@Test
	public void shouldAllowUpdateWhenResultsChange() {
		// Results can be replaced in UPDATE operations
		DiagnosticReport original = new DiagnosticReport();
		original.setSubject(new Reference("Patient/patient-123"));
		original.addResult(new Reference("Observation/obs-1"));
		
		DiagnosticReport updated = new DiagnosticReport();
		updated.setSubject(new Reference("Patient/patient-123"));
		updated.addResult(new Reference("Observation/obs-2"));
		
		// Should not throw exception - results are mutable in UPDATE
		validator.validateUpdateChanges(original, updated);
	}
	
	@Test
	public void shouldAllowUpdateWhenPerformersChange() {
		// Performers can be replaced in UPDATE operations
		DiagnosticReport original = new DiagnosticReport();
		original.setSubject(new Reference("Patient/patient-123"));
		original.addPerformer(new Reference("Practitioner/doc-1"));
		
		DiagnosticReport updated = new DiagnosticReport();
		updated.setSubject(new Reference("Patient/patient-123"));
		updated.addPerformer(new Reference("Practitioner/doc-2"));
		
		// Should not throw exception - performers are mutable in UPDATE
		validator.validateUpdateChanges(original, updated);
	}
	
	@Test
	public void shouldRejectUpdateWithNullOriginalReport() {
		DiagnosticReport updated = new DiagnosticReport();
		
		InvalidRequestException exception = assertThrows(InvalidRequestException.class,
				() -> validator.validateUpdateChanges(null, updated));
		
		assertTrue(exception.getMessage().contains("must not be null"));
	}
	
	@Test
	public void shouldRejectUpdateWithNullUpdatedReport() {
		DiagnosticReport original = new DiagnosticReport();
		
		InvalidRequestException exception = assertThrows(InvalidRequestException.class,
				() -> validator.validateUpdateChanges(original, null));
		
		assertTrue(exception.getMessage().contains("must not be null"));
	}
	
	@Test
	public void shouldAllowUpdatesToResultsBasedOnPerformers() {
		// Original report
		DiagnosticReport original = new DiagnosticReport();
		original.setSubject(new Reference("Patient/patient-123"));
		original.setEncounter(new Reference("Encounter/encounter-123"));
		original.setStatus(DiagnosticReport.DiagnosticReportStatus.PARTIAL);
		original.addBasedOn(new Reference("ServiceRequest/order-1"));
		original.addResult(new Reference("Observation/obs-1"));
		original.addPerformer(new Reference("Practitioner/doc-1"));
		
		// Updated report - same patient and encounter, everything else different
		DiagnosticReport updated = new DiagnosticReport();
		updated.setSubject(new Reference("Patient/patient-123")); // Same
		updated.setEncounter(new Reference("Encounter/encounter-123")); // Same
		updated.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL); // Different - OK
		updated.addBasedOn(new Reference("ServiceRequest/order-2")); // Different - OK
		updated.addBasedOn(new Reference("ServiceRequest/order-3")); // Additional - OK
		updated.addResult(new Reference("Observation/obs-2")); // Different - OK
		updated.addPerformer(new Reference("Practitioner/doc-2")); // Different - OK
		
		// Should not throw exception
		validator.validateNotInTerminalState(original);
		validator.validateUpdateChanges(original, updated);
	}
}
