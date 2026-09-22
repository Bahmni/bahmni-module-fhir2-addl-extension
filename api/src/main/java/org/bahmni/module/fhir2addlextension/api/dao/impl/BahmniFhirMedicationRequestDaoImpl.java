package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2addlextension.api.dao.SystemOnlyCodeQuery;
import org.openmrs.DrugOrder;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.api.dao.impl.FhirMedicationRequestDaoImpl;
import org.openmrs.module.fhir2.api.dao.internals.OpenmrsFhirCriteriaContext;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.persistence.criteria.Join;
import java.util.Optional;

@Component
@Primary
public class BahmniFhirMedicationRequestDaoImpl extends FhirMedicationRequestDaoImpl {
	
	@Autowired
	private OrderService orderService;
	
	@Override
	public DrugOrder createOrUpdate(@Nonnull DrugOrder newEntry) {
		return (DrugOrder) orderService.saveOrder(newEntry, null);
	}
	
	@Override
	protected <U> Optional<javax.persistence.criteria.Predicate> handleCodedConcept(
	        OpenmrsFhirCriteriaContext<DrugOrder, U> ctx, TokenAndListParam code) {
		if (code == null) {
			return Optional.empty();
		}

		if (!SystemOnlyCodeQuery.hasSystemOnlyToken(code)) {
			return super.handleCodedConcept(ctx, code);
		}

		Join<?, ?> cJoin = ctx.getJoin("c").orElseGet(() -> ctx.addJoin("concept", "c"));
		Join<?, ?> cmJoin = ctx.getJoin("cm").orElseGet(() -> ctx.addJoin(cJoin, "conceptMappings", "cm"));
		Join<?, ?> crtJoin = ctx.getJoin("crt").orElseGet(() -> ctx.addJoin(cmJoin, "conceptReferenceTerm", "crt"));

		return handleAndListParam(ctx.getCriteriaBuilder(), code,
		    token -> SystemOnlyCodeQuery.tokenPredicate(ctx, getSearchQueryHelper(), crtJoin, "crt", token));
	}
	
	@Override
	protected <U> void setupSearchParams(OpenmrsFhirCriteriaContext<DrugOrder, U> ctx, SearchParameterMap theParams) {
		super.setupSearchParams(ctx, theParams);
		theParams.getParameters().forEach(entry -> {
			if (BahmniFhirConstants.ORDER_LOCATION_SEARCH_HANDLER.equals(entry.getKey())) {
				entry.getValue().forEach(
				    param -> handleLocationReference(ctx, (ReferenceAndListParam) param.getParam()));
			} else if (BahmniFhirConstants.VISIT_REFERENCE_SEARCH_HANDLER.equals(entry.getKey())) {
				entry.getValue().forEach(
				    param -> handleVisitReference(ctx, (ReferenceAndListParam) param.getParam()));
			}
		});
	}
	
	private <U> void handleLocationReference(OpenmrsFhirCriteriaContext<DrugOrder, U> ctx,
	        ReferenceAndListParam locationReference) {
		if (locationReference == null) {
			return;
		}
		Join<?, ?> eJoin = ctx.getJoin("e").orElseGet(() -> ctx.addJoin("encounter", "e"));
		Join<?, ?> lJoin = ctx.getJoin("l").orElseGet(() -> ctx.addJoin(eJoin, "location", "l"));
		handleAndListParam(ctx.getCriteriaBuilder(), locationReference,
		    token -> Optional.of(ctx.getCriteriaBuilder().equal(lJoin.get("uuid"), token.getValue())))
		        .ifPresent(ctx::addPredicate);
	}
	
	private <U> void handleVisitReference(OpenmrsFhirCriteriaContext<DrugOrder, U> ctx,
	        ReferenceAndListParam visitReference) {
		if (visitReference == null) {
			return;
		}
		Join<?, ?> eJoin = ctx.getJoin("e").orElseGet(() -> ctx.addJoin("encounter", "e"));
		Join<?, ?> vJoin = ctx.getJoin("v").orElseGet(() -> ctx.addJoin(eJoin, "visit", "v"));
		handleAndListParam(ctx.getCriteriaBuilder(), visitReference,
		    token -> Optional.of(ctx.getCriteriaBuilder().equal(vJoin.get("uuid"), token.getIdPart())))
		        .ifPresent(ctx::addPredicate);
	}
}
