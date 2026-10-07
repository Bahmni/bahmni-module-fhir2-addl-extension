package org.bahmni.module.fhir2addlextension.api.dao.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.hibernate.internal.CriteriaImpl;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.DrugOrder;
import org.openmrs.api.OrderService;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirMedicationRequestDaoImplTest {
	
	@Mock
	private OrderService orderService;
	
	@InjectMocks
	private BahmniFhirMedicationRequestDaoImpl bahmniFhirMedicationRequestDao;
	
	@Before
	public void setup() throws Exception {
		Field orderServiceField = BahmniFhirMedicationRequestDaoImpl.class.getDeclaredField("orderService");
		orderServiceField.setAccessible(true);
		orderServiceField.set(bahmniFhirMedicationRequestDao, orderService);
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
	public void setupSearchParams_shouldAddVisitUuidRestriction() {
		Criteria criteria = mock(Criteria.class);
		SearchParameterMap params = new SearchParameterMap();

		ReferenceAndListParam visitRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Visit", "visit-uuid-123")));
		params.addParameter(BahmniFhirConstants.VISIT_REFERENCE_SEARCH_HANDLER, visitRef);

		bahmniFhirMedicationRequestDao.setupSearchParams(criteria, params);

		ArgumentCaptor<Criterion> captor = ArgumentCaptor.forClass(Criterion.class);
		verify(criteria, atLeastOnce()).add(captor.capture());
		assertThat(captor.getAllValues().stream().anyMatch(c -> c.toString().contains("v.uuid")), is(true));
	}
	
	@Test
	public void setupSearchParams_shouldNotFailWithNullVisitReference() {
		Criteria criteria = mock(Criteria.class);
		SearchParameterMap params = new SearchParameterMap();
		
		params.addParameter(BahmniFhirConstants.VISIT_REFERENCE_SEARCH_HANDLER, null);
		
		bahmniFhirMedicationRequestDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	@Test
	public void setupSearchParams_shouldAddVisitAliasesAndUuidRestriction() {
		CriteriaImpl criteria = new CriteriaImpl(DrugOrder.class.getName(), null);
		
		bahmniFhirMedicationRequestDao.setupSearchParams(criteria, visitParams("Visit", "visit-uuid-123"));
		
		assertThat(aliasPaths(criteria), is(Arrays.asList("encounter=e", "e.visit=v")));
		assertThat(visitRestrictions(criteria), is(Collections.singletonList("((v.uuid=visit-uuid-123))")));
	}
	
	@Test
	public void setupSearchParams_shouldMatchVisitUuidWhenTypedReferenceIsUsed() {
		CriteriaImpl criteria = new CriteriaImpl(DrugOrder.class.getName(), null);
		SearchParameterMap params = new SearchParameterMap();
		ReferenceAndListParam visitRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Visit/visit-uuid-123")));
		params.addParameter(BahmniFhirConstants.VISIT_REFERENCE_SEARCH_HANDLER, visitRef);
		
		bahmniFhirMedicationRequestDao.setupSearchParams(criteria, params);
		
		assertThat(visitRestrictions(criteria), is(Collections.singletonList("((v.uuid=visit-uuid-123))")));
	}
	
	@Test
	public void setupSearchParams_shouldNotDuplicateAliasesWhenTheyAlreadyExist() {
		CriteriaImpl criteria = new CriteriaImpl(DrugOrder.class.getName(), null);
		criteria.createAlias("encounter", "e");
		criteria.createAlias("e.visit", "v");
		
		bahmniFhirMedicationRequestDao.setupSearchParams(criteria, visitParams("Visit", "visit-uuid-123"));
		
		assertThat(aliasPaths(criteria), is(Arrays.asList("encounter=e", "e.visit=v")));
		assertThat(visitRestrictions(criteria), is(Collections.singletonList("((v.uuid=visit-uuid-123))")));
	}
	
	@Test
	public void setupSearchParams_shouldNotCreateVisitAliasesOrRestrictionWithNullVisitReference() {
		CriteriaImpl criteria = new CriteriaImpl(DrugOrder.class.getName(), null);
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(BahmniFhirConstants.VISIT_REFERENCE_SEARCH_HANDLER, null);
		
		bahmniFhirMedicationRequestDao.setupSearchParams(criteria, params);
		
		assertThat(aliasPaths(criteria).contains("e.visit=v"), is(false));
		assertThat(visitRestrictions(criteria).isEmpty(), is(true));
	}
	
	private SearchParameterMap visitParams(String type, String uuid) {
		SearchParameterMap params = new SearchParameterMap();
		ReferenceAndListParam visitRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam(type, uuid)));
		params.addParameter(BahmniFhirConstants.VISIT_REFERENCE_SEARCH_HANDLER, visitRef);
		return params;
	}
	
	private List<String> aliasPaths(CriteriaImpl criteria) {
		List<String> aliases = new ArrayList<>();
		Iterator<CriteriaImpl.Subcriteria> it = criteria.iterateSubcriteria();
		while (it.hasNext()) {
			CriteriaImpl.Subcriteria sub = it.next();
			aliases.add(sub.getPath() + "=" + sub.getAlias());
		}
		return aliases;
	}
	
	private List<String> visitRestrictions(CriteriaImpl criteria) {
		List<String> restrictions = new ArrayList<>();
		Iterator<CriteriaImpl.CriterionEntry> it = criteria.iterateExpressionEntries();
		while (it.hasNext()) {
			String text = it.next().getCriterion().toString();
			if (text.contains("v.uuid")) {
				restrictions.add(text);
			}
		}
		return restrictions;
	}
}
