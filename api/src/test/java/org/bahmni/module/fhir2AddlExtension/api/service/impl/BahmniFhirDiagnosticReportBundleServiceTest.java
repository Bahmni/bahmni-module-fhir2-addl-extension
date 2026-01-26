package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2AddlExtension.api.context.AppContext;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirDiagnosticReportDao;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirServiceRequestDao;
import org.bahmni.module.fhir2AddlExtension.api.domain.DiagnosticReportBundle;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDiagnosticReportExt;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirDiagnosticReportBundleService;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniFhirDiagnosticReportBundleTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniFhirDiagnosticReportTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniOrderReferenceTranslator;
import org.bahmni.module.fhir2AddlExtension.api.utils.BahmniFhirUtils;
import org.bahmni.module.fhir2AddlExtension.api.validators.impl.DiagnosticReportValidatorImpl;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Observation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.api.EncounterService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.api.db.ContextDAO;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.FhirEncounterService;
import org.openmrs.module.fhir2.api.FhirObservationService;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.springframework.beans.BeanUtils;

import java.io.IOException;
import java.util.Optional;

import static org.bahmni.module.fhir2AddlExtension.api.TestDataFactory.loadDiagnosticReportBundle;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirDiagnosticReportBundleServiceTest {
	
	BahmniFhirDiagnosticReportBundleService diagnosticReportBundleService;
	
	@Mock
	BahmniFhirDiagnosticReportDao bahmniFhirDiagnosticReportDao;
	
	@Mock
	BahmniFhirDiagnosticReportBundleTranslator bahmniFhirDiagnosticReportBundleTranslator;
	
	@Mock
	SearchQueryInclude<DiagnosticReport> searchQueryInclude;
	
	@Mock
	SearchQuery<FhirDiagnosticReportExt, DiagnosticReportBundle, BahmniFhirDiagnosticReportDao, BahmniFhirDiagnosticReportBundleTranslator, SearchQueryInclude<DiagnosticReportBundle>> searchQuery;
	
	@Mock
	BahmniFhirServiceRequestDao<Order> serviceRequestDao;
	
	@Mock
	AppContext appContext;
	
	@Mock
	VisitService visitService;
	
	@Mock
	BahmniFhirDiagnosticReportTranslator diagnosticReportTranslator;
	
	@Mock
	private BahmniOrderReferenceTranslator serviceRequestReferenceTranslator;
	
	@Mock
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Mock
	private FhirObservationService observationService;
	
	@Mock
	private FhirEncounterService fhirEncounterService;
	
	@Mock
	private ProviderService providerService;
	
	@Mock
	private User user;
	
	@Mock
	private UserContext userContext;
	
	@Mock
	private ContextDAO contextDAO;
	
	@Mock
	private EncounterService encounterService;
	
	@Before
	public void setUp() throws Exception {
		//when(userContext.getAuthenticatedUser()).thenReturn(user);
		Context.setDAO(contextDAO);
		Context.openSession();
		Context.setUserContext(userContext);
		
		LabResultsEncounterServiceImpl labEncounterService = new LabResultsEncounterServiceImpl(appContext, visitService,
		        encounterService, providerService);
		
		diagnosticReportBundleService = new BahmniFhirDiagnosticReportBundleServiceImpl(bahmniFhirDiagnosticReportDao,
		        bahmniFhirDiagnosticReportBundleTranslator, searchQueryInclude, searchQuery,
		        new DiagnosticReportValidatorImpl(serviceRequestDao), diagnosticReportTranslator,
		        serviceRequestReferenceTranslator, patientReferenceTranslator, observationService, fhirEncounterService,
		        labEncounterService);
	}
	
	@Test
    public void shouldNotCreateReportWithMissingPatientReference() throws IOException {
        DiagnosticReportBundle reportBundle = loadDiagnosticReportWithEncounterReference();
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> diagnosticReportBundleService.create(reportBundle));
        assertEquals("Diagnostic Report must have valid patient reference", exception.getMessage());
    }
	
	@Test
    public void shouldNotCreateReportWithMissingServiceRequestReference() throws IOException {
        DiagnosticReportBundle reportBundle = loadDiagnosticReportWithEncounterReference();
        org.openmrs.Patient patient = examplePatient("fa2a71fd-895d-428f-8245-dcb4f0664b60");
        when(patientReferenceTranslator.toOpenmrsType(
                argThat(reference -> reference.getReference().equals("Patient/fa2a71fd-895d-428f-8245-dcb4f0664b60"))
        )).thenReturn(patient);
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> diagnosticReportBundleService.create(reportBundle));
        assertEquals("Invalid Service Request Reference for Diagnostic Report", exception.getMessage());
    }
	
	@Test
    public void shouldNotCreateReportWithNonExistentEncounterReference() throws IOException {
        DiagnosticReportBundle reportBundle = loadDiagnosticReportWithEncounterReference();
        org.openmrs.Patient patient = examplePatient("fa2a71fd-895d-428f-8245-dcb4f0664b60");
        when(patientReferenceTranslator.toOpenmrsType(
                argThat(reference -> reference.getReference().equals("Patient/fa2a71fd-895d-428f-8245-dcb4f0664b60"))
        )).thenReturn(patient);
        Order order = exampleOrder("99e8cb58-3d40-4d41-99b0-20449df8edac", patient, null);
        when(serviceRequestReferenceTranslator.toOpenmrsType(
                argThat(reference -> reference.getReference().equals("ServiceRequest/99e8cb58-3d40-4d41-99b0-20449df8edac"))
        )).thenReturn(order);
		//when(userContext.getLocation()).thenReturn(new Location());
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> diagnosticReportBundleService.create(reportBundle));
        assertEquals("Invalid encounter reference for Diagnostic Report", exception.getMessage());
    }
	
	@Test
    public void shouldResolveResultObservationAndCreateReport() throws IOException {
        DiagnosticReportBundle reportBundle = loadDiagnosticReportBundle("example-diagnostic-report-bundle-with-encounter-reference-nested-results.json");
        org.openmrs.Patient patient = examplePatient("fa2a71fd-895d-428f-8245-dcb4f0664b60");
        Encounter encounter = exampleEncounter("81db700c-93f5-4b04-b9ae-d162a775da84", patient);
        when(patientReferenceTranslator.toOpenmrsType(
                argThat(reference -> reference.getReference().equals("Patient/fa2a71fd-895d-428f-8245-dcb4f0664b60"))
        )).thenReturn(patient);
        Order order = exampleOrder("99e8cb58-3d40-4d41-99b0-20449df8edac", patient, encounter);
        when(serviceRequestReferenceTranslator.toOpenmrsType(
                argThat(reference -> reference.getReference().equals("ServiceRequest/99e8cb58-3d40-4d41-99b0-20449df8edac"))
        )).thenReturn(order);
        final int[] counter = {0};
        when(observationService.create(any(Observation.class))).thenAnswer(invocation -> {
            //Observation argument = invocation.getArgument(0);
            ++counter[0];
            Observation mockedResponse = new Observation();
            mockedResponse.setId("result-observation-id-" + counter[0]);
            return mockedResponse;
        });
        FhirDiagnosticReportExt mockedReport = new FhirDiagnosticReportExt();
        mockedReport.setUuid("example-diagnostic-report");
        when(bahmniFhirDiagnosticReportDao.createOrUpdate(any(FhirDiagnosticReportExt.class))).thenReturn(mockedReport);
        when(diagnosticReportTranslator.toOpenmrsType(any(DiagnosticReport.class))).thenReturn(mockedReport);
		when(fhirEncounterService.get("81db700c-93f5-4b04-b9ae-d162a775da84")).thenReturn(exampleFhirEncounterResource(encounter.getUuid()));
        diagnosticReportBundleService.create(reportBundle);
        verify(observationService, times(3)).create(any(Observation.class));
        verify(bahmniFhirDiagnosticReportDao, times(1)).createOrUpdate(any(FhirDiagnosticReportExt.class));
        verify(diagnosticReportTranslator, times(1)).toOpenmrsType(any(DiagnosticReport.class));
        verify(bahmniFhirDiagnosticReportBundleTranslator, times(1)).toFhirResource(any(FhirDiagnosticReportExt.class));
    }
	
	@Test
	public void shouldCreateReportWithBundledEncounterResourceAndUpdateObservationEncounterReference() throws IOException {
		DiagnosticReportBundle reportBundle = loadDiagnosticReportBundle("example-diagnostic-report-bundle-with-encounter-resource.json");
		org.openmrs.Patient patient = examplePatient("fa2a71fd-895d-428f-8245-dcb4f0664b60");
		Encounter encounter = exampleEncounter("81db700c-93f5-4b04-b9ae-d162a775da84", patient);
		when(patientReferenceTranslator.toOpenmrsType(
				argThat(reference -> reference.getReference().equals("Patient/fa2a71fd-895d-428f-8245-dcb4f0664b60"))
		)).thenReturn(patient);
		Order order = exampleOrder("99e8cb58-3d40-4d41-99b0-20449df8edac", patient, encounter);
		when(serviceRequestReferenceTranslator.toOpenmrsType(
				argThat(reference -> reference.getReference().equals("ServiceRequest/99e8cb58-3d40-4d41-99b0-20449df8edac"))
		)).thenReturn(order);
		final int[] counter = {0};
		when(observationService.create(any(Observation.class))).thenAnswer(invocation -> {
			//Observation argument = invocation.getArgument(0);
			++counter[0];
			Observation mockedResponse = new Observation();
			mockedResponse.setId("result-observation-id-" + counter[0]);
			return mockedResponse;
		});
		FhirDiagnosticReportExt mockedReport = new FhirDiagnosticReportExt();
		mockedReport.setUuid("example-diagnostic-report");
		when(bahmniFhirDiagnosticReportDao.createOrUpdate(any(FhirDiagnosticReportExt.class))).thenReturn(mockedReport);
		when(diagnosticReportTranslator.toOpenmrsType(any(DiagnosticReport.class))).thenReturn(mockedReport);

		//copy over and return the encounter resource with changed uuid, to assert observations are created with new encounter
		org.hl7.fhir.r4.model.Encounter bundledEncounter = BahmniFhirUtils.findResourcesOfTypeInBundle(reportBundle, org.hl7.fhir.r4.model.Encounter.class).get(0);
		org.hl7.fhir.r4.model.Encounter encounterResource = new org.hl7.fhir.r4.model.Encounter();
		BeanUtils.copyProperties(bundledEncounter, encounterResource);
		encounterResource.setId("newly-created-encounter-uuid");
		when(fhirEncounterService.create(any())).thenReturn(encounterResource);

		diagnosticReportBundleService.create(reportBundle);
		verify(fhirEncounterService, times(1)).create(bundledEncounter);
		//TODO verify that observation has been created with replaced encounter UUID reference
		verify(observationService, times(2)).create(
				argThat(observation -> {
					Assert.assertEquals("Encounter/newly-created-encounter-uuid", observation.getEncounter().getReference());
					return !observation.getEncounter().getReference().contains("1d87ab20-8b86-4b41-a30d-984b2208d945");
				})
		);
		verify(observationService, times(2)).create(any(Observation.class));
		ArgumentCaptor<FhirDiagnosticReportExt> reportCaptor = ArgumentCaptor.forClass(FhirDiagnosticReportExt.class);
		verify(bahmniFhirDiagnosticReportDao, times(1)).createOrUpdate(reportCaptor.capture());
		FhirDiagnosticReportExt newReport = reportCaptor.getValue();
		Assert.assertEquals(mockedReport.getUuid(), newReport.getUuid());
		verify(bahmniFhirDiagnosticReportDao, times(1)).createOrUpdate(any(FhirDiagnosticReportExt.class));
		verify(diagnosticReportTranslator, times(1)).toOpenmrsType(any(DiagnosticReport.class));
		verify(bahmniFhirDiagnosticReportBundleTranslator, times(1)).toFhirResource(any(FhirDiagnosticReportExt.class));
	}
	
	@Test
	public void shouldCreateValidateObservationReferenceInDiagnosticReport() throws IOException {
		DiagnosticReportBundle reportBundle = loadDiagnosticReportBundle("example-diagnostic-report-with-encounter-and-service-request-reference-and-result-observation.json");
		org.openmrs.Patient patient = examplePatient("23a8371b-df7a-47c7-a015-b24198392cfa");
		Encounter encounter = exampleEncounter("a60c4455-b27a-4704-a6ea-3354081d269c", patient);
		when(patientReferenceTranslator.toOpenmrsType(
				argThat(reference -> reference.getReference().equals("Patient/23a8371b-df7a-47c7-a015-b24198392cfa"))
		)).thenReturn(patient);
		Order order = exampleOrder("e3db5d5c-49cd-4690-aba0-f87addb518a5", patient, encounter);
		when(serviceRequestReferenceTranslator.toOpenmrsType(
				argThat(reference -> reference.getReference().equals("ServiceRequest/e3db5d5c-49cd-4690-aba0-f87addb518a5"))
		)).thenReturn(order);
		final int[] counter = {0};
		when(observationService.create(any(Observation.class))).thenAnswer(invocation -> {
			//Observation argument = invocation.getArgument(0);
			++counter[0];
			Observation mockedResponse = new Observation();
			mockedResponse.setId("result-observation-id-" + counter[0]);
			return mockedResponse;
		});

		FhirDiagnosticReportExt mockedReport = new FhirDiagnosticReportExt();
		mockedReport.setUuid("example-diagnostic-report");
		when(bahmniFhirDiagnosticReportDao.createOrUpdate(any(FhirDiagnosticReportExt.class))).thenReturn(mockedReport);
		when(diagnosticReportTranslator.toOpenmrsType(any(DiagnosticReport.class))).thenReturn(mockedReport);
		when(fhirEncounterService.get("a60c4455-b27a-4704-a6ea-3354081d269c")).thenReturn(exampleFhirEncounterResource(encounter.getUuid()));
		diagnosticReportBundleService.create(reportBundle);
		verify(bahmniFhirDiagnosticReportDao, times(1)).createOrUpdate(any(FhirDiagnosticReportExt.class));
	}
	
	private org.hl7.fhir.r4.model.Encounter exampleFhirEncounterResource(String uuid) {
		org.hl7.fhir.r4.model.Encounter encounter = new org.hl7.fhir.r4.model.Encounter();
		encounter.setId(uuid);
		return encounter;
	}
	
	private Order exampleOrder(String uuid, Patient patient, Encounter encounter) {
		OrderType orderType = new OrderType();
		orderType.setName("Radiology Order");
		Order order = new Order();
		order.setUuid(uuid);
		order.setOrderType(orderType);
		order.setPatient(patient);
		order.setEncounter(encounter);
		return order;
	}
	
	private Patient examplePatient(String uuid) {
		Patient patient = new Patient();
		patient.setUuid(uuid);
		return patient;
	}
	
	private Encounter exampleEncounter(String uuid, Patient patient) {
		Visit visit = new Visit();
		visit.setUuid("visit-".concat(uuid));
		Location location = new Location();
		location.setUuid("789");
		Encounter existingEncounter = new org.openmrs.Encounter();
		existingEncounter.setPatient(patient);
		existingEncounter.setVisit(visit);
		existingEncounter.setLocation(location);
		return existingEncounter;
	}
	
	private DiagnosticReportBundle loadDiagnosticReportWithEncounterReference() throws IOException {
		DiagnosticReportBundle reportBundle = loadDiagnosticReportBundle("example-diagnostic-report-bundle-with-encounter-reference.json");
		Optional<DiagnosticReport> diagnosticReport = findFirstResourceOfTypeInBundle(reportBundle,
		    FhirConstants.DIAGNOSTIC_REPORT);
		assertTrue(diagnosticReport.isPresent());
		assertNull(diagnosticReport.get().getEncounter().getResource());
		return reportBundle;
	}
	
	private <T> Optional<T> findFirstResourceOfTypeInBundle(DiagnosticReportBundle bundle, String resourceType) {
        return bundle.getEntry().stream()
                .filter(entry -> entry.getResource().getResourceType().name().equals(resourceType))
                .findFirst().map(entry -> (T) entry.getResource());
    }
}
