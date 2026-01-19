package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2AddlExtension.api.model.Attachment;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDiagnosticReportExt;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniFhirDiagnosticReportTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniServiceRequestReferenceTranslator;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.openmrs.Order;
import org.openmrs.Provider;
import org.openmrs.module.fhir2.api.translators.DiagnosticReportTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class BahmniFhirDiagnosticReportTranslatorImpl implements BahmniFhirDiagnosticReportTranslator {
	
	private static final String INVALID_PERFORMER_REFERENCE = "Invalid performer for Diagnostic Report";
	
	//using openmrs fhir diagnostic report translator to delegate calls.
	private DiagnosticReportTranslator diagnosticReportTranslator;
	
	private BahmniServiceRequestReferenceTranslator serviceRequestReferenceTranslator;
	
	private PractitionerReferenceTranslator<Provider> providerReferenceTranslator;
	
	@Autowired
	public BahmniFhirDiagnosticReportTranslatorImpl(DiagnosticReportTranslator diagnosticReportTranslator,
	    BahmniServiceRequestReferenceTranslator serviceRequestReferenceTranslator,
	    PractitionerReferenceTranslator<Provider> providerReferenceTranslator) {
		this.diagnosticReportTranslator = diagnosticReportTranslator;
		this.serviceRequestReferenceTranslator = serviceRequestReferenceTranslator;
		this.providerReferenceTranslator = providerReferenceTranslator;
	}
	
	@Override
    public DiagnosticReport toFhirResource(@Nonnull FhirDiagnosticReportExt fhirDiagnosticReportExt) {
        DiagnosticReport diagnosticReport = diagnosticReportTranslator.toFhirResource(fhirDiagnosticReportExt);
        diagnosticReport.setConclusion(fhirDiagnosticReportExt.getConclusion());
        if (fhirDiagnosticReportExt.getOrders() != null) {
            fhirDiagnosticReportExt.getOrders()
                .forEach(order -> diagnosticReport.addBasedOn(serviceRequestReferenceTranslator.toFhirResource(order)));
        }

        if (fhirDiagnosticReportExt.getPresentedForms() != null) {
            fhirDiagnosticReportExt.getPresentedForms().forEach(pf -> {
                org.hl7.fhir.r4.model.Attachment attachment = new org.hl7.fhir.r4.model.Attachment();
                attachment.setId(pf.getUuid());
                attachment.setContentType(pf.getContentType());
                attachment.setUrl((pf.getContentUrl()));
                attachment.setTitle(pf.getTitle());
                attachment.setCreation(pf.getDateCreated());
                diagnosticReport.addPresentedForm(attachment);
            });
        }
        return diagnosticReport;
    }
	
	@Override
    public FhirDiagnosticReportExt toOpenmrsType(@Nonnull DiagnosticReport resource) {
        FhirDiagnosticReportExt newObject = new FhirDiagnosticReportExt();
        newObject = toOpenmrsType(newObject, resource);
        if (!newObject.getResults().isEmpty()) {
            //the openmrs fhir diagnostic report translator creates null entries onto the result set and does not filter the null obs
            newObject.setResults(newObject.getResults().stream().filter(Objects::nonNull).collect(Collectors.toSet()));
        }
        return newObject;
    }
	
	@Override
    public FhirDiagnosticReportExt toOpenmrsType(@Nonnull FhirDiagnosticReportExt existingObject, @Nonnull DiagnosticReport resource) {
        FhirDiagnosticReportExt reportExt = (FhirDiagnosticReportExt) diagnosticReportTranslator.toOpenmrsType(existingObject, resource);
        if (resource.hasConclusion()) {
            existingObject.setConclusion(resource.getConclusion());
        }
        if (resource.hasBasedOn()) {
            Set<Order> orders = new HashSet<>();
            resource.getBasedOn().forEach(reference -> {
                Order aOrder = serviceRequestReferenceTranslator.toOpenmrsType(reference);
                if (aOrder == null) {
                    throw new InvalidRequestException("Invalid Service Request Reference for Diagnostic Report");
                }
                orders.add(aOrder);
            });
            existingObject.setOrders(orders);
        }

        if (resource.hasPerformer()) {
            resource.getPerformer().forEach(reference -> {
                Provider performer = providerReferenceTranslator.toOpenmrsType(reference);
                if (performer == null) {
                    log.error(INVALID_PERFORMER_REFERENCE);
                    throw new InvalidRequestException(INVALID_PERFORMER_REFERENCE);
                }
                existingObject.getPerformers().add(performer);
            });
        }

        if (resource.hasPresentedForm()) {
            reportExt.setPresentedForms(
                resource.getPresentedForm().stream().map( pf -> {
                    Attachment attachment = new Attachment();
                    attachment.setContentType(pf.getContentType());
                    attachment.setContentUrl(pf.getUrl());
                    attachment.setTitle(pf.getTitle());
                    return attachment;
                }).collect(Collectors.toSet()));
        }
        return reportExt;
    }
}
