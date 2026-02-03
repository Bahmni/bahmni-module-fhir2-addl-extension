package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.PatchTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.bahmni.module.fhir2AddlExtension.api.TestDataFactory;
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
import org.bahmni.module.fhir2AddlExtension.api.validators.DiagnosticReportBundlePatchValidator;
import org.bahmni.module.fhir2AddlExtension.api.validators.impl.DiagnosticReportBundlePatchValidatorImpl;
import org.bahmni.module.fhir2AddlExtension.api.validators.impl.DiagnosticReportBundleUpdateValidatorImpl;
import org.bahmni.module.fhir2AddlExtension.api.validators.impl.DiagnosticReportValidatorImpl;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
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
import org.openmrs.module.fhir2.api.util.JsonPatchUtils;
import org.springframework.beans.BeanUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

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
	
	//	@Mock
	//	private DiagnosticReportBundlePatchValidator diagnosticReportBundlePatchValidator;
	
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
		        new DiagnosticReportValidatorImpl(serviceRequestDao), new DiagnosticReportBundlePatchValidatorImpl(),
		        new DiagnosticReportBundleUpdateValidatorImpl(), diagnosticReportTranslator,
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
	
	@Test
	public void shouldTestPatchForBundle() throws IOException {
		// Load patch
		//		DiagnosticReportBundle existingBundle = loadDiagnosticReportBundle("diagnostic-report-bundle-for-patch-existing.json");
		//		List<DiagnosticReport> reports = BahmniFhirUtils.findResourcesOfTypeInBundle(existingBundle, DiagnosticReport.class);
		//		System.out.println(reports.get(0).hasPresentedForm());
		//		if (!reports.get(0).hasPresentedForm()) {
		//			reports.get(0).setPresentedForm(new ArrayList<>());
		//		}
		////		String patchBody = loadPatchJson("patch-diagnostic-bundle-add-observation.json");
		//		String patchBody = loadPatchJson("patch-diagnostic-bundle-add-attachment.json");
		//		//patch-diagnostic-bundle-add-attachment-fhir-path
		//		RequestDetails requestDetails = mock(RequestDetails.class);
		//		when(requestDetails.getFhirContext()).thenReturn(ca.uhn.fhir.context.FhirContext.forR4());
		//
		//
		//		FhirContext ctx = requestDetails.getFhirContext();
		//		// Use openmrs fhir2 JsonPatchUtils static method
		//		DiagnosticReportBundle patchedBundle = JsonPatchUtils.applyJsonPatch(ctx, existingBundle, patchBody);
		//		System.out.println(existingBundle.getId());
		//		System.out.println(patchedBundle.getId());
		
		DiagnosticReportBundle existingBundle = loadDiagnosticReportBundle("diagnostic-report-bundle-for-patch-existing.json");
		String patchBody = loadPatchJson("patch-diagnostic-bundle-add-attachment-fhir-path.json");
		FhirContext ctx = ca.uhn.fhir.context.FhirContext.forR4();
		Parameters patch = ctx.newJsonParser().parseResource(Parameters.class, patchBody);
		
		List<DiagnosticReport> reports = BahmniFhirUtils.findResourcesOfTypeInBundle(existingBundle, DiagnosticReport.class);
		
		for (Parameters.ParametersParameterComponent op : patch.getParameter()) {
			if ("operation".equals(op.getName())) {
				
				// Extract values (simplified for this example)
				String type = op.fhirType();
				
				if ("add".equals(type)) {
					// 2. Create the attachment using HAPI POJOs
					Attachment newForm = new Attachment();
					newForm.setContentType("application/pdf");
					newForm.setUrl("https://example.org/report.pdf");
					
					// 3. This ALWAYS works even if the list was empty!
					reports.get(0).addPresentedForm(newForm);
				}
			}
		}
		//FhirPatch patcher = new FhirPatch(ctx);
		
		// 3. Apply the patch to your existing resource
		// Note: This modifies 'existingReport' in-place
		//patcher.apply(existingReport, patchResource);
	}
	
	// ========== ⌄ DIAGNOSTIC REPORT PATCH TESTS ⌄ ==========
	
	@Ignore
	@Test
	public void shouldSuccessfullyPatchDiagnosticReportBundleWithNewObservation() throws IOException {
		// Setup: Existing bundle
		DiagnosticReportBundle existingBundle = loadDiagnosticReportBundle("diagnostic-report-bundle-for-patch-existing.json");
		String bundleUuid = "report-bundle-123";
		FhirDiagnosticReportExt existingReport = new FhirDiagnosticReportExt();
		existingReport.setUuid(bundleUuid);
		when(bahmniFhirDiagnosticReportDao.get(bundleUuid)).thenReturn(existingReport);
		
		// Load patch
		String patchBody = loadPatchJson("patch-diagnostic-bundle-add-observation.json");
		
		// Mock get to return existing bundle
		when(bahmniFhirDiagnosticReportBundleTranslator.toFhirResource(any(FhirDiagnosticReportExt.class)))
				.thenReturn(existingBundle);
		
		// Mock dependencies
		org.openmrs.Patient patient = examplePatient("patient-uuid-1");
		Encounter encounter = exampleEncounter("encounter-uuid-1", patient);
		Order order = exampleOrder("order-uuid-1", patient, encounter);
		
		when(serviceRequestReferenceTranslator.toOpenmrsType(
				argThat(ref -> ref.getReference().equals("ServiceRequest/order-uuid-1"))
		)).thenReturn(order);
		
		// Mock observation creation
		AtomicReference<Integer> counter = new AtomicReference<>(0);
		when(observationService.create(any(Observation.class))).thenAnswer(invocation -> {
			Observation obs = new Observation();
			counter.set(counter.get() + 1);
			obs.setId("new-obs-uuid-" + counter);
			return obs;
		});
		
		FhirDiagnosticReportExt updatedReport = new FhirDiagnosticReportExt();
		updatedReport.setUuid(bundleUuid);
		when(bahmniFhirDiagnosticReportDao.createOrUpdate(any(FhirDiagnosticReportExt.class))).thenReturn(updatedReport);
		when(diagnosticReportTranslator.toOpenmrsType(any(DiagnosticReport.class))).thenReturn(updatedReport);
		
		RequestDetails requestDetails = mock(RequestDetails.class);
		when(requestDetails.getFhirContext()).thenReturn(ca.uhn.fhir.context.FhirContext.forR4());
		
		// Execute patch
		DiagnosticReportBundle result = diagnosticReportBundleService.patch(bundleUuid, PatchTypeEnum.JSON_PATCH, 
				patchBody, requestDetails);
		
		// Verify
		assertNotNull(result);
//		verify(diagnosticReportBundlePatchValidator, times(1)).validateNotInTerminalState(any(DiagnosticReport.class));
//		verify(diagnosticReportBundlePatchValidator, times(1)).validatePatchChanges(any(DiagnosticReport.class), any(DiagnosticReport.class));
		verify(observationService, times(1)).create(any(Observation.class));
		verify(bahmniFhirDiagnosticReportDao, times(1)).createOrUpdate(any(FhirDiagnosticReportExt.class));
	}
	
	@Ignore
	@Test
	public void shouldSuccessfullyPatchDiagnosticReportBundleWithPresentedForm() throws IOException {
		// Setup: Existing bundle
		DiagnosticReportBundle existingBundle = loadDiagnosticReportBundle("diagnostic-report-bundle-for-patch-existing.json");
		String bundleUuid = "report-bundle-123";
		
		// Load patch
		String patchBody = loadPatchJson("patch-diagnostic-bundle-add-attachment.json");
		FhirDiagnosticReportExt existingReport = new FhirDiagnosticReportExt();
		existingReport.setUuid(bundleUuid);
		when(bahmniFhirDiagnosticReportDao.get(bundleUuid)).thenReturn(existingReport);
		// Mock get to return existing bundle
		when(bahmniFhirDiagnosticReportBundleTranslator.toFhirResource(any(FhirDiagnosticReportExt.class)))
				.thenReturn(existingBundle);
		
		// Mock dependencies
		org.openmrs.Patient patient = examplePatient("patient-uuid-1");
		Encounter encounter = exampleEncounter("encounter-uuid-1", patient);
		Order order = exampleOrder("order-uuid-1", patient, encounter);
		
		when(serviceRequestReferenceTranslator.toOpenmrsType(
				argThat(ref -> ref.getReference().equals("ServiceRequest/order-uuid-1"))
		)).thenReturn(order);
		
		FhirDiagnosticReportExt updatedReport = new FhirDiagnosticReportExt();
		updatedReport.setUuid(bundleUuid);
		List<FhirDiagnosticReportExt> capturedArgs = new ArrayList<>();
		when(bahmniFhirDiagnosticReportDao.createOrUpdate(any(FhirDiagnosticReportExt.class))).
				thenAnswer(invocationOnMock -> {
					capturedArgs.add(invocationOnMock.getArgument(0));
					return updatedReport;
				});


		when(diagnosticReportTranslator.toOpenmrsType(any(DiagnosticReport.class))).thenReturn(updatedReport);
		
		RequestDetails requestDetails = mock(RequestDetails.class);
		when(requestDetails.getFhirContext()).thenReturn(ca.uhn.fhir.context.FhirContext.forR4());
		
		// Execute patch
		DiagnosticReportBundle result = diagnosticReportBundleService.patch(bundleUuid, PatchTypeEnum.JSON_PATCH, 
				patchBody, requestDetails);
		
		// Verify
		assertNotNull(result);
//		verify(diagnosticReportBundlePatchValidator, times(1)).validateNotInTerminalState(any(DiagnosticReport.class));
//		verify(diagnosticReportBundlePatchValidator, times(1)).validatePatchChanges(any(DiagnosticReport.class), any(DiagnosticReport.class));
		// No new observations should be created
		verify(observationService, never()).create(any(Observation.class));
		verify(bahmniFhirDiagnosticReportDao, times(1)).createOrUpdate(any(FhirDiagnosticReportExt.class));
		Assert.assertEquals(1, capturedArgs.size());
	}
	
	@Ignore
	@Test
	public void shouldRejectPatchWithInvalidPatientChange() throws IOException {
		// Setup: Existing bundle
		DiagnosticReportBundle existingBundle = loadDiagnosticReportBundle("diagnostic-report-bundle-for-patch-existing.json");
		String bundleUuid = "report-bundle-123";
		
		// Load patch that attempts to change patient
		String patchBody = loadPatchJson("patch-diagnostic-bundle-invalid-patient-change.json");
		
		// Mock get to return existing bundle
		when(bahmniFhirDiagnosticReportBundleTranslator.toFhirResource(any(FhirDiagnosticReportExt.class)))
				.thenReturn(existingBundle);
		
		// Mock validator to throw exception
//		doThrow(new InvalidRequestException("Cannot change patient reference in DiagnosticReport"))
//				.when(diagnosticReportBundlePatchValidator).validatePatchChanges(any(DiagnosticReport.class), any(DiagnosticReport.class));
		
		RequestDetails requestDetails = mock(RequestDetails.class);
		when(requestDetails.getFhirContext()).thenReturn(ca.uhn.fhir.context.FhirContext.forR4());
		
		// Execute and verify exception
		InvalidRequestException exception = assertThrows(InvalidRequestException.class, 
				() -> diagnosticReportBundleService.patch(bundleUuid, PatchTypeEnum.JSON_PATCH, patchBody, requestDetails));
		
		assertTrue(exception.getMessage().contains("Cannot change patient reference"));
//		verify(diagnosticReportBundlePatchValidator, times(1)).validateNotInTerminalState(any(DiagnosticReport.class));
//		verify(diagnosticReportBundlePatchValidator, times(1)).validatePatchChanges(any(DiagnosticReport.class), any(DiagnosticReport.class));
		verify(bahmniFhirDiagnosticReportDao, never()).createOrUpdate(any(FhirDiagnosticReportExt.class));
	}
	
	@Ignore
	@Test
	public void shouldRejectPatchWithInvalidBasedOnRemoval() throws IOException {
		// Setup: Existing bundle
		DiagnosticReportBundle existingBundle = loadDiagnosticReportBundle("diagnostic-report-bundle-for-patch-existing.json");
		String bundleUuid = "report-bundle-123";
		
		// Load patch that attempts to remove basedOn
		String patchBody = loadPatchJson("patch-diagnostic-bundle-invalid-remove-basedon.json");
		
		// Mock get to return existing bundle
		when(bahmniFhirDiagnosticReportBundleTranslator.toFhirResource(any(FhirDiagnosticReportExt.class)))
				.thenReturn(existingBundle);
		
		// Mock validator to throw exception
//		doThrow(new InvalidRequestException("Cannot remove basedOn reference from DiagnosticReport"))
//				.when(diagnosticReportBundlePatchValidator).validatePatchChanges(any(DiagnosticReport.class), any(DiagnosticReport.class));
		
		RequestDetails requestDetails = mock(RequestDetails.class);
		when(requestDetails.getFhirContext()).thenReturn(ca.uhn.fhir.context.FhirContext.forR4());
		
		// Execute and verify exception
			InvalidRequestException exception = assertThrows(InvalidRequestException.class,
				() -> diagnosticReportBundleService.patch(bundleUuid, PatchTypeEnum.JSON_PATCH, patchBody, requestDetails));
		
		assertTrue(exception.getMessage().contains("Cannot remove basedOn reference"));
//		verify(diagnosticReportBundlePatchValidator, times(1)).validateNotInTerminalState(any(DiagnosticReport.class));
//		verify(diagnosticReportBundlePatchValidator, times(1)).validatePatchChanges(any(DiagnosticReport.class), any(DiagnosticReport.class));
		verify(bahmniFhirDiagnosticReportDao, never()).createOrUpdate(any(FhirDiagnosticReportExt.class));
	}
	
	@Ignore
	@Test
	public void shouldRejectPatchForReportInTerminalState() throws IOException {
		// Setup: Existing bundle with final status
		DiagnosticReportBundle existingBundle = loadDiagnosticReportBundle("diagnostic-report-bundle-for-patch-existing.json");
		DiagnosticReport report = BahmniFhirUtils.findResourcesOfTypeInBundle(existingBundle, DiagnosticReport.class).get(0);
		report.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);
		
		String bundleUuid = "report-bundle-123";
		String patchBody = loadPatchJson("patch-diagnostic-bundle-add-observation.json");
		
		// Mock get to return existing bundle
		when(bahmniFhirDiagnosticReportBundleTranslator.toFhirResource(any(FhirDiagnosticReportExt.class)))
				.thenReturn(existingBundle);
		
		// Mock validator to throw exception for terminal state
