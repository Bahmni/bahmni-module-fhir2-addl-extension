package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.context.RequestContextHolder;
import org.bahmni.module.fhir2AddlExtension.api.domain.DiagnosticReportBundle;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDiagnosticReportExt;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniFhirDiagnosticReportBundleTranslator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.openmrs.DrugOrder;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Provider;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.translators.DiagnosticReportTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component
public class BahmniFhirDiagnosticReportBundleTranslatorImpl implements BahmniFhirDiagnosticReportBundleTranslator {
	
	private final DiagnosticReportTranslator diagnosticReportTranslator;
	
	private final ObservationTranslator observationTranslator;
	
	private final PractitionerReferenceTranslator<Provider> providerReferenceTranslator;
	
	@Autowired
	public BahmniFhirDiagnosticReportBundleTranslatorImpl(DiagnosticReportTranslator diagnosticReportTranslator,
	    ObservationTranslator observationTranslator, PractitionerReferenceTranslator<Provider> providerReferenceTranslator) {
		this.diagnosticReportTranslator = diagnosticReportTranslator;
		this.observationTranslator = observationTranslator;
		this.providerReferenceTranslator = providerReferenceTranslator;
	}
	
	@Override
	public DiagnosticReportBundle toFhirResource(@Nonnull FhirDiagnosticReportExt report) {
		String fhirServerBase = RequestContextHolder.getValue();
		DiagnosticReportBundle reportBundle = new DiagnosticReportBundle();
		DiagnosticReport diagnosticReport = diagnosticReportTranslator.toFhirResource(report);
		report.getOrders().forEach(order -> diagnosticReport.addBasedOn(getOrderReference(order)));

		report.getPerformers().forEach(provider -> diagnosticReport.addPerformer(providerReferenceTranslator.toFhirResource(provider)));

		Bundle.BundleEntryComponent reportEntry = new Bundle.BundleEntryComponent();
		reportEntry.setFullUrl(getFullUrlForEntry(diagnosticReport, fhirServerBase));
		reportEntry.setResource(diagnosticReport);
		reportBundle.addEntry(reportEntry);

		report.getResults().forEach(obs -> {
			if (obs.hasGroupMembers()) {
				obs.getGroupMembers().forEach(member -> {
					reportBundle.addEntry(createObservationEntry(member, fhirServerBase));
				});
			}
			reportBundle.addEntry(createObservationEntry(obs, fhirServerBase));
		});
		return reportBundle;
	}
	
	private Bundle.BundleEntryComponent createObservationEntry(Obs obs, String fhirServerBase) {
		Bundle.BundleEntryComponent resultEntry = new Bundle.BundleEntryComponent();
		Observation observation = observationTranslator.toFhirResource(obs);
		resultEntry.setFullUrl(getFullUrlForEntry(observation, fhirServerBase));
		resultEntry.setResource(observation);
		return resultEntry;
	}
	
	private Reference getOrderReference(Order order) {
		if (order == null) {
			return null;
		}
		Reference reference = new Reference();
		if (order instanceof DrugOrder) {
			reference.setReference(FhirConstants.MEDICATION_REQUEST + "/" + order.getUuid());
			reference.setType(FhirConstants.MEDICATION_REQUEST);
		} else {
			reference.setReference(FhirConstants.SERVICE_REQUEST + "/" + order.getUuid());
			reference.setType(FhirConstants.SERVICE_REQUEST);
		}
		return reference;
	}
	
	private String getFullUrlForEntry(Resource resource, String fhirServerBase) {
		String serverBase = fhirServerBase != null && !fhirServerBase.isEmpty() ? fhirServerBase.concat("/") : "urn:uuid:";
		return serverBase.concat(resource.getResourceType().name()).concat("/").concat(resource.getId());
	}
	
	private Reference getReference(String url, String type) {
		Reference reference = new Reference(url);
		reference.setType(type);
		return reference;
	}
	
	@Override
	public FhirDiagnosticReportExt toOpenmrsType(@Nonnull DiagnosticReportBundle resource) {
		return null;
	}
	
	@Override
	public FhirDiagnosticReportExt toOpenmrsType(@Nonnull FhirDiagnosticReportExt existingObject,
	        @Nonnull DiagnosticReportBundle resource) {
		return null;
	}
}
