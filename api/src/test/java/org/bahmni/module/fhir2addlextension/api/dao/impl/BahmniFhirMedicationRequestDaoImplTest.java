package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2addlextension.api.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.DrugOrder;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.api.dao.impl.BaseDao;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;
import org.openmrs.module.fhir2.api.dao.internals.BaseFhirCriteriaHolder;
import org.openmrs.module.fhir2.api.dao.internals.FhirSearchQueryHelper;
import org.openmrs.module.fhir2.api.dao.internals.OpenmrsFhirCriteriaContext;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.test.util.ReflectionTestUtils;

import javax.persistence.criteria.Predicate;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirMedicationRequestDaoImplTest {
	
	private static final String VISIT_UUID = "visit-uuid-123";
	
	@Mock
	private OrderService orderService;
	
	private BahmniFhirMedicationRequestDaoImpl bahmniFhirMedicationRequestDao;
	
	private OpenmrsFhirCriteriaContext<DrugOrder, DrugOrder> ctx;
	
	@Before
	public void setup() {
		bahmniFhirMedicationRequestDao = new BahmniFhirMedicationRequestDaoImpl();
		org.hibernate.SessionFactory sf = mock(org.hibernate.SessionFactory.class);
		ReflectionTestUtils.setField(bahmniFhirMedicationRequestDao, BaseDao.class, "sessionFactory", sf,
		    org.hibernate.SessionFactory.class);
		ReflectionTestUtils.setField(bahmniFhirMedicationRequestDao, BaseFhirDao.class, "searchQueryHelper",
		    new FhirSearchQueryHelper(), FhirSearchQueryHelper.class);
		ReflectionTestUtils.setField(bahmniFhirMedicationRequestDao, "orderService", orderService, OrderService.class);
		ctx = TestUtils.mockCriteriaContext(DrugOrder.class);
	}
	
	@Test
	public void createOrUpdate_shouldSaveOrderWithNullPriorityAndReturnSavedOrder() {
		DrugOrder drugOrder = new DrugOrder();
		org.mockito.Mockito.when(orderService.saveOrder(drugOrder, null)).thenReturn(drugOrder);
		
		DrugOrder result = bahmniFhirMedicationRequestDao.createOrUpdate(drugOrder);
		
		assertThat(result, sameInstance(drugOrder));
		verify(orderService).saveOrder(drugOrder, null);
	}
	
	@Test
	public void setupSearchParams_shouldCreateVisitJoinWhenVisitParamPresent() {
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(BahmniFhirConstants.VISIT_REFERENCE_SEARCH_HANDLER, visitParams());
		
		bahmniFhirMedicationRequestDao.setupSearchParams(ctx, params);
		
		assertThat(ctx.getJoin("e").isPresent(), is(true));
		assertThat(ctx.getJoin("v").isPresent(), is(true));
	}
	
	@Test
	public void setupSearchParams_shouldNotCreateVisitJoinWithNullVisitReference() {
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(BahmniFhirConstants.VISIT_REFERENCE_SEARCH_HANDLER, null);
		
		bahmniFhirMedicationRequestDao.setupSearchParams(ctx, params);
		
		assertThat(ctx.getJoin("v").isPresent(), is(false));
	}
	
	@Test
	public void setupSearchParams_shouldAddPredicateForVisitReference() {
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(BahmniFhirConstants.VISIT_REFERENCE_SEARCH_HANDLER, visitParams());
		
		@SuppressWarnings("unchecked")
		List<Predicate> predicatesBefore = (List<Predicate>) ReflectionTestUtils.getField(ctx, BaseFhirCriteriaHolder.class,
		    "predicates");
		int countBefore = predicatesBefore != null ? predicatesBefore.size() : 0;
		
		bahmniFhirMedicationRequestDao.setupSearchParams(ctx, params);
		
		@SuppressWarnings("unchecked")
		List<Predicate> predicatesAfter = (List<Predicate>) ReflectionTestUtils.getField(ctx, BaseFhirCriteriaHolder.class,
		    "predicates");
		assertThat(predicatesAfter, notNullValue());
		assertThat(predicatesAfter.size() > countBefore, is(true));
	}
	
	@Test
	public void setupSearchParams_shouldMatchVisitUuidWhenTypedReferenceIsUsed() {
		ReferenceAndListParam visitRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Visit/visit-uuid-123")));
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(BahmniFhirConstants.VISIT_REFERENCE_SEARCH_HANDLER, visitRef);
		
		bahmniFhirMedicationRequestDao.setupSearchParams(ctx, params);
		
		assertThat(ctx.getJoin("v").isPresent(), is(true));
	}
	
	@Test
	public void setupSearchParams_shouldNotDuplicateAliasesWhenTheyAlreadyExist() {
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(BahmniFhirConstants.VISIT_REFERENCE_SEARCH_HANDLER, visitParams());
		
		bahmniFhirMedicationRequestDao.setupSearchParams(ctx, params);
		bahmniFhirMedicationRequestDao.setupSearchParams(ctx, params);
		
		assertThat(ctx.getJoin("v").isPresent(), is(true));
	}
	
	private ReferenceAndListParam visitParams() {
		return new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam("Visit", VISIT_UUID)));
	}
}
