package org.bahmni.module.fhir2AddlExtension.api.providers;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Patch;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.PatchTypeEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.bahmni.module.fhir2AddlExtension.api.context.RequestContextHolder;
import org.bahmni.module.fhir2AddlExtension.api.search.param.BahmniDiagnosticReportSearchParams;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirDiagnosticReportBundleService;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirDiagnosticReportService;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.providers.util.FhirProviderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.HashSet;

@Component("bahmniFhirDiagnosticReportR4Provider")
@R4Provider
@Slf4j
public class BahmniDiagnosticReportFhirR4Provider implements IResourceProvider {
	
	private BahmniFhirDiagnosticReportService diagnosticReportService;
	
	private BahmniFhirDiagnosticReportBundleService fhirDiagnosticReportBundleService;
	
	@Autowired
	public BahmniDiagnosticReportFhirR4Provider(
	    @Qualifier("bahmniFhirDiagnosticReportServiceImpl") BahmniFhirDiagnosticReportService diagnosticReportService,
	    BahmniFhirDiagnosticReportBundleService fhirDiagnosticReportBundleService) {
		this.diagnosticReportService = diagnosticReportService;
		this.fhirDiagnosticReportBundleService = fhirDiagnosticReportBundleService;
	}
	
	@Read
	public DiagnosticReport getDiagnosticReportById(@IdParam @Nonnull IdType id) {
		DiagnosticReport diagnosticReport = diagnosticReportService.get(id.getIdPart());
		if (diagnosticReport == null) {
			throw new ResourceNotFoundException("Could not find Diagnostic Report with Id " + id.getIdPart());
		}
		return diagnosticReport;
	}
	
	@Create
	public MethodOutcome createDiagnosticReport(@ResourceParam DiagnosticReport diagnosticReport) {
		DiagnosticReport resource = diagnosticReportService.create(diagnosticReport);
		return FhirProviderUtils.buildCreate(resource);
	}
	
	@Update
	public MethodOutcome updateDiagnosticReport(@IdParam IdType id, @ResourceParam DiagnosticReport diagnosticReport) {
		if (id == null || id.getIdPart() == null) {
			throw new InvalidRequestException("id must be specified to update");
		}
		
		return FhirProviderUtils.buildUpdate(diagnosticReportService.update(id.getIdPart(), diagnosticReport));
	}
	
	@Patch
	public MethodOutcome patchDiagnosticReport(@IdParam IdType id, PatchTypeEnum patchType, @ResourceParam String body,
	        RequestDetails requestDetails) {
		if (id == null || id.getIdPart() == null) {
			throw new InvalidRequestException("id must be specified to update DiagnosticReport resource");
		}
		
		DiagnosticReport diagnosticReport = diagnosticReportService.patch(id.getIdPart(), patchType, body, requestDetails);
		
		return FhirProviderUtils.buildPatch(diagnosticReport);
	}
	
	@Delete
	public OperationOutcome deleteDiagnosticReport(@IdParam @Nonnull IdType id) {
		diagnosticReportService.delete(id.getIdPart());
		return FhirProviderUtils.buildDeleteR4();
	}
	
	@Search
	public IBundleProvider searchForDiagnosticReports(
	        @OptionalParam(name = DiagnosticReport.SP_ENCOUNTER, chainWhitelist = { "" }, targetTypes = Encounter.class) ReferenceAndListParam encounterReference,
	        @OptionalParam(name = DiagnosticReport.SP_PATIENT, chainWhitelist = { "", Patient.SP_IDENTIFIER,
	                Patient.SP_GIVEN, Patient.SP_FAMILY, Patient.SP_NAME }, targetTypes = Patient.class) ReferenceAndListParam patientReference,
	        @OptionalParam(name = DiagnosticReport.SP_SUBJECT, chainWhitelist = { "", Patient.SP_IDENTIFIER,
	                Patient.SP_NAME, Patient.SP_GIVEN, Patient.SP_FAMILY }) ReferenceAndListParam subjectReference,
	        @OptionalParam(name = DiagnosticReport.SP_ISSUED) DateRangeParam issueDate,
	        @OptionalParam(name = DiagnosticReport.SP_CODE) TokenAndListParam code,
	        @OptionalParam(name = DiagnosticReport.SP_RESULT) ReferenceAndListParam result,
	        @OptionalParam(name = DiagnosticReport.SP_RES_ID) TokenAndListParam id,
	        @OptionalParam(name = DiagnosticReport.SP_BASED_ON, chainWhitelist = { "" }, targetTypes = ServiceRequest.class) ReferenceAndListParam basedOnReference,
	        @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated, @Sort SortSpec sort, @IncludeParam(allow = {
	                "DiagnosticReport:" + DiagnosticReport.SP_ENCOUNTER, "DiagnosticReport:" + DiagnosticReport.SP_PATIENT,
	                "DiagnosticReport:" + DiagnosticReport.SP_RESULT }) HashSet<Include> includes) {
		if (patientReference == null) {
			patientReference = subjectReference;
		}
		
		if (CollectionUtils.isEmpty(includes)) {
			includes = null;
		}
		
		return diagnosticReportService.searchForDiagnosticReports(new BahmniDiagnosticReportSearchParams(encounterReference,
		        patientReference, basedOnReference, issueDate, code, result, id, lastUpdated, sort, includes));
	}
	
	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return DiagnosticReport.class;
	}
	
	@Description(shortDefinition = "Retrieves a DiagnosticReport as Bundle", value = "This operation gathers the DiagnosticReport, all linked Observations, and the associated Encounter into a single Collection Bundle.")
	@Operation(name = "$fetch-bundle", idempotent = true, type = DiagnosticReport.class, returnParameters = { @OperationParam(name = "return", type = Bundle.class, min = 1, max = 1) })
	public Bundle fetchDiagnosticReportBundle(@IdParam @Nonnull IdType id, RequestDetails theRequestDetails) {
		RequestContextHolder.setValue(theRequestDetails.getFhirServerBase());
		return fhirDiagnosticReportBundleService.get(id.getIdPart());
	}
	
	@Description(shortDefinition = "Creates a DiagnosticReport as Bundle", value = "This operation creates a DiagnosticReport (equivalent of HTTP POST), with all linked Observations, sent as a single Collection Bundle.")
	@Operation(name = "$submit-bundle", type = DiagnosticReport.class, returnParameters = { @OperationParam(name = "return", type = Bundle.class, min = 1, max = 1) })
	public Bundle saveDiagnosticReportBundle(@OperationParam(name = "input", min = 1, max = 1) Bundle theInput,
	        RequestDetails requestDetails) {
		Bundle resource = (Bundle) theInput;
		RequestContextHolder.setValue(requestDetails.getFhirServerBase());
		Bundle reportBundle = fhirDiagnosticReportBundleService.create(resource);
		return reportBundle;
	}
	
	@Description(shortDefinition = "Updates a DiagnosticReport as Bundle", value = "This operation updates an existing DiagnosticReport (equivalent of HTTP PUT), with all linked Observations, sent as a single Collection Bundle.")
	@Operation(name = "$update-bundle", type = DiagnosticReport.class, returnParameters = { @OperationParam(name = "return", type = Bundle.class, min = 1, max = 1) })
	public Bundle updateDiagnosticReportBundle(@IdParam @Nonnull IdType id,
	        @OperationParam(name = "input", min = 1, max = 1) Bundle updateBundle, RequestDetails requestDetails) {
		RequestContextHolder.setValue(requestDetails.getFhirServerBase());
		Bundle resource = fhirDiagnosticReportBundleService.update(id.getIdPart(), updateBundle);
		return resource;
	}
}
