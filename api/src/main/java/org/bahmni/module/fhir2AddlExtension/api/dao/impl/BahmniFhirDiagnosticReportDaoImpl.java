package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirDiagnosticReportDao;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDiagnosticReportExt;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.Order;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Optional;

import static org.hibernate.criterion.Restrictions.eq;

@Component("bahmniFhirDiagnosticReportDao")
public class BahmniFhirDiagnosticReportDaoImpl extends BaseFhirDao<FhirDiagnosticReportExt> implements BahmniFhirDiagnosticReportDao {
	
	@Override
    protected void setupSearchParams(Criteria criteria, SearchParameterMap theParams) {
        theParams.getParameters().forEach((entry) -> {
            switch (entry.getKey()) {
                case FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER:
                    entry.getValue().forEach((param) -> {
                        this.handleEncounterReference(criteria, (ReferenceAndListParam)param.getParam(), "e");
                    });
                    break;
                case FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER:
                    entry.getValue().forEach((param) -> {
                        this.handlePatientReference(criteria, (ReferenceAndListParam)param.getParam(), "subject");
                    });
                    break;
                case FhirConstants.CODED_SEARCH_HANDLER:
                    entry.getValue().forEach((param) -> {
                        this.handleCodedConcept(criteria, (TokenAndListParam)param.getParam());
                    });
                    break;
                case FhirConstants.DATE_RANGE_SEARCH_HANDLER:
                    entry.getValue().forEach((param) -> {
                        this.handleDateRange("issued", (DateRangeParam)param.getParam()).ifPresent(criteria::add);
                    });
                    break;
                case FhirConstants.RESULT_SEARCH_HANDLER:
                    entry.getValue().forEach((param) -> {
                        this.handleObservationReference(criteria, (ReferenceAndListParam)param.getParam());
                    });
                    break;
                case FhirConstants.COMMON_SEARCH_HANDLER:
                    this.handleCommonSearchParameters(entry.getValue()).ifPresent(criteria::add);
                    break;
                case FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER:
                    entry.getValue().forEach((param) -> {
                        this.handleBasedOnReference(criteria, (ReferenceAndListParam) param.getParam(), "orders");
                    });
                    break;
            }

        });
    }
	
	private void handleBasedOnReference(Criteria criteria, ReferenceAndListParam basedOnReference, String associationPath) {
        if (basedOnReference == null) {
            return;
        }

        if (lacksAlias(criteria, associationPath)) {
            criteria.createAlias(associationPath, "ro");
        }

        handleAndListParam(basedOnReference, token -> {
            return Optional.of(eq("ro.uuid", token.getIdPart()));
        }).ifPresent(criteria::add);
    }
	
	private void handleCodedConcept(Criteria criteria, TokenAndListParam code) {
        if (code != null) {
            if (this.lacksAlias(criteria, "c")) {
                criteria.createAlias("code", "c");
            }
            this.handleCodeableConcept(criteria, code, "c", "cm", "crt").ifPresent(criteria::add);
        }

    }
	
	private void handleObservationReference(Criteria criteria, ReferenceAndListParam result) {
        if (result != null) {
            if (this.lacksAlias(criteria, "obs")) {
                criteria.createAlias("results", "obs");
            }

            this.handleAndListParam(result, (token) -> {
                return Optional.of(Restrictions.eq("obs.uuid", token.getIdPart()));
            }).ifPresent(criteria::add);
        }

    }

	@Override
	public FhirDiagnosticReportExt findByOrder(@Nonnull Order order) {
		Criteria criteria = getSessionFactory().getCurrentSession().createCriteria(typeToken.getRawType());
		criteria.createAlias("orders", "o");
		criteria.add(Restrictions.eq("o.orderId", order.getOrderId()));
		criteria.add(Restrictions.eq("retired", false));

		return (FhirDiagnosticReportExt) criteria.uniqueResult();
	}
}
