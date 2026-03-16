package org.bahmni.module.fhir2addlextension.api.translator.impl;

import org.bahmni.module.fhir2addlextension.api.context.RequestContextHolder;
import org.bahmni.module.fhir2addlextension.api.model.FhirDiagnosticReportExt;
import org.bahmni.module.fhir2addlextension.api.translator.BahmniFhirDiagnosticReportBundleTranslator;
import org.bahmni.module.fhir2addlextension.api.translator.BahmniFhirDiagnosticReportTranslator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Resource;
import org.openmrs.Obs;
import org.openmrs.Provider;
import org.openmrs.module.fhir2.api.translators.ObservationTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component
public class BahmniFhirDiagnosticReportBundleTranslatorImpl implements BahmniFhirDiagnosticReportBundleTranslator {
	
	private final BahmniFhirDiagnosticReportTranslator diagnosticReportTranslator;
	
	private final ObservationTranslator observationTranslator;
	
	private final PractitionerReferenceTranslator<Provider> providerReferenceTranslator;
	
	@Autowired
	public BahmniFhirDiagnosticReportBundleTranslatorImpl(BahmniFhirDiagnosticReportTranslator diagnosticReportTranslator,
	    ObservationTranslator observationTranslator, PractitionerReferenceTranslator<Provider> providerReferenceTranslator) {
		this.diagnosticReportTranslator = diagnosticReportTranslator;
		this.observationTranslator = observationTranslator;
		this.providerReferenceTranslator = providerReferenceTranslator;
	}
	
	@Override
	public Bundle toFhirResource(@Nonnull FhirDiagnosticReportExt report) {
		String fhirServerBase = RequestContextHolder.getValue();
		Bundle reportBundle = new Bundle();
		reportBundle.setId(report.getUuid());
		reportBundle.setMeta(new Meta());
		reportBundle.getMeta().setLastUpdated(report.getDateChanged());
		reportBundle.setType(Bundle.BundleType.COLLECTION);

		DiagnosticReport diagnosticReport = diagnosticReportTranslator.toFhirResource(report);

		Bundle.BundleEntryComponent reportEntry = new Bundle.BundleEntryComponent();
		reportEntry.setFullUrl(getFullUrlForEntry(diagnosticReport, fhirServerBase));
		reportEntry.setResource(diagnosticReport);
		reportBundle.addEntry(reportEntry);

		report.getResults().forEach(obs -> {
			if (obs.hasGroupMembers()) {
				obs.getGroupMembers().forEach(member -> reportBundle.addEntry(createObservationEntry(member, fhirServerBase)));
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
	
	private String getFullUrlForEntry(Resource resource, String fhirServerBase) {
		if (fhirServerBase != null && !fhirServerBase.isEmpty()) {
			return fhirServerBase.concat("/").concat(resource.getResourceType().name()).concat("/").concat(resource.getId());
		} else {
			return "urn:uuid:".concat(resource.getId());
		}
	}
	
	@Override
	public FhirDiagnosticReportExt toOpenmrsType(@Nonnull Bundle resource) {
		return null;
	}
	
	@Override
	public FhirDiagnosticReportExt toOpenmrsType(@Nonnull FhirDiagnosticReportExt existingObject, @Nonnull Bundle resource) {
		return null;
	}
}
