package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirEpisodeOfCareDao;
import org.hibernate.Criteria;
import org.openmrs.module.episodes.Episode;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.stereotype.Component;

@Component
public class BahmniFhirEpisodeOfCareDaoImpl extends BaseFhirDao<Episode> implements BahmniFhirEpisodeOfCareDao {
	
	@Override
    protected void setupSearchParams(Criteria criteria, SearchParameterMap theParams) {
        super.setupSearchParams(criteria, theParams);
        theParams.getParameters().forEach(param -> {
            switch (param.getKey()) {
                case FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER :
                    param.getValue().forEach(patientReference -> handlePatientReference(criteria,
                            (ReferenceAndListParam) patientReference.getParam(), "patient"));
                    break;
                case FhirConstants.COMMON_SEARCH_HANDLER:
                    handleCommonSearchParameters(param.getValue()).ifPresent(criteria::add);
                    break;
            }
        });
    }
}
