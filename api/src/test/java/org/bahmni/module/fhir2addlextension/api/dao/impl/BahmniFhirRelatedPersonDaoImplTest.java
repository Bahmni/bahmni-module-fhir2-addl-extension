package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;

import java.util.List;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.bahmni.module.fhir2addlextension.api.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Relationship;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.impl.BaseDao;
import org.openmrs.module.fhir2.api.dao.internals.BaseFhirCriteriaHolder;
import org.openmrs.module.fhir2.api.dao.internals.OpenmrsFhirCriteriaContext;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.test.util.ReflectionTestUtils;

import javax.persistence.criteria.Predicate;

public class BahmniFhirRelatedPersonDaoImplTest {
	
	private BahmniFhirRelatedPersonDaoImpl dao;
	
	private OpenmrsFhirCriteriaContext<Relationship, Relationship> ctx;
	
	@Before
	public void setup() {
		dao = new BahmniFhirRelatedPersonDaoImpl();
		org.hibernate.SessionFactory sf = mock(org.hibernate.SessionFactory.class);
		ReflectionTestUtils.setField(dao, BaseDao.class, "sessionFactory", sf, org.hibernate.SessionFactory.class);
		ctx = TestUtils.mockCriteriaContext(Relationship.class);
	}
	
	@Test
	public void handlePatientBothSides_shouldAddPredicateForValidUuid() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		patientRef.addAnd(new ReferenceOrListParam().add(new ReferenceParam("patient-uuid-123")));
		
		@SuppressWarnings("unchecked")
		List<Predicate> before = (List<Predicate>) ReflectionTestUtils.getField(ctx, BaseFhirCriteriaHolder.class,
		    "predicates");
		int countBefore = before != null ? before.size() : 0;
		
		dao.handlePatientBothSides(ctx, patientRef);
		
		@SuppressWarnings("unchecked")
		List<Predicate> after = (List<Predicate>) ReflectionTestUtils.getField(ctx, BaseFhirCriteriaHolder.class,
		    "predicates");
		assertThat(after, notNullValue());
		assertThat(after.size() > countBefore, is(true));
	}
	
	@Test
	public void handlePatientBothSides_shouldSkipTokenWithNullUuid() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		patientRef.addAnd(new ReferenceOrListParam().add(new ReferenceParam((String) null)));
		
		@SuppressWarnings("unchecked")
		List<Predicate> before = (List<Predicate>) ReflectionTestUtils.getField(ctx, BaseFhirCriteriaHolder.class,
		    "predicates");
		int countBefore = before != null ? before.size() : 0;
		
		dao.handlePatientBothSides(ctx, patientRef);
		
		@SuppressWarnings("unchecked")
		List<Predicate> after = (List<Predicate>) ReflectionTestUtils.getField(ctx, BaseFhirCriteriaHolder.class,
		    "predicates");
		assertThat(after == null || after.size() == countBefore, is(true));
	}
	
	@Test
	public void handlePatientBothSides_shouldSkipTokenWithEmptyUuid() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		patientRef.addAnd(new ReferenceOrListParam().add(new ReferenceParam("")));
		
		@SuppressWarnings("unchecked")
		List<Predicate> before = (List<Predicate>) ReflectionTestUtils.getField(ctx, BaseFhirCriteriaHolder.class,
		    "predicates");
		int countBefore = before != null ? before.size() : 0;
		
		dao.handlePatientBothSides(ctx, patientRef);
		
		@SuppressWarnings("unchecked")
		List<Predicate> after = (List<Predicate>) ReflectionTestUtils.getField(ctx, BaseFhirCriteriaHolder.class,
		    "predicates");
		assertThat(after == null || after.size() == countBefore, is(true));
	}
	
	@Test
	public void handlePatientBothSides_shouldOrMultipleTokensInSameAndGroup() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		patientRef.addAnd(new ReferenceOrListParam().add(new ReferenceParam("uuid-1")).add(new ReferenceParam("uuid-2")));
		
		@SuppressWarnings("unchecked")
		List<Predicate> before = (List<Predicate>) ReflectionTestUtils.getField(ctx, BaseFhirCriteriaHolder.class,
		    "predicates");
		int countBefore = before != null ? before.size() : 0;
		
		dao.handlePatientBothSides(ctx, patientRef);
		
		@SuppressWarnings("unchecked")
		List<Predicate> after = (List<Predicate>) ReflectionTestUtils.getField(ctx, BaseFhirCriteriaHolder.class,
		    "predicates");
		// multiple UUIDs in one OR group → cb.or(ors) → one predicate added
		assertThat(after, notNullValue());
		assertThat(after.size(), is(countBefore + 1));
	}
	
	@Test
	public void handlePatientBothSides_shouldAddOneRestrictionPerAndGroup() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		patientRef.addAnd(new ReferenceOrListParam().add(new ReferenceParam("uuid-1")));
		patientRef.addAnd(new ReferenceOrListParam().add(new ReferenceParam("uuid-2")));
		
		@SuppressWarnings("unchecked")
		List<Predicate> before = (List<Predicate>) ReflectionTestUtils.getField(ctx, BaseFhirCriteriaHolder.class,
		    "predicates");
		int countBefore = before != null ? before.size() : 0;
		
		dao.handlePatientBothSides(ctx, patientRef);
		
		@SuppressWarnings("unchecked")
		List<Predicate> after = (List<Predicate>) ReflectionTestUtils.getField(ctx, BaseFhirCriteriaHolder.class,
		    "predicates");
		// two AND groups → two predicates added
		assertThat(after, notNullValue());
		assertThat(after.size(), is(countBefore + 2));
	}
	
	@Test
	public void setupSearchParams_withPatientReference_shouldAddPredicate() {
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		patientRef.addAnd(new ReferenceOrListParam().add(new ReferenceParam("patient-uuid-123")));
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, patientRef);
		
		dao.setupSearchParams(ctx, params);
		
		@SuppressWarnings("unchecked")
		List<Predicate> predicates = (List<Predicate>) ReflectionTestUtils.getField(ctx, BaseFhirCriteriaHolder.class,
		    "predicates");
		assertThat(predicates, notNullValue());
		assertThat(predicates.isEmpty(), is(false));
	}
	
	@Test
	public void daoIsInstantiable() {
		assertThat(dao, notNullValue());
	}
}
