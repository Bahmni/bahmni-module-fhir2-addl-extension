package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirEpisodeOfCareDao;
import org.openmrs.module.episodes.Episode;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;
import org.openmrs.module.fhir2.api.dao.internals.OpenmrsFhirCriteriaContext;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.stereotype.Component;

@Component
public class BahmniFhirEpisodeOfCareDaoImpl extends BaseFhirDao<Episode> implements BahmniFhirEpisodeOfCareDao {
	
	@Override
	protected <U> void setupSearchParams(OpenmrsFhirCriteriaContext<Episode, U> ctx, SearchParameterMap theParams) {
		super.setupSearchParams(ctx, theParams);
		theParams.getParameters().forEach(param -> {
			switch (param.getKey()) {
				case FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER:
					param.getValue().forEach(patientReference -> getSearchQueryHelper().handlePatientReference(ctx,
					    (ReferenceAndListParam) patientReference.getParam(), "patient"));
					break;
				case FhirConstants.COMMON_SEARCH_HANDLER:
					handleCommonSearchParameters(ctx, param.getValue()).ifPresent(ctx::addPredicate);
					break;
			}
		});
	}
}
