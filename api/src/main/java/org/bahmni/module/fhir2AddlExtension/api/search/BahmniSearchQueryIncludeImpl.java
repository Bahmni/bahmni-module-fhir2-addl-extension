package org.bahmni.module.fhir2AddlExtension.api.search;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2AddlExtension.api.search.param.BahmniImagingStudySearchParams;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirEpisodeOfCareEncounterService;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirImagingStudyService;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.openmrs.annotation.OpenmrsProfile;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.SearchQueryIncludeImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@NoArgsConstructor
@Primary
@OpenmrsProfile(openmrsPlatformVersion = "2.* - 2.5.*")
public class BahmniSearchQueryIncludeImpl<U extends IBaseResource> extends SearchQueryIncludeImpl<U> {
	
	@Setter(onMethod_ = @Autowired)
	BahmniFhirEpisodeOfCareEncounterService episodeOfCareEncounterService;
	
	@Setter(onMethod_ = @Autowired)
	BahmniFhirImagingStudyService imagingStudyService;
	
	@Override
	protected IBundleProvider handleRevIncludeParam(Set<Include> includeSet, Set<Include> revIncludeSet,
	        ReferenceAndListParam referenceParams, Include revIncludeParam) {
		String paramName = revIncludeParam.getParamName();
		switch (paramName) {
			case BahmniFhirConstants.INCLUDE_EPISODE_OF_CARE_PARAM:
				return handleEpisodeOfCareRevInclude(referenceParams, revIncludeParam);
			case BahmniFhirConstants.INCLUDE_BASED_ON_PARAM:
				return handleBasedOnRevInclude(referenceParams, revIncludeParam);
			default:
				return super.handleRevIncludeParam(includeSet, revIncludeSet, referenceParams, revIncludeParam);
		}
	}
	
	private IBundleProvider handleBasedOnRevInclude(ReferenceAndListParam referenceParams, Include revIncludeParam) {
		String targetType = revIncludeParam.getParamType();
		switch (targetType) {
			case BahmniFhirConstants.IMAGING_STUDY:
				return imagingStudyService.searchImagingStudy(new BahmniImagingStudySearchParams(null, referenceParams,
				        null, null, null));
			default:
				return null;
		}
	}
	
	private IBundleProvider handleEpisodeOfCareRevInclude(ReferenceAndListParam referenceParams, Include revIncludeParam) {
		String targetType = revIncludeParam.getParamType();
		switch (targetType) {
			case FhirConstants.ENCOUNTER:
				return episodeOfCareEncounterService.encountersForEpisodes(referenceParams);
			default:
				return null;
		}
	}
}
