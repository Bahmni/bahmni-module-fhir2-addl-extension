package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import org.bahmni.module.fhir2AddlExtension.api.utils.ModuleUtils;
import org.hibernate.criterion.Criterion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ModuleUtils.class)
@PowerMockIgnore({ "javax.*", "org.apache.*", "org.slf4j.*", "org.xml.*" })
public class BahmniFhirMedicationRequestDaoImplTest {
	
	private static final String SYSTEM_URL = "http://example.com/system";
	
	private static final String CONCEPT_REFERENCE_TERM_ALIAS = "crt";
	
	@Mock
	private Criterion mockCriterion;
	
	@InjectMocks
	private BahmniFhirMedicationRequestDaoImpl bahmniFhirMedicationRequestDao;
	
	@Test
	public void generateSystemQuery_shouldCallModuleUtilsWhenCodesIsEmpty() {
		PowerMockito.mockStatic(ModuleUtils.class);
		
		when(ModuleUtils.isConceptReferenceCodeEmpty(null)).thenReturn(true);
		when(ModuleUtils.generateSystemQueryForEmptyCodes(SYSTEM_URL, CONCEPT_REFERENCE_TERM_ALIAS)).thenReturn(
		    mockCriterion);
		
		bahmniFhirMedicationRequestDao.generateSystemQuery(SYSTEM_URL, null, CONCEPT_REFERENCE_TERM_ALIAS);
		
		verifyStatic(ModuleUtils.class);
		ModuleUtils.isConceptReferenceCodeEmpty(null);
		
		verifyStatic(ModuleUtils.class);
		ModuleUtils.generateSystemQueryForEmptyCodes(SYSTEM_URL, CONCEPT_REFERENCE_TERM_ALIAS);
	}
	
	@Test
	public void generateSystemQuery_shouldNotCallGenerateSystemQueryForEmptyCodesWhenCodesHasValues() {
		List<String> codes = Collections.singletonList("validCode");
		
		PowerMockito.mockStatic(ModuleUtils.class);
		
		when(ModuleUtils.isConceptReferenceCodeEmpty(codes)).thenReturn(false);
		
		bahmniFhirMedicationRequestDao.generateSystemQuery(SYSTEM_URL, codes, CONCEPT_REFERENCE_TERM_ALIAS);
		
		verifyStatic(ModuleUtils.class);
		ModuleUtils.isConceptReferenceCodeEmpty(codes);
		
		verifyStatic(ModuleUtils.class, never());
		ModuleUtils.generateSystemQueryForEmptyCodes(SYSTEM_URL, CONCEPT_REFERENCE_TERM_ALIAS);
	}
}