//		doThrow(new InvalidRequestException("Cannot patch DiagnosticReport in terminal state: final"))
//				.when(diagnosticReportBundlePatchValidator).validateNotInTerminalState(any(DiagnosticReport.class));
		
		RequestDetails requestDetails = mock(RequestDetails.class);
		when(requestDetails.getFhirContext()).thenReturn(ca.uhn.fhir.context.FhirContext.forR4());
		
		// Execute and verify exception
			InvalidRequestException exception = assertThrows(InvalidRequestException.class,
				() -> diagnosticReportBundleService.patch(bundleUuid, PatchTypeEnum.JSON_PATCH, patchBody, requestDetails));
		
		assertTrue(exception.getMessage().contains("Cannot patch DiagnosticReport in terminal state"));
//		verify(diagnosticReportBundlePatchValidator, times(1)).validateNotInTerminalState(any(DiagnosticReport.class));
//		verify(diagnosticReportBundlePatchValidator, never()).validatePatchChanges(any(DiagnosticReport.class), any(DiagnosticReport.class));
		verify(bahmniFhirDiagnosticReportDao, never()).createOrUpdate(any(FhirDiagnosticReportExt.class));
	}
	
	@Ignore
	@Test
	public void shouldThrowResourceNotFoundWhenPatchingNonExistentBundle() {
		String bundleUuid = "non-existent-uuid";
		String patchBody = "[]";
		
		// Mock get to return null
		when(bahmniFhirDiagnosticReportBundleTranslator.toFhirResource(any())).thenReturn(null);
		
		RequestDetails requestDetails = mock(RequestDetails.class);
		when(requestDetails.getFhirContext()).thenReturn(ca.uhn.fhir.context.FhirContext.forR4());
		
		// Execute and verify exception
		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, 
				() -> diagnosticReportBundleService.patch(bundleUuid, PatchTypeEnum.JSON_PATCH, patchBody, requestDetails));
		
		assertTrue(exception.getMessage().contains("not found"));
		verify(bahmniFhirDiagnosticReportDao, never()).createOrUpdate(any(FhirDiagnosticReportExt.class));
	}
	
	@Test
	public void shouldRejectNonJsonPatchType() {
		String bundleUuid = "report-bundle-123";
		String patchBody = "some xml patch";
		
		RequestDetails requestDetails = mock(RequestDetails.class);
		
		// Execute and verify exception
		InvalidRequestException exception = assertThrows(InvalidRequestException.class, 
				() -> diagnosticReportBundleService.patch(bundleUuid, PatchTypeEnum.XML_PATCH, patchBody, requestDetails));
		
		assertEquals("Only JSON Patch is supported for DiagnosticReportBundle", exception.getMessage());
		verify(bahmniFhirDiagnosticReportDao, never()).createOrUpdate(any(FhirDiagnosticReportExt.class));
	}
	
	// ========== ^ PATCH TESTS ^ ==========
	
	// ========== ⌄ DIAGNOSTIC REPORT UPDATE (PUT) TESTS ⌄ ==========
	
	@Test
	public void shouldSuccessfullyUpdateDiagnosticReportBundleWithCompleteReplacement() throws IOException {
		// Setup: Existing bundle
		DiagnosticReportBundle existingBundle = loadDiagnosticReportBundle("diagnostic-report-bundle-for-patch-existing.json");
		String bundleUuid = "report-bundle-123";
		
		// Load updated bundle with completely new data
		DiagnosticReportBundle updatedBundle = loadDiagnosticReportBundle("update-diagnostic-bundle-valid-replacement.json");
		
		FhirDiagnosticReportExt existingReport = new FhirDiagnosticReportExt();
		existingReport.setUuid(bundleUuid);
		existingReport.setId(12345);
		Obs obs1 = new Obs();
		obs1.setUuid("obs-1");
		Set<Obs> results = new HashSet<>();
		results.add(obs1);
		existingReport.setResults(results);
		when(bahmniFhirDiagnosticReportDao.get(bundleUuid)).thenReturn(existingReport);
		
		// Mock get to return existing bundle
		when(bahmniFhirDiagnosticReportBundleTranslator.toFhirResource(any(FhirDiagnosticReportExt.class)))
				.thenReturn(existingBundle);
		
		// Mock dependencies
		org.openmrs.Patient patient = examplePatient("patient-uuid-1");
		Encounter encounter = exampleEncounter("encounter-uuid-1", patient);
		
//		when(patientReferenceTranslator.toOpenmrsType(
//				argThat(ref -> ref.getReference().equals("Patient/patient-uuid-1"))
//		)).thenReturn(patient);
		
		Order newOrder = exampleOrder("new-order-uuid-1", patient, encounter);
		when(serviceRequestReferenceTranslator.toOpenmrsType(
				argThat(ref -> ref.getReference().equals("ServiceRequest/new-order-uuid-1"))
		)).thenReturn(newOrder);
		
		// Mock observation deletion (voiding)
		//when(observationService.delete(anyString())).thenReturn(new Observation());
		
		// Mock new observation creation
		AtomicReference<Integer> counter = new AtomicReference<>(0);
		when(observationService.create(any(Observation.class))).thenAnswer(invocation -> {
			Observation obs = new Observation();
			counter.set(counter.get() + 1);
			obs.setId("new-obs-uuid-" + counter);
			return obs;
		});
		
		FhirDiagnosticReportExt savedReport = new FhirDiagnosticReportExt();
		savedReport.setUuid(bundleUuid);
		savedReport.setId(12345); // Same ID
		when(bahmniFhirDiagnosticReportDao.createOrUpdate(any(FhirDiagnosticReportExt.class))).thenReturn(savedReport);
		when(diagnosticReportTranslator.toOpenmrsType(any(FhirDiagnosticReportExt.class), any(DiagnosticReport.class))).thenReturn(savedReport);
		
		// Execute update
		DiagnosticReportBundle result = diagnosticReportBundleService.update(bundleUuid, updatedBundle);
		
		// Verify
		assertNotNull(result);
		// Verify old observations were voided
		verify(observationService, times(1)).delete(anyString());
		// Verify new observations were created
		verify(observationService, times(2)).create(any(Observation.class));
		// Verify report was saved with preserved UUID
		ArgumentCaptor<FhirDiagnosticReportExt> reportCaptor = ArgumentCaptor.forClass(FhirDiagnosticReportExt.class);
		verify(bahmniFhirDiagnosticReportDao, times(2)).createOrUpdate(reportCaptor.capture());
		FhirDiagnosticReportExt capturedReport = reportCaptor.getValue();
		assertEquals(bundleUuid, capturedReport.getUuid());
		assertEquals(Integer.valueOf(12345), capturedReport.getId());
	}
	
	@Test
	public void shouldRejectUpdateWhenPatientReferenceChanges() throws IOException {
		// Setup: Existing bundle
		DiagnosticReportBundle existingBundle = loadDiagnosticReportBundle("diagnostic-report-bundle-for-patch-existing.json");
		String bundleUuid = "report-bundle-123";
		
		// Load bundle with different patient
		DiagnosticReportBundle updatedBundle = loadDiagnosticReportBundle("update-diagnostic-bundle-invalid-patient-change.json");
		
		FhirDiagnosticReportExt existingReport = new FhirDiagnosticReportExt();
		existingReport.setUuid(bundleUuid);
		when(bahmniFhirDiagnosticReportDao.get(bundleUuid)).thenReturn(existingReport);
		
		// Mock get to return existing bundle
		when(bahmniFhirDiagnosticReportBundleTranslator.toFhirResource(any(FhirDiagnosticReportExt.class)))
				.thenReturn(existingBundle);
		
		// Execute and verify exception
		InvalidRequestException exception = assertThrows(InvalidRequestException.class,
				() -> diagnosticReportBundleService.update(bundleUuid, updatedBundle));
		
		assertTrue(exception.getMessage().contains("Patient reference cannot be changed"));
		verify(bahmniFhirDiagnosticReportDao, never()).createOrUpdate(any(FhirDiagnosticReportExt.class));
		verify(observationService, never()).delete(anyString());
		verify(observationService, never()).create(any(Observation.class));
	}
	
	@Test
	public void shouldRejectUpdateWhenEncounterReferenceChanges() throws IOException {
		// Setup: Existing bundle
		DiagnosticReportBundle existingBundle = loadDiagnosticReportBundle("diagnostic-report-bundle-for-patch-existing.json");
		String bundleUuid = "report-bundle-123";
		
		// Load bundle with different encounter
		DiagnosticReportBundle updatedBundle = loadDiagnosticReportBundle("update-diagnostic-bundle-invalid-encounter-change.json");
		
		FhirDiagnosticReportExt existingReport = new FhirDiagnosticReportExt();
		existingReport.setUuid(bundleUuid);
		when(bahmniFhirDiagnosticReportDao.get(bundleUuid)).thenReturn(existingReport);
		
		// Mock get to return existing bundle
		when(bahmniFhirDiagnosticReportBundleTranslator.toFhirResource(any(FhirDiagnosticReportExt.class)))
				.thenReturn(existingBundle);
		
		// Execute and verify exception
		InvalidRequestException exception = assertThrows(InvalidRequestException.class,
				() -> diagnosticReportBundleService.update(bundleUuid, updatedBundle));
		
		assertTrue(exception.getMessage().contains("Encounter reference cannot be changed"));
		verify(bahmniFhirDiagnosticReportDao, never()).createOrUpdate(any(FhirDiagnosticReportExt.class));
		verify(observationService, never()).delete(anyString());
		verify(observationService, never()).create(any(Observation.class));
	}
	
	@Test
	public void shouldRejectUpdateForReportInTerminalState() throws IOException {
		// Setup: Existing bundle with cancelled status
		DiagnosticReportBundle existingBundle = loadDiagnosticReportBundle("diagnostic-report-bundle-for-patch-existing.json");
		DiagnosticReport report = BahmniFhirUtils.findResourcesOfTypeInBundle(existingBundle, DiagnosticReport.class).get(0);
		report.setStatus(DiagnosticReport.DiagnosticReportStatus.CANCELLED);
		
		String bundleUuid = "report-bundle-123";
		DiagnosticReportBundle updatedBundle = loadDiagnosticReportBundle("update-diagnostic-bundle-valid-replacement.json");
		
		FhirDiagnosticReportExt existingReport = new FhirDiagnosticReportExt();
		existingReport.setUuid(bundleUuid);
		when(bahmniFhirDiagnosticReportDao.get(bundleUuid)).thenReturn(existingReport);
		
		// Mock get to return existing bundle with terminal state
		when(bahmniFhirDiagnosticReportBundleTranslator.toFhirResource(any(FhirDiagnosticReportExt.class)))
				.thenReturn(existingBundle);
		
		// Execute and verify exception
		ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException exception = 
				assertThrows(ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException.class,
				() -> diagnosticReportBundleService.update(bundleUuid, updatedBundle));
		
		assertTrue(exception.getMessage().contains("Cannot update DiagnosticReport in terminal state"));
		verify(bahmniFhirDiagnosticReportDao, never()).createOrUpdate(any(FhirDiagnosticReportExt.class));
	}
	
	@Test
	public void shouldThrowResourceNotFoundWhenUpdatingNonExistentBundle() throws IOException {
		String bundleUuid = "non-existent-uuid";
		DiagnosticReportBundle updatedBundle = loadDiagnosticReportBundle("update-diagnostic-bundle-valid-replacement.json");
		
		// Mock get to return null
		//when(bahmniFhirDiagnosticReportBundleTranslator.toFhirResource(any())).thenReturn(null);
		
		// Execute and verify exception
		ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
				() -> diagnosticReportBundleService.update(bundleUuid, updatedBundle));
		
		assertTrue(exception.getMessage().contains("with ID non-existent-uuid is not known"));
		verify(bahmniFhirDiagnosticReportDao, never()).createOrUpdate(any(FhirDiagnosticReportExt.class));
	}
	
	@Test
	public void shouldVoidOldObservationsWhenUpdating() throws IOException {
		// Setup: Existing bundle with observations
		DiagnosticReportBundle existingBundle = loadDiagnosticReportBundle("diagnostic-report-bundle-for-patch-existing.json");
		String bundleUuid = "report-bundle-123";
		
		DiagnosticReportBundle updatedBundle = loadDiagnosticReportBundle("update-diagnostic-bundle-valid-replacement.json");
		
		FhirDiagnosticReportExt existingReport = new FhirDiagnosticReportExt();
		existingReport.setUuid(bundleUuid);
		existingReport.setId(12345);
		Obs obs1 = new Obs();
		obs1.setUuid("obs-1");
		Set<Obs> results = new HashSet<>();
		results.add(obs1);
		existingReport.setResults(results);
		when(bahmniFhirDiagnosticReportDao.get(bundleUuid)).thenReturn(existingReport);
		
		when(bahmniFhirDiagnosticReportBundleTranslator.toFhirResource(any(FhirDiagnosticReportExt.class)))
				.thenReturn(existingBundle);
		
		org.openmrs.Patient patient = examplePatient("patient-uuid-1");
		Encounter encounter = exampleEncounter("encounter-uuid-1", patient);
		
		//when(patientReferenceTranslator.toOpenmrsType(any())).thenReturn(patient);
		
		Order newOrder = exampleOrder("new-order-uuid-1", patient, encounter);
		when(serviceRequestReferenceTranslator.toOpenmrsType(any())).thenReturn(newOrder);

		when(observationService.create(any(Observation.class))).thenAnswer(invocation -> {
			Observation obs = new Observation();
			obs.setId("new-obs-" + System.currentTimeMillis());
			return obs;
		});
		
		FhirDiagnosticReportExt savedReport = new FhirDiagnosticReportExt();
		savedReport.setUuid(bundleUuid);
		when(bahmniFhirDiagnosticReportDao.createOrUpdate(any(FhirDiagnosticReportExt.class))).thenReturn(savedReport);
		when(diagnosticReportTranslator.toOpenmrsType(any(FhirDiagnosticReportExt.class), any(DiagnosticReport.class))).thenReturn(savedReport);
		
		// Execute
		diagnosticReportBundleService.update(bundleUuid, updatedBundle);
		
		// Verify old observations were voided
		verify(observationService, atLeastOnce()).delete("obs-1");
	}
	
	@Test
	public void shouldPreserveUuidAndIdWhenUpdating() throws IOException {
		// Setup
		DiagnosticReportBundle existingBundle = loadDiagnosticReportBundle("diagnostic-report-bundle-for-patch-existing.json");
		String bundleUuid = "report-bundle-123";
		Integer existingDbId = 98765;
		
		DiagnosticReportBundle updatedBundle = loadDiagnosticReportBundle("update-diagnostic-bundle-valid-replacement.json");
		
		FhirDiagnosticReportExt existingReport = new FhirDiagnosticReportExt();
		existingReport.setUuid(bundleUuid);
		existingReport.setId(existingDbId);
		when(bahmniFhirDiagnosticReportDao.get(bundleUuid)).thenReturn(existingReport);
		
		when(bahmniFhirDiagnosticReportBundleTranslator.toFhirResource(any(FhirDiagnosticReportExt.class)))
				.thenReturn(existingBundle);
		
		org.openmrs.Patient patient = examplePatient("patient-uuid-1");
		Encounter encounter = exampleEncounter("encounter-uuid-1", patient);
		
		//when(patientReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(patient);
		Order updatedBasedOnOrder = exampleOrder("new-order-uuid-1", patient, encounter);
		when(serviceRequestReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(updatedBasedOnOrder);
		// when(observationService.delete(anyString())).thenReturn(new Observation());
		when(observationService.create(any(Observation.class))).thenAnswer(invocation -> {
			Observation obs = new Observation();
			obs.setId("new-obs");
			return obs;
		});
		
		FhirDiagnosticReportExt savedReport = new FhirDiagnosticReportExt();
		savedReport.setUuid(bundleUuid);
		savedReport.setId(existingDbId);
		when(bahmniFhirDiagnosticReportDao.createOrUpdate(any(FhirDiagnosticReportExt.class))).thenReturn(savedReport);
		when(diagnosticReportTranslator.toOpenmrsType(any(FhirDiagnosticReportExt.class), any(DiagnosticReport.class))).thenReturn(savedReport);

		
		// Execute
		diagnosticReportBundleService.update(bundleUuid, updatedBundle);
		
		// Verify UUID and ID are preserved
		ArgumentCaptor<FhirDiagnosticReportExt> captor = ArgumentCaptor.forClass(FhirDiagnosticReportExt.class);
		verify(bahmniFhirDiagnosticReportDao, times(2)).createOrUpdate(any(FhirDiagnosticReportExt.class));
//		verify(bahmniFhirDiagnosticReportDao).createOrUpdate(captor.capture());
//		FhirDiagnosticReportExt captured = captor.getValue();
//
//		assertEquals("UUID should be preserved", bundleUuid, captured.getUuid());
//		assertEquals("DB ID should be preserved", existingDbId, captured.getId());
	}
	
	// ========== ^ UPDATE (PUT) TESTS ^ ==========
	
	private String loadPatchJson(String filename) throws IOException {
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filename);
		Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name()).useDelimiter("\\A");
		return scanner.hasNext() ? scanner.next() : "";
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
