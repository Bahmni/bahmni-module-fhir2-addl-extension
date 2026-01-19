package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import org.hibernate.criterion.Criterion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirMedicationRequestDaoImplTest {
	
	private static final String SYSTEM_URL = "http://example.com/system";
	
	private static final String CONCEPT_REFERENCE_TERM_ALIAS = "crt";
	
	@InjectMocks
	private BahmniFhirMedicationRequestDaoImpl bahmniFhirMedicationRequestDao;
	
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
}
