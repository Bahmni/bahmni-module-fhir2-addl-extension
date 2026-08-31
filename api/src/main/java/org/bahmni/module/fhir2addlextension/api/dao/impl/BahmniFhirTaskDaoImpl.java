package org.bahmni.module.fhir2addlextension.api.dao.impl;

import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirTaskDao;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.impl.FhirTaskDaoImpl;
import org.openmrs.module.fhir2.api.search.param.PropParam;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.model.FhirTask;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@Slf4j
public class BahmniFhirTaskDaoImpl extends FhirTaskDaoImpl implements BahmniFhirTaskDao {
	
	@Override
	protected void setupSearchParams(Criteria criteria, SearchParameterMap theParams) {
		List<PropParam<?>> forReferenceParams = theParams.getParameters(FhirConstants.FOR_REFERENCE_SEARCH_HANDLER);

		SearchParameterMap paramsCopy = new SearchParameterMap();
		theParams.getParameters().forEach(entry -> {
			if (!FhirConstants.FOR_REFERENCE_SEARCH_HANDLER.equals(entry.getKey())) {
				entry.getValue().forEach(param -> paramsCopy.addParameter(entry.getKey(), param.getParam()));
			}
		});

		super.setupSearchParams(criteria, paramsCopy);

		if (forReferenceParams != null) {
			forReferenceParams.forEach(
			    param -> handleForReference(criteria, (ReferenceAndListParam) param.getParam()));
		}

		theParams.getParameters().forEach(entry -> {
			switch (entry.getKey()) {
				case FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER:
					entry.getValue().forEach(
					    param -> handleEncounterForTask(criteria, (ReferenceAndListParam) param.getParam()));
					break;
				case BahmniFhirConstants.NAME_SEARCH_HANDLER:
					entry.getValue().forEach(
					    param -> handleName(criteria, (StringAndListParam) param.getParam()));
					break;
			}
		});
	}
	
	private void handleEncounterForTask(Criteria criteria, ReferenceAndListParam encounterReference) {
		if (encounterReference == null) {
			return;
		}
		if (lacksAlias(criteria, "er")) {
			criteria.createAlias("encounterReference", "er");
		}
		handleAndListParam(encounterReference,
		    ref -> ref.getIdPart() != null ? Optional.of(Restrictions.eq("er.targetUuid", ref.getIdPart()))
		            : Optional.empty()).ifPresent(criteria::add);
	}
	
	private void handleName(Criteria criteria, StringAndListParam name) {
		if (name == null) {
			return;
		}
		handleAndListParam(name,
		    param -> param.getValue() != null ? Optional.of(Restrictions.eq("name", param.getValue()))
		            : Optional.empty()).ifPresent(criteria::add);
	}
	
	private void handleForReference(Criteria criteria, ReferenceAndListParam forReference) {
		if (forReference == null) {
			return;
		}
		if (lacksAlias(criteria, "fr")) {
			criteria.createAlias("forReference", "fr");
		}
		handleAndListParam(forReference,
		    ref -> ref.getIdPart() != null ? Optional.of(Restrictions.eq("fr.targetUuid", ref.getIdPart()))
		            : Optional.empty()).ifPresent(criteria::add);
	}
}
