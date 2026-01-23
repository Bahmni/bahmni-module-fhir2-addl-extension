package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirDiagnosticReportDaoImplTest {
	
	private static final String ORDER_UUID = "456e7890-e89b-12d3-a456-426614174001";
	
	@InjectMocks
	private BahmniFhirDiagnosticReportDaoImpl diagnosticReportDao;
	
	@Test
	public void findByOrderUuid_shouldAcceptOrderUuidParameter() {
		assertEquals("Method should accept String parameter", String.class, 
				java.util.Arrays.stream(BahmniFhirDiagnosticReportDaoImpl.class.getMethods())
				.filter(m -> m.getName().equals("findByOrderUuid"))
				.findFirst()
				.map(m -> m.getParameterTypes()[0])
				.orElse(null));
	}
}
