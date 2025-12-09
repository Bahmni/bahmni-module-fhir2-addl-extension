package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirImagingStudyDao;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirServiceRequestDao;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirImagingStudy;
import org.bahmni.module.fhir2AddlExtension.api.search.param.BahmniImagingStudySearchParams;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirImagingStudyService;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniFhirImagingStudyTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniServiceRequestReferenceTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.impl.BahmniFhirImagingStudyTranslatorImpl;
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
import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.api.db.ContextDAO;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.FhirGlobalPropertyService;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryBundleProvider;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.translators.LocationReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.bahmni.module.fhir2AddlExtension.api.TestDataFactory.loadResourceFromFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
	private BahmniServiceRequestReferenceTranslator basedOnReferenceTranslator;
	
	@Mock
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Mock
	private LocationReferenceTranslator locationReferenceTranslator;
	
	@Mock
	private PractitionerReferenceTranslator<Provider> practitionerReferenceTranslator;
	
	@Mock
	private BahmniFhirServiceRequestDao<Order> serviceRequestDao;
	
	@Mock
	private SearchQueryInclude<ImagingStudy> searchQueryInclude;
	
	@Mock
	private SearchQuery<FhirImagingStudy, ImagingStudy, BahmniFhirImagingStudyDao, BahmniFhirImagingStudyTranslator, SearchQueryInclude<ImagingStudy>> searchQuery;
	
	@Mock
	private FhirGlobalPropertyService globalPropertyService;
	
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
		        patientReferenceTranslator, locationReferenceTranslator, practitionerReferenceTranslator);
		fhirImagingStudyService = new BahmniFhirImagingStudyServiceImpl(
		                                                                imagingStudyDao, imagingStudyTranslator,
		                                                                serviceRequestDao, searchQueryInclude, searchQuery) {
			
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
        //we are not considering client id for resoruces
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
        when(practitionerReferenceTranslator.toFhirResource(
                ArgumentMatchers.argThat(provider -> {
                    return provider.getUuid().equals("example-technician-id");
                })))
                .thenReturn(new Reference("Practitioner/example-technician-id"));
        ImagingStudy imagingStudy = fhirImagingStudyService.update("example-imaging-study", (ImagingStudy)  fhirResource);
        Assert.assertEquals(ImagingStudy.ImagingStudyStatus.REGISTERED, imagingStudy.getStatus());
        Extension performerExt = imagingStudy.getExtensionByUrl(BahmniFhirConstants.FHIR_EXT_IMAGING_STUDY_PERFORMER);
        Assert.assertNotNull("Performer extension should not be null for Imaging Study", performerExt);
    }
	
	@Test
    public void shouldUpdateImagingStudyWithNullOrderGracefully() throws IOException {
        Location studyLocation = new Location();
        studyLocation.setName("Radiology Center");
        studyLocation.setUuid("example-radiology-center");

        Provider performer = new Provider();
        performer.setUuid("example-technician-id");

        FhirImagingStudy existingStudy = new FhirImagingStudy();
        existingStudy.setStudyInstanceUuid("urn:oid:2.16.124.113543.6003.1154777499.30246.19789.3503430045");
        existingStudy.setStatus(FhirImagingStudy.FhirImagingStudyStatus.REGISTERED);

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
        when(practitionerReferenceTranslator.toFhirResource(
                ArgumentMatchers.argThat(provider -> {
                    return provider.getUuid().equals("example-technician-id");
                })))
                .thenReturn(new Reference("Practitioner/example-technician-id"));
        
        ImagingStudy imagingStudy = fhirImagingStudyService.update("example-imaging-study", (ImagingStudy)  fhirResource);
        
        Assert.assertEquals(ImagingStudy.ImagingStudyStatus.REGISTERED, imagingStudy.getStatus());
        verify(serviceRequestDao, never()).updateOrder(any());
    }
	
	@Test
    public void shouldNotUpdateOrderWhenFulfillerStatusIsNull() throws IOException {
        Location studyLocation = new Location();
        studyLocation.setName("Radiology Center");
        studyLocation.setUuid("example-radiology-center");

        Order existingOrder = new Order();
        existingOrder.setUuid("existing-order-uuid");
        existingOrder.setFulfillerStatus(Order.FulfillerStatus.RECEIVED);

        FhirImagingStudy existingStudy = new FhirImagingStudy();
        existingStudy.setStudyInstanceUuid("urn:oid:2.16.124.113543.6003.1154777499.30246.19789.3503430045");
        existingStudy.setStatus(FhirImagingStudy.FhirImagingStudyStatus.REGISTERED);
        existingStudy.setOrder(existingOrder);

        IBaseResource fhirResource = loadResourceFromFile("example-imaging-study-registered.json");
        ((ImagingStudy) fhirResource).setStatus(ImagingStudy.ImagingStudyStatus.UNKNOWN);

        when(imagingStudyDao.get("example-imaging-study")).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(locationReferenceTranslator.toOpenmrsType(
                ArgumentMatchers.argThat(reference -> {
                    return reference.getReference().equals("Location/example-radiology-center");
                })))
                .thenReturn(studyLocation);

        ImagingStudy imagingStudy = fhirImagingStudyService.update("example-imaging-study", (ImagingStudy)  fhirResource);

        Assert.assertEquals(ImagingStudy.ImagingStudyStatus.UNKNOWN, imagingStudy.getStatus());
        verify(serviceRequestDao, never()).updateOrder(any());
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
}
