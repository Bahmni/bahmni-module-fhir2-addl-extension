package org.bahmni.module.fhir2addlextension.api.service.impl;

import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirImagingStudyDao;
import org.bahmni.module.fhir2addlextension.api.model.FhirImagingStudy;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniImagingStudySearchParams;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirImagingStudyService;
import org.bahmni.module.fhir2addlextension.api.translator.BahmniFhirImagingStudyTranslator;
import org.bahmni.module.fhir2addlextension.api.translator.BahmniOrderReferenceTranslator;
import org.bahmni.module.fhir2addlextension.api.translator.impl.BahmniFhirImagingStudyTranslatorImpl;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.ImagingStudy;
import org.hl7.fhir.r4.model.Reference;
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
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.LocationReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.hl7.fhir.r4.model.Observation;

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

@RunWith(MockitoJUnitRunner.class)
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
	
	@Mock
	private ObservationTranslator observationTranslator;
	
	@Mock
	private EncounterReferenceTranslator<Encounter> encounterReferenceTranslator;
	
	@Mock
	private org.openmrs.module.fhir2.api.translators.ObservationReferenceTranslator observationReferenceTranslator;
	
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
		        patientReferenceTranslator, locationReferenceTranslator, practitionerReferenceTranslator, observationTranslator, encounterReferenceTranslator, observationReferenceTranslator);
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
		
		ImagingStudy result = fhirImagingStudyService.submitQualityAssessment(request);
		
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
	
	private FhirImagingStudy createExistingStudyWithEncounter(String uuid) {
		FhirImagingStudy study = createTestFhirImagingStudy(uuid);
		Encounter encounter = new Encounter();
		encounter.setUuid("encounter-uuid-default");
		study.setEncounter(encounter);
		return study;
	}
}
