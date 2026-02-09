package org.bahmni.module.fhir2AddlExtension.api.providers;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Patch;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.PatchTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2AddlExtension.api.context.RequestContextHolder;
import org.bahmni.module.fhir2AddlExtension.api.domain.DiagnosticReportBundle;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirDiagnosticReportBundleService;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.openmrs.module.fhir2.providers.util.FhirProviderUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnull;

/**
 * FHIR Resource Provider for handling DiagnosticReportBundle resources. This provider supports
 * create, update, and patch operations on DiagnosticReportBundle resources. It uses the
 * BahmniFhirDiagnosticReportBundleService to perform the necessary business logic for these
 * operations. TODO: We would purge/delete this resource. This is kept just as reference for now to
 * understand how to handle bundles in FHIR providers. The create and update operations are
 * implemented as DiagnosticReport resource provider custom operations
 * {@link org.bahmni.module.fhir2AddlExtension.api.providers.BahmniDiagnosticReportFhirR4Provider#fetchDiagnosticReportBundle(IdType, RequestDetails)}
 * fetch-bundle operation}
 * {@link org.bahmni.module.fhir2AddlExtension.api.providers.BahmniDiagnosticReportFhirR4Provider#saveDiagnosticReportBundle(Bundle, RequestDetails)}
 * submit-bundle operation}
 * {@link org.bahmni.module.fhir2AddlExtension.api.providers.BahmniDiagnosticReportFhirR4Provider#updateDiagnosticReportBundle(IdType, Bundle, RequestDetails)}
 * update-bundle operation}
 */
//@Component("diagnosticReportBundleFhirR4ResourceProvider")
//@R4Provider
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
	
	//	@Read
	//	public DiagnosticReportBundle getDiagnosticReport(@IdParam @Nonnull IdType id, RequestDetails theRequestDetails) {
	//		RequestContextHolder.setValue(theRequestDetails.getFhirServerBase());
	//		return fhirDiagnosticReportBundleService.get(id.getIdPart());
	//	}
	
	@Create
	public MethodOutcome createDiagnosticReport(@ResourceParam DiagnosticReportBundle reportBundle,
	        RequestDetails requestDetails) {
		RequestContextHolder.setValue(requestDetails.getFhirServerBase());
		Bundle resource = fhirDiagnosticReportBundleService.create(reportBundle);
		return FhirProviderUtils.buildCreate(resource);
	}
	
	@Update
	public MethodOutcome updateDiagnosticReport(@IdParam @Nonnull IdType id,
	        @ResourceParam DiagnosticReportBundle reportBundle, RequestDetails requestDetails) {
		RequestContextHolder.setValue(requestDetails.getFhirServerBase());
		Bundle resource = fhirDiagnosticReportBundleService.update(id.getIdPart(), reportBundle);
		return FhirProviderUtils.buildUpdate(resource);
	}
	
	@Patch
	public MethodOutcome patchDiagnosticReport(@IdParam IdType id, PatchTypeEnum patchType, @ResourceParam String body,
	        RequestDetails requestDetails) {
		if (id == null || id.getIdPart() == null) {
			throw new InvalidRequestException("id must be specified to patch");
		}
		RequestContextHolder.setValue(requestDetails.getFhirServerBase());
		Bundle patchedReportBundle = fhirDiagnosticReportBundleService
		        .patch(id.getIdPart(), patchType, body, requestDetails);
		return FhirProviderUtils.buildUpdate(patchedReportBundle);
	}
	
}
