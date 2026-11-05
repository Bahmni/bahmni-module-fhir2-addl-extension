package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirImagingStudyDao;
import org.bahmni.module.fhir2addlextension.api.model.FhirImagingStudy;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;
import org.openmrs.module.fhir2.api.dao.internals.OpenmrsFhirCriteriaContext;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.Join;

@Component
public class BahmniFhirImagingStudyDaoImpl extends BaseFhirDao<FhirImagingStudy> implements BahmniFhirImagingStudyDao {
	
	@Override
	protected <U> void setupSearchParams(OpenmrsFhirCriteriaContext<FhirImagingStudy, U> ctx, SearchParameterMap theParams) {
		super.setupSearchParams(ctx, theParams);
		theParams.getParameters().forEach(param -> {
			switch (param.getKey()) {
				case FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER:
					param.getValue().forEach(patientReference -> getSearchQueryHelper().handlePatientReference(ctx,
					    (ReferenceAndListParam) patientReference.getParam(), "subject"));
					break;
				case FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER:
					param.getValue().forEach(basedOnReference -> handleBasedOnReference(ctx,
					    (ReferenceAndListParam) basedOnReference.getParam()));
					break;
				case FhirConstants.COMMON_SEARCH_HANDLER:
					handleCommonSearchParameters(ctx, param.getValue()).ifPresent(ctx::addPredicate);
					break;
			}
		});
	}
	
	private <U> void handleBasedOnReference(OpenmrsFhirCriteriaContext<FhirImagingStudy, U> ctx,
	        ReferenceAndListParam basedOnReference) {
		if (basedOnReference == null) {
			return;
		}
		Join<?, ?> oJoin = ctx.getJoin("o").orElseGet(() -> ctx.addJoin("order", "o"));
		handleAndListParam(ctx.getCriteriaBuilder(), basedOnReference,
		    param -> getSearchQueryHelper().propertyLike(ctx, oJoin, "uuid", new StringParam(param.getValue())))
		        .ifPresent(ctx::addPredicate);
	}
}
