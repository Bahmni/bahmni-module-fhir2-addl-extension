package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import ca.uhn.fhir.rest.param.QuantityAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import org.bahmni.module.fhir2AddlExtension.api.dao.FhirEncounterDiagnosisDao;
import org.hibernate.Criteria;
import org.openmrs.ConditionVerificationStatus;
import org.openmrs.Diagnosis;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static org.hibernate.criterion.Restrictions.eq;

@Component
public class FhirEncounterDiagnosisDaoImpl extends BaseConditionDao<Diagnosis> implements FhirEncounterDiagnosisDao {
	
	@Override
	protected void handleOnsetAge(Criteria criteria, QuantityAndListParam param) {
	}
	
	@Override
	protected void handleClinicalStatus(Criteria criteria, TokenAndListParam param) {
	}
	
	@Override
    protected void handleCode(Criteria criteria, TokenAndListParam code) {
        if (code != null) {
            criteria.createAlias("diagnosis.coded", "cd");
            handleCodeableConcept(criteria, code, "cd", "map", "term").ifPresent(criteria::add);
        }
    }
	
	@Override
    protected void handleVerificationStatus(Criteria criteria, TokenAndListParam status) {
        if (status != null) {
            handleAndListParam(status, tokenParam -> Optional.of(eq("certainty", convertVerificationStatus(tokenParam.getValue()))))
                    .ifPresent(criteria::add);
        }
    }
	
	private ConditionVerificationStatus convertVerificationStatus(String status) {
		switch (status) {
			case "provisional":
				return ConditionVerificationStatus.PROVISIONAL;
			case "confirmed":
				return ConditionVerificationStatus.CONFIRMED;
			default:
				return null;
		}
	}
}
