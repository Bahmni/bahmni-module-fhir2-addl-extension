package org.bahmni.module.fhir2AddlExtension.api.search;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirEpisodeOfCareEncounterService;
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
	
	@Override
	protected IBundleProvider handleRevIncludeParam(Set<Include> includeSet, Set<Include> revIncludeSet,
	        ReferenceAndListParam referenceParams, Include revIncludeParam) {
		if (!revIncludeParam.getParamName().equals(BahmniFhirConstants.INCLUDE_EPISODE_OF_CARE_PARAM)) {
			return super.handleRevIncludeParam(includeSet, revIncludeSet, referenceParams, revIncludeParam);
		}
		
		String targetType = revIncludeParam.getParamType();
		switch (targetType) {
			case FhirConstants.ENCOUNTER:
				return episodeOfCareEncounterService.encountersForEpisodes(referenceParams);
				
		}
		return null;
	}
}
