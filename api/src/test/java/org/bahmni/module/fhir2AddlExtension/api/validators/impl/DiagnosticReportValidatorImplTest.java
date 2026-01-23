package org.bahmni.module.fhir2AddlExtension.api.validators.impl;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirServiceRequestDao;
import org.bahmni.module.fhir2AddlExtension.api.validators.DiagnosticReportValidator;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Order;

import java.util.Collections;

import static org.bahmni.module.fhir2AddlExtension.api.validators.impl.DiagnosticReportValidatorImpl.RESULT_OR_ATTACHMENT_NOT_PRESENT_ERROR_MESSAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

@RunWith(MockitoJUnitRunner.class)
public class DiagnosticReportValidatorImplTest {
	
	@Mock
	private BahmniFhirServiceRequestDao<Order> serviceRequestDao;
	
	DiagnosticReportValidator diagnosticReportValidator;
	
	@Before
	public void setUp() {
		diagnosticReportValidator = new DiagnosticReportValidatorImpl(serviceRequestDao);
	}
	
	@Test
    public void shouldThrowExpectedExceptionWhenNeitherResultNorAttachmentIsPresent() {
        DiagnosticReport diagnosticReport = new DiagnosticReport();

        Exception exception = assertThrows(UnprocessableEntityException.class, () -> {
            diagnosticReportValidator.validate(diagnosticReport);
        });
        assertEquals(RESULT_OR_ATTACHMENT_NOT_PRESENT_ERROR_MESSAGE, exception.getMessage());

    }
	
	@Test
	public void shouldNotThrowExceptionWhenResultIsPresent() {
		DiagnosticReport diagnosticReport = new DiagnosticReport();
		Observation observation = new Observation();
		observation.setId("test");
		Reference reference = new Reference("#test");
		reference.setType("Observation");
		reference.setResource(observation);
		diagnosticReport.setResult(Collections.singletonList(reference));
		
		diagnosticReportValidator.validate(diagnosticReport);
	}
	
	@Test
	public void shouldNotThrowExceptionWhenReportIsPartial() {
		DiagnosticReport diagnosticReport = new DiagnosticReport();
		diagnosticReport.setStatus(DiagnosticReport.DiagnosticReportStatus.PARTIAL);
		diagnosticReportValidator.validate(diagnosticReport);
	}
	
	@Test
	public void shouldNotThrowExceptionWhenAttachmentsIsPresent() {
		DiagnosticReport diagnosticReport = new DiagnosticReport();
		diagnosticReport.setStatus(DiagnosticReport.DiagnosticReportStatus.PARTIAL);
		diagnosticReport.setPresentedForm(Collections.singletonList(new Attachment()));
		diagnosticReportValidator.validate(diagnosticReport);
	}
	
	@Test
	public void shouldNotThrowExceptionWhenReferencesHaveRespectiveResources() {
		DiagnosticReport diagnosticReport = new DiagnosticReport();
		Observation observation = new Observation();
		observation.setId("test");
		Reference reference = new Reference("#test");
		reference.setType("Observation");
		reference.setResource(observation);
		diagnosticReport.setResult(Collections.singletonList(reference));
		diagnosticReport.setPresentedForm(Collections.singletonList(new Attachment()));
		
		diagnosticReportValidator.validate(diagnosticReport);
	}
	
}
