package org.bahmni.module.fhir2addlextension.api.dao.impl;

import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirRelatedPersonDao;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StandardBasicTypes;
import org.openmrs.Relationship;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;

@Component
@Primary
public class BahmniFhirRelatedPersonDaoImpl extends BaseFhirDao<Relationship> implements BahmniFhirRelatedPersonDao {
	
	@Override
	protected void setupSearchParams(Criteria criteria, SearchParameterMap theParams) {
		super.setupSearchParams(criteria, theParams);
		theParams.getParameters().forEach(entry -> {
			if (FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER.equals(entry.getKey())) {
				entry.getValue().forEach(param -> handlePatientBothSides(criteria,
				    (ReferenceAndListParam) param.getParam()));
			}
		});
	}
	
	private void handlePatientBothSides(Criteria criteria, ReferenceAndListParam patientRef) {
		patientRef.getValuesAsQueryTokens().forEach(andParam -> andParam.getValuesAsQueryTokens().forEach(token -> {
			String uuid = token.getIdPart();
			if (uuid != null && !uuid.isEmpty()) {
				criteria.add(Restrictions.sqlRestriction(
				    "(this_.person_a in (select p.person_id from person p where p.uuid = ?) "
				            + "or this_.person_b in (select p.person_id from person p where p.uuid = ?))",
				    new Object[] { uuid, uuid },
				    new org.hibernate.type.Type[] { StandardBasicTypes.STRING, StandardBasicTypes.STRING }));
			}
		}));
	}
}
