package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirDiagnosticReportDao;
import org.bahmni.module.fhir2addlextension.api.model.FhirDiagnosticReportExt;
import org.openmrs.Order;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;
import org.openmrs.module.fhir2.api.dao.internals.OpenmrsFhirCriteriaContext;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import java.util.Optional;

@Component("bahmniFhirDiagnosticReportDao")
public class BahmniFhirDiagnosticReportDaoImpl extends BaseFhirDao<FhirDiagnosticReportExt> implements BahmniFhirDiagnosticReportDao {
	
	@Override
	protected <U> void setupSearchParams(OpenmrsFhirCriteriaContext<FhirDiagnosticReportExt, U> ctx,
	        SearchParameterMap theParams) {
		theParams.getParameters().forEach(entry -> {
			switch (entry.getKey()) {
				case FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER:
					entry.getValue().forEach(param -> getSearchQueryHelper().handleEncounterReference(ctx,
					    (ReferenceAndListParam) param.getParam(), "e"));
					break;
				case FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER:
					entry.getValue().forEach(param -> getSearchQueryHelper().handlePatientReference(ctx,
					    (ReferenceAndListParam) param.getParam(), "subject"));
					break;
				case FhirConstants.CODED_SEARCH_HANDLER:
					entry.getValue().forEach(param -> handleCodedConcept(ctx, (TokenAndListParam) param.getParam()));
					break;
				case FhirConstants.DATE_RANGE_SEARCH_HANDLER:
					entry.getValue().forEach(param -> getSearchQueryHelper()
					        .handleDateRange(ctx, "issued", (DateRangeParam) param.getParam())
					        .ifPresent(ctx::addPredicate));
					break;
				case FhirConstants.RESULT_SEARCH_HANDLER:
					entry.getValue()
					        .forEach(param -> handleObservationReference(ctx, (ReferenceAndListParam) param.getParam()));
					break;
				case FhirConstants.COMMON_SEARCH_HANDLER:
					handleCommonSearchParameters(ctx, entry.getValue()).ifPresent(ctx::addPredicate);
					break;
				case FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER:
					entry.getValue()
					        .forEach(param -> handleBasedOnReference(ctx, (ReferenceAndListParam) param.getParam()));
					break;
			}
		});
	}
	
	private <U> void handleBasedOnReference(OpenmrsFhirCriteriaContext<FhirDiagnosticReportExt, U> ctx,
	        ReferenceAndListParam basedOnReference) {
		if (basedOnReference == null) {
			return;
		}
		Join<?, ?> roJoin = ctx.getJoin("ro").orElseGet(() -> ctx.addJoin("orders", "ro"));
		handleAndListParam(ctx.getCriteriaBuilder(), basedOnReference,
		    token -> Optional.of(ctx.getCriteriaBuilder().equal(roJoin.get("uuid"), token.getIdPart())))
		        .ifPresent(ctx::addPredicate);
	}
	
	private <U> void handleCodedConcept(OpenmrsFhirCriteriaContext<FhirDiagnosticReportExt, U> ctx,
	        TokenAndListParam code) {
		if (code == null) {
			return;
		}
		Join<?, ?> cJoin = ctx.getJoin("c").orElseGet(() -> ctx.addJoin("code", "c"));
		getSearchQueryHelper().handleCodeableConcept(ctx, code, cJoin, "cm", "crt").ifPresent(ctx::addPredicate);
	}
	
	private <U> void handleObservationReference(OpenmrsFhirCriteriaContext<FhirDiagnosticReportExt, U> ctx,
	        ReferenceAndListParam result) {
		if (result == null) {
			return;
		}
		Join<?, ?> obsJoin = ctx.getJoin("obs").orElseGet(() -> ctx.addJoin("results", "obs"));
		handleAndListParam(ctx.getCriteriaBuilder(), result,
		    token -> Optional.of(ctx.getCriteriaBuilder().equal(obsJoin.get("uuid"), token.getIdPart())))
		        .ifPresent(ctx::addPredicate);
	}
	
	@Override
	public FhirDiagnosticReportExt findByOrderUuid(@Nonnull String orderUuid) {
		org.hibernate.Session currentSession = getSessionFactory().getCurrentSession();
		CriteriaBuilder cb = currentSession.getCriteriaBuilder();
		CriteriaQuery<FhirDiagnosticReportExt> cq = cb.createQuery(FhirDiagnosticReportExt.class);
		Root<FhirDiagnosticReportExt> root = cq.from(FhirDiagnosticReportExt.class);
		Join<FhirDiagnosticReportExt, Order> ordersJoin = root.join("orders");
		cq.select(root).where(cb.equal(ordersJoin.get("uuid"), orderUuid), cb.equal(root.get("voided"), false));
		FhirDiagnosticReportExt result = currentSession.createQuery(cq).uniqueResult();
		return result == null ? null : deproxyResult(result);
	}
}
