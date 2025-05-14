package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import static org.hibernate.criterion.Restrictions.and;
import static org.hibernate.criterion.Restrictions.or;

import java.util.Optional;
import java.util.stream.Stream;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.openmrs.Order;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.FhirServiceRequestDao;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class BahmniFhirServiceRequestDaoImpl extends BaseFhirDao<Order> implements FhirServiceRequestDao<Order> {
	
	@Override
	public boolean hasDistinctResults() {
		return false;
	}
	
	@Override
    protected void setupSearchParams(Criteria criteria, SearchParameterMap theParams) {
        theParams.getParameters().forEach(entry -> {
            switch (entry.getKey()) {
                case FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER:
                    entry.getValue().forEach(
                            param -> handleEncounterReference(criteria, (ReferenceAndListParam) param.getParam(), "e"));
                    break;
                case FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER:
                    entry.getValue().forEach(patientReference -> handlePatientReference(criteria,
                            (ReferenceAndListParam) patientReference.getParam(), "patient"));
                    break;
                case FhirConstants.CODED_SEARCH_HANDLER:
                    entry.getValue().forEach(code -> handleCodedConcept(criteria, (TokenAndListParam) code.getParam()));
                    break;
                case FhirConstants.PARTICIPANT_REFERENCE_SEARCH_HANDLER:
                    entry.getValue().forEach(participantReference -> handleProviderReference(criteria,
                            (ReferenceAndListParam) participantReference.getParam()));
                    break;
                case FhirConstants.DATE_RANGE_SEARCH_HANDLER:
                    entry.getValue().forEach(dateRangeParam -> handleDateRange((DateRangeParam) dateRangeParam.getParam())
                            .ifPresent(criteria::add));
                    break;
                case FhirConstants.COMMON_SEARCH_HANDLER:
                    handleCommonSearchParameters(entry.getValue()).ifPresent(criteria::add);
                    break;
            }
        });
    }
	
	private void handleCodedConcept(Criteria criteria, TokenAndListParam code) {
        if (code != null) {
            if (lacksAlias(criteria, "c")) {
                criteria.createAlias("concept", "c");
            }

            handleCodeableConcept(criteria, code, "c", "cm", "crt").ifPresent(criteria::add);
        }
    }
	
	private Optional<Criterion> handleDateRange(DateRangeParam dateRangeParam) {
		if (dateRangeParam == null) {
			return Optional.empty();
		}
		
		return Optional.of(and(toCriteriaArray(Stream.of(Optional.of(or(toCriteriaArray(Stream.of(
		    handleDate("scheduledDate", dateRangeParam.getLowerBound()),
		    handleDate("dateActivated", dateRangeParam.getLowerBound()))))), Optional.of(or(toCriteriaArray(Stream.of(
		    handleDate("dateStopped", dateRangeParam.getUpperBound()),
		    handleDate("autoExpireDate", dateRangeParam.getUpperBound())))))))));
	}
	
}
