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
	private SearchQuery<FhirDiagnosticReportExt, DiagnosticReport, BahmniFhirDiagnosticReportDao, BahmniFhirDiagnosticReportTranslator, SearchQueryInclude<DiagnosticReport>> searchQuery;
	
	@Mock
	private SearchQueryInclude<DiagnosticReport> searchQueryInclude;
	
	@Mock
	private PractitionerReferenceTranslator<Provider> providerReferenceTranslator;
	
	@Before
	public void setUp() throws Exception {
		DiagnosticReportTranslatorImpl openmrsTranslator = new DiagnosticReportTranslatorImpl();
		TestUtils.setPropertyOnObject(openmrsTranslator, "observationReferenceTranslator", observationReferenceTranslator);
		TestUtils.setPropertyOnObject(openmrsTranslator, "conceptTranslator", conceptTranslator);
		TestUtils.setPropertyOnObject(openmrsTranslator, "encounterReferenceTranslator", encounterReferenceTranslator);
		TestUtils.setPropertyOnObject(openmrsTranslator, "patientReferenceTranslator", patientReferenceTranslator);
		
		BahmniFhirDiagnosticReportTranslatorImpl diagnosticReportTranslator = new BahmniFhirDiagnosticReportTranslatorImpl(
		        openmrsTranslator, serviceRequestReferenceTranslator, providerReferenceTranslator);
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
        Order testOrder = new Order();
        testOrder.setUuid("0137d86f-e27b-4f3b-a701-5f3ca9a6756f");
        when(serviceRequestReferenceTranslator.toOpenmrsType(
                ArgumentMatchers.argThat(reference -> {
                    return reference.getReference().equals("ServiceRequest/0137d86f-e27b-4f3b-a701-5f3ca9a6756f");
                })))
                .thenReturn(testOrder);
		Provider performer = new Provider();
		performer.setUuid("444a609f-263f-11ee-8e08-02d2d2293862");
		when(providerReferenceTranslator.toOpenmrsType(
				ArgumentMatchers.argThat(reference -> reference.getReference().equals("Practitioner/444a609f-263f-11ee-8e08-02d2d2293862"))
		)).thenReturn(performer);
        when(bahmniFhirDiagnosticReportDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));
        DiagnosticReport savedReport = diagnosticReportService.create(diagnosticReport);
        Assert.assertTrue(savedReport != null);
    }
	
	@Test
	public void searchForDiagnosticReports() {
	}
	
	@Test
	public void createObsBasedReport() {
	}
}
