package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirImagingStudyDao;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirImagingStudy;
import org.hibernate.Criteria;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.stereotype.Component;

@Component
public class BahmniFhirImagingStudyDaoImpl extends BaseFhirDao<FhirImagingStudy> implements BahmniFhirImagingStudyDao {
	
	@Override
	protected void setupSearchParams(Criteria criteria, SearchParameterMap theParams) {
		super.setupSearchParams(criteria, theParams);
		theParams.getParameters().forEach(param -> {
			switch (param.getKey()) {
				case FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER:
					param.getValue().forEach(patientReference -> handlePatientReference(criteria,
					    (ReferenceAndListParam) patientReference.getParam(), "subject"));
					break;
				case FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER:
					param.getValue().forEach(basedOnReference -> handleBasedOnReference(criteria,
					    (ReferenceAndListParam) basedOnReference.getParam()));
					break;
				case FhirConstants.COMMON_SEARCH_HANDLER:
					handleCommonSearchParameters(param.getValue()).ifPresent(criteria::add);
					break;
			}
		});
	}
	
	private void handleBasedOnReference(Criteria criteria, ReferenceAndListParam basedOnReference) {
		if (basedOnReference != null) {
			if (lacksAlias(criteria, "o")) {
				criteria.createAlias("order", "o");
			}
			handleAndListParam(basedOnReference, param -> propertyLike("o.uuid", param.getValue())).ifPresent(criteria::add);
		}
	}
}
