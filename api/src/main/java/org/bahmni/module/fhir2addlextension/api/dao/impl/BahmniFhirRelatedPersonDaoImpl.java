package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirRelatedPersonDao;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StandardBasicTypes;
import org.openmrs.Relationship;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Primary
public class BahmniFhirRelatedPersonDaoImpl extends BaseFhirDao<Relationship> implements BahmniFhirRelatedPersonDao {
	
	@Override
	protected void setupSearchParams(Criteria criteria, SearchParameterMap theParams) {
		super.setupSearchParams(criteria, theParams);
		theParams.getParameters().forEach(entry -> {
			switch (entry.getKey()) {
				case FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER:
					entry.getValue().forEach(
					    param -> handlePatientBothSides(criteria, (ReferenceAndListParam) param.getParam()));
					break;
				case FhirConstants.COMMON_SEARCH_HANDLER:
					handleCommonSearchParameters(entry.getValue()).ifPresent(criteria::add);
					break;
			}
		});
	}
	
	void handlePatientBothSides(Criteria criteria, ReferenceAndListParam patientRef) {
		patientRef.getValuesAsQueryTokens().forEach(andParam -> {
			List<String> uuids = andParam.getValuesAsQueryTokens().stream()
			        .map(token -> token.getIdPart())
			        .filter(uuid -> uuid != null && !uuid.isEmpty())
			        .collect(Collectors.toList());

			if (!uuids.isEmpty()) {
				if (uuids.size() == 1) {
					criteria.add(buildPatientRestriction(uuids.get(0)));
				} else {
					Criterion[] ors = uuids.stream()
					        .map(this::buildPatientRestriction)
					        .toArray(Criterion[]::new);
					criteria.add(Restrictions.or(ors));
				}
			}
		});
	}
	
	private Criterion buildPatientRestriction(String uuid) {
		return Restrictions.sqlRestriction("(this_.person_a in (select p.person_id from person p where p.uuid = ?) "
		        + "or this_.person_b in (select p.person_id from person p where p.uuid = ?))", new Object[] { uuid, uuid },
		    new org.hibernate.type.Type[] { StandardBasicTypes.STRING, StandardBasicTypes.STRING });
	}
}
