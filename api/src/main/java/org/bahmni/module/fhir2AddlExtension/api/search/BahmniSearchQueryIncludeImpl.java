package org.bahmni.module.fhir2AddlExtension.api.search;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
	
	private static final Log log = LogFactory.getLog(BahmniSearchQueryIncludeImpl.class);
	
	@Setter(onMethod_ = @Autowired)
	private BahmniFhirEpisodeOfCareEncounterService episodeOfCareEncounterService;
	
	@Setter(onMethod_ = @Autowired)
	private BahmniFhirImagingStudyService imagingStudyService;
	
	@Override
	protected IBundleProvider handleRevIncludeParam(Set<Include> includeSet, Set<Include> revIncludeSet,
	        ReferenceAndListParam referenceParams, Include revIncludeParam) {
		
		if (revIncludeParam == null) {
			log.warn("revIncludeParam is null, delegating to parent implementation");
			return super.handleRevIncludeParam(includeSet, revIncludeSet, referenceParams, null);
		}
		
		String resourceType = revIncludeParam.getParamType();
		
		IBundleProvider result = null;
		
		switch (resourceType) {
			case BahmniFhirConstants.IMAGING_STUDY:
				result = handleImagingStudyRevInclude(referenceParams, revIncludeParam);
				break;
			
			case FhirConstants.ENCOUNTER:
				result = handleEncounterRevInclude(referenceParams, revIncludeParam);
				break;
			
			default:
				return super.handleRevIncludeParam(includeSet, revIncludeSet, referenceParams, revIncludeParam);
		}
		
		if (result == null) {
			log.debug("No handler found for resourceType '" + resourceType + "' with paramName '"
			        + revIncludeParam.getParamName() + "', delegating to parent implementation");
			return super.handleRevIncludeParam(includeSet, revIncludeSet, referenceParams, revIncludeParam);
		}
		
		return result;
	}
	
	private IBundleProvider handleImagingStudyRevInclude(ReferenceAndListParam referenceParams, Include revIncludeParam) {
		String paramName = revIncludeParam.getParamName();
		
		switch (paramName) {
			case BahmniFhirConstants.INCLUDE_BASED_ON_PARAM:
				return imagingStudyService.searchImagingStudy(new BahmniImagingStudySearchParams(null, referenceParams,
				        null, null, null));
			default:
				log.debug("Unhandled paramName '" + paramName + "' for resourceType '" + revIncludeParam.getParamType()
				        + "'");
				return null;
		}
	}
	
	private IBundleProvider handleEncounterRevInclude(ReferenceAndListParam referenceParams, Include revIncludeParam) {
		String paramName = revIncludeParam.getParamName();
		
		switch (paramName) {
			case BahmniFhirConstants.INCLUDE_EPISODE_OF_CARE_PARAM:
				return episodeOfCareEncounterService.encountersForEpisodes(referenceParams);
			default:
				log.debug("Unhandled paramName '" + paramName + "' for resourceType '" + revIncludeParam.getParamType()
				        + "'");
				return null;
		}
	}
}
