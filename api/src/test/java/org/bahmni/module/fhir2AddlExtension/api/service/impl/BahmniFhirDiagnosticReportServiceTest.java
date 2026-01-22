package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import org.bahmni.module.fhir2AddlExtension.api.TestUtils;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirDiagnosticReportDao;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirServiceRequestDao;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDiagnosticReportExt;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirDiagnosticReportService;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniFhirDiagnosticReportTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniServiceRequestReferenceTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.impl.BahmniFhirDiagnosticReportTranslatorImpl;
import org.bahmni.module.fhir2AddlExtension.api.validators.impl.DiagnosticReportValidatorImpl;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.Provider;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.impl.DiagnosticReportTranslatorImpl;

import java.io.IOException;

import static org.bahmni.module.fhir2AddlExtension.api.TestDataFactory.loadResourceFromFile;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirDiagnosticReportServiceTest {
	
	BahmniFhirDiagnosticReportService diagnosticReportService;
	
	@Mock
	BahmniFhirDiagnosticReportDao bahmniFhirDiagnosticReportDao;
	
	@Mock
	BahmniFhirServiceRequestDao<Order> serviceRequestDao;
	
	@Mock
	private ObservationReferenceTranslator observationReferenceTranslator;
	
	@Mock
	private ConceptTranslator conceptTranslator;
	
	@Mock
	private EncounterReferenceTranslator<Encounter> encounterReferenceTranslator;
	
	@Mock
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Mock
	private BahmniServiceRequestReferenceTranslator serviceRequestReferenceTranslator;
	
	@Mock
	private BahmniFhirDiagnosticReportTranslator diagnosticReportTranslator;
	
	@Mock
	private SearchQuery<FhirDiagnosticReportExt, DiagnosticReport, BahmniFhirDiagnosticReportDao, BahmniFhirDiagnosticReportTranslator, SearchQueryInclude<DiagnosticReport>> searchQuery;
	
	@Mock
	private SearchQueryInclude<DiagnosticReport> searchQueryInclude;
	
	@Mock
	private PractitionerReferenceTranslator<Provider> providerReferenceTranslator;
	
	@Before
	public void setUp() throws Exception {
		DiagnosticReportValidatorImpl validator = new DiagnosticReportValidatorImpl(serviceRequestDao);
		diagnosticReportService = new BahmniFhirDiagnosticReportServiceImpl(
		                                                                    bahmniFhirDiagnosticReportDao, validator,
		                                                                    diagnosticReportTranslator, searchQuery,
		                                                                    searchQueryInclude) {
			
			@Override
			protected void validateObject(FhirDiagnosticReportExt object) {
				//Done to avoid failure in ValidateUtil.validate(object) which calls Context.getAdministrativeService()
			}
		};
	}
	
	@Test
	public void shouldCreateDiagnosticReport() throws IOException {
		DiagnosticReport diagnosticReport = (DiagnosticReport) loadResourceFromFile("example-diagnostic-report-with-contained-resources.json");
		
		FhirDiagnosticReportExt openmrsReport = new FhirDiagnosticReportExt();
		openmrsReport.setUuid("openmrs-report-uuid");
		
		DiagnosticReport translatedReport = new DiagnosticReport();
		translatedReport.setId("translated-report-id");
		
		when(diagnosticReportTranslator.toOpenmrsType(diagnosticReport)).thenReturn(openmrsReport);
		when(diagnosticReportTranslator.toFhirResource(openmrsReport)).thenReturn(translatedReport);
		when(bahmniFhirDiagnosticReportDao.createOrUpdate(openmrsReport)).thenReturn(openmrsReport);
		
		DiagnosticReport savedReport = diagnosticReportService.create(diagnosticReport);
		
		Assert.assertNotNull("Should return created diagnostic report", savedReport);
		Assert.assertEquals("Should return translated report", translatedReport.getId(), savedReport.getId());
		
		verify(diagnosticReportTranslator).toOpenmrsType(diagnosticReport);
		verify(bahmniFhirDiagnosticReportDao).createOrUpdate(openmrsReport);
		verify(diagnosticReportTranslator).toFhirResource(openmrsReport);
	}
	
	@Test
	public void findByOrder_shouldReturnDiagnosticReportWhenFound() {
		Order order = new Order();
		order.setOrderId(123);
		order.setUuid("order-uuid-123");
		
		FhirDiagnosticReportExt diagnosticReportExt = new FhirDiagnosticReportExt();
		diagnosticReportExt.setUuid("diagnostic-report-uuid-123");
		
		DiagnosticReport expectedDiagnosticReport = new DiagnosticReport();
		expectedDiagnosticReport.setId("diagnostic-report-fhir-id-123");
		expectedDiagnosticReport.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);
		
		when(bahmniFhirDiagnosticReportDao.findByOrder(order)).thenReturn(diagnosticReportExt);
		when(diagnosticReportTranslator.toFhirResource(diagnosticReportExt)).thenReturn(expectedDiagnosticReport);
		
		DiagnosticReport result = diagnosticReportService.findByOrder(order);
		
		Assert.assertNotNull("Should return a DiagnosticReport when found", result);
		Assert.assertEquals("Should return the translated DiagnosticReport", expectedDiagnosticReport.getId(),
		    result.getId());
		Assert.assertEquals("Should preserve DiagnosticReport status", expectedDiagnosticReport.getStatus(),
		    result.getStatus());
		
		verify(bahmniFhirDiagnosticReportDao).findByOrder(order);
		verify(diagnosticReportTranslator).toFhirResource(diagnosticReportExt);
	}
	
	@Test(expected = NullPointerException.class)
	public void findByOrder_shouldThrowExceptionForNullOrder() {
		when(bahmniFhirDiagnosticReportDao.findByOrder(null)).thenThrow(new NullPointerException("Order cannot be null"));
		
		diagnosticReportService.findByOrder(null);
	}
	
	@Test
	public void findByOrder_shouldVerifyDaoInteractionWithSpecificOrder() {
		Order specificOrder = new Order();
		specificOrder.setOrderId(12345);
		specificOrder.setUuid("specific-test-uuid");
		
		FhirDiagnosticReportExt daoResult = new FhirDiagnosticReportExt();
		daoResult.setUuid("dao-result-uuid");
		
		DiagnosticReport translatedResult = new DiagnosticReport();
		translatedResult.setId("translated-result-id");
		translatedResult.setStatus(DiagnosticReport.DiagnosticReportStatus.REGISTERED);
		
		when(bahmniFhirDiagnosticReportDao.findByOrder(specificOrder)).thenReturn(daoResult);
		when(diagnosticReportTranslator.toFhirResource(daoResult)).thenReturn(translatedResult);

		DiagnosticReport result = diagnosticReportService.findByOrder(specificOrder);
		
		Assert.assertNotNull("Should return translated result", result);
		Assert.assertEquals("Should preserve translated ID", translatedResult.getId(), result.getId());
		Assert.assertEquals("Should preserve translated status", translatedResult.getStatus(), result.getStatus());
		
		verify(bahmniFhirDiagnosticReportDao).findByOrder(eq(specificOrder));
		verify(diagnosticReportTranslator).toFhirResource(eq(daoResult));
	}
	
	@Test
	public void findByOrder_shouldNotCallTranslatorWhenDaoReturnsNull() {
		Order order = new Order();
		order.setOrderId(404);
		order.setUuid("not-found-order");
		
		when(bahmniFhirDiagnosticReportDao.findByOrder(order)).thenReturn(null);
		
		DiagnosticReport result = diagnosticReportService.findByOrder(order);
		
		Assert.assertNull("Should return null when DAO returns null", result);
		
		verify(bahmniFhirDiagnosticReportDao).findByOrder(order);
		verify(diagnosticReportTranslator, never()).toFhirResource(any());
	}
	
	@Test
	public void findByOrder_shouldReturnNullWhenTranslatorReturnsNull() {
		Order order = new Order();
		order.setOrderId(500);
		order.setUuid("translator-null-test");
		
		FhirDiagnosticReportExt daoResult = new FhirDiagnosticReportExt();
		daoResult.setUuid("valid-dao-result");
		
		when(bahmniFhirDiagnosticReportDao.findByOrder(order)).thenReturn(daoResult);
		when(diagnosticReportTranslator.toFhirResource(daoResult)).thenReturn(null);
		
		DiagnosticReport result = diagnosticReportService.findByOrder(order);
		
		Assert.assertNull("Should return null when translator returns null", result);
		
		verify(bahmniFhirDiagnosticReportDao).findByOrder(order);
		verify(diagnosticReportTranslator).toFhirResource(daoResult);
	}
	
	@Test
	public void findByOrder_shouldHandleDifferentDiagnosticReportStatuses() {
		DiagnosticReport.DiagnosticReportStatus[] statuses = { DiagnosticReport.DiagnosticReportStatus.REGISTERED,
		        DiagnosticReport.DiagnosticReportStatus.PARTIAL, DiagnosticReport.DiagnosticReportStatus.PRELIMINARY,
		        DiagnosticReport.DiagnosticReportStatus.FINAL, DiagnosticReport.DiagnosticReportStatus.AMENDED,
		        DiagnosticReport.DiagnosticReportStatus.CORRECTED, DiagnosticReport.DiagnosticReportStatus.APPENDED,
		        DiagnosticReport.DiagnosticReportStatus.CANCELLED, DiagnosticReport.DiagnosticReportStatus.ENTEREDINERROR,
		        DiagnosticReport.DiagnosticReportStatus.UNKNOWN };
		
		for (int i = 0; i < statuses.length; i++) {
			Order order = new Order();
			order.setOrderId(1000 + i);
			order.setUuid("status-test-order-" + i);
			
			FhirDiagnosticReportExt daoResult = new FhirDiagnosticReportExt();
			daoResult.setUuid("status-test-report-" + i);
			
			DiagnosticReport translatedResult = new DiagnosticReport();
			translatedResult.setId("status-test-id-" + i);
			translatedResult.setStatus(statuses[i]);
			
			when(bahmniFhirDiagnosticReportDao.findByOrder(order)).thenReturn(daoResult);
			when(diagnosticReportTranslator.toFhirResource(daoResult)).thenReturn(translatedResult);
			
			DiagnosticReport result = diagnosticReportService.findByOrder(order);
			
			Assert.assertNotNull("Should return result for status: " + statuses[i], result);
			Assert.assertEquals("Should preserve status: " + statuses[i], statuses[i], result.getStatus());
			
			org.mockito.Mockito.reset(bahmniFhirDiagnosticReportDao, diagnosticReportTranslator);
		}
	}
	
	@Test
	public void findByOrder_shouldMaintainTransactionIntegrity() {
		Order order = new Order();
		order.setOrderId(2000);
		order.setUuid("transaction-test-order");
		
		FhirDiagnosticReportExt daoResult = new FhirDiagnosticReportExt();
		daoResult.setUuid("transaction-test-report");
		
		DiagnosticReport translatedResult = new DiagnosticReport();
		translatedResult.setId("transaction-test-id");
		
		when(bahmniFhirDiagnosticReportDao.findByOrder(order)).thenReturn(daoResult);
		when(diagnosticReportTranslator.toFhirResource(daoResult)).thenReturn(translatedResult);
		
		DiagnosticReport result = diagnosticReportService.findByOrder(order);
		
		Assert.assertNotNull("Should return result", result);
		Assert.assertEquals("Should return correct result", translatedResult, result);
		
		org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(bahmniFhirDiagnosticReportDao, diagnosticReportTranslator);
		inOrder.verify(bahmniFhirDiagnosticReportDao).findByOrder(order);
		inOrder.verify(diagnosticReportTranslator).toFhirResource(daoResult);
	}
	
	@Test
	public void findByOrder_shouldHandleOrderWithZeroId() {
		Order zeroIdOrder = new Order();
		zeroIdOrder.setOrderId(0);
		zeroIdOrder.setUuid("zero-id-order");
		
		FhirDiagnosticReportExt daoResult = new FhirDiagnosticReportExt();
		daoResult.setUuid("zero-id-report");
		
		DiagnosticReport translatedResult = new DiagnosticReport();
		translatedResult.setId("zero-id-result");
		
		when(bahmniFhirDiagnosticReportDao.findByOrder(zeroIdOrder)).thenReturn(daoResult);
		when(diagnosticReportTranslator.toFhirResource(daoResult)).thenReturn(translatedResult);
		
		DiagnosticReport result = diagnosticReportService.findByOrder(zeroIdOrder);
		
		Assert.assertNotNull("Should handle zero ID order", result);
		Assert.assertEquals("Should return correct result", translatedResult.getId(), result.getId());
		
		verify(bahmniFhirDiagnosticReportDao).findByOrder(zeroIdOrder);
		verify(diagnosticReportTranslator).toFhirResource(daoResult);
	}
	
	@Test
	public void findByOrder_shouldHandleOrderWithNegativeId() {
		Order negativeIdOrder = new Order();
		negativeIdOrder.setOrderId(-1);
		negativeIdOrder.setUuid("negative-id-order");
		
		when(bahmniFhirDiagnosticReportDao.findByOrder(negativeIdOrder)).thenReturn(null);
		
		DiagnosticReport result = diagnosticReportService.findByOrder(negativeIdOrder);
		
		Assert.assertNull("Should handle negative ID order gracefully", result);
		
		verify(bahmniFhirDiagnosticReportDao).findByOrder(negativeIdOrder);
		verify(diagnosticReportTranslator, never()).toFhirResource(any());
	}
	
	@Test
	public void findByOrder_shouldPreserveComplexDiagnosticReportData() {
		Order order = new Order();
		order.setOrderId(555);
		order.setUuid("complex-order-uuid");
		
		FhirDiagnosticReportExt diagnosticReportExt = new FhirDiagnosticReportExt();
		diagnosticReportExt.setUuid("complex-diagnostic-report-uuid");
		
		DiagnosticReport complexReport = new DiagnosticReport();
		complexReport.setId("complex-fhir-report");
		complexReport.setStatus(DiagnosticReport.DiagnosticReportStatus.PRELIMINARY);
		complexReport.setConclusion("Test conclusion");
		
		when(bahmniFhirDiagnosticReportDao.findByOrder(order)).thenReturn(diagnosticReportExt);
		when(diagnosticReportTranslator.toFhirResource(diagnosticReportExt)).thenReturn(complexReport);
		
		DiagnosticReport result = diagnosticReportService.findByOrder(order);
		
		Assert.assertNotNull("Should return translated report", result);
		Assert.assertEquals("Should preserve report ID", complexReport.getId(), result.getId());
		Assert.assertEquals("Should preserve report status", complexReport.getStatus(), result.getStatus());
		Assert.assertEquals("Should preserve conclusion", complexReport.getConclusion(), result.getConclusion());
		
		verify(bahmniFhirDiagnosticReportDao).findByOrder(order);
		verify(diagnosticReportTranslator).toFhirResource(diagnosticReportExt);
	}
	
	@Test
	public void searchForDiagnosticReports() {
	}
	
	@Test
	public void createObsBasedReport() {
	}
}
