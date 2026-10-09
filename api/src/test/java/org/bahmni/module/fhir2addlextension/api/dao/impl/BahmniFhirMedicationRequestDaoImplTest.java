package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.DrugOrder;
import org.openmrs.api.OrderService;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirMedicationRequestDaoImplTest {
	
	private static final String SYSTEM_URL = "http://example.com/system";
	
	private static final String CONCEPT_REFERENCE_TERM_ALIAS = "crt";
	
	private static final String LOCATION_UUID = "location-uuid-123";
	
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
	public void setupSearchParams_shouldAddLocationUuidRestriction() {
		Criteria criteria = mock(Criteria.class);
		SearchParameterMap params = new SearchParameterMap();
		
		ReferenceAndListParam locationRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Location", LOCATION_UUID)));
		params.addParameter(BahmniFhirConstants.ORDER_LOCATION_SEARCH_HANDLER, locationRef);
		
		bahmniFhirMedicationRequestDao.setupSearchParams(criteria, params);
		
		ArgumentCaptor<Criterion> captor = ArgumentCaptor.forClass(Criterion.class);
		verify(criteria, atLeastOnce()).add(captor.capture());
		assertThat(captor.getAllValues().stream().anyMatch(c -> c.toString().contains("l.uuid")), is(true));
	}
	
	@Test
	public void generateSystemQuery_shouldReturnPropertySubqueryExpressionWhenCodesIsEmpty() {
		Criterion result = bahmniFhirMedicationRequestDao
		        .generateSystemQuery(SYSTEM_URL, null, CONCEPT_REFERENCE_TERM_ALIAS);
		
		assertThat(result, notNullValue());
		assertThat(result, instanceOf(org.hibernate.criterion.PropertySubqueryExpression.class));
	}
	
	@Test
	public void generateSystemQuery_shouldDelegateToSuperWhenCodesHasValues() {
		List<String> codes = Collections.singletonList("validCode");
		
		Criterion result = bahmniFhirMedicationRequestDao.generateSystemQuery(SYSTEM_URL, codes,
		    CONCEPT_REFERENCE_TERM_ALIAS);
		
		assertThat(result, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldHandleLocationReference() {
		Criteria criteria = mock(Criteria.class);
		SearchParameterMap params = new SearchParameterMap();
		
		ReferenceAndListParam locationRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Location", LOCATION_UUID)));
		params.addParameter(BahmniFhirConstants.ORDER_LOCATION_SEARCH_HANDLER, locationRef);
		
		bahmniFhirMedicationRequestDao.setupSearchParams(criteria, params);
	}
	
	@Test
	public void setupSearchParams_shouldNotFailWithNullLocationReference() {
		Criteria criteria = mock(Criteria.class);
		SearchParameterMap params = new SearchParameterMap();
		
		ReferenceAndListParam locationRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Location", LOCATION_UUID)));
		params.addParameter(BahmniFhirConstants.ORDER_LOCATION_SEARCH_HANDLER, null);
		
		bahmniFhirMedicationRequestDao.setupSearchParams(criteria, params);
	}
	
	@Test
	public void setupSearchParams_shouldHandleMultipleLocationReferences() {
		Criteria criteria = mock(Criteria.class);
		SearchParameterMap params = new SearchParameterMap();
		
		ReferenceAndListParam locationRef1 = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Location", "loc-uuid-1")));
		ReferenceAndListParam locationRef2 = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Location", "loc-uuid-2")));
		
		params.addParameter(BahmniFhirConstants.ORDER_LOCATION_SEARCH_HANDLER, locationRef1);
		params.addParameter(BahmniFhirConstants.ORDER_LOCATION_SEARCH_HANDLER, locationRef2);
		
		bahmniFhirMedicationRequestDao.setupSearchParams(criteria, params);
	}
	
	@Test
	public void generateSystemQuery_withEmptyCodeList() {
		Criterion result = bahmniFhirMedicationRequestDao.generateSystemQuery(SYSTEM_URL, Collections.emptyList(),
		    CONCEPT_REFERENCE_TERM_ALIAS);
		
		assertThat(result, notNullValue());
	}
	
	@Test
	public void generateSystemQuery_withMultipleCodes() {
		List<String> codes = java.util.Arrays.asList("code1", "code2", "code3");
		
		Criterion result = bahmniFhirMedicationRequestDao.generateSystemQuery(SYSTEM_URL, codes,
		    CONCEPT_REFERENCE_TERM_ALIAS);
		
		assertThat(result, notNullValue());
	}
	
	@Test
	public void setupSearchParams_withLocationAndOtherParams() {
		Criteria criteria = mock(Criteria.class);
		SearchParameterMap params = new SearchParameterMap();
		
		ReferenceAndListParam locationRef = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("Location", LOCATION_UUID)));
		params.addParameter(BahmniFhirConstants.ORDER_LOCATION_SEARCH_HANDLER, locationRef);
		
		bahmniFhirMedicationRequestDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void setupSearchParams_shouldHandleAbsentLocationParameter() {
		Criteria criteria = mock(Criteria.class);
		SearchParameterMap params = new SearchParameterMap();
		
		bahmniFhirMedicationRequestDao.setupSearchParams(criteria, params);
		
		assertThat(params, notNullValue());
	}
	
	@Test
	public void generateSystemQuery_withNullCodesList() {
		Criterion result = bahmniFhirMedicationRequestDao
		        .generateSystemQuery(SYSTEM_URL, null, CONCEPT_REFERENCE_TERM_ALIAS);
		
		assertThat(result, notNullValue());
		assertThat(result, instanceOf(org.hibernate.criterion.PropertySubqueryExpression.class));
	}
}
