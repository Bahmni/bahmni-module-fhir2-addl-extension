package org.bahmni.module.fhir2addlextension.api.providers;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.*;
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
import org.bahmni.module.fhir2addlextension.api.context.RequestContextHolder;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniImagingStudySearchParams;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirImagingStudyService;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ImagingStudy;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.providers.util.FhirProviderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component("bahmniImagingStudyFhirR4ResourceProvider")
@R4Provider
public class BahmniImagingStudyR4ResourceProvider implements IResourceProvider {
	
	private final BahmniFhirImagingStudyService fhirImagingStudyService;
	
	@Autowired
	public BahmniImagingStudyR4ResourceProvider(BahmniFhirImagingStudyService fhirImagingStudyService) {
		this.fhirImagingStudyService = fhirImagingStudyService;
	}
	
	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return ImagingStudy.class;
	}
	
	@Create
	public MethodOutcome createImagingStudy(@ResourceParam ImagingStudy imagingStudy) {
		ImagingStudy study = fhirImagingStudyService.create(imagingStudy);
		return FhirProviderUtils.buildCreate(study);
	}
	
	@Update
	public MethodOutcome updateImagingStudy(@IdParam IdType id, @ResourceParam ImagingStudy updatedStudy) {
		if (id == null || id.getIdPart() == null) {
			throw new InvalidRequestException("id must be specified to update");
		}
		updatedStudy.setId(id);
		return FhirProviderUtils.buildUpdate(fhirImagingStudyService.update(id.getIdPart(), updatedStudy));
	}
	
	@Patch
	public MethodOutcome patchImagingStudy(@IdParam IdType id, PatchTypeEnum patchType, @ResourceParam String body,
	        RequestDetails requestDetails) {
		if (id == null || id.getIdPart() == null) {
			throw new InvalidRequestException("id must be specified to patch");
		}
		ImagingStudy imagingStudy = fhirImagingStudyService.patch(id.getIdPart(), patchType, body, requestDetails);
		return FhirProviderUtils.buildPatch(imagingStudy);
	}
	
	@Read
	public ImagingStudy getImagingStudyByUuid(@IdParam IdType id) {
		ImagingStudy imagingStudy = fhirImagingStudyService.get(id.getIdPart());
		if (imagingStudy == null) {
			throw new ResourceNotFoundException("Could not find ImagingStudy with Id " + id.getIdPart());
		}
		return imagingStudy;
	}
	
	@Search
	public IBundleProvider searchImagingStudy(
	        @OptionalParam(name = ImagingStudy.SP_PATIENT, chainWhitelist = { "", Patient.SP_IDENTIFIER, Patient.SP_NAME,
	                Patient.SP_GIVEN, Patient.SP_FAMILY }, targetTypes = Patient.class) ReferenceAndListParam patientReference,
	        @OptionalParam(name = ImagingStudy.SP_BASEDON, chainWhitelist = { "" }) ReferenceAndListParam basedOnReference,
	        @OptionalParam(name = ImagingStudy.SP_RES_ID) TokenAndListParam id,
	        @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated, @Sort SortSpec sort) {
		BahmniImagingStudySearchParams searchParams = new BahmniImagingStudySearchParams(patientReference, basedOnReference,
		        id, lastUpdated, sort);
		return fhirImagingStudyService.searchImagingStudy(searchParams);
	}
	
	@Description(shortDefinition = "Submit quality assessment observations for ImagingStudy", value = "This operation submits quality assessment observations as contained resources within the ImagingStudy. "
	        + "The ImagingStudy should include contained Observation resources referenced by quality-observation extensions.")
	@Operation(name = "$submit-quality-assessment", idempotent = false, type = ImagingStudy.class, returnParameters = { @OperationParam(name = "return", type = ImagingStudy.class, min = 1, max = 1) })
	public ImagingStudy submitQualityAssessment(@IdParam @Nonnull IdType id,
	        @OperationParam(name = "input", min = 1, max = 1) ImagingStudy imagingStudy) {
		
		if (id == null || id.getIdPart() == null) {
			throw new InvalidRequestException("ImagingStudy ID must be specified");
		}
		
		imagingStudy.setId(id.getIdPart());
		return fhirImagingStudyService.submitQualityAssessment(imagingStudy);
	}
	
	@Description(shortDefinition = "Fetch ImagingStudy with quality assessment observations", value = "This operation retrieves an ImagingStudy with its quality assessment observations included as contained resources.")
	@Operation(name = "$fetch-quality-assessment", idempotent = true, type = ImagingStudy.class, returnParameters = { @OperationParam(name = "return", type = ImagingStudy.class, min = 1, max = 1) })
	public ImagingStudy fetchQualityAssessment(@IdParam @Nonnull IdType id) {
		if (id == null || id.getIdPart() == null) {
			throw new InvalidRequestException("ImagingStudy ID must be specified");
		}
		return fhirImagingStudyService.fetchWithQualityAssessment(id.getIdPart());
	}
}
