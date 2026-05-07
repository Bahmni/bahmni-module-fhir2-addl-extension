package org.bahmni.module.fhir2addlextension.api.service.impl;

import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirImagingStudyDao;
import org.bahmni.module.fhir2addlextension.api.model.FhirImagingStudy;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniImagingStudySearchParams;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirImagingStudyService;
import org.bahmni.module.fhir2addlextension.api.translator.BahmniFhirImagingStudyTranslator;
import org.bahmni.module.fhir2addlextension.api.translator.BahmniOrderReferenceTranslator;
import org.bahmni.module.fhir2addlextension.api.translator.impl.BahmniFhirImagingStudyTranslatorImpl;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.api.db.ContextDAO;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.FhirGlobalPropertyService;
import org.openmrs.module.fhir2.api.FhirObservationService;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryBundleProvider;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.translators.*;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants.FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION;
import static org.bahmni.module.fhir2addlextension.api.TestDataFactory.loadResourceFromFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BahmniFhirImagingStudyServiceImplTest {
	
	@Mock
	private ContextDAO contextDAO;
	
	@Mock
	private UserContext userContext;
	
	@Mock
	private User user;
	
	@Mock
	private BahmniFhirImagingStudyDao imagingStudyDao;
	
	@Mock
	private BahmniOrderReferenceTranslator basedOnReferenceTranslator;
	
	@Mock
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Mock
	private LocationReferenceTranslator locationReferenceTranslator;
	
	@Mock
	private PractitionerReferenceTranslator<Provider> practitionerReferenceTranslator;
	
	@Mock
	private SearchQueryInclude<ImagingStudy> searchQueryInclude;
	
	@Mock
	private SearchQuery<FhirImagingStudy, ImagingStudy, BahmniFhirImagingStudyDao, BahmniFhirImagingStudyTranslator, SearchQueryInclude<ImagingStudy>> searchQuery;
	
	@Mock
	private FhirGlobalPropertyService globalPropertyService;
	
	@Mock
	private FhirObservationService fhirObservationService;
	
	@Mock(lenient = true)
	private ObservationTranslator observationTranslator;
	
	@Mock
	private EncounterReferenceTranslator<Encounter> encounterReferenceTranslator;
	
	@Mock(lenient = true)
	private ObservationReferenceTranslator observationReferenceTranslator;
	
	private BahmniFhirImagingStudyTranslator imagingStudyTranslator;
	
	private BahmniFhirImagingStudyService fhirImagingStudyService;
	
	private org.openmrs.Patient openmrsPatient;
	
	private Order openmrsOrder;
	
	private Location openmrsLocation;
	
	private static final String IMAGING_STUDY_UUID = "imaging-study-uuid-123";
	
	private static final String PATIENT_UUID = "patient-uuid-456";
	
	private static final String SERVICE_REQUEST_UUID = "service-request-uuid-789";
	
	@Before
    public void setUp() {
        when(userContext.getAuthenticatedUser()).thenReturn(user);
        Context.setDAO(contextDAO);
        Context.openSession();
        Context.setUserContext(userContext);

        openmrsPatient = new Patient();
        openmrsPatient.setUuid(PATIENT_UUID);

        openmrsOrder = new Order();
        openmrsOrder.setUuid(SERVICE_REQUEST_UUID);

        openmrsLocation = new Location();
        openmrsLocation.setUuid("location-uuid");
        openmrsLocation.setName("Test Location");

        imagingStudyTranslator = new BahmniFhirImagingStudyTranslatorImpl(basedOnReferenceTranslator,
                patientReferenceTranslator, locationReferenceTranslator, practitionerReferenceTranslator,
                encounterReferenceTranslator);
        fhirImagingStudyService = new BahmniFhirImagingStudyServiceImpl(
                imagingStudyDao, imagingStudyTranslator,
                searchQueryInclude, searchQuery,
                fhirObservationService, observationTranslator,
                observationReferenceTranslator,
                encounterReferenceTranslator) {

            @Override
            protected void validateObject(FhirImagingStudy object) {
            }
        };

        when(patientReferenceTranslator.toFhirResource(any(org.openmrs.Patient.class)))
                .thenAnswer(invocation -> {
                    org.openmrs.Patient patient = invocation.getArgument(0);
                    Reference ref = new Reference();
                    ref.setReference("Patient/" + patient.getUuid());
                    return ref;
                });

        when(basedOnReferenceTranslator.toFhirResource(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    if (order == null) return null;
                    Reference ref = new Reference();
                    ref.setReference("ServiceRequest/" + order.getUuid());
                    return ref;
                });

        when(locationReferenceTranslator.toFhirResource(any(Location.class)))
                .thenAnswer(invocation -> {
                    Location location = invocation.getArgument(0);
                    if (location == null) return null;
                    Reference ref = new Reference();
                    ref.setReference("Location/" + location.getUuid());
                    return ref;
                });
        
        // Default mock for encounterReferenceTranslator - returns RADIOLOGY QUALITY ASSESSMENT encounter
        when(encounterReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
            Reference ref = invocation.getArgument(0);
            if (ref == null || ref.getReference() == null) return null;
            Encounter encounter = new Encounter();
            // Extract UUID from reference like "Encounter/uuid"
            String refString = ref.getReference();
            String encounterUuid = refString.contains("/") ? refString.substring(refString.lastIndexOf("/") + 1) : refString;
            encounter.setUuid(encounterUuid);
            org.openmrs.EncounterType encounterType = new org.openmrs.EncounterType();
            encounterType.setName("RADIOLOGY QUALITY ASSESSMENT");
            encounter.setEncounterType(encounterType);
            return encounter;
        });
    }
	
	@Test
    public void shouldCreateImagingStudy() throws IOException {
        Location studyLocation = new Location();
        studyLocation.setName("Radiology Center");
        studyLocation.setUuid("example-radiology-center");

        IBaseResource fhirResource = loadResourceFromFile("example-imaging-study-registered.json");
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(locationReferenceTranslator.toOpenmrsType(
                ArgumentMatchers.argThat(reference -> {
                    return reference.getReference().equals("Location/example-radiology-center");
                })))
                .thenReturn(studyLocation);
        ImagingStudy imagingStudy = fhirImagingStudyService.create((ImagingStudy) fhirResource);
        Assert.assertFalse("Client Id is not accepted", imagingStudy.getId().equals("example-imaging-study"));
        Assert.assertEquals(ImagingStudy.ImagingStudyStatus.REGISTERED, imagingStudy.getStatus());
    }
	
	@Test
    public void shouldUpdateImagingStudy() throws IOException {
        Location studyLocation = new Location();
        studyLocation.setName("Radiology Center");
        studyLocation.setUuid("example-radiology-center");

        Provider performer = new Provider();
        performer.setUuid("example-technician-id");

        Order existingOrder = new Order();
        existingOrder.setUuid("existing-order-uuid");
        existingOrder.setFulfillerStatus(Order.FulfillerStatus.RECEIVED);

        FhirImagingStudy existingStudy = new FhirImagingStudy();
        existingStudy.setStudyInstanceUuid("urn:oid:2.16.124.113543.6003.1154777499.30246.19789.3503430045");
        existingStudy.setStatus(FhirImagingStudy.FhirImagingStudyStatus.REGISTERED);
        existingStudy.setOrder(existingOrder);

        IBaseResource fhirResource = loadResourceFromFile("example-imaging-study-performed.json");
        when(imagingStudyDao.get("example-imaging-study")).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(locationReferenceTranslator.toOpenmrsType(
                ArgumentMatchers.argThat(reference -> {
                    return reference.getReference().equals("Location/example-radiology-center");
                })))
                .thenReturn(studyLocation);
        when(practitionerReferenceTranslator.toOpenmrsType(
                ArgumentMatchers.argThat(reference -> {
                    return reference.getReference().equals("Practitioner/example-technician-id");
                })))
                .thenReturn(performer);
        when(practitionerReferenceTranslator.toFhirResource(any(Provider.class)))
                .thenAnswer(invocation -> {
                    Provider provider = invocation.getArgument(0);
                    if (provider == null) return null;
                    Reference ref = new Reference();
                    ref.setReference("Practitioner/" + provider.getUuid());
                    return ref;
                });
        ImagingStudy imagingStudy = fhirImagingStudyService.update("example-imaging-study", (ImagingStudy) fhirResource);
        Assert.assertEquals(ImagingStudy.ImagingStudyStatus.REGISTERED, imagingStudy.getStatus());
        Extension performerExt = imagingStudy.getExtensionByUrl(BahmniFhirConstants.FHIR_EXT_IMAGING_STUDY_PERFORMER);
        assertNotNull("Performer extension should not be null for Imaging Study", performerExt);
    }
	
	@Test
    public void searchImagingStudy_shouldReturnImagingStudiesByPatientReference() {
        ReferenceAndListParam patientReference = new ReferenceAndListParam()
                .addAnd(new ReferenceOrListParam().add(new ReferenceParam().setValue(PATIENT_UUID)));

        BahmniImagingStudySearchParams searchParams = new BahmniImagingStudySearchParams(
                patientReference, null, null, null, null);

        FhirImagingStudy fhirImagingStudy = createTestFhirImagingStudy(IMAGING_STUDY_UUID);

        SearchParameterMap theParams = searchParams.toSearchParameterMap();

        when(imagingStudyDao.getSearchResults(any())).thenReturn(Collections.singletonList(fhirImagingStudy));
        when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
                new SearchQueryBundleProvider<>(theParams, imagingStudyDao, imagingStudyTranslator,
                        globalPropertyService, searchQueryInclude));
        when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());

        IBundleProvider results = fhirImagingStudyService.searchImagingStudy(searchParams);

        List<IBaseResource> resultList = results.getResources(0, 10);

        assertThat(results, notNullValue());
        assertThat(resultList, not(empty()));
        assertThat(resultList, hasSize(equalTo(1)));
        assertThat(resultList.get(0), instanceOf(ImagingStudy.class));
        assertThat(((ImagingStudy) resultList.get(0)).getId(), equalTo(IMAGING_STUDY_UUID));
    }
	
	@Test
    public void searchImagingStudy_shouldReturnImagingStudiesById() {
        TokenAndListParam id = new TokenAndListParam().addAnd(new TokenParam(IMAGING_STUDY_UUID));

        BahmniImagingStudySearchParams searchParams = new BahmniImagingStudySearchParams(
                null, null, id, null, null);

        FhirImagingStudy fhirImagingStudy = createTestFhirImagingStudy(IMAGING_STUDY_UUID);

        SearchParameterMap theParams = searchParams.toSearchParameterMap();

        when(imagingStudyDao.getSearchResults(any())).thenReturn(Collections.singletonList(fhirImagingStudy));
        when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
                new SearchQueryBundleProvider<>(theParams, imagingStudyDao, imagingStudyTranslator,
                        globalPropertyService, searchQueryInclude));
        when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());

        IBundleProvider results = fhirImagingStudyService.searchImagingStudy(searchParams);

        List<IBaseResource> resultList = results.getResources(0, 10);

        assertThat(results, notNullValue());
        assertThat(resultList, not(empty()));
        assertThat(resultList, hasSize(equalTo(1)));
        assertThat(resultList.get(0), instanceOf(ImagingStudy.class));
    }
	
	@Test
    public void searchImagingStudy_shouldReturnImagingStudiesByBasedOnReference() {
        ReferenceAndListParam basedOnReference = new ReferenceAndListParam()
                .addAnd(new ReferenceOrListParam().add(new ReferenceParam().setValue(SERVICE_REQUEST_UUID)));

        BahmniImagingStudySearchParams searchParams = new BahmniImagingStudySearchParams(
                null, basedOnReference, null, null, null);

        FhirImagingStudy fhirImagingStudy = createTestFhirImagingStudy(IMAGING_STUDY_UUID);

        SearchParameterMap theParams = searchParams.toSearchParameterMap();

        when(imagingStudyDao.getSearchResults(any())).thenReturn(Collections.singletonList(fhirImagingStudy));
        when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
                new SearchQueryBundleProvider<>(theParams, imagingStudyDao, imagingStudyTranslator,
                        globalPropertyService, searchQueryInclude));
        when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());

        IBundleProvider results = fhirImagingStudyService.searchImagingStudy(searchParams);

        List<IBaseResource> resultList = results.getResources(0, 10);

        assertThat(results, notNullValue());
        assertThat(resultList, not(empty()));
        assertThat(resultList, hasSize(equalTo(1)));
        assertThat(resultList.get(0), instanceOf(ImagingStudy.class));
    }
	
	@Test(expected = UnsupportedOperationException.class)
	public void searchImagingStudy_shouldThrowExceptionWhenNoRequiredParametersProvided() {
		BahmniImagingStudySearchParams searchParams = new BahmniImagingStudySearchParams(null, null, null, null, null);
		
		fhirImagingStudyService.searchImagingStudy(searchParams);
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void searchImagingStudy_shouldThrowExceptionWhenPatientReferenceIsEmpty() {
		ReferenceAndListParam emptyPatientReference = new ReferenceAndListParam();
		BahmniImagingStudySearchParams searchParams = new BahmniImagingStudySearchParams(emptyPatientReference, null, null,
		        null, null);
		
		fhirImagingStudyService.searchImagingStudy(searchParams);
	}
	
	@Test
	public void searchImagingStudy_shouldPassCorrectSearchParameterMapToDao() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(PATIENT_UUID)));
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(SERVICE_REQUEST_UUID)));
		TokenAndListParam id = new TokenAndListParam().addAnd(new TokenParam(IMAGING_STUDY_UUID));
		DateRangeParam lastUpdated = new DateRangeParam().setLowerBound("2023-01-01").setUpperBound("2023-12-31");
		SortSpec sort = new SortSpec("_lastUpdated");
		
		BahmniImagingStudySearchParams searchParams = new BahmniImagingStudySearchParams(patientReference, basedOnReference,
		        id, lastUpdated, sort);
		
		fhirImagingStudyService.searchImagingStudy(searchParams);
		
		ArgumentCaptor<SearchParameterMap> mapCaptor = ArgumentCaptor.forClass(SearchParameterMap.class);
		verify(searchQuery).getQueryResults(mapCaptor.capture(), eq(imagingStudyDao), eq(imagingStudyTranslator),
		    eq(searchQueryInclude));
		
		SearchParameterMap actualMap = mapCaptor.getValue();
		
		assertThat(actualMap.getParameters(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER), notNullValue());
		assertEquals(patientReference, actualMap.getParameters(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER).get(0)
		        .getParam());
		
		assertThat(actualMap.getParameters(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER), notNullValue());
		assertEquals(basedOnReference, actualMap.getParameters(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER).get(0)
		        .getParam());
		
		assertThat(actualMap.getParameters(FhirConstants.COMMON_SEARCH_HANDLER), notNullValue());
		assertThat(actualMap.getParameters(FhirConstants.COMMON_SEARCH_HANDLER).size(), greaterThanOrEqualTo(2));
	}
	
	@Test
    public void searchImagingStudy_shouldReturnEmptyListWhenNoResultsFound() {
        ReferenceAndListParam patientReference = new ReferenceAndListParam()
                .addAnd(new ReferenceOrListParam().add(new ReferenceParam().setValue(PATIENT_UUID)));

        BahmniImagingStudySearchParams searchParams = new BahmniImagingStudySearchParams(
                patientReference, null, null, null, null);

        SearchParameterMap theParams = searchParams.toSearchParameterMap();

        when(imagingStudyDao.getSearchResults(any())).thenReturn(Collections.emptyList());
        when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
                new SearchQueryBundleProvider<>(theParams, imagingStudyDao, imagingStudyTranslator,
                        globalPropertyService, searchQueryInclude));
        when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());

        IBundleProvider results = fhirImagingStudyService.searchImagingStudy(searchParams);

        List<IBaseResource> resultList = results.getResources(0, 10);

        assertThat(results, notNullValue());
        assertThat(resultList, empty());
    }
	
	private FhirImagingStudy createTestFhirImagingStudy(String uuid) {
		FhirImagingStudy study = new FhirImagingStudy();
		study.setUuid(uuid);
		study.setStudyInstanceUuid("urn:oid:2.16.124.113543.6003.1154777499.30246.19789." + uuid);
		study.setStatus(FhirImagingStudy.FhirImagingStudyStatus.AVAILABLE);
		study.setSubject(openmrsPatient);
		study.setOrder(openmrsOrder);
		study.setLocation(openmrsLocation);
		study.setDescription("Test imaging study");
		study.setDateStarted(new java.util.Date());
		return study;
	}
	
	/**
	 * Helper method to add encounter references to all contained observations in an ImagingStudy
	 */
	private void addEncounterToContainedObservations(ImagingStudy imagingStudy, String encounterUuid) {
		if (!imagingStudy.hasContained()) {
			return;
		}
		imagingStudy.getContained().forEach(resource -> {
			if (resource instanceof Observation) {
				Observation obs = (Observation) resource;
				if (!obs.hasEncounter() || obs.getEncounter().getReference() == null || 
						obs.getEncounter().getReference().isEmpty()) {
					obs.setEncounter(new Reference("Encounter/" + encounterUuid));
				}
			}
		});
	}
	
	@Test
    public void testFetchWithQualityAssessment_shouldReturnStudyWithContainedObservations() {
        String studyId = "test-study-uuid";
        FhirImagingStudy study = createExistingStudyWithEncounter(studyId);

        org.openmrs.Obs obs1 = new org.openmrs.Obs();
        obs1.setUuid("obs-uuid-1");
        org.openmrs.Obs obs2 = new org.openmrs.Obs();
        obs2.setUuid("obs-uuid-2");

        Set<org.openmrs.Obs> assessment = new LinkedHashSet<>();
        assessment.add(obs1);
        assessment.add(obs2);
        study.setAssessment(assessment);

        when(imagingStudyDao.get(studyId)).thenReturn(study);
        when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
            org.openmrs.Obs obs = invocation.getArgument(0);
            Observation fhirObs = new Observation();
            fhirObs.setId(obs.getUuid());
            return fhirObs;
        });

        ImagingStudy result = fhirImagingStudyService.fetchWithQualityAssessment(studyId);

        assertNotNull(result);
        Assert.assertEquals(studyId, result.getId());
        Assert.assertEquals(2, result.getContained().size());
        Assert.assertEquals(2, result.getExtensionsByUrl(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION).size());
    }
	
	@Test(expected = ResourceNotFoundException.class)
	public void testFetchWithQualityAssessment_shouldThrowExceptionWhenStudyNotFound() {
		String studyId = "non-existent-uuid";
		
		when(imagingStudyDao.get(studyId)).thenReturn(null);
		
		fhirImagingStudyService.fetchWithQualityAssessment(studyId);
	}
	
	@Test
	public void testSubmitQualityAssessment_shouldNotThrowExceptionWhenNoExtensions() {
		ImagingStudy request = new ImagingStudy();
		request.setId("test-study-uuid");
		request.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);
		request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.instance");
		request.setSubject(new Reference("Patient/" + PATIENT_UUID));
		
		FhirImagingStudy newStudy = createTestFhirImagingStudy("test-study-uuid");
		when(imagingStudyDao.createOrUpdate(any())).thenReturn(newStudy);
		when(patientReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(openmrsPatient);
		
		ImagingStudy result = fhirImagingStudyService.create(request);
		
		assertNotNull(result);
		Assert.assertEquals(0, result.getExtensionsByUrl(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION).size());
	}
	
	@Test
	public void testSubmitQualityAssessment_shouldNotThrowExceptionWhenNoContainedResources() {
		String studyId = "test-study-uuid";
		ImagingStudy request = new ImagingStudy();
		request.setId(studyId);
		request.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);
		request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.instance");
		request.setSubject(new Reference("Patient/" + PATIENT_UUID));
		request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#obs-1"));
		
		FhirImagingStudy newStudy = createTestFhirImagingStudy(studyId);
		when(imagingStudyDao.createOrUpdate(any())).thenReturn(newStudy);
		when(patientReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(openmrsPatient);
		
		ImagingStudy result = fhirImagingStudyService.create(request);
		assertNotNull(result);
	}
	
	private FhirImagingStudy createExistingStudyWithEncounter(String uuid) {
		FhirImagingStudy study = createTestFhirImagingStudy(uuid);
		Encounter encounter = new Encounter();
		encounter.setUuid("encounter-uuid-default");
		study.setEncounter(encounter);
		return study;
	}
	
	@Test
    public void testFetchWithQualityAssessment_shouldHandleEmptyResults() {
        String studyId = "study-without-assessment";
        FhirImagingStudy study = createExistingStudyWithEncounter(studyId);
        study.setAssessment(new LinkedHashSet<>());

        when(imagingStudyDao.get(studyId)).thenReturn(study);

        ImagingStudy result = fhirImagingStudyService.fetchWithQualityAssessment(studyId);

        assertNotNull(result);
        Assert.assertEquals(0, result.getContained().size());
        Assert.assertEquals(0, result.getExtensionsByUrl(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION).size());
    }
	
	@Test
	public void testFetchWithQualityAssessment_shouldHandleNullResults() {
		String studyId = "study-null-assessment";
		FhirImagingStudy study = createExistingStudyWithEncounter(studyId);
		study.setAssessment(null);
		
		when(imagingStudyDao.get(studyId)).thenReturn(study);
		
		ImagingStudy result = fhirImagingStudyService.fetchWithQualityAssessment(studyId);
		
		assertNotNull(result);
		Assert.assertEquals(0, result.getContained().size());
		Assert.assertEquals(0, result.getExtensionsByUrl(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION).size());
	}
	
	@Test
    public void testUpdateWithQualityAssessments_shouldVoidExistingAndCreateNew() throws IOException {
        String studyId = "18046e64-cf94-4adf-b0d3-a83aa9fb165a";
        ImagingStudy request = (ImagingStudy) loadResourceFromFile("example-imaging-study-with-quality-assessment.json");
        request.setId(studyId);
        
        // Ensure contained observations have the correct encounter
        String qualityEncounterUuid = "quality-radiology-encounter-uuid";
        request.getContained().forEach(resource -> {
            if (resource instanceof Observation) {
                Observation obs = (Observation) resource;
                obs.setEncounter(new Reference("Encounter/" + qualityEncounterUuid));
            }
        });

        FhirImagingStudy existingStudy = createExistingStudyWithRadiologyEncounter(studyId);
        org.openmrs.Obs existingObs1 = new org.openmrs.Obs();
        existingObs1.setUuid("existing-obs-1");
        org.openmrs.Obs existingObs2 = new org.openmrs.Obs();
        existingObs2.setUuid("existing-obs-2");
        Set<org.openmrs.Obs> existingAssessment = new LinkedHashSet<>();
        existingAssessment.add(existingObs1);
        existingAssessment.add(existingObs2);
        existingStudy.setAssessment(existingAssessment);

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Provider performer = new Provider();
        performer.setUuid("60b31d2a-1d0c-11f1-b099-5a3ed7acdb7e");
        when(practitionerReferenceTranslator.toOpenmrsType(any())).thenReturn(performer);
        
        // Mock the quality encounter
        Encounter qualityEncounter = new Encounter();
        qualityEncounter.setUuid(qualityEncounterUuid);
        org.openmrs.EncounterType encounterType = new org.openmrs.EncounterType();
        encounterType.setName("RADIOLOGY QUALITY ASSESSMENT");
        qualityEncounter.setEncounterType(encounterType);
        
        when(encounterReferenceTranslator.toOpenmrsType(ArgumentMatchers.argThat(ref -> 
            ref != null && ref.getReference() != null && ref.getReference().contains(qualityEncounterUuid))))
            .thenReturn(qualityEncounter);

        setupObservationMocks();

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
        verify(fhirObservationService, times(1)).delete("existing-obs-1");
        verify(fhirObservationService, times(1)).delete("existing-obs-2");
        verify(fhirObservationService, times(3)).create(any(Observation.class));
    }
	
	@Test(expected = InvalidRequestException.class)
	public void testUpdateWithQualityAssessments_shouldThrowExceptionWhenEncounterNotRadiology() throws IOException {
		String studyId = "study-with-wrong-encounter-type";
		ImagingStudy request = (ImagingStudy) loadResourceFromFile("example-imaging-study-with-quality-assessment.json");
		request.setId(studyId);
		
		// Update contained observations to have wrong encounter type
		String wrongEncounterUuid = "wrong-encounter-uuid";
		request.getContained().forEach(resource -> {
			if (resource instanceof Observation) {
				Observation obs = (Observation) resource;
				obs.setEncounter(new Reference("Encounter/" + wrongEncounterUuid));
			}
		});
		
		FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
		
		when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
		
		// Mock the encounter with wrong type
		Encounter wrongEncounter = new Encounter();
		wrongEncounter.setUuid(wrongEncounterUuid);
		org.openmrs.EncounterType encounterType = new org.openmrs.EncounterType();
		encounterType.setName("CONSULTATION");
		wrongEncounter.setEncounterType(encounterType);
		
		Reference wrongEncounterRef = new Reference("Encounter/" + wrongEncounterUuid);
		when(encounterReferenceTranslator.toOpenmrsType(ArgumentMatchers.argThat(ref -> 
			ref != null && ref.getReference() != null && ref.getReference().contains(wrongEncounterUuid))))
			.thenReturn(wrongEncounter);
		
		fhirImagingStudyService.update(studyId, request);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void testUpdateWithQualityAssessments_shouldThrowExceptionWhenEncounterMissing() throws IOException {
		String studyId = "study-without-encounter";
		ImagingStudy request = (ImagingStudy) loadResourceFromFile("example-imaging-study-with-quality-assessment.json");
		request.setId(studyId);
		
		// Remove encounter from contained observations
		request.getContained().forEach(resource -> {
			if (resource instanceof Observation) {
				Observation obs = (Observation) resource;
				obs.setEncounter(null);
			}
		});
		
		FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
		
		when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
		
		fhirImagingStudyService.update(studyId, request);
	}
	
	@Test
    public void testUpdateWithoutQualityAssessments_shouldUpdateNormally() throws IOException {
        Location studyLocation = new Location();
        studyLocation.setName("Radiology Center");
        studyLocation.setUuid("example-radiology-center");

        Provider performer = new Provider();
        performer.setUuid("example-technician-id");

        Order existingOrder = new Order();
        existingOrder.setUuid("existing-order-uuid");
        existingOrder.setFulfillerStatus(Order.FulfillerStatus.RECEIVED);

        FhirImagingStudy existingStudy = new FhirImagingStudy();
        existingStudy.setStudyInstanceUuid("urn:oid:2.16.124.113543.6003.1154777499.30246.19789.3503430045");
        existingStudy.setStatus(FhirImagingStudy.FhirImagingStudyStatus.REGISTERED);
        existingStudy.setOrder(existingOrder);

        IBaseResource fhirResource = loadResourceFromFile("example-imaging-study-performed.json");
        when(imagingStudyDao.get("example-imaging-study")).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(locationReferenceTranslator.toOpenmrsType(
                ArgumentMatchers.argThat(reference -> {
                    return reference.getReference().equals("Location/example-radiology-center");
                })))
                .thenReturn(studyLocation);
        when(practitionerReferenceTranslator.toOpenmrsType(
                ArgumentMatchers.argThat(reference -> {
                    return reference.getReference().equals("Practitioner/example-technician-id");
                })))
                .thenReturn(performer);
        when(practitionerReferenceTranslator.toFhirResource(any(Provider.class)))
                .thenAnswer(invocation -> {
                    Provider provider = invocation.getArgument(0);
                    if (provider == null) return null;
                    Reference ref = new Reference();
                    ref.setReference("Practitioner/" + provider.getUuid());
                    return ref;
                });

        ImagingStudy result = fhirImagingStudyService.update("example-imaging-study", (ImagingStudy) fhirResource);

        assertNotNull(result);
        Assert.assertEquals(ImagingStudy.ImagingStudyStatus.REGISTERED, result.getStatus());
        verify(fhirObservationService, times(0)).delete(any());
        verify(fhirObservationService, times(0)).create(any(Observation.class));
    }
	
	@Test(expected = InvalidRequestException.class)
	public void testUpdate_shouldThrowExceptionWhenUuidIsNull() throws IOException {
		ImagingStudy request = (ImagingStudy) loadResourceFromFile("example-imaging-study-performed.json");
		
		fhirImagingStudyService.update(null, request);
	}
	
	@Test(expected = ResourceNotFoundException.class)
	public void testUpdate_shouldThrowExceptionWhenStudyNotFound() throws IOException {
		String studyId = "non-existent-study";
		ImagingStudy request = (ImagingStudy) loadResourceFromFile("example-imaging-study-performed.json");
		request.setId(studyId);
		
		when(imagingStudyDao.get(studyId)).thenReturn(null);
		
		fhirImagingStudyService.update(studyId, request);
	}
	
	private FhirImagingStudy createExistingStudyWithRadiologyEncounter(String uuid) {
		FhirImagingStudy study = createTestFhirImagingStudy(uuid);
		Encounter encounter = new Encounter();
		encounter.setUuid("radiology-encounter-uuid");
		org.openmrs.EncounterType encounterType = new org.openmrs.EncounterType();
		encounterType.setName("RADIOLOGY QUALITY ASSESSMENT");
		encounter.setEncounterType(encounterType);
		study.setEncounter(encounter);
		return study;
	}
	
	@Test(expected = InvalidRequestException.class)
	public void testUpdate_shouldThrowExceptionWhenResourceIsNull() {
		fhirImagingStudyService.update("test-uuid", null);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void testUpdate_shouldThrowExceptionWhenResourceIdIsNull() {
		ImagingStudy request = new ImagingStudy();
		request.setId((String) null);
		
		fhirImagingStudyService.update("test-uuid", request);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void testUpdate_shouldThrowExceptionWhenIdMismatch() {
		ImagingStudy request = new ImagingStudy();
		request.setId("different-uuid");
		
		fhirImagingStudyService.update("test-uuid", request);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void testUpdateWithQualityAssessments_shouldThrowExceptionWhenEncounterTypeIsNull() throws IOException {
		String studyId = "study-with-null-encounter-type";
		ImagingStudy request = (ImagingStudy) loadResourceFromFile("example-imaging-study-with-quality-assessment.json");
		request.setId(studyId);
		
		// Update contained observations to have encounter with null type
		String nullTypeEncounterUuid = "null-type-encounter-uuid";
		request.getContained().forEach(resource -> {
			if (resource instanceof Observation) {
				Observation obs = (Observation) resource;
				obs.setEncounter(new Reference("Encounter/" + nullTypeEncounterUuid));
			}
		});
		
		FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
		
		when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
		
		// Mock the encounter with null type
		Encounter encounterWithNullType = new Encounter();
		encounterWithNullType.setUuid(nullTypeEncounterUuid);
		encounterWithNullType.setEncounterType(null);
		
		when(encounterReferenceTranslator.toOpenmrsType(ArgumentMatchers.argThat(ref -> 
			ref != null && ref.getReference() != null && ref.getReference().contains(nullTypeEncounterUuid))))
			.thenReturn(encounterWithNullType);
		
		fhirImagingStudyService.update(studyId, request);
	}
	
	@Test
    public void testUpdateWithQualityAssessments_shouldHandleEmptyExistingResults() throws IOException {
        String studyId = "study-with-empty-assessment";
        ImagingStudy request = (ImagingStudy) loadResourceFromFile("example-imaging-study-with-quality-assessment.json");
        request.setId(studyId);
        
        
        addEncounterToContainedObservations(request, "radiology-encounter-uuid");

        FhirImagingStudy existingStudy = createExistingStudyWithRadiologyEncounter(studyId);
        existingStudy.setAssessment(new LinkedHashSet<>());

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Provider performer = new Provider();
        performer.setUuid("60b31d2a-1d0c-11f1-b099-5a3ed7acdb7e");
        when(practitionerReferenceTranslator.toOpenmrsType(any())).thenReturn(performer);

        setupObservationMocks();

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
        verify(fhirObservationService, times(0)).delete(any());
        verify(fhirObservationService, times(3)).create(any(Observation.class));
    }
	
	@Test
    public void testUpdateWithQualityAssessments_shouldUpdateExistingObservationWhenAlreadyPersisted() throws IOException {
        String studyId = "18046e64-cf94-4adf-b0d3-a83aa9fb165a";
        ImagingStudy request = (ImagingStudy) loadResourceFromFile("example-imaging-study-with-quality-assessment.json");
        request.setId(studyId);
        
        
        addEncounterToContainedObservations(request, "radiology-encounter-uuid");

        FhirImagingStudy existingStudy = createExistingStudyWithRadiologyEncounter(studyId);
        existingStudy.setAssessment(new LinkedHashSet<>());

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Provider performer = new Provider();
        performer.setUuid("60b31d2a-1d0c-11f1-b099-5a3ed7acdb7e");
        when(practitionerReferenceTranslator.toOpenmrsType(any())).thenReturn(performer);

        AtomicInteger counter = new AtomicInteger(0);
        Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();

        List<Extension> qualityExts = request.getExtensionsByUrl(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION);
        String firstContainedRef = qualityExts.isEmpty() ? null
                : ((Reference) qualityExts.get(0).getValue()).getReference();
        String preExistingUuid = firstContainedRef != null && firstContainedRef.startsWith("#")
                ? firstContainedRef.substring(1)
                : null;

        Observation preExistingFhirObs = new Observation();
        if (preExistingUuid != null) {
            preExistingFhirObs.setId(preExistingUuid);
            when(fhirObservationService.get(preExistingUuid)).thenReturn(preExistingFhirObs);
        }

        when(fhirObservationService.update(any(String.class), any(Observation.class))).thenAnswer(invocation -> {
            Observation result2 = new Observation();
            String obsId = "updated-obs-" + counter.incrementAndGet();
            result2.setId(obsId);
            org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
            openmrsObs.setUuid(obsId);
            createdObsMap.put(obsId, openmrsObs);
            return result2;
        });

        when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
            Observation result2 = new Observation();
            String obsId = "new-obs-" + counter.incrementAndGet();
            result2.setId(obsId);
            org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
            openmrsObs.setUuid(obsId);
            createdObsMap.put(obsId, openmrsObs);
            return result2;
        });

        when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
            Reference ref = invocation.getArgument(0);
            if (ref != null && ref.getReference() != null && ref.getReference().startsWith("Observation/")) {
                String obsId = ref.getReference().substring("Observation/".length());
                return createdObsMap.get(obsId);
            }
            return null;
        });

        when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
            org.openmrs.Obs openmrsObs = invocation.getArgument(0);
            if (openmrsObs == null) return null;
            Observation fhirObs = new Observation();
            fhirObs.setId(openmrsObs.getUuid());
            return fhirObs;
        });

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
    }
	
	@Test
    public void testUpdateWithQualityAssessments_shouldSetEncounterReferenceOnObservations() throws IOException {
        String studyId = "18046e64-cf94-4adf-b0d3-a83aa9fb165a";
        ImagingStudy request = (ImagingStudy) loadResourceFromFile("example-imaging-study-with-quality-assessment.json");
        request.setId(studyId);
        
        
        addEncounterToContainedObservations(request, "radiology-encounter-uuid");

        Encounter encounter = new Encounter();
        encounter.setUuid("radiology-encounter-uuid");
        org.openmrs.EncounterType encounterType = new org.openmrs.EncounterType();
        encounterType.setName("RADIOLOGY QUALITY ASSESSMENT");
        encounter.setEncounterType(encounterType);

        FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
        existingStudy.setEncounter(encounter);
        existingStudy.setAssessment(new LinkedHashSet<>());

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Provider performer = new Provider();
        performer.setUuid("60b31d2a-1d0c-11f1-b099-5a3ed7acdb7e");
        when(practitionerReferenceTranslator.toOpenmrsType(any())).thenReturn(performer);

        setupObservationMocksWithEncounterCheck(encounter);

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
    }
	
	@Test
    public void testUpdateWithQualityAssessments_shouldHandleObservationWithEmptyEncounterReference() throws IOException {
        String studyId = "18046e64-cf94-4adf-b0d3-a83aa9fb165a";
        ImagingStudy request = (ImagingStudy) loadResourceFromFile("example-imaging-study-with-quality-assessment.json");
        request.setId(studyId);
        
        // Set empty encounter references first
        request.getContained().forEach(resource -> {
            if (resource instanceof Observation) {
                Observation obs = (Observation) resource;
                obs.setEncounter(new Reference(""));
            }
        });
        
        // Then add proper encounter references (helper will replace empty ones)
        addEncounterToContainedObservations(request, "radiology-encounter-uuid");

        FhirImagingStudy existingStudy = createExistingStudyWithRadiologyEncounter(studyId);
        existingStudy.setAssessment(new LinkedHashSet<>());

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Provider performer = new Provider();
        performer.setUuid("60b31d2a-1d0c-11f1-b099-5a3ed7acdb7e");
        when(practitionerReferenceTranslator.toOpenmrsType(any())).thenReturn(performer);

        setupObservationMocks();

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
    }
	
	@Test
    public void testUpdateWithQualityAssessments_shouldResolveHasMemberReferences() throws IOException {
        String studyId = "test-has-member-study";

        Encounter encounter = new Encounter();
        encounter.setUuid("radiology-encounter-uuid");
        org.openmrs.EncounterType encounterType = new org.openmrs.EncounterType();
        encounterType.setName("RADIOLOGY QUALITY ASSESSMENT");
        encounter.setEncounterType(encounterType);

        FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
        existingStudy.setEncounter(encounter);
        existingStudy.setAssessment(new LinkedHashSet<>());

        ImagingStudy request = new ImagingStudy();
        request.setId(studyId);
        request.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
        request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study");
        request.setSubject(new Reference("Patient/" + PATIENT_UUID));

        Observation parentObs = new Observation();
        parentObs.setId("#parent-obs-id");
        parentObs.setStatus(Observation.ObservationStatus.FINAL);
        parentObs.addHasMember(new Reference("#child-obs-id"));
        parentObs.setEncounter(new Reference("Encounter/" + encounter.getUuid()));

        Observation childObs = new Observation();
        childObs.setId("#child-obs-id");
        childObs.setStatus(Observation.ObservationStatus.FINAL);
        childObs.setEncounter(new Reference("Encounter/" + encounter.getUuid()));

        request.addContained(childObs);
        request.addContained(parentObs);

        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#parent-obs-id"));
        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#child-obs-id"));

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        when(encounterReferenceTranslator.toOpenmrsType(ArgumentMatchers.argThat(ref -> 
            ref != null && ref.getReference() != null && ref.getReference().contains(encounter.getUuid()))))
            .thenReturn(encounter);

        AtomicInteger counter = new AtomicInteger(0);
        Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();

        when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
            Observation result = new Observation();
            String obsId = "created-obs-" + counter.incrementAndGet();
            result.setId(obsId);
            org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
            openmrsObs.setUuid(obsId);
            createdObsMap.put(obsId, openmrsObs);
            return result;
        });

        when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
            Reference ref = invocation.getArgument(0);
            if (ref != null && ref.getReference() != null && ref.getReference().startsWith("Observation/")) {
                String obsId = ref.getReference().substring("Observation/".length());
                return createdObsMap.get(obsId);
            }
            return null;
        });

        when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
            org.openmrs.Obs obs = invocation.getArgument(0);
            if (obs == null) return null;
            Observation fhirObs = new Observation();
            fhirObs.setId(obs.getUuid());
            return fhirObs;
        });

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
        verify(fhirObservationService, times(2)).create(any(Observation.class));
    }
	
	@Test
    public void testUpdateWithQualityAssessments_shouldHandleHasMemberWithAbsoluteReference() throws IOException {
        String studyId = "test-absolute-ref-study";

        Encounter encounter = new Encounter();
        encounter.setUuid("radiology-encounter-uuid");
        org.openmrs.EncounterType encounterType = new org.openmrs.EncounterType();
        encounterType.setName("RADIOLOGY QUALITY ASSESSMENT");
        encounter.setEncounterType(encounterType);

        FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
        existingStudy.setEncounter(encounter);
        existingStudy.setAssessment(new LinkedHashSet<>());

        ImagingStudy request = new ImagingStudy();
        request.setId(studyId);
        request.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
        request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.abs");
        request.setSubject(new Reference("Patient/" + PATIENT_UUID));

        Observation obsWithAbsRef = new Observation();
        obsWithAbsRef.setId("#obs-with-abs-ref");
        obsWithAbsRef.setStatus(Observation.ObservationStatus.FINAL);
        obsWithAbsRef.addHasMember(new Reference("Observation/some-absolute-obs-uuid"));

        request.addContained(obsWithAbsRef);
        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#obs-with-abs-ref"));

        
        addEncounterToContainedObservations(request, encounter.getUuid());

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicInteger counter = new AtomicInteger(0);
        Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();

        when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
            Observation result = new Observation();
            String obsId = "created-obs-" + counter.incrementAndGet();
            result.setId(obsId);
            org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
            openmrsObs.setUuid(obsId);
            createdObsMap.put(obsId, openmrsObs);
            return result;
        });

        when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
            Reference ref = invocation.getArgument(0);
            if (ref != null && ref.getReference() != null && ref.getReference().startsWith("Observation/")) {
                String obsId = ref.getReference().substring("Observation/".length());
                return createdObsMap.get(obsId);
            }
            return null;
        });

        when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
            org.openmrs.Obs obs = invocation.getArgument(0);
            if (obs == null) return null;
            Observation fhirObs = new Observation();
            fhirObs.setId(obs.getUuid());
            return fhirObs;
        });

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
        verify(fhirObservationService, times(1)).create(any(Observation.class));
    }
	
	@Test
    public void testUpdateWithQualityAssessments_shouldHandleHasMemberWithPlainIdReference() throws IOException {
        String studyId = "test-plain-ref-study";

        Encounter encounter = new Encounter();
        encounter.setUuid("radiology-encounter-uuid");
        org.openmrs.EncounterType encounterType = new org.openmrs.EncounterType();
        encounterType.setName("RADIOLOGY QUALITY ASSESSMENT");
        encounter.setEncounterType(encounterType);

        FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
        existingStudy.setEncounter(encounter);
        existingStudy.setAssessment(new LinkedHashSet<>());

        ImagingStudy request = new ImagingStudy();
        request.setId(studyId);
        request.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
        request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.plain");
        request.setSubject(new Reference("Patient/" + PATIENT_UUID));

        Observation childObs = new Observation();
        childObs.setId("#child-plain-obs");
        childObs.setStatus(Observation.ObservationStatus.FINAL);

        Observation parentObs = new Observation();
        parentObs.setId("#parent-plain-obs");
        parentObs.setStatus(Observation.ObservationStatus.FINAL);
        parentObs.addHasMember(new Reference("child-plain-obs"));

        request.addContained(childObs);
        request.addContained(parentObs);
        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#child-plain-obs"));
        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#parent-plain-obs"));

        
        addEncounterToContainedObservations(request, encounter.getUuid());

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicInteger counter = new AtomicInteger(0);
        Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();

        when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
            Observation result = new Observation();
            String obsId = "created-obs-" + counter.incrementAndGet();
            result.setId(obsId);
            org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
            openmrsObs.setUuid(obsId);
            createdObsMap.put(obsId, openmrsObs);
            return result;
        });

        when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
            Reference ref = invocation.getArgument(0);
            if (ref != null && ref.getReference() != null && ref.getReference().startsWith("Observation/")) {
                String obsId = ref.getReference().substring("Observation/".length());
                return createdObsMap.get(obsId);
            }
            return null;
        });

        when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
            org.openmrs.Obs obs = invocation.getArgument(0);
            if (obs == null) return null;
            Observation fhirObs = new Observation();
            fhirObs.setId(obs.getUuid());
            return fhirObs;
        });

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
        verify(fhirObservationService, times(2)).create(any(Observation.class));
    }
	
	@Test
    public void testUpdateWithQualityAssessments_shouldSkipExtensionWithNonReferenceValue() throws IOException {
        String studyId = "test-non-reference-ext-study";

        Encounter encounter = new Encounter();
        encounter.setUuid("radiology-encounter-uuid");
        org.openmrs.EncounterType encounterType = new org.openmrs.EncounterType();
        encounterType.setName("RADIOLOGY QUALITY ASSESSMENT");
        encounter.setEncounterType(encounterType);

        FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
        existingStudy.setEncounter(encounter);
        existingStudy.setAssessment(new LinkedHashSet<>());

        ImagingStudy request = new ImagingStudy();
        request.setId(studyId);
        request.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
        request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.non.ref");
        request.setSubject(new Reference("Patient/" + PATIENT_UUID));

        Observation validObs = new Observation();
        validObs.setId("#valid-obs");
        validObs.setStatus(Observation.ObservationStatus.FINAL);
        request.addContained(validObs);

        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new StringType("not-a-reference"));
        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#valid-obs"));

        
        addEncounterToContainedObservations(request, encounter.getUuid());

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicInteger counter = new AtomicInteger(0);
        Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();

        when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
            Observation result = new Observation();
            String obsId = "created-obs-" + counter.incrementAndGet();
            result.setId(obsId);
            org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
            openmrsObs.setUuid(obsId);
            createdObsMap.put(obsId, openmrsObs);
            return result;
        });

        when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
            Reference ref = invocation.getArgument(0);
            if (ref != null && ref.getReference() != null && ref.getReference().startsWith("Observation/")) {
                String obsId = ref.getReference().substring("Observation/".length());
                return createdObsMap.get(obsId);
            }
            return null;
        });

        when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
            org.openmrs.Obs obs = invocation.getArgument(0);
            if (obs == null) return null;
            Observation fhirObs = new Observation();
            fhirObs.setId(obs.getUuid());
            return fhirObs;
        });

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
        verify(fhirObservationService, times(1)).create(any(Observation.class));
    }
	
	@Test
    public void testUpdateWithQualityAssessments_shouldSkipExtensionWithInvalidContainedRef() throws IOException {
        String studyId = "test-invalid-ref-study";

        Encounter encounter = new Encounter();
        encounter.setUuid("radiology-encounter-uuid");
        org.openmrs.EncounterType encounterType = new org.openmrs.EncounterType();
        encounterType.setName("RADIOLOGY QUALITY ASSESSMENT");
        encounter.setEncounterType(encounterType);

        FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
        existingStudy.setEncounter(encounter);
        existingStudy.setAssessment(new LinkedHashSet<>());

        ImagingStudy request = new ImagingStudy();
        request.setId(studyId);
        request.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
        request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.invalid.ref");
        request.setSubject(new Reference("Patient/" + PATIENT_UUID));

        Observation validObs = new Observation();
        validObs.setId("#valid-obs-2");
        validObs.setStatus(Observation.ObservationStatus.FINAL);
        request.addContained(validObs);

        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("Observation/not-contained"));
        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#valid-obs-2"));

        
        addEncounterToContainedObservations(request, encounter.getUuid());

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicInteger counter = new AtomicInteger(0);
        Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();

        when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
            Observation result = new Observation();
            String obsId = "created-obs-" + counter.incrementAndGet();
            result.setId(obsId);
            org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
            openmrsObs.setUuid(obsId);
            createdObsMap.put(obsId, openmrsObs);
            return result;
        });

        when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
            Reference ref = invocation.getArgument(0);
            if (ref != null && ref.getReference() != null && ref.getReference().startsWith("Observation/")) {
                String obsId = ref.getReference().substring("Observation/".length());
                return createdObsMap.get(obsId);
            }
            return null;
        });

        when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
            org.openmrs.Obs obs = invocation.getArgument(0);
            if (obs == null) return null;
            Observation fhirObs = new Observation();
            fhirObs.setId(obs.getUuid());
            return fhirObs;
        });

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
        verify(fhirObservationService, times(1)).create(any(Observation.class));
    }
	
	@Test
    public void testUpdateWithQualityAssessments_shouldHandleNullHasMemberReference() throws IOException {
        String studyId = "test-null-hasmember-study";

        Encounter encounter = new Encounter();
        encounter.setUuid("radiology-encounter-uuid");
        org.openmrs.EncounterType encounterType = new org.openmrs.EncounterType();
        encounterType.setName("RADIOLOGY QUALITY ASSESSMENT");
        encounter.setEncounterType(encounterType);

        FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
        existingStudy.setEncounter(encounter);
        existingStudy.setAssessment(new LinkedHashSet<>());

        ImagingStudy request = new ImagingStudy();
        request.setId(studyId);
        request.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
        request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.null.hasmember");
        request.setSubject(new Reference("Patient/" + PATIENT_UUID));

        Observation obsWithNullMemberRef = new Observation();
        obsWithNullMemberRef.setId("#obs-null-member");
        obsWithNullMemberRef.setStatus(Observation.ObservationStatus.FINAL);
        Reference nullRef = new Reference();
        obsWithNullMemberRef.addHasMember(nullRef);

        request.addContained(obsWithNullMemberRef);
        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#obs-null-member"));

        
        addEncounterToContainedObservations(request, encounter.getUuid());

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicInteger counter = new AtomicInteger(0);
        Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();

        when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
            Observation result = new Observation();
            String obsId = "created-obs-" + counter.incrementAndGet();
            result.setId(obsId);
            org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
            openmrsObs.setUuid(obsId);
            createdObsMap.put(obsId, openmrsObs);
            return result;
        });

        when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
            Reference ref = invocation.getArgument(0);
            if (ref != null && ref.getReference() != null && ref.getReference().startsWith("Observation/")) {
                String obsId = ref.getReference().substring("Observation/".length());
                return createdObsMap.get(obsId);
            }
            return null;
        });

        when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
            org.openmrs.Obs obs = invocation.getArgument(0);
            if (obs == null) return null;
            Observation fhirObs = new Observation();
            fhirObs.setId(obs.getUuid());
            return fhirObs;
        });

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
        verify(fhirObservationService, times(1)).create(any(Observation.class));
    }
	
	@Test
    public void testUpdateWithQualityAssessments_shouldCallUpdateWhenObservationAlreadyExists() throws IOException {
        String studyId = "test-update-existing-obs-study";

        Encounter encounter = new Encounter();
        encounter.setUuid("radiology-encounter-uuid");
        org.openmrs.EncounterType encounterType = new org.openmrs.EncounterType();
        encounterType.setName("RADIOLOGY QUALITY ASSESSMENT");
        encounter.setEncounterType(encounterType);

        FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
        existingStudy.setEncounter(encounter);
        existingStudy.setAssessment(new LinkedHashSet<>());

        String preExistingObsUuid = "pre-existing-obs-uuid-abc";

        ImagingStudy request = new ImagingStudy();
        request.setId(studyId);
        request.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
        request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.existing.obs");
        request.setSubject(new Reference("Patient/" + PATIENT_UUID));

        Observation existingObservation = new Observation();
        existingObservation.setId("#" + preExistingObsUuid);
        existingObservation.setStatus(Observation.ObservationStatus.FINAL);
        request.addContained(existingObservation);
        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#" + preExistingObsUuid));

        
        addEncounterToContainedObservations(request, encounter.getUuid());

        Observation preExistingFhirObs = new Observation();
        preExistingFhirObs.setId(preExistingObsUuid);
        when(fhirObservationService.get(preExistingObsUuid)).thenReturn(preExistingFhirObs);

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();

        when(fhirObservationService.update(eq(preExistingObsUuid), any(Observation.class))).thenAnswer(invocation -> {
            Observation result = new Observation();
            result.setId("updated-obs-id");
            org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
            openmrsObs.setUuid("updated-obs-id");
            createdObsMap.put("updated-obs-id", openmrsObs);
            return result;
        });

        when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
            Reference ref = invocation.getArgument(0);
            if (ref != null && ref.getReference() != null && ref.getReference().startsWith("Observation/")) {
                String obsId = ref.getReference().substring("Observation/".length());
                return createdObsMap.get(obsId);
            }
            return null;
        });

        when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
            org.openmrs.Obs obs = invocation.getArgument(0);
            if (obs == null) return null;
            Observation fhirObs = new Observation();
            fhirObs.setId(obs.getUuid());
            return fhirObs;
        });

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
        verify(fhirObservationService, times(1)).update(eq(preExistingObsUuid), any(Observation.class));
        verify(fhirObservationService, times(0)).create(any(Observation.class));
    }
	
	private void setupObservationMocks() {
        AtomicInteger counter = new AtomicInteger(0);
        Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();

        when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
            Observation resultObs = new Observation();
            String obsId = "new-obs-" + counter.incrementAndGet();
            resultObs.setId(obsId);

            org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
            openmrsObs.setUuid(obsId);
            createdObsMap.put(obsId, openmrsObs);

            return resultObs;
        });

        when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
            Reference ref = invocation.getArgument(0);
            if (ref != null && ref.getReference() != null && ref.getReference().startsWith("Observation/")) {
                String obsId = ref.getReference().substring("Observation/".length());
                return createdObsMap.get(obsId);
            }
            return null;
        });

        when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
            org.openmrs.Obs openmrsObs = invocation.getArgument(0);
            if (openmrsObs == null) return null;
            Observation fhirObs = new Observation();
            fhirObs.setId(openmrsObs.getUuid());
            return fhirObs;
        });
    }
	
	@Test
    public void testUpdateWithQualityAssessments_shouldSkipWhenContainedResourceIsNotObservation() throws IOException {
        String studyId = "test-non-obs-contained-study";

        Encounter encounter = new Encounter();
        encounter.setUuid("radiology-encounter-uuid");
        org.openmrs.EncounterType encounterType = new org.openmrs.EncounterType();
        encounterType.setName("RADIOLOGY QUALITY ASSESSMENT");
        encounter.setEncounterType(encounterType);

        FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
        existingStudy.setEncounter(encounter);
        existingStudy.setAssessment(new LinkedHashSet<>());

        ImagingStudy request = new ImagingStudy();
        request.setId(studyId);
        request.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
        request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.non.obs");
        request.setSubject(new Reference("Patient/" + PATIENT_UUID));

        org.hl7.fhir.r4.model.Practitioner nonObsContained = new org.hl7.fhir.r4.model.Practitioner();
        nonObsContained.setId("#not-an-obs");

        Observation validObs = new Observation();
        validObs.setId("#valid-obs-3");
        validObs.setStatus(Observation.ObservationStatus.FINAL);

        request.addContained(nonObsContained);
        request.addContained(validObs);

        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#not-an-obs"));
        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#valid-obs-3"));

        
        addEncounterToContainedObservations(request, encounter.getUuid());

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicInteger counter = new AtomicInteger(0);
        Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();

        when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
            Observation result = new Observation();
            String obsId = "created-obs-" + counter.incrementAndGet();
            result.setId(obsId);
            org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
            openmrsObs.setUuid(obsId);
            createdObsMap.put(obsId, openmrsObs);
            return result;
        });

        when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
            Reference ref = invocation.getArgument(0);
            if (ref != null && ref.getReference() != null && ref.getReference().startsWith("Observation/")) {
                String obsId = ref.getReference().substring("Observation/".length());
                return createdObsMap.get(obsId);
            }
            return null;
        });

        when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
            org.openmrs.Obs obs = invocation.getArgument(0);
            if (obs == null) return null;
            Observation fhirObs = new Observation();
            fhirObs.setId(obs.getUuid());
            return fhirObs;
        });

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
        verify(fhirObservationService, times(1)).create(any(Observation.class));
    }
	
	@Test
    public void testUpdateWithQualityAssessments_shouldHandleResourceNotFoundExceptionFromFindExistingObservation() throws IOException {
        String studyId = "test-resource-not-found-study";

        Encounter encounter = new Encounter();
        encounter.setUuid("radiology-encounter-uuid");
        org.openmrs.EncounterType encounterType = new org.openmrs.EncounterType();
        encounterType.setName("RADIOLOGY QUALITY ASSESSMENT");
        encounter.setEncounterType(encounterType);

        FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
        existingStudy.setEncounter(encounter);
        existingStudy.setAssessment(new LinkedHashSet<>());

        ImagingStudy request = new ImagingStudy();
        request.setId(studyId);
        request.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
        request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.rnf");
        request.setSubject(new Reference("Patient/" + PATIENT_UUID));

        String observationUuid = "some-existing-obs-uuid";
        Observation observation = new Observation();
        observation.setId("#" + observationUuid);
        observation.setStatus(Observation.ObservationStatus.FINAL);
        request.addContained(observation);
        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#" + observationUuid));

        
        addEncounterToContainedObservations(request, encounter.getUuid());

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(fhirObservationService.get(observationUuid)).thenThrow(new ResourceNotFoundException("Not found"));

        AtomicInteger counter = new AtomicInteger(0);
        Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();

        when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
            Observation result = new Observation();
            String obsId = "new-obs-" + counter.incrementAndGet();
            result.setId(obsId);
            org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
            openmrsObs.setUuid(obsId);
            createdObsMap.put(obsId, openmrsObs);
            return result;
        });

        when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
            Reference ref = invocation.getArgument(0);
            if (ref != null && ref.getReference() != null && ref.getReference().startsWith("Observation/")) {
                String obsId = ref.getReference().substring("Observation/".length());
                return createdObsMap.get(obsId);
            }
            return null;
        });

        when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
            org.openmrs.Obs obs = invocation.getArgument(0);
            if (obs == null) return null;
            Observation fhirObs = new Observation();
            fhirObs.setId(obs.getUuid());
            return fhirObs;
        });

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
        verify(fhirObservationService, times(1)).create(any(Observation.class));
        verify(fhirObservationService, times(0)).update(any(), any(Observation.class));
    }
	
	@Test
    public void testUpdateWithQualityAssessments_shouldSkipExtensionWithNullReferenceValue() throws IOException {
        String studyId = "test-null-ref-value-study";

        Encounter encounter = new Encounter();
        encounter.setUuid("radiology-encounter-uuid");
        org.openmrs.EncounterType encounterType = new org.openmrs.EncounterType();
        encounterType.setName("RADIOLOGY QUALITY ASSESSMENT");
        encounter.setEncounterType(encounterType);

        FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
        existingStudy.setEncounter(encounter);
        existingStudy.setAssessment(new LinkedHashSet<>());

        ImagingStudy request = new ImagingStudy();
        request.setId(studyId);
        request.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
        request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.null.ref.val");
        request.setSubject(new Reference("Patient/" + PATIENT_UUID));

        Observation validObs = new Observation();
        validObs.setId("#valid-obs-null-ref");
        validObs.setStatus(Observation.ObservationStatus.FINAL);
        request.addContained(validObs);

        Reference nullRefValue = new Reference();
        nullRefValue.setReference(null);
        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, nullRefValue);
        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#valid-obs-null-ref"));

        
        addEncounterToContainedObservations(request, encounter.getUuid());

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicInteger counter = new AtomicInteger(0);
        Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();

        when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
            Observation result = new Observation();
            String obsId = "created-obs-" + counter.incrementAndGet();
            result.setId(obsId);
            org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
            openmrsObs.setUuid(obsId);
            createdObsMap.put(obsId, openmrsObs);
            return result;
        });

        when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
            Reference ref = invocation.getArgument(0);
            if (ref != null && ref.getReference() != null && ref.getReference().startsWith("Observation/")) {
                String obsId = ref.getReference().substring("Observation/".length());
                return createdObsMap.get(obsId);
            }
            return null;
        });

        when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
            org.openmrs.Obs obs = invocation.getArgument(0);
            if (obs == null) return null;
            Observation fhirObs = new Observation();
            fhirObs.setId(obs.getUuid());
            return fhirObs;
        });

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
        verify(fhirObservationService, times(1)).create(any(Observation.class));
    }
	
	@Test
    public void testUpdateWithQualityAssessments_shouldHandleObservationWithNullIdElement() throws IOException {
        String studyId = "test-null-id-element-study";

        Encounter encounter = new Encounter();
        encounter.setUuid("radiology-encounter-uuid");
        org.openmrs.EncounterType encounterType = new org.openmrs.EncounterType();
        encounterType.setName("RADIOLOGY QUALITY ASSESSMENT");
        encounter.setEncounterType(encounterType);

        FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
        existingStudy.setEncounter(encounter);
        existingStudy.setAssessment(new LinkedHashSet<>());

        ImagingStudy request = new ImagingStudy();
        request.setId(studyId);
        request.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
        request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.null.id.element");
        request.setSubject(new Reference("Patient/" + PATIENT_UUID));

        Observation obsWithNullIdElement = new Observation();
        obsWithNullIdElement.setId("#obs-id-element-test");
        obsWithNullIdElement.setStatus(Observation.ObservationStatus.FINAL);
        request.addContained(obsWithNullIdElement);
        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#obs-id-element-test"));

        
        addEncounterToContainedObservations(request, encounter.getUuid());

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicInteger counter = new AtomicInteger(0);
        Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();

        when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
            Observation result = new Observation();
            String obsId = "created-obs-" + counter.incrementAndGet();
            result.setId(obsId);
            org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
            openmrsObs.setUuid(obsId);
            createdObsMap.put(obsId, openmrsObs);
            return result;
        });

        when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
            Reference ref = invocation.getArgument(0);
            if (ref != null && ref.getReference() != null && ref.getReference().startsWith("Observation/")) {
                String obsId = ref.getReference().substring("Observation/".length());
                return createdObsMap.get(obsId);
            }
            return null;
        });

        when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
            org.openmrs.Obs obs = invocation.getArgument(0);
            if (obs == null) return null;
            Observation fhirObs = new Observation();
            fhirObs.setId(obs.getUuid());
            return fhirObs;
        });

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
        verify(fhirObservationService, times(1)).create(any(Observation.class));
    }
	
	@Test
    public void testUpdateWithQualityAssessments_shouldHandleNullAssessmentSet() throws IOException {
        String studyId = "test-null-assessment-set";
        ImagingStudy request = (ImagingStudy) loadResourceFromFile("example-imaging-study-with-quality-assessment.json");
        request.setId(studyId);
        
        
        addEncounterToContainedObservations(request, "radiology-encounter-uuid");

        FhirImagingStudy existingStudy = createExistingStudyWithRadiologyEncounter(studyId);
        existingStudy.setAssessment(null);

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Provider performer = new Provider();
        performer.setUuid("60b31d2a-1d0c-11f1-b099-5a3ed7acdb7e");
        when(practitionerReferenceTranslator.toOpenmrsType(any())).thenReturn(performer);

        setupObservationMocks();

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
        verify(fhirObservationService, times(0)).delete(any());
        verify(fhirObservationService, times(3)).create(any(Observation.class));
    }
	
	@Test
    public void testUpdateWithQualityAssessments_shouldSkipQualityProcessingWhenExtensionsExistButNoContained() throws IOException {
        String studyId = "test-no-contained-study";

        Encounter encounter = new Encounter();
        encounter.setUuid("radiology-encounter-uuid");
        org.openmrs.EncounterType encounterType = new org.openmrs.EncounterType();
        encounterType.setName("RADIOLOGY QUALITY ASSESSMENT");
        encounter.setEncounterType(encounterType);

        FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
        existingStudy.setEncounter(encounter);
        existingStudy.setAssessment(new LinkedHashSet<>());

        ImagingStudy request = new ImagingStudy();
        request.setId(studyId);
        request.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
        request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.no.contained");
        request.setSubject(new Reference("Patient/" + PATIENT_UUID));
        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#missing-obs"));

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
        verify(fhirObservationService, times(0)).create(any(Observation.class));
        verify(fhirObservationService, times(0)).delete(any());
    }
	
	@Test
    public void testUpdateWithQualityAssessments_shouldHandleContainedResourceWithNullId() throws IOException {
        String studyId = "test-null-contained-id-study";

        Encounter encounter = new Encounter();
        encounter.setUuid("radiology-encounter-uuid");
        org.openmrs.EncounterType encounterType = new org.openmrs.EncounterType();
        encounterType.setName("RADIOLOGY QUALITY ASSESSMENT");
        encounter.setEncounterType(encounterType);

        FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
        existingStudy.setEncounter(encounter);
        existingStudy.setAssessment(new LinkedHashSet<>());

        ImagingStudy request = new ImagingStudy();
        request.setId(studyId);
        request.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
        request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.null.contained.id");
        request.setSubject(new Reference("Patient/" + PATIENT_UUID));

        Observation obsWithNullId = new Observation();
        obsWithNullId.setId((String) null);
        obsWithNullId.setStatus(Observation.ObservationStatus.FINAL);

        Observation validObs = new Observation();
        validObs.setId("#valid-contained-obs");
        validObs.setStatus(Observation.ObservationStatus.FINAL);

        request.addContained(obsWithNullId);
        request.addContained(validObs);
        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#null-id-obs"));
        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#valid-contained-obs"));

        
        addEncounterToContainedObservations(request, encounter.getUuid());

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicInteger counter = new AtomicInteger(0);
        Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();

        when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
            Observation result = new Observation();
            String obsId = "created-obs-" + counter.incrementAndGet();
            result.setId(obsId);
            org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
            openmrsObs.setUuid(obsId);
            createdObsMap.put(obsId, openmrsObs);
            return result;
        });

        when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
            Reference ref = invocation.getArgument(0);
            if (ref != null && ref.getReference() != null && ref.getReference().startsWith("Observation/")) {
                String obsId = ref.getReference().substring("Observation/".length());
                return createdObsMap.get(obsId);
            }
            return null;
        });

        when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
            org.openmrs.Obs obs = invocation.getArgument(0);
            if (obs == null) return null;
            Observation fhirObs = new Observation();
            fhirObs.setId(obs.getUuid());
            return fhirObs;
        });

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
        verify(fhirObservationService, times(1)).create(any(Observation.class));
    }
	
	@Test
    public void testUpdateWithQualityAssessments_shouldHandleNullEncounterWhenNoQualityAssessments() throws IOException {
        String studyId = "test-null-encounter-no-qa";

        FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
        existingStudy.setEncounter(null);

        ImagingStudy request = new ImagingStudy();
        request.setId(studyId);
        request.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
        request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.no.qa.null.enc");
        request.setSubject(new Reference("Patient/" + PATIENT_UUID));

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
        verify(fhirObservationService, times(0)).create(any(Observation.class));
        verify(fhirObservationService, times(0)).delete(any());
    }
	
	@Test
    public void searchImagingStudy_shouldSearchWithDateRangeOnlyWhenOtherParamsPresent() {
        ReferenceAndListParam patientReference = new ReferenceAndListParam()
                .addAnd(new ReferenceOrListParam().add(new ReferenceParam().setValue(PATIENT_UUID)));
        DateRangeParam lastUpdated = new DateRangeParam().setLowerBound("2023-01-01");

        BahmniImagingStudySearchParams searchParams = new BahmniImagingStudySearchParams(
                patientReference, null, null, lastUpdated, null);

        FhirImagingStudy fhirImagingStudy = createTestFhirImagingStudy(IMAGING_STUDY_UUID);

        SearchParameterMap theParams = searchParams.toSearchParameterMap();

        when(imagingStudyDao.getSearchResults(any())).thenReturn(Collections.singletonList(fhirImagingStudy));
        when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
                new SearchQueryBundleProvider<>(theParams, imagingStudyDao, imagingStudyTranslator,
                        globalPropertyService, searchQueryInclude));
        when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());

        IBundleProvider results = fhirImagingStudyService.searchImagingStudy(searchParams);

        assertThat(results, notNullValue());
    }
	
	@Test
    public void testFetchWithQualityAssessment_shouldIncludeContainedObservationsWithCorrectReferences() {
        String studyId = "test-study-with-multiple-assessment";
        FhirImagingStudy study = createExistingStudyWithEncounter(studyId);

        org.openmrs.Obs obs1 = new org.openmrs.Obs();
        obs1.setUuid("assessment-obs-1");
        org.openmrs.Obs obs2 = new org.openmrs.Obs();
        obs2.setUuid("assessment-obs-2");
        org.openmrs.Obs obs3 = new org.openmrs.Obs();
        obs3.setUuid("assessment-obs-3");

        Set<org.openmrs.Obs> assessment = new LinkedHashSet<>();
        assessment.add(obs1);
        assessment.add(obs2);
        assessment.add(obs3);
        study.setAssessment(assessment);

        when(imagingStudyDao.get(studyId)).thenReturn(study);
        when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
            org.openmrs.Obs obs = invocation.getArgument(0);
            Observation fhirObs = new Observation();
            fhirObs.setId(obs.getUuid());
            fhirObs.setStatus(Observation.ObservationStatus.FINAL);
            return fhirObs;
        });

        ImagingStudy result = fhirImagingStudyService.fetchWithQualityAssessment(studyId);

        assertNotNull(result);
        Assert.assertEquals(3, result.getContained().size());
        Assert.assertEquals(3, result.getExtensionsByUrl(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION).size());
        
        result.getContained().forEach(resource -> {
            Assert.assertTrue(resource.getId().startsWith("#"));
        });
    }
	
	private void setupObservationMocksWithEncounterCheck(Encounter encounter) {
        Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();

        when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
            Observation createdObs = invocation.getArgument(0);
            assertNotNull("Encounter should be set", createdObs.getEncounter());
            Assert.assertEquals("Encounter/" + encounter.getUuid(), createdObs.getEncounter().getReference());

            Observation result = new Observation();
            result.setId("created-obs-id");
            org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
            openmrsObs.setUuid("created-obs-id");
            createdObsMap.put("created-obs-id", openmrsObs);
            return result;
        });

        when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
            Reference ref = invocation.getArgument(0);
            if (ref != null && ref.getReference() != null && ref.getReference().startsWith("Observation/")) {
                String obsId = ref.getReference().substring("Observation/".length());
                return createdObsMap.get(obsId);
            }
            return null;
        });

        when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
            org.openmrs.Obs openmrsObs = invocation.getArgument(0);
            if (openmrsObs == null) return null;
            Observation fhirObs = new Observation();
            fhirObs.setId(openmrsObs.getUuid());
            return fhirObs;
        });
    }
	
	@Test
    public void testUpdateWithQualityAssessments_shouldHandleHasMemberReferenceNotInMap() throws IOException {
        String studyId = "test-hasmember-not-in-map-study";

        Encounter encounter = new Encounter();
        encounter.setUuid("radiology-encounter-uuid");
        org.openmrs.EncounterType encounterType = new org.openmrs.EncounterType();
        encounterType.setName("RADIOLOGY QUALITY ASSESSMENT");
        encounter.setEncounterType(encounterType);

        FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
        existingStudy.setEncounter(encounter);
        existingStudy.setAssessment(new LinkedHashSet<>());

        ImagingStudy request = new ImagingStudy();
        request.setId(studyId);
        request.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
        request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.hasmember.notinmap");
        request.setSubject(new Reference("Patient/" + PATIENT_UUID));

        Observation obsWithUnresolvedMember = new Observation();
        obsWithUnresolvedMember.setId("#obs-unresolved-member");
        obsWithUnresolvedMember.setStatus(Observation.ObservationStatus.FINAL);
        obsWithUnresolvedMember.addHasMember(new Reference("#non-existent-obs"));

        request.addContained(obsWithUnresolvedMember);
        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#obs-unresolved-member"));

        
        addEncounterToContainedObservations(request, encounter.getUuid());

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicInteger counter = new AtomicInteger(0);
        Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();

        when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
            Observation result = new Observation();
            String obsId = "created-obs-" + counter.incrementAndGet();
            result.setId(obsId);
            org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
            openmrsObs.setUuid(obsId);
            createdObsMap.put(obsId, openmrsObs);
            return result;
        });

        when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
            Reference ref = invocation.getArgument(0);
            if (ref != null && ref.getReference() != null && ref.getReference().startsWith("Observation/")) {
                String obsId = ref.getReference().substring("Observation/".length());
                return createdObsMap.get(obsId);
            }
            return null;
        });

        when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
            org.openmrs.Obs obs = invocation.getArgument(0);
            if (obs == null) return null;
            Observation fhirObs = new Observation();
            fhirObs.setId(obs.getUuid());
            return fhirObs;
        });

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
        verify(fhirObservationService, times(1)).create(any(Observation.class));
    }
	
	@Test
    public void testUpdateWithQualityAssessments_shouldHandleNullEncounterReference() throws IOException {
        String studyId = "test-null-encounter-ref-study";

        Encounter encounter = new Encounter();
        encounter.setUuid("radiology-encounter-uuid");
        org.openmrs.EncounterType encounterType = new org.openmrs.EncounterType();
        encounterType.setName("RADIOLOGY QUALITY ASSESSMENT");
        encounter.setEncounterType(encounterType);

        FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
        existingStudy.setEncounter(encounter);
        existingStudy.setAssessment(new LinkedHashSet<>());

        ImagingStudy request = new ImagingStudy();
        request.setId(studyId);
        request.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
        request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.enc.ref");
        request.setSubject(new Reference("Patient/" + PATIENT_UUID));

        Observation obs = new Observation();
        obs.setId("#test-obs-enc-ref");
        obs.setStatus(Observation.ObservationStatus.FINAL);
        request.addContained(obs);
        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#test-obs-enc-ref"));
        
        
        addEncounterToContainedObservations(request, encounter.getUuid());

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicInteger counter = new AtomicInteger(0);
        Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();

        when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
            Observation result = new Observation();
            String obsId = "created-obs-" + counter.incrementAndGet();
            result.setId(obsId);
            org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
            openmrsObs.setUuid(obsId);
            createdObsMap.put(obsId, openmrsObs);
            return result;
        });

        when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
            Reference ref = invocation.getArgument(0);
            if (ref != null && ref.getReference() != null && ref.getReference().startsWith("Observation/")) {
                String obsId = ref.getReference().substring("Observation/".length());
                return createdObsMap.get(obsId);
            }
            return null;
        });

        when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
            org.openmrs.Obs openmrsObs = invocation.getArgument(0);
            if (openmrsObs == null) return null;
            Observation fhirObs = new Observation();
            fhirObs.setId(openmrsObs.getUuid());
            return fhirObs;
        });

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
        verify(fhirObservationService, times(1)).create(any(Observation.class));
    }
	
	@Test
    public void testUpdateWithQualityAssessments_shouldHandleObservationWithoutEncounterAndNullEncounterRef() throws IOException {
        String studyId = "test-obs-no-encounter-study";

        Encounter encounter = new Encounter();
        encounter.setUuid("radiology-encounter-uuid");
        org.openmrs.EncounterType encounterType = new org.openmrs.EncounterType();
        encounterType.setName("RADIOLOGY QUALITY ASSESSMENT");
        encounter.setEncounterType(encounterType);

        FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
        existingStudy.setEncounter(encounter);
        existingStudy.setAssessment(new LinkedHashSet<>());

        ImagingStudy request = new ImagingStudy();
        request.setId(studyId);
        request.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
        request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.no.enc");
        request.setSubject(new Reference("Patient/" + PATIENT_UUID));

        Observation obsWithoutEncounter = new Observation();
        obsWithoutEncounter.setId("#obs-without-encounter");
        obsWithoutEncounter.setStatus(Observation.ObservationStatus.FINAL);

        request.addContained(obsWithoutEncounter);
        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#obs-without-encounter"));
        
        
        addEncounterToContainedObservations(request, encounter.getUuid());

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicInteger counter = new AtomicInteger(0);
        Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();

        when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
            Observation result = new Observation();
            String obsId = "created-obs-" + counter.incrementAndGet();
            result.setId(obsId);
            org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
            openmrsObs.setUuid(obsId);
            createdObsMap.put(obsId, openmrsObs);
            return result;
        });

        when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
            Reference ref = invocation.getArgument(0);
            if (ref != null && ref.getReference() != null && ref.getReference().startsWith("Observation/")) {
                String obsId = ref.getReference().substring("Observation/".length());
                return createdObsMap.get(obsId);
            }
            return null;
        });

        when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
            org.openmrs.Obs obs = invocation.getArgument(0);
            if (obs == null) return null;
            Observation fhirObs = new Observation();
            fhirObs.setId(obs.getUuid());
            return fhirObs;
        });

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
        verify(fhirObservationService, times(1)).create(any(Observation.class));
    }
	
	@Test
    public void testUpdateWithQualityAssessments_shouldHandleObservationWithEncounterNullReference() throws IOException {
        String studyId = "test-obs-null-enc-ref-study";

        Encounter encounter = new Encounter();
        encounter.setUuid("radiology-encounter-uuid");
        org.openmrs.EncounterType encounterType = new org.openmrs.EncounterType();
        encounterType.setName("RADIOLOGY QUALITY ASSESSMENT");
        encounter.setEncounterType(encounterType);

        FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
        existingStudy.setEncounter(encounter);
        existingStudy.setAssessment(new LinkedHashSet<>());

        ImagingStudy request = new ImagingStudy();
        request.setId(studyId);
        request.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
        request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.enc.null.ref");
        request.setSubject(new Reference("Patient/" + PATIENT_UUID));

        Observation obsWithNullEncRef = new Observation();
        obsWithNullEncRef.setId("#obs-null-enc-ref");
        obsWithNullEncRef.setStatus(Observation.ObservationStatus.FINAL);
        request.addContained(obsWithNullEncRef);
        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#obs-null-enc-ref"));
        
        addEncounterToContainedObservations(request, encounter.getUuid());

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicInteger counter = new AtomicInteger(0);
        Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();

        when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
            Observation result = new Observation();
            String obsId = "created-obs-" + counter.incrementAndGet();
            result.setId(obsId);
            org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
            openmrsObs.setUuid(obsId);
            createdObsMap.put(obsId, openmrsObs);
            return result;
        });

        when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
            Reference ref = invocation.getArgument(0);
            if (ref != null && ref.getReference() != null && ref.getReference().startsWith("Observation/")) {
                String obsId = ref.getReference().substring("Observation/".length());
                return createdObsMap.get(obsId);
            }
            return null;
        });

        when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
            org.openmrs.Obs obs = invocation.getArgument(0);
            if (obs == null) return null;
            Observation fhirObs = new Observation();
            fhirObs.setId(obs.getUuid());
            return fhirObs;
        });

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
        verify(fhirObservationService, times(1)).create(any(Observation.class));
    }
	
	@Test
    public void testUpdateWithQualityAssessments_shouldHandleObservationReferenceTranslatorReturningNull() throws IOException {
        String studyId = "test-obs-ref-null-study";

        Encounter encounter = new Encounter();
        encounter.setUuid("radiology-encounter-uuid");
        org.openmrs.EncounterType encounterType = new org.openmrs.EncounterType();
        encounterType.setName("RADIOLOGY QUALITY ASSESSMENT");
        encounter.setEncounterType(encounterType);

        FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
        existingStudy.setEncounter(encounter);
        existingStudy.setAssessment(new LinkedHashSet<>());

        ImagingStudy request = new ImagingStudy();
        request.setId(studyId);
        request.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
        request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.obs.ref.null");
        request.setSubject(new Reference("Patient/" + PATIENT_UUID));

        Observation obs = new Observation();
        obs.setId("#test-obs-ref-null");
        obs.setStatus(Observation.ObservationStatus.FINAL);
        request.addContained(obs);
        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#test-obs-ref-null"));
        
        
        addEncounterToContainedObservations(request, encounter.getUuid());

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
            Observation result = new Observation();
            result.setId("created-obs-id");
            return result;
        });

        when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(null);

        when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
            org.openmrs.Obs openmrsObs = invocation.getArgument(0);
            if (openmrsObs == null) return null;
            Observation fhirObs = new Observation();
            fhirObs.setId(openmrsObs.getUuid());
            return fhirObs;
        });

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
        verify(fhirObservationService, times(1)).create(any(Observation.class));
    }
	
	@Test
	public void testUpdateWithQualityAssessments_shouldHandleStudyWithNullEncounterForQualityObservations() throws IOException {
		String studyId = "test-null-study-encounter";

		Encounter encounter = new Encounter();
		encounter.setUuid("radiology-encounter-uuid");
		org.openmrs.EncounterType encounterType = new org.openmrs.EncounterType();
		encounterType.setName("RADIOLOGY QUALITY ASSESSMENT");
		encounter.setEncounterType(encounterType);

		FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
		existingStudy.setEncounter(encounter);
		existingStudy.setAssessment(new LinkedHashSet<>());

		ImagingStudy request = new ImagingStudy();
		request.setId(studyId);
		request.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
		request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.null.study.enc");
		request.setSubject(new Reference("Patient/" + PATIENT_UUID));

		Observation obs = new Observation();
		obs.setId("#test-obs-study-enc");
		obs.setStatus(Observation.ObservationStatus.FINAL);
		request.addContained(obs);
		request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#test-obs-study-enc"));
		
		
		addEncounterToContainedObservations(request, encounter.getUuid());

		when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
		when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> {
			FhirImagingStudy saved = invocation.getArgument(0);
			saved.setEncounter(null);
			return saved;
		});

		AtomicInteger counter = new AtomicInteger(0);
		Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();

		when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
			Observation result = new Observation();
			String obsId = "created-obs-" + counter.incrementAndGet();
			result.setId(obsId);
			org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
			openmrsObs.setUuid(obsId);
			createdObsMap.put(obsId, openmrsObs);
			return result;
		});

		when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
			Reference ref = invocation.getArgument(0);
			if (ref != null && ref.getReference() != null && ref.getReference().startsWith("Observation/")) {
				String obsId = ref.getReference().substring("Observation/".length());
				return createdObsMap.get(obsId);
			}
			return null;
		});

		when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
			org.openmrs.Obs openmrsObs = invocation.getArgument(0);
			if (openmrsObs == null) return null;
			Observation fhirObs = new Observation();
			fhirObs.setId(openmrsObs.getUuid());
			return fhirObs;
		});

		ImagingStudy result = fhirImagingStudyService.update(studyId, request);

		assertNotNull(result);
		verify(fhirObservationService, times(1)).create(any(Observation.class));
	}
	
	@Test
    public void testUpdateWithQualityAssessments_shouldHandleMultipleHasMemberReferences() throws IOException {
        String studyId = "test-multiple-hasmember-study";

        Encounter encounter = new Encounter();
        encounter.setUuid("radiology-encounter-uuid");
        org.openmrs.EncounterType encounterType = new org.openmrs.EncounterType();
        encounterType.setName("RADIOLOGY QUALITY ASSESSMENT");
        encounter.setEncounterType(encounterType);

        FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
        existingStudy.setEncounter(encounter);
        existingStudy.setAssessment(new LinkedHashSet<>());

        ImagingStudy request = new ImagingStudy();
        request.setId(studyId);
        request.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
        request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.multi.member");
        request.setSubject(new Reference("Patient/" + PATIENT_UUID));

        Observation childObs1 = new Observation();
        childObs1.setId("#child-obs-1");
        childObs1.setStatus(Observation.ObservationStatus.FINAL);

        Observation childObs2 = new Observation();
        childObs2.setId("#child-obs-2");
        childObs2.setStatus(Observation.ObservationStatus.FINAL);

        Observation parentObs = new Observation();
        parentObs.setId("#parent-multi-obs");
        parentObs.setStatus(Observation.ObservationStatus.FINAL);
        parentObs.addHasMember(new Reference("#child-obs-1"));
        parentObs.addHasMember(new Reference("#child-obs-2"));
        parentObs.addHasMember(new Reference("#non-existent-child"));  // This one won't be resolved

        request.addContained(childObs1);
        request.addContained(childObs2);
        request.addContained(parentObs);
        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#child-obs-1"));
        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#child-obs-2"));
        request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#parent-multi-obs"));
        
        
        addEncounterToContainedObservations(request, encounter.getUuid());

        when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AtomicInteger counter = new AtomicInteger(0);
        Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();

        when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
            Observation result = new Observation();
            String obsId = "created-obs-" + counter.incrementAndGet();
            result.setId(obsId);
            org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
            openmrsObs.setUuid(obsId);
            createdObsMap.put(obsId, openmrsObs);
            return result;
        });

        when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
            Reference ref = invocation.getArgument(0);
            if (ref != null && ref.getReference() != null && ref.getReference().startsWith("Observation/")) {
                String obsId = ref.getReference().substring("Observation/".length());
                return createdObsMap.get(obsId);
            }
            return null;
        });

        when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
            org.openmrs.Obs obs = invocation.getArgument(0);
            if (obs == null) return null;
            Observation fhirObs = new Observation();
            fhirObs.setId(obs.getUuid());
            return fhirObs;
        });

        ImagingStudy result = fhirImagingStudyService.update(studyId, request);

        assertNotNull(result);
        verify(fhirObservationService, times(3)).create(any(Observation.class));
    }
}
