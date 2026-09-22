package org.bahmni.module.fhir2addlextension.api.dao.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.DrugOrder;
import org.openmrs.api.OrderService;

import java.lang.reflect.Field;

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
}
