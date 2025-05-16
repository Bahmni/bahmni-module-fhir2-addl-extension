package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Auditable;
import org.openmrs.Encounter;
import org.openmrs.OpenmrsObject;
import org.openmrs.Visit;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;

import java.util.List;

public abstract class BahmniBaseFhirDao<T extends OpenmrsObject & Auditable> extends BaseFhirDao<T> {
	
	public ReferenceAndListParam getEncounterReferencesByNumberOfVisit(NumberParam numberOfVisitsParam,
																	   ReferenceParam patientReferenceParam) {
		Session currentSession = super.getSessionFactory().getCurrentSession();
		
		Criteria visitCriteria = currentSession.createCriteria(Visit.class, "v");
		ReferenceAndListParam referenceAndListParam = new ReferenceAndListParam();
		referenceAndListParam.addAnd(new ReferenceOrListParam().add(patientReferenceParam));
		handlePatientReference(visitCriteria, referenceAndListParam);
		visitCriteria.addOrder(Order.desc("v.startDatetime"));
		visitCriteria.setMaxResults(numberOfVisitsParam.getValue().intValue());
		visitCriteria.setProjection(Projections.property("v.visitId"));
		
		List<Integer> lastNVisitIds = (List<Integer>) visitCriteria.list();
		Criteria encounterSubquery = currentSession.createCriteria(Encounter.class, "e").createAlias("e.visit", "v")
		        .add(Restrictions.in("v.visitId", lastNVisitIds)).setProjection(Projections.property("e.uuid"));
		List<String> encounterUUIDS = (List<String>) encounterSubquery.list();
		
		if (encounterUUIDS.isEmpty())
			return null;
		ReferenceOrListParam encounterReferenceOrListParam = new ReferenceOrListParam();
		for (String encounterUUID : encounterUUIDS) {
			encounterReferenceOrListParam.add(new ReferenceParam(encounterUUID));
		}
		return new ReferenceAndListParam().addAnd(encounterReferenceOrListParam);
		
	}
}
