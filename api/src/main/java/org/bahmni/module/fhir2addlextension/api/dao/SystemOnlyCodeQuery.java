package org.bahmni.module.fhir2addlextension.api.dao;

import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.module.fhir2.api.dao.internals.FhirSearchQueryHelper;
import org.openmrs.module.fhir2.api.dao.internals.OpenmrsFhirCriteriaContext;
import org.openmrs.module.fhir2.api.dao.internals.OpenmrsFhirCriteriaSubquery;
import org.openmrs.module.fhir2.model.FhirConceptSource;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import java.util.Collections;
import java.util.Optional;

public final class SystemOnlyCodeQuery {
	
	private SystemOnlyCodeQuery() {
	}
	
	public static boolean isSystemOnly(TokenParam token) {
		return StringUtils.isNotEmpty(token.getSystem()) && StringUtils.isEmpty(token.getValue());
	}
	
	public static boolean hasSystemOnlyToken(TokenAndListParam code) {
		return code != null && code.getValuesAsQueryTokens().stream().flatMap(or -> or.getValuesAsQueryTokens().stream())
		        .anyMatch(SystemOnlyCodeQuery::isSystemOnly);
	}
	
	public static <T, U> Optional<Predicate> tokenPredicate(OpenmrsFhirCriteriaContext<T, U> ctx,
	        FhirSearchQueryHelper helper, Join<?, ?> referenceTermJoin, String referenceTermAlias, TokenParam token) {
		if (StringUtils.isEmpty(token.getSystem())) {
			return Optional.empty();
		}
		if (isSystemOnly(token)) {
			OpenmrsFhirCriteriaSubquery<FhirConceptSource, String> sourceQuery = ctx.addSubquery(FhirConceptSource.class,
			    String.class);
			sourceQuery.addPredicate(ctx.getCriteriaBuilder().equal(sourceQuery.getRoot().get("url"), token.getSystem()));
			sourceQuery.getSubquery().select(sourceQuery.getRoot().get("conceptSource"));
			return Optional.of(ctx.getCriteriaBuilder().in(referenceTermJoin.get("conceptSource"))
			        .value(sourceQuery.finalizeQuery()));
		}
		return helper.handleQueryForSystem(ctx, token.getSystem(), Collections.singletonList(token.getValue()),
		    referenceTermAlias);
	}
}
