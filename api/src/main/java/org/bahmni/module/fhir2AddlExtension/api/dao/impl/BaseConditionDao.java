package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.QuantityAndListParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.hibernate.Criteria;
import org.openmrs.Auditable;
import org.openmrs.OpenmrsObject;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

public abstract class BaseConditionDao<T extends OpenmrsObject & Auditable> extends BaseFhirDao<T> {
	
	@Override
    protected void setupSearchParams(Criteria criteria, SearchParameterMap theParams) {
        theParams.getParameters().forEach(entry -> {
            switch (entry.getKey()) {
                case FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER:
                    entry.getValue()
                            .forEach(param -> handlePatientReference(criteria, (ReferenceAndListParam) param.getParam()));
                    break;
                case FhirConstants.CODED_SEARCH_HANDLER:
                    entry.getValue().forEach(param -> handleCode(criteria, (TokenAndListParam) param.getParam()));
                    break;
                case FhirConstants.CONDITION_CLINICAL_STATUS_HANDLER:
                    entry.getValue().forEach(param -> handleClinicalStatus(criteria, (TokenAndListParam) param.getParam()));
                    break;
                case FhirConstants.DATE_RANGE_SEARCH_HANDLER:
                    entry.getValue()
                            .forEach(param -> handleDateRange(param.getPropertyName(), (DateRangeParam) param.getParam())
                                    .ifPresent(criteria::add));
                    break;
                case FhirConstants.QUANTITY_SEARCH_HANDLER:
                    entry.getValue().forEach(param -> handleOnsetAge(criteria, (QuantityAndListParam) param.getParam()));
                    break;
                case BahmniFhirConstants.CONDITION_VERIFICATION_STATUS_SEARCH_HANDLER:
                    entry.getValue().forEach(param -> {
                        handleVerificationStatus(criteria, (TokenAndListParam) param.getParam());
                    });
                    break;
                case FhirConstants.COMMON_SEARCH_HANDLER:
                    handleCommonSearchParameters(entry.getValue()).ifPresent(criteria::add);
                    break;
            }
        });
    }
	
	protected abstract void handleVerificationStatus(Criteria criteria, TokenAndListParam param);
	
	protected abstract void handleOnsetAge(Criteria criteria, QuantityAndListParam param);
	
	protected abstract void handleClinicalStatus(Criteria criteria, TokenAndListParam param);
	
	protected abstract void handleCode(Criteria criteria, TokenAndListParam param);
}
