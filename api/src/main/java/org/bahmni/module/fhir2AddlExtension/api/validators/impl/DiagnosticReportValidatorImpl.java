package org.bahmni.module.fhir2AddlExtension.api.validators.impl;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirServiceRequestDao;
import org.bahmni.module.fhir2AddlExtension.api.validators.DiagnosticReportValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.Order;
import org.openmrs.module.fhir2.FhirConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.openmrs.module.fhir2.api.translators.impl.ReferenceHandlingTranslator.getReferenceType;
import static org.openmrs.module.fhir2.api.util.FhirUtils.createExceptionErrorOperationOutcome;

/**
 * This class is a replacement of bahmni fhir2extension DiagnosticReportRequestValidator
 */
@Component
public class DiagnosticReportValidatorImpl implements DiagnosticReportValidator {
	
	private BahmniFhirServiceRequestDao<Order> serviceRequestDao;
	
	static final String INVALID_ORDER_ERROR_MESSAGE = "Given lab order is not prescribed by the doctor";
	
	static final String RESULT_OR_ATTACHMENT_NOT_PRESENT_ERROR_MESSAGE = "Given lab order does not have any test results or attachments";
	
	static final String RESOURCE_NOT_PRESENT_FOR_GIVEN_REFERENCE_ERROR_MESSAGE = "Given references does not have any matching resource";
	
	@Autowired
	public DiagnosticReportValidatorImpl(BahmniFhirServiceRequestDao<Order> serviceRequestDao) {
		this.serviceRequestDao = serviceRequestDao;
	}
	
	@Override
	public void validate(DiagnosticReport diagnosticReport) {
		validateBasedOn(diagnosticReport);
		validateEitherResultOrAttachmentIsPresent(diagnosticReport);
		validateReferencesHaveRespectiveResources(diagnosticReport);
	}
	
	//TODO: Lab lite sends reference as identifier for basedOn. Need to change that
	private void validateBasedOn(DiagnosticReport diagnosticReport) {
		/**
		 * This validation is different from fhir2Extension (to be replaced). There are many assumptions made there that are correct
		 * 	- i. Report code must be same as request code. This is not really the case
		 * 	- ii. A diagnostic report maybe submitted without an order. we do not want to restrict that
		 * 	- iii. Many diagnostic report may submit against a given order. e.g. for a panel the report code can for a specific test
		 * 	- iv. A report may be published after the fulfiller status has reached completed. We don't want the resources to determine workflow lifecycle
		 * 	- v. Report for voided order. Unlikely to happen. but dont think we should have to address it now. the clients can check order status first
		 */
        for (Reference basedOn : diagnosticReport.getBasedOn()) {
            if (getReferenceType(basedOn).map(ref -> !ref.equals(FhirConstants.SERVICE_REQUEST)).orElse(true)) {
                throw new IllegalArgumentException(
                        "Reference must be to an ServiceRequest not a " + getReferenceType(basedOn).orElse(""));
            }
        }
    }
	
	private void validateEitherResultOrAttachmentIsPresent(DiagnosticReport diagnosticReport) {
		if (!diagnosticReport.hasPresentedForm() && !diagnosticReport.hasResult()
		        && !DIAGNOSTIC_REPORT_DRAFT_STATES.contains(diagnosticReport.getStatus())) {
			throw new UnprocessableEntityException(RESULT_OR_ATTACHMENT_NOT_PRESENT_ERROR_MESSAGE,
			        createExceptionErrorOperationOutcome(RESULT_OR_ATTACHMENT_NOT_PRESENT_ERROR_MESSAGE));
		}
	}
	
	private void validateReferencesHaveRespectiveResources(DiagnosticReport diagnosticReport) {
        if (diagnosticReport.getResult().size() != 0) {
            diagnosticReport.getResult().forEach(reference -> {
                IBaseResource resource = reference.getResource();
                if (resource == null)
                    throw new UnprocessableEntityException(
                            RESOURCE_NOT_PRESENT_FOR_GIVEN_REFERENCE_ERROR_MESSAGE,
                            createExceptionErrorOperationOutcome(
                                    RESOURCE_NOT_PRESENT_FOR_GIVEN_REFERENCE_ERROR_MESSAGE));
            });
        }
    }
}
