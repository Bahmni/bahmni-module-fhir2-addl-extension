package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirRelatedPersonDao;
import org.openmrs.Person;
import org.openmrs.Relationship;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;
import org.openmrs.module.fhir2.api.dao.internals.OpenmrsFhirCriteriaContext;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Primary
public class BahmniFhirRelatedPersonDaoImpl extends BaseFhirDao<Relationship> implements BahmniFhirRelatedPersonDao {
	
	@Override
	protected <U> void setupSearchParams(OpenmrsFhirCriteriaContext<Relationship, U> ctx,
	        SearchParameterMap theParams) {
		super.setupSearchParams(ctx, theParams);
		theParams.getParameters().forEach(entry -> {
			switch (entry.getKey()) {
				case FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER:
					entry.getValue().forEach(
					    param -> handlePatientBothSides(ctx, (ReferenceAndListParam) param.getParam()));
					break;
				case FhirConstants.COMMON_SEARCH_HANDLER:
					handleCommonSearchParameters(ctx, entry.getValue()).ifPresent(ctx::addPredicate);
					break;
			}
		});
	}
	
	<U> void handlePatientBothSides(OpenmrsFhirCriteriaContext<Relationship, U> ctx,
	        ReferenceAndListParam patientRef) {
		CriteriaBuilder cb = ctx.getCriteriaBuilder();
		patientRef.getValuesAsQueryTokens().forEach(andParam -> {
			List<String> uuids = andParam.getValuesAsQueryTokens().stream()
			        .map(token -> token.getIdPart())
			        .filter(uuid -> uuid != null && !uuid.isEmpty())
			        .collect(Collectors.toList());

			if (!uuids.isEmpty()) {
				if (uuids.size() == 1) {
					ctx.addPredicate(buildPatientRestriction(ctx, uuids.get(0)));
				} else {
					Predicate[] ors = uuids.stream().map(uuid -> buildPatientRestriction(ctx, uuid))
					        .toArray(Predicate[]::new);
					ctx.addPredicate(cb.or(ors));
				}
			}
		});
	}
	
	private <U> Predicate buildPatientRestriction(OpenmrsFhirCriteriaContext<Relationship, U> ctx, String uuid) {
		CriteriaBuilder cb = ctx.getCriteriaBuilder();
		Root<Relationship> root = ctx.getRoot();
		
		Subquery<Integer> subqueryA = ctx.getCriteriaQuery().subquery(Integer.class);
		Root<Person> personRootA = subqueryA.from(Person.class);
		subqueryA.select(personRootA.<Integer> get("personId")).where(cb.equal(personRootA.get("uuid"), uuid));
		
		Subquery<Integer> subqueryB = ctx.getCriteriaQuery().subquery(Integer.class);
		Root<Person> personRootB = subqueryB.from(Person.class);
		subqueryB.select(personRootB.<Integer> get("personId")).where(cb.equal(personRootB.get("uuid"), uuid));
		
		Predicate personAMatch = root.get("personA").get("personId").in(subqueryA);
		Predicate personBMatch = root.get("personB").get("personId").in(subqueryB);
		return cb.or(personAMatch, personBMatch);
	}
}
