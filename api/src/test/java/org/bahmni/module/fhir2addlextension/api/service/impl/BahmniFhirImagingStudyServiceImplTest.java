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
                                                                        observationReferenceTranslator) {
			
			@Override
			protected void validateObject(FhirImagingStudy object) {
				//Done because baseFhirService implementation uses Context.getAdministrativeService()
				//we are not mocking Context.serviceContext
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
        ImagingStudy imagingStudy = fhirImagingStudyService.create((ImagingStudy)  fhirResource);
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
        ImagingStudy imagingStudy = fhirImagingStudyService.update("example-imaging-study", (ImagingStudy)  fhirResource);
        Assert.assertEquals(ImagingStudy.ImagingStudyStatus.REGISTERED, imagingStudy.getStatus());
        Extension performerExt = imagingStudy.getExtensionByUrl(BahmniFhirConstants.FHIR_EXT_IMAGING_STUDY_PERFORMER);
        Assert.assertNotNull("Performer extension should not be null for Imaging Study", performerExt);
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
	
	@Test
	public void shouldSubmitQualityAssessmentWithContainedObservations() throws IOException {
		String studyId = "18046e64-cf94-4adf-b0d3-a83aa9fb165a";
		ImagingStudy request = (ImagingStudy) loadResourceFromFile("example-imaging-study-with-quality-assessment.json");
		
		FhirImagingStudy existingStudy = createExistingStudyWithEncounter(studyId);
		when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
		when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));
		
		Provider performer = new Provider();
		performer.setUuid("60b31d2a-1d0c-11f1-b099-5a3ed7acdb7e");
		when(practitionerReferenceTranslator.toOpenmrsType(
				ArgumentMatchers.argThat(reference -> {
					return reference != null && reference.getReference() != null &&
							reference.getReference().equals("Practitioner/60b31d2a-1d0c-11f1-b099-5a3ed7acdb7e");
				})))
				.thenReturn(performer);
		
		AtomicInteger counter = new AtomicInteger(0);
		Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();
		
		ArgumentCaptor<Observation> obsCaptor = ArgumentCaptor.forClass(Observation.class);
		
		when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
			Observation inputObs = invocation.getArgument(0);
			Observation obs = new Observation();
			String obsId = "obs-" + counter.incrementAndGet();
			obs.setId(obsId);
			
			if (inputObs.hasHasMember()) {
				obs.setHasMember(inputObs.getHasMember());
			}
			
			org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
			openmrsObs.setUuid(obsId);
			createdObsMap.put(obsId, openmrsObs);
			
			return obs;
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
		
		ImagingStudy result = fhirImagingStudyService.create(request);
		
		Assert.assertNotNull(result);
		verify(fhirObservationService, times(3)).create(obsCaptor.capture());
		
		List<Observation> createdObservations = obsCaptor.getAllValues();
		Observation obsWithMembers = createdObservations.stream()
				.filter(Observation::hasHasMember)
				.findFirst()
				.orElse(null);
		
		Assert.assertNotNull("Should have observation with hasMember", obsWithMembers);
		Assert.assertTrue("hasMember reference should be resolved to Observation/uuid format",
				obsWithMembers.getHasMember().get(0).getReference().startsWith("Observation/"));
		Assert.assertFalse("hasMember reference should not contain # prefix",
				obsWithMembers.getHasMember().get(0).getReference().contains("#"));
		
		List<Extension> extensions = result.getExtensionsByUrl(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION);
		assertEquals(3, extensions.size());
		extensions.forEach(ext -> {
			Reference ref = (Reference) ext.getValue();
			Assert.assertTrue(ref.getReference().startsWith("#"));
		});
		assertEquals(3, result.getContained().size());
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
	
	@Test
	public void testFetchWithQualityAssessment_shouldReturnStudyWithContainedObservations() {
		String studyId = "test-study-uuid";
		FhirImagingStudy study = createExistingStudyWithEncounter(studyId);
		
		org.openmrs.Obs obs1 = new org.openmrs.Obs();
		obs1.setUuid("obs-uuid-1");
		org.openmrs.Obs obs2 = new org.openmrs.Obs();
		obs2.setUuid("obs-uuid-2");
		
		Set<org.openmrs.Obs> results = new LinkedHashSet<>();
		results.add(obs1);
		results.add(obs2);
		study.setResults(results);
		
		when(imagingStudyDao.get(studyId)).thenReturn(study);
		when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
			org.openmrs.Obs obs = invocation.getArgument(0);
			Observation fhirObs = new Observation();
			fhirObs.setId(obs.getUuid());
			return fhirObs;
		});
		
		ImagingStudy result = fhirImagingStudyService.fetchWithQualityAssessment(studyId);
		
		Assert.assertNotNull(result);
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
		
		// Should go through normal create flow since no quality assessments
		ImagingStudy result = fhirImagingStudyService.create(request);
		
		Assert.assertNotNull(result);
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
		// No contained resources - this goes through normal create flow, not quality assessment
		
		FhirImagingStudy newStudy = createTestFhirImagingStudy(studyId);
		when(imagingStudyDao.createOrUpdate(any())).thenReturn(newStudy);
		when(patientReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(openmrsPatient);
		
		// Should go through normal create flow since no contained resources
		ImagingStudy result = fhirImagingStudyService.create(request);
		Assert.assertNotNull(result);
	}
	
	private FhirImagingStudy createExistingStudyWithEncounter(String uuid) {
		FhirImagingStudy study = createTestFhirImagingStudy(uuid);
		Encounter encounter = new Encounter();
		encounter.setUuid("encounter-uuid-default");
		study.setEncounter(encounter);
		return study;
	}
	
	@Test
	public void testSubmitQualityAssessment_shouldNotProcessWhenStudyNotFound() {
		String studyId = "non-existent-uuid";
		ImagingStudy request = new ImagingStudy();
		request.setId(studyId);
		request.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);
		request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.instance");
		request.setSubject(new Reference("Patient/" + PATIENT_UUID));
		request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#obs-1"));
		// No contained - triggers normal create flow, not quality assessment flow
		
		FhirImagingStudy newStudy = createTestFhirImagingStudy(studyId);
		when(imagingStudyDao.createOrUpdate(any())).thenReturn(newStudy);
		when(patientReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(openmrsPatient);
		
		// Should go through normal create flow since no contained resources
		ImagingStudy result = fhirImagingStudyService.create(request);
		Assert.assertNotNull(result);
	}
	
	@Test
	public void testSubmitQualityAssessment_shouldHandleEmptyId() {
		ImagingStudy request = new ImagingStudy();
		request.setId(new IdType());
		request.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);
		request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.instance");
		request.setSubject(new Reference("Patient/" + PATIENT_UUID));
		
		FhirImagingStudy newStudy = createTestFhirImagingStudy("generated-uuid");
		when(imagingStudyDao.createOrUpdate(any())).thenReturn(newStudy);
		when(patientReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(openmrsPatient);
		
		// Should create successfully with generated ID
		ImagingStudy result = fhirImagingStudyService.create(request);
		Assert.assertNotNull(result);
	}
	
	@Test
	public void testSubmitQualityAssessment_shouldHandleStudyWithoutEncounter() throws IOException {
		String studyId = "study-without-encounter";
		ImagingStudy request = (ImagingStudy) loadResourceFromFile("example-imaging-study-with-quality-assessment.json");
		request.setId(studyId);
		
		FhirImagingStudy existingStudy = createTestFhirImagingStudy(studyId);
		existingStudy.setEncounter(null);
		
		when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
		when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));
		
		Provider performer = new Provider();
		performer.setUuid("60b31d2a-1d0c-11f1-b099-5a3ed7acdb7e");
		when(practitionerReferenceTranslator.toOpenmrsType(any())).thenReturn(performer);
		
		AtomicInteger counter = new AtomicInteger(0);
		Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();
		
		when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
			Observation obs = new Observation();
			String obsId = "obs-" + counter.incrementAndGet();
			obs.setId(obsId);
			
			org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
			openmrsObs.setUuid(obsId);
			createdObsMap.put(obsId, openmrsObs);
			
			return obs;
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
		
		ImagingStudy result = fhirImagingStudyService.create(request);
		
		Assert.assertNotNull(result);
		verify(fhirObservationService, times(3)).create(any(Observation.class));
	}
	
	@Test
	public void testSubmitQualityAssessment_shouldUpdateExistingObservations() throws IOException {
		String studyId = "study-with-existing-obs";
		ImagingStudy request = (ImagingStudy) loadResourceFromFile("example-imaging-study-with-quality-assessment.json");
		request.setId(studyId);
		
		FhirImagingStudy existingStudy = createExistingStudyWithEncounter(studyId);
		
		when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
		when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));
		
		Provider performer = new Provider();
		performer.setUuid("60b31d2a-1d0c-11f1-b099-5a3ed7acdb7e");
		when(practitionerReferenceTranslator.toOpenmrsType(any())).thenReturn(performer);
		
		Observation existingObs = new Observation();
		existingObs.setId("obs-1775130800624-wkl0xssb7");
		when(fhirObservationService.get("obs-1775130800624-wkl0xssb7")).thenReturn(existingObs);
		
		AtomicInteger counter = new AtomicInteger(0);
		Map<String, org.openmrs.Obs> obsMap = new HashMap<>();
		
		when(fhirObservationService.update(eq("obs-1775130800624-wkl0xssb7"), any(Observation.class))).thenAnswer(invocation -> {
			Observation obs = new Observation();
			obs.setId("obs-1775130800624-wkl0xssb7");
			
			org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
			openmrsObs.setUuid("obs-1775130800624-wkl0xssb7");
			obsMap.put("obs-1775130800624-wkl0xssb7", openmrsObs);
			
			return obs;
		});
		
		when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
			Observation obs = new Observation();
			String obsId = "new-obs-" + counter.incrementAndGet();
			obs.setId(obsId);
			
			org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
			openmrsObs.setUuid(obsId);
			obsMap.put(obsId, openmrsObs);
			
			return obs;
		});
		
		when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
			Reference ref = invocation.getArgument(0);
			if (ref != null && ref.getReference() != null && ref.getReference().startsWith("Observation/")) {
				String obsId = ref.getReference().substring("Observation/".length());
				return obsMap.get(obsId);
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
		
		ImagingStudy result = fhirImagingStudyService.create(request);
		
		Assert.assertNotNull(result);
		verify(fhirObservationService, times(1)).update(eq("obs-1775130800624-wkl0xssb7"), any(Observation.class));
	}
	
	@Test
	public void testSubmitQualityAssessment_shouldSkipInvalidExtensionValues() {
		String studyId = "study-with-invalid-ext";
		ImagingStudy request = new ImagingStudy();
		request.setId(studyId);
		request.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);
		
		Extension invalidExt = new Extension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION);
		invalidExt.setValue(new org.hl7.fhir.r4.model.StringType("invalid-value"));
		request.addExtension(invalidExt);
		
		Observation validObs = new Observation();
		validObs.setId("#valid-obs");
		request.addContained(validObs);
		request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#valid-obs"));
		
		FhirImagingStudy existingStudy = createExistingStudyWithEncounter(studyId);
		when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));
		
		when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
			Observation obs = new Observation();
			obs.setId("created-obs-1");
			return obs;
		});
		
		when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
			org.openmrs.Obs obs = new org.openmrs.Obs();
			obs.setUuid("created-obs-1");
			return obs;
		});
		
		when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
			org.openmrs.Obs obs = invocation.getArgument(0);
			if (obs == null) return null;
			Observation fhirObs = new Observation();
			fhirObs.setId(obs.getUuid());
			return fhirObs;
		});
		
		ImagingStudy result = fhirImagingStudyService.create(request);
		
		Assert.assertNotNull(result);
		verify(fhirObservationService, times(1)).create(any(Observation.class));
	}
	
	@Test
	public void testSubmitQualityAssessment_shouldSkipNonObservationContainedResources() {
		String studyId = "study-with-invalid-contained";
		ImagingStudy request = new ImagingStudy();
		request.setId(studyId);
		request.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);
		
		org.hl7.fhir.r4.model.Patient invalidResource = new org.hl7.fhir.r4.model.Patient();
		invalidResource.setId("#invalid-resource");
		request.addContained(invalidResource);
		request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#invalid-resource"));
		
		Observation validObs = new Observation();
		validObs.setId("#valid-obs");
		request.addContained(validObs);
		request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#valid-obs"));
		
		FhirImagingStudy existingStudy = createExistingStudyWithEncounter(studyId);
		when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));
		
		when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
			Observation obs = new Observation();
			obs.setId("created-obs-1");
			return obs;
		});
		
		when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
			org.openmrs.Obs obs = new org.openmrs.Obs();
			obs.setUuid("created-obs-1");
			return obs;
		});
		
		when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
			org.openmrs.Obs obs = invocation.getArgument(0);
			if (obs == null) return null;
			Observation fhirObs = new Observation();
			fhirObs.setId(obs.getUuid());
			return fhirObs;
		});
		
		ImagingStudy result = fhirImagingStudyService.create(request);
		
		Assert.assertNotNull(result);
		verify(fhirObservationService, times(1)).create(any(Observation.class));
	}
	
	@Test
	public void testSubmitQualityAssessment_shouldSkipInvalidContainedReferences() {
		String studyId = "study-with-invalid-ref";
		ImagingStudy request = new ImagingStudy();
		request.setId(studyId);
		request.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);
		
		request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("invalid-ref"));
		request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#non-existent-obs"));
		
		Observation validObs = new Observation();
		validObs.setId("#valid-obs");
		request.addContained(validObs);
		request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#valid-obs"));
		
		FhirImagingStudy existingStudy = createExistingStudyWithEncounter(studyId);
		when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));
		
		when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
			Observation obs = new Observation();
			obs.setId("created-obs-1");
			return obs;
		});
		
		when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
			org.openmrs.Obs obs = new org.openmrs.Obs();
			obs.setUuid("created-obs-1");
			return obs;
		});
		
		when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
			org.openmrs.Obs obs = invocation.getArgument(0);
			if (obs == null) return null;
			Observation fhirObs = new Observation();
			fhirObs.setId(obs.getUuid());
			return fhirObs;
		});
		
		ImagingStudy result = fhirImagingStudyService.create(request);
		
		Assert.assertNotNull(result);
		verify(fhirObservationService, times(1)).create(any(Observation.class));
	}
	
	@Test
	public void testSubmitQualityAssessment_shouldHandleEmptyEncounterReference() throws IOException {
		String studyId = "study-with-empty-encounter";
		ImagingStudy request = (ImagingStudy) loadResourceFromFile("example-imaging-study-with-quality-assessment.json");
		request.setId(studyId);
		
		request.getContained().forEach(resource -> {
			if (resource instanceof Observation) {
				Observation obs = (Observation) resource;
				obs.setEncounter(new Reference(""));
			}
		});
		
		FhirImagingStudy existingStudy = createExistingStudyWithEncounter(studyId);
		when(imagingStudyDao.get(studyId)).thenReturn(existingStudy);
		when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));
		
		Provider performer = new Provider();
		performer.setUuid("60b31d2a-1d0c-11f1-b099-5a3ed7acdb7e");
		when(practitionerReferenceTranslator.toOpenmrsType(any())).thenReturn(performer);
		
		AtomicInteger counter = new AtomicInteger(0);
		Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();
		
		when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
			Observation inputObs = invocation.getArgument(0);
			Observation obs = new Observation();
			String obsId = "obs-" + counter.incrementAndGet();
			obs.setId(obsId);
			
			Assert.assertNotNull("Encounter should be set", inputObs.getEncounter());
			
			org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
			openmrsObs.setUuid(obsId);
			createdObsMap.put(obsId, openmrsObs);
			
			return obs;
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
		
		ImagingStudy result = fhirImagingStudyService.create(request);
		
		Assert.assertNotNull(result);
	}
	
	@Test
	public void testFetchWithQualityAssessment_shouldHandleEmptyResults() {
		String studyId = "study-without-results";
		FhirImagingStudy study = createExistingStudyWithEncounter(studyId);
		study.setResults(new LinkedHashSet<>());
		
		when(imagingStudyDao.get(studyId)).thenReturn(study);
		
		ImagingStudy result = fhirImagingStudyService.fetchWithQualityAssessment(studyId);
		
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.getContained().size());
		Assert.assertEquals(0, result.getExtensionsByUrl(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION).size());
	}
	
	@Test
	public void testFetchWithQualityAssessment_shouldHandleNullResults() {
		String studyId = "study-with-null-results";
		FhirImagingStudy study = createExistingStudyWithEncounter(studyId);
		study.setResults(null);
		
		when(imagingStudyDao.get(studyId)).thenReturn(study);
		
		ImagingStudy result = fhirImagingStudyService.fetchWithQualityAssessment(studyId);
		
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.getContained().size());
		Assert.assertEquals(0, result.getExtensionsByUrl(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION).size());
	}
	
	@Test
	public void testCreate_shouldGoThroughNormalFlowWhenQualityExtensionsPresentButNoContained() {
		String studyId = "study-with-extensions-no-contained";
		ImagingStudy request = new ImagingStudy();
		request.setId(studyId);
		request.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);
		request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.instance");
		request.setSubject(new Reference("Patient/" + PATIENT_UUID));
		
		// Add quality extension but NO contained resources
		// hasQualityAssessmentExtensions will return false since no contained
		request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#obs-1"));
		
		FhirImagingStudy newStudy = createTestFhirImagingStudy(studyId);
		when(imagingStudyDao.createOrUpdate(any())).thenReturn(newStudy);
		when(patientReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(openmrsPatient);
		
		// Should go through normal create flow since no contained resources (hasQualityAssessmentExtensions returns false)
		ImagingStudy result = fhirImagingStudyService.create(request);
		
		Assert.assertNotNull(result);
	}
	
	@Test
	public void testCreate_shouldSkipNonExistentContainedReference() {
		String studyId = "study-with-nonexistent-contained-ref";
		ImagingStudy request = new ImagingStudy();
		request.setId(studyId);
		request.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);
		request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.instance");
		request.setSubject(new Reference("Patient/" + PATIENT_UUID));
		
		// Add contained observation with id #obs-1
		Observation containedObs = new Observation();
		containedObs.setId("#obs-1");
		request.addContained(containedObs);
		
		// Add extension referencing #obs-2 which doesn't exist - should be skipped
		request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#obs-2"));
		// Add extension referencing #obs-1 which exists - should be processed
		request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#obs-1"));
		
		when(patientReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(openmrsPatient);
		when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));
		
		Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();
		
		when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
			Observation obs = new Observation();
			obs.setId("created-obs-1");
			
			org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
			openmrsObs.setUuid("created-obs-1");
			createdObsMap.put("created-obs-1", openmrsObs);
			
			return obs;
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
		
		// Should succeed - only the valid obs-1 reference should be processed
		ImagingStudy result = fhirImagingStudyService.create(request);
		
		Assert.assertNotNull(result);
		// Only one observation should be created (the one that exists)
		verify(fhirObservationService, times(1)).create(any(Observation.class));
	}
	
	@Test
	public void testCreate_shouldHandleHasMemberWithSlashReference() throws IOException {
		String studyId = "study-with-slash-reference";
		ImagingStudy request = new ImagingStudy();
		request.setId(studyId);
		request.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);
		request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.instance");
		request.setSubject(new Reference("Patient/" + PATIENT_UUID));
		
		// Create observation with hasMember using slash reference format
		Observation parentObs = new Observation();
		parentObs.setId("#parent-obs");
		parentObs.addHasMember(new Reference("Observation/child-obs-uuid"));
		
		Observation childObs = new Observation();
		childObs.setId("#child-obs");
		
		request.addContained(childObs);
		request.addContained(parentObs);
		request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#child-obs"));
		request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#parent-obs"));
		
		FhirImagingStudy existingStudy = createExistingStudyWithEncounter(studyId);
		when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(patientReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(openmrsPatient);
		
		AtomicInteger counter = new AtomicInteger(0);
		Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();
		
		when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
			Observation obs = new Observation();
			String obsId = "obs-" + counter.incrementAndGet();
			obs.setId(obsId);
			
			org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
			openmrsObs.setUuid(obsId);
			createdObsMap.put(obsId, openmrsObs);
			
			return obs;
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
		
		ImagingStudy result = fhirImagingStudyService.create(request);
		
		Assert.assertNotNull(result);
		verify(fhirObservationService, times(2)).create(any(Observation.class));
	}
	
	@Test
	public void testCreate_shouldHandleNullHasMemberReference() throws IOException {
		String studyId = "study-with-null-member-ref";
		ImagingStudy request = new ImagingStudy();
		request.setId(studyId);
		request.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);
		request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.instance");
		request.setSubject(new Reference("Patient/" + PATIENT_UUID));
		
		// Create observation with hasMember that has null reference
		Observation parentObs = new Observation();
		parentObs.setId("#parent-obs");
		Reference nullMemberRef = new Reference();
		nullMemberRef.setReference(null);
		parentObs.addHasMember(nullMemberRef);
		
		request.addContained(parentObs);
		request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#parent-obs"));
		
		FhirImagingStudy existingStudy = createExistingStudyWithEncounter(studyId);
		when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(patientReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(openmrsPatient);
		
		Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();
		
		when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
			Observation obs = new Observation();
			obs.setId("created-obs-1");
			
			org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
			openmrsObs.setUuid("created-obs-1");
			createdObsMap.put("created-obs-1", openmrsObs);
			
			return obs;
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
		
		ImagingStudy result = fhirImagingStudyService.create(request);
		
		Assert.assertNotNull(result);
	}
	
	@Test
	public void testCreate_shouldHandleResourceNotFoundExceptionWhenCheckingExistingObs() throws IOException {
		String studyId = "study-with-nonexistent-obs";
		ImagingStudy request = new ImagingStudy();
		request.setId(studyId);
		request.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);
		request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.instance");
		request.setSubject(new Reference("Patient/" + PATIENT_UUID));
		
		Observation obs = new Observation();
		obs.setId("#nonexistent-obs-uuid");
		request.addContained(obs);
		request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#nonexistent-obs-uuid"));
		
		FhirImagingStudy existingStudy = createExistingStudyWithEncounter(studyId);
		when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(patientReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(openmrsPatient);
		
		// Throw ResourceNotFoundException when checking for existing observation
		when(fhirObservationService.get("nonexistent-obs-uuid")).thenThrow(new ResourceNotFoundException("Not found"));
		
		Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();
		
		when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
			Observation createdObs = new Observation();
			createdObs.setId("new-obs-id");
			
			org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
			openmrsObs.setUuid("new-obs-id");
			createdObsMap.put("new-obs-id", openmrsObs);
			
			return createdObs;
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
		
		// Should not throw - exception is caught and null returned for existing observation check
		ImagingStudy result = fhirImagingStudyService.create(request);
		
		Assert.assertNotNull(result);
		// Verify create was called since no existing observation was found
		verify(fhirObservationService, times(1)).create(any(Observation.class));
	}
	
	@Test
	public void testCreate_shouldHandleExtensionWithNullContainedId() throws IOException {
		String studyId = "study-with-null-contained-id";
		ImagingStudy request = new ImagingStudy();
		request.setId(studyId);
		request.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);
		request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.instance");
		request.setSubject(new Reference("Patient/" + PATIENT_UUID));
		
		// Add extension with null reference
		Reference nullRef = new Reference();
		nullRef.setReference(null);
		request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, nullRef);
		
		// Add valid observation
		Observation validObs = new Observation();
		validObs.setId("#valid-obs");
		request.addContained(validObs);
		request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#valid-obs"));
		
		when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(patientReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(openmrsPatient);
		
		Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();
		
		when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
			Observation obs = new Observation();
			obs.setId("created-obs-1");
			
			org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
			openmrsObs.setUuid("created-obs-1");
			createdObsMap.put("created-obs-1", openmrsObs);
			
			return obs;
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
		
		ImagingStudy result = fhirImagingStudyService.create(request);
		
		Assert.assertNotNull(result);
		// Only the valid observation should be created
		verify(fhirObservationService, times(1)).create(any(Observation.class));
	}
	
	@Test
	public void testCreate_shouldHandleObservationWithNullEncounter() throws IOException {
		String studyId = "study-with-obs-null-encounter";
		ImagingStudy request = new ImagingStudy();
		request.setId(studyId);
		request.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);
		request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.instance");
		request.setSubject(new Reference("Patient/" + PATIENT_UUID));
		
		// Create observation with null encounter
		Observation obs = new Observation();
		obs.setId("#obs-with-null-encounter");
		obs.setEncounter(null);
		
		request.addContained(obs);
		request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#obs-with-null-encounter"));
		
		FhirImagingStudy existingStudy = createExistingStudyWithEncounter(studyId);
		when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(patientReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(openmrsPatient);
		
		Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();
		
		when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
			Observation inputObs = invocation.getArgument(0);
			Observation createdObs = new Observation();
			createdObs.setId("created-obs-1");
			
			// Verify encounter was set from the study
			Assert.assertNotNull("Encounter reference should be set", inputObs.getEncounter());
			
			org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
			openmrsObs.setUuid("created-obs-1");
			createdObsMap.put("created-obs-1", openmrsObs);
			
			return createdObs;
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
		
		ImagingStudy result = fhirImagingStudyService.create(request);
		
		Assert.assertNotNull(result);
	}
	
	@Test
	public void testCreate_shouldHandleContainedResourceWithNullId() {
		String studyId = "study-with-null-id-resource";
		ImagingStudy request = new ImagingStudy();
		request.setId(studyId);
		request.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);
		request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.instance");
		request.setSubject(new Reference("Patient/" + PATIENT_UUID));
		
		// Add contained observation with null ID (should be filtered out)
		Observation obsWithNullId = new Observation();
		obsWithNullId.setId((String) null);
		request.addContained(obsWithNullId);
		
		// Add valid contained observation
		Observation validObs = new Observation();
		validObs.setId("#valid-obs");
		request.addContained(validObs);
		
		request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#valid-obs"));
		
		when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(patientReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(openmrsPatient);
		
		Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();
		
		when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
			Observation obs = new Observation();
			obs.setId("created-obs-1");
			
			org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
			openmrsObs.setUuid("created-obs-1");
			createdObsMap.put("created-obs-1", openmrsObs);
			
			return obs;
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
		
		ImagingStudy result = fhirImagingStudyService.create(request);
		
		Assert.assertNotNull(result);
		verify(fhirObservationService, times(1)).create(any(Observation.class));
	}
	
	@Test
	public void testCreate_shouldHandlePlainReferenceString() throws IOException {
		String studyId = "study-with-plain-reference";
		ImagingStudy request = new ImagingStudy();
		request.setId(studyId);
		request.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);
		request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.instance");
		request.setSubject(new Reference("Patient/" + PATIENT_UUID));
		
		Observation parentObs = new Observation();
		parentObs.setId("#parent-obs");
		parentObs.addHasMember(new Reference("plain-uuid-reference"));
		
		request.addContained(parentObs);
		request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#parent-obs"));
		
		FhirImagingStudy existingStudy = createExistingStudyWithEncounter(studyId);
		when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(patientReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(openmrsPatient);
		
		Map<String, org.openmrs.Obs> createdObsMap = new HashMap<>();
		
		when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
			Observation obs = new Observation();
			obs.setId("created-obs-1");
			
			org.openmrs.Obs openmrsObs = new org.openmrs.Obs();
			openmrsObs.setUuid("created-obs-1");
			createdObsMap.put("created-obs-1", openmrsObs);
			
			return obs;
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
		
		ImagingStudy result = fhirImagingStudyService.create(request);
		
		Assert.assertNotNull(result);
	}
	
	@Test
	public void testCreate_shouldHandleObsReferenceTranslatorReturningNull() throws IOException {
		String studyId = "study-with-null-obs-translation";
		ImagingStudy request = new ImagingStudy();
		request.setId(studyId);
		request.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);
		request.addIdentifier().setSystem("urn:dicom:uid").setValue("urn:oid:test.study.instance");
		request.setSubject(new Reference("Patient/" + PATIENT_UUID));
		
		Observation obs = new Observation();
		obs.setId("#test-obs");
		request.addContained(obs);
		request.addExtension(FHIR_EXT_IMAGING_STUDY_QUALITY_OBSERVATION, new Reference("#test-obs"));
		
		when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(patientReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(openmrsPatient);
		
		when(fhirObservationService.create(any(Observation.class))).thenAnswer(invocation -> {
			Observation createdObs = new Observation();
			createdObs.setId("created-obs-1");
			return createdObs;
		});
		
		when(observationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(null);
		
		when(observationTranslator.toFhirResource(any(org.openmrs.Obs.class))).thenAnswer(invocation -> {
			org.openmrs.Obs openmrsObs = invocation.getArgument(0);
			if (openmrsObs == null) return null;
			Observation fhirObs = new Observation();
			fhirObs.setId(openmrsObs.getUuid());
			return fhirObs;
		});
		
		ImagingStudy result = fhirImagingStudyService.create(request);
		
		Assert.assertNotNull(result);
		verify(fhirObservationService, times(1)).create(any(Observation.class));
	}
}
