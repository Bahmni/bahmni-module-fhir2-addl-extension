package org.bahmni.module.fhir2AddlExtension.api.providers;

import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.bahmni.module.fhir2AddlExtension.api.search.param.BahmniImagingStudySearchParams;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirImagingStudyService;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ImagingStudy;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.providers.util.FhirProviderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
}
