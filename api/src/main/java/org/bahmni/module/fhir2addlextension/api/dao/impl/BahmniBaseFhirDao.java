package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.hibernate.Session;
import org.openmrs.Auditable;
import org.openmrs.Encounter;
import org.openmrs.OpenmrsObject;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import java.util.List;

public abstract class BahmniBaseFhirDao<T extends OpenmrsObject & Auditable> extends BaseFhirDao<T> {
	
	public ReferenceAndListParam getEncounterReferencesByNumberOfVisit(NumberParam numberOfVisitsParam,
	        ReferenceParam patientReferenceParam) {
		String patientUuid = patientReferenceParam.getIdPart();
		if (patientUuid == null || patientUuid.isEmpty()) {
			return null;
		}
		
		Session session = getSessionFactory().getCurrentSession();
		CriteriaBuilder cb = session.getCriteriaBuilder();
		
		CriteriaQuery<Integer> visitQuery = cb.createQuery(Integer.class);
		Root<Visit> visitRoot = visitQuery.from(Visit.class);
		Join<Visit, Patient> patientJoin = visitRoot.join("patient");
		visitQuery.select(visitRoot.get("visitId")).where(cb.equal(patientJoin.get("uuid"), patientUuid))
		        .orderBy(cb.desc(visitRoot.get("startDatetime")));
		
		List<Integer> lastNVisitIds = session.createQuery(visitQuery)
		        .setMaxResults(numberOfVisitsParam.getValue().intValue()).getResultList();
		
		if (lastNVisitIds.isEmpty()) {
			return null;
		}
		
		CriteriaQuery<String> encounterQuery = cb.createQuery(String.class);
		Root<Encounter> encounterRoot = encounterQuery.from(Encounter.class);
		Join<Encounter, Visit> visitJoin = encounterRoot.join("visit");
		encounterQuery.select(encounterRoot.<String> get("uuid")).where(visitJoin.get("visitId").in(lastNVisitIds));
		
		List<String> encounterUuids = session.createQuery(encounterQuery).getResultList();
		
		if (encounterUuids.isEmpty()) {
			return null;
		}
		
		ReferenceOrListParam encounterReferenceOrListParam = new ReferenceOrListParam();
		for (String encounterUuid : encounterUuids) {
			encounterReferenceOrListParam.add(new ReferenceParam(encounterUuid));
		}
		return new ReferenceAndListParam().addAnd(encounterReferenceOrListParam);
	}
}
