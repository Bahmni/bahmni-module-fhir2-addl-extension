package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirTaskDao;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.impl.FhirTaskDaoImpl;
import org.openmrs.module.fhir2.api.dao.internals.OpenmrsFhirCriteriaContext;
import org.openmrs.module.fhir2.api.search.param.PropParam;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.model.FhirTask;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.Join;
import java.util.List;
import java.util.Optional;

@Component
@Primary
@Slf4j
public class BahmniFhirTaskDaoImpl extends FhirTaskDaoImpl implements BahmniFhirTaskDao {
	
	@Override
	protected <U> void setupSearchParams(OpenmrsFhirCriteriaContext<FhirTask, U> ctx, SearchParameterMap theParams) {
		List<? extends PropParam<?>> forReferenceParams = theParams.getParameters(FhirConstants.FOR_REFERENCE_SEARCH_HANDLER);

		SearchParameterMap paramsCopy = new SearchParameterMap();
		theParams.getParameters().forEach(entry -> {
			if (!FhirConstants.FOR_REFERENCE_SEARCH_HANDLER.equals(entry.getKey())) {
				entry.getValue().forEach(param -> paramsCopy.addParameter(entry.getKey(), param.getParam()));
			}
		});

		super.setupSearchParams(ctx, paramsCopy);

		if (forReferenceParams != null) {
			forReferenceParams.forEach(param -> handleForReference(ctx, (ReferenceAndListParam) param.getParam()));
		}

		theParams.getParameters().forEach(entry -> {
			switch (entry.getKey()) {
				case FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER:
					entry.getValue()
					        .forEach(param -> handleEncounterForTask(ctx, (ReferenceAndListParam) param.getParam()));
					break;
				case BahmniFhirConstants.NAME_SEARCH_HANDLER:
					entry.getValue().forEach(param -> handleName(ctx, (StringAndListParam) param.getParam()));
					break;
			}
		});
	}
	
	private <U> void handleEncounterForTask(OpenmrsFhirCriteriaContext<FhirTask, U> ctx,
	        ReferenceAndListParam encounterReference) {
		if (encounterReference == null) {
			return;
		}
		Join<?, ?> erJoin = ctx.getJoin("er").orElseGet(() -> ctx.addJoin("encounterReference", "er"));
		handleAndListParam(ctx.getCriteriaBuilder(), encounterReference,
		    ref -> ref.getIdPart() != null
		            ? Optional.of(ctx.getCriteriaBuilder().equal(erJoin.get("targetUuid"), ref.getIdPart()))
		            : Optional.empty()).ifPresent(ctx::addPredicate);
	}
	
	private <U> void handleName(OpenmrsFhirCriteriaContext<FhirTask, U> ctx, StringAndListParam name) {
		if (name == null) {
			return;
		}
		handleAndListParam(ctx.getCriteriaBuilder(), name,
		    param -> param.getValue() != null
		            ? Optional.of(ctx.getCriteriaBuilder().equal(ctx.getRoot().get("name"), param.getValue()))
		            : Optional.empty()).ifPresent(ctx::addPredicate);
	}
	
	/**
	 * Handles the forReference (subject) search parameter.
	 * 
	 * @note The forReference field stores Visit UUIDs from the write-side (fhir2Extension #81), not
	 *       Patient UUIDs. This method matches by targetUuid without type validation. Callers
	 *       should be aware that subject=Patient/{uuid} searches may return empty results if the
	 *       write-side stored a Visit UUID instead.
	 */
	private <U> void handleForReference(OpenmrsFhirCriteriaContext<FhirTask, U> ctx,
	        ReferenceAndListParam forReference) {
		if (forReference == null) {
			return;
		}
		Join<?, ?> frJoin = ctx.getJoin("fr").orElseGet(() -> ctx.addJoin("forReference", "fr"));
		handleAndListParam(ctx.getCriteriaBuilder(), forReference,
		    ref -> ref.getIdPart() != null
		            ? Optional.of(ctx.getCriteriaBuilder().equal(frJoin.get("targetUuid"), ref.getIdPart()))
		            : Optional.empty()).ifPresent(ctx::addPredicate);
	}
}
