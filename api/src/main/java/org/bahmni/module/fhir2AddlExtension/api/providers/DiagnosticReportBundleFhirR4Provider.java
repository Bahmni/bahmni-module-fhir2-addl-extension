package org.bahmni.module.fhir2AddlExtension.api.providers;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.IResourceProvider;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2AddlExtension.api.context.RequestContextHolder;
import org.bahmni.module.fhir2AddlExtension.api.domain.DiagnosticReportBundle;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirDiagnosticReportBundleService;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.providers.util.FhirProviderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component("diagnosticReportBundleFhirR4ResourceProvider")
@R4Provider
@Slf4j
public class DiagnosticReportBundleFhirR4Provider implements IResourceProvider {
	
	private BahmniFhirDiagnosticReportBundleService fhirDiagnosticReportBundleService;
	
	@Autowired
	public DiagnosticReportBundleFhirR4Provider(BahmniFhirDiagnosticReportBundleService fhirDiagnosticReportBundleService) {
		this.fhirDiagnosticReportBundleService = fhirDiagnosticReportBundleService;
	}
	
	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return DiagnosticReportBundle.class;
	}
	
	@Read
	public DiagnosticReportBundle getDiagnosticReport(@IdParam @Nonnull IdType id, RequestDetails theRequestDetails) {
		RequestContextHolder.setValue(theRequestDetails.getFhirServerBase());
		return fhirDiagnosticReportBundleService.get(id.getIdPart());
	}
	
	@Create
	public MethodOutcome createDiagnosticReport(@ResourceParam DiagnosticReportBundle reportBundle) {
		DiagnosticReportBundle resource = fhirDiagnosticReportBundleService.create(reportBundle);
		return FhirProviderUtils.buildCreate(resource);
	}
	
}
