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
	public void searchForDiagnosticReports() {
	}
	
	@Test
	public void createObsBasedReport() {
	}
}
