package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirServiceRequestDao;
import org.hamcrest.Matchers;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Order;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.FhirGlobalPropertyService;
import org.openmrs.module.fhir2.api.dao.FhirServiceRequestDao;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryBundleProvider;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.search.param.PropParam;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.translators.ServiceRequestTranslator;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hl7.fhir.r4.model.Patient.SP_GIVEN;
import static org.hl7.fhir.r4.model.Practitioner.SP_IDENTIFIER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.openmrs.module.fhir2.FhirConstants.*;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirServiceRequestServiceImplTest {
	
	private static final Integer SERVICE_REQUEST_ID = 123;
	
	private static final String SERVICE_REQUEST_UUID = "249b9094-b812-4b0c-a204-0052a05c657f";
	
	private static final ServiceRequest.ServiceRequestStatus STATUS = ServiceRequest.ServiceRequestStatus.ACTIVE;
	
	private static final ServiceRequest.ServiceRequestPriority PRIORITY = ServiceRequest.ServiceRequestPriority.ROUTINE;
	
	private static final String LAST_UPDATED_DATE = "2020-09-03";
	
	private static final String PATIENT_GIVEN_NAME = "Meantex";
	
	private static final String CODE = "5089";
	
	private static final String ENCOUNTER_UUID = "y403fafb-e5e4-42d0-9d11-4f52e89d123r";
	
	private static final String PARTICIPANT_IDENTIFIER = "101-6";
	
	private static final String OCCURRENCE = "2020-09-03";
	
	@Mock
	private ServiceRequestTranslator<Order> translator;
	
	@Mock
	private BahmniFhirServiceRequestDao<Order> dao;
	
	@Mock
	private FhirGlobalPropertyService globalPropertyService;
	
	@Mock
	private SearchQueryInclude<ServiceRequest> searchQueryInclude;
	
	@Mock
	private SearchQuery<Order, ServiceRequest, FhirServiceRequestDao<Order>, ServiceRequestTranslator<Order>, SearchQueryInclude<ServiceRequest>> searchQuery;
	
	private BahmniFhirServiceRequestServiceImpl serviceRequestService;
	
	private ServiceRequest fhirServiceRequest;
	
	private Order order;
	
	@Before
	public void setUp() {
		serviceRequestService = new BahmniFhirServiceRequestServiceImpl() {
			
			@Override
			protected void validateObject(Order object) {
			}
		};
		
		serviceRequestService.setDao(dao);
		serviceRequestService.setTranslator(translator);
		serviceRequestService.setSearchQuery(searchQuery);
		serviceRequestService.setSearchQueryInclude(searchQueryInclude);
		
		order = new Order();
		order.setUuid(SERVICE_REQUEST_UUID);
		
		fhirServiceRequest = new ServiceRequest();
		fhirServiceRequest.setId(SERVICE_REQUEST_UUID);
		fhirServiceRequest.setStatus(STATUS);
		fhirServiceRequest.setPriority(PRIORITY);
	}
	
	private List<IBaseResource> get(IBundleProvider results) {
		return results.getResources(0, 10);
	}
	
	@Test
	public void shouldRetrieveServiceRequestByUUID() {
		when(dao.get(SERVICE_REQUEST_UUID)).thenReturn(order);
		when(translator.toFhirResource(order)).thenReturn(fhirServiceRequest);
		
		ServiceRequest result = serviceRequestService.get(SERVICE_REQUEST_UUID);
		
		assertThat(result, notNullValue());
		assertThat(result.getId(), equalTo(SERVICE_REQUEST_UUID));
	}
	
	@Test
    public void searchForServiceRequest_shouldReturnCollectionOfServiceRequestByPatientParam() {
        ReferenceAndListParam patientReference = new ReferenceAndListParam().addAnd(
                new ReferenceOrListParam().add(new ReferenceParam().setValue(PATIENT_GIVEN_NAME).setChain(SP_GIVEN)));

        SearchParameterMap theParams = new SearchParameterMap();
        theParams.addParameter(PATIENT_REFERENCE_SEARCH_HANDLER, patientReference);

        when(dao.getSearchResults(any())).thenReturn(Collections.singletonList(order));
        when(translator.toFhirResource(order)).thenReturn(fhirServiceRequest);
        when(translator.toFhirResources(anyCollection())).thenCallRealMethod();
        when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
                new SearchQueryBundleProvider<>(theParams, dao, translator, globalPropertyService, searchQueryInclude));
        when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());

        IBundleProvider results = serviceRequestService.searchForServiceRequests(patientReference, null, null, null, null,
                null, null, null);

        List<IBaseResource> resultList = get(results);

        assertThat(results, notNullValue());
        assertThat(resultList, not(empty()));
        assertThat(resultList, hasSize(equalTo(1)));
    }
	
	@Test
    public void searchForServiceRequest_shouldReturnCollectionOfServiceRequestByCode() {
        TokenAndListParam code = new TokenAndListParam().addAnd(new TokenParam(CODE));

        SearchParameterMap theParams = new SearchParameterMap();
        theParams.addParameter(CODED_SEARCH_HANDLER, code);

        when(dao.getSearchResults(any())).thenReturn(Collections.singletonList(order));
        when(translator.toFhirResource(order)).thenReturn(fhirServiceRequest);
        when(translator.toFhirResources(anyCollection())).thenCallRealMethod();
        when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
                new SearchQueryBundleProvider<>(theParams, dao, translator, globalPropertyService, searchQueryInclude));
        when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());

        IBundleProvider results = serviceRequestService.searchForServiceRequests(null, code, null, null, null, null, null,
                null);

        List<IBaseResource> resultList = get(results);

        assertThat(results, notNullValue());
        assertThat(resultList, not(empty()));
        assertThat(resultList, hasSize(equalTo(1)));
    }
	
	@Test
    public void searchForServiceRequest_shouldReturnCollectionOfServiceRequestByEncounter() {
        ReferenceAndListParam encounterReference = new ReferenceAndListParam()
                .addAnd(new ReferenceOrListParam().add(new ReferenceParam().setValue(ENCOUNTER_UUID).setChain(null)));

        SearchParameterMap theParams = new SearchParameterMap();
        theParams.addParameter(ENCOUNTER_REFERENCE_SEARCH_HANDLER, encounterReference);

        when(dao.getSearchResults(any())).thenReturn(Collections.singletonList(order));
        when(translator.toFhirResource(order)).thenReturn(fhirServiceRequest);
        when(translator.toFhirResources(anyCollection())).thenCallRealMethod();
        when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
                new SearchQueryBundleProvider<>(theParams, dao, translator, globalPropertyService, searchQueryInclude));
        when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());

        IBundleProvider results = serviceRequestService.searchForServiceRequests(null, null, encounterReference, null, null,
                null, null, null);

        List<IBaseResource> resultList = get(results);

        assertThat(results, notNullValue());
        assertThat(resultList, not(empty()));
        assertThat(resultList, hasSize(equalTo(1)));
    }
	
	@Test
    public void searchForServiceRequest_shouldReturnCollectionOfServiceRequestByRequester() {
        ReferenceAndListParam participantReference = new ReferenceAndListParam().addAnd(
                new ReferenceOrListParam().add(new ReferenceParam().setValue(PARTICIPANT_IDENTIFIER).setChain(SP_IDENTIFIER)));

        SearchParameterMap theParams = new SearchParameterMap();
        theParams.addParameter(PARTICIPANT_REFERENCE_SEARCH_HANDLER, participantReference);

        when(dao.getSearchResults(any())).thenReturn(Collections.singletonList(order));
        when(translator.toFhirResource(order)).thenReturn(fhirServiceRequest);
        when(translator.toFhirResources(anyCollection())).thenCallRealMethod();
        when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
                new SearchQueryBundleProvider<>(theParams, dao, translator, globalPropertyService, searchQueryInclude));
        when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());

        IBundleProvider results = serviceRequestService.searchForServiceRequests(null, null, null, participantReference,
                null, null, null, null);

        List<IBaseResource> resultList = get(results);

        assertThat(results, notNullValue());
        assertThat(resultList, not(empty()));
        assertThat(resultList, hasSize(equalTo(1)));
    }
	
	@Test
    public void searchForServiceRequest_shouldReturnCollectionOfServiceRequestByOccurrence() {
        DateRangeParam occurrence = new DateRangeParam().setLowerBound(OCCURRENCE).setUpperBound(OCCURRENCE);

        SearchParameterMap theParams = new SearchParameterMap();
        theParams.addParameter(DATE_RANGE_SEARCH_HANDLER, occurrence);

        when(dao.getSearchResults(any())).thenReturn(Collections.singletonList(order));
        when(translator.toFhirResource(order)).thenReturn(fhirServiceRequest);
        when(translator.toFhirResources(anyCollection())).thenCallRealMethod();
        when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
                new SearchQueryBundleProvider<>(theParams, dao, translator, globalPropertyService, searchQueryInclude));
        when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());

        IBundleProvider results = serviceRequestService.searchForServiceRequests(null, null, null, null, occurrence, null,
                null, null);

        List<IBaseResource> resultList = get(results);

        assertThat(results, notNullValue());
        assertThat(resultList, not(empty()));
        assertThat(resultList, hasSize(equalTo(1)));
    }
	
	@Test
    public void searchForServiceRequest_shouldReturnCollectionOfServiceRequestByUUID() {
        TokenAndListParam uuid = new TokenAndListParam().addAnd(new TokenParam(SERVICE_REQUEST_UUID));

        SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.COMMON_SEARCH_HANDLER,
                FhirConstants.ID_PROPERTY, uuid);

        when(dao.getSearchResults(any())).thenReturn(Collections.singletonList(order));
        when(translator.toFhirResource(order)).thenReturn(fhirServiceRequest);
        when(translator.toFhirResources(anyCollection())).thenCallRealMethod();
        when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
                new SearchQueryBundleProvider<>(theParams, dao, translator, globalPropertyService, searchQueryInclude));
        when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());

        IBundleProvider results = serviceRequestService.searchForServiceRequests(null, null, null, null, null, uuid, null,
                null);

        List<IBaseResource> resultList = get(results);

        assertThat(results, Matchers.notNullValue());
        assertThat(resultList, not(empty()));
        assertThat(resultList, hasSize(greaterThanOrEqualTo(1)));
    }
	
	@Test
    public void searchForServiceRequest_shouldReturnCollectionOfServiceRequestByLastUpdated() {
        DateRangeParam lastUpdated = new DateRangeParam().setUpperBound(LAST_UPDATED_DATE).setLowerBound(LAST_UPDATED_DATE);

        SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.COMMON_SEARCH_HANDLER,
                FhirConstants.LAST_UPDATED_PROPERTY, lastUpdated);

        when(dao.getSearchResults(any())).thenReturn(Collections.singletonList(order));
        when(translator.toFhirResource(order)).thenReturn(fhirServiceRequest);
        when(translator.toFhirResources(anyCollection())).thenCallRealMethod();
        when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
                new SearchQueryBundleProvider<>(theParams, dao, translator, globalPropertyService, searchQueryInclude));
        when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());

        IBundleProvider results = serviceRequestService.searchForServiceRequests(null, null, null, null, null, null,
                lastUpdated, null);

        List<IBaseResource> resultList = get(results);

        assertThat(results, Matchers.notNullValue());
        assertThat(resultList, not(empty()));
        assertThat(resultList, hasSize(greaterThanOrEqualTo(1)));
    }
	
	@Test
    public void searchForPeople_shouldAddRelatedResourcesWhenIncluded() {
        HashSet<Include> includes = new HashSet<>();
        includes.add(new Include("ServiceRequest:patient"));

        SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.INCLUDE_SEARCH_HANDLER, includes);

        when(dao.getSearchResults(any())).thenReturn(Collections.singletonList(order));
        when(translator.toFhirResource(order)).thenReturn(fhirServiceRequest);
        when(translator.toFhirResources(anyCollection())).thenCallRealMethod();
        when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
                new SearchQueryBundleProvider<>(theParams, dao, translator, globalPropertyService, searchQueryInclude));
        when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.singleton(new Patient()));

        IBundleProvider results = serviceRequestService.searchForServiceRequests(null, null, null, null, null, null, null,
                includes);

        List<IBaseResource> resultList = get(results);

        assertThat(results, notNullValue());
        assertThat(resultList, not(empty()));
        assertThat(resultList.size(), equalTo(2));
        assertThat(resultList, hasItem(is(instanceOf(Patient.class))));
    }
	
	@Test
    public void searchForPeople_shouldAddRelatedResourcesWhenIncludedR3() {
        HashSet<Include> includes = new HashSet<>();
        includes.add(new Include("ProcedureRequest:patient"));

        SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.INCLUDE_SEARCH_HANDLER, includes);

        when(dao.getSearchResults(any())).thenReturn(Collections.singletonList(order));
        when(translator.toFhirResource(order)).thenReturn(fhirServiceRequest);
        when(translator.toFhirResources(anyCollection())).thenCallRealMethod();
        when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
                new SearchQueryBundleProvider<>(theParams, dao, translator, globalPropertyService, searchQueryInclude));
        when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.singleton(new Patient()));

        IBundleProvider results = serviceRequestService.searchForServiceRequests(null, null, null, null, null, null, null,
                includes);

        List<IBaseResource> resultList = get(results);

        assertThat(results, notNullValue());
        assertThat(resultList, not(empty()));
        assertThat(resultList.size(), equalTo(2));
        assertThat(resultList, hasItem(is(instanceOf(Patient.class))));
    }
	
	@Test
    public void searchForPeople_shouldNotAddRelatedResourcesForEmptyInclude() {
        HashSet<Include> includes = new HashSet<>();

        SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.INCLUDE_SEARCH_HANDLER, includes);

        when(dao.getSearchResults(any())).thenReturn(Collections.singletonList(order));
        when(translator.toFhirResource(order)).thenReturn(fhirServiceRequest);
        when(translator.toFhirResources(anyCollection())).thenCallRealMethod();
        when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
                new SearchQueryBundleProvider<>(theParams, dao, translator, globalPropertyService, searchQueryInclude));
        when(searchQueryInclude.getIncludedResources(any(), any())).thenReturn(Collections.emptySet());

        IBundleProvider results = serviceRequestService.searchForServiceRequests(null, null, null, null, null, null, null,
                includes);

        List<IBaseResource> resultList = get(results);

        assertThat(results, notNullValue());
        assertThat(resultList, not(empty()));
        assertThat(resultList.size(), equalTo(1));
    }
	
	/**
	 * Helper method to create a ReferenceAndListParam with a given value and chain
	 */
	private ReferenceAndListParam createReferenceParam(String value, String chain) {
		return new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam().setValue(value)
		        .setChain(chain)));
	}
	
	/**
	 * Helper method to create a TokenAndListParam with a given system and code
	 */
	private TokenAndListParam createTokenParam(String system, String code) {
		return new TokenAndListParam().addAnd(new TokenParam(system, code));
	}
	
	/**
	 * Helper method to create a DateRangeParam with given bounds
	 */
	private DateRangeParam createDateRangeParam(String lowerBound, String upperBound) {
		return new DateRangeParam().setLowerBound(lowerBound).setUpperBound(upperBound);
	}
	
	@Test
    public void searchForServiceRequests_shouldInvokeDaoWithProperSearchParamMap() {
        // Create test parameters for all search parameters
        ReferenceAndListParam patientReference = createReferenceParam(PATIENT_GIVEN_NAME, SP_GIVEN);
        TokenAndListParam code = createTokenParam("CIEL", "1234567898");
        ReferenceAndListParam encounterReference = createReferenceParam(ENCOUNTER_UUID, null);
        ReferenceAndListParam participantReference = createReferenceParam(PARTICIPANT_IDENTIFIER, SP_IDENTIFIER);
        DateRangeParam occurrence = createDateRangeParam(OCCURRENCE, OCCURRENCE);
        TokenAndListParam uuid = createTokenParam(null, SERVICE_REQUEST_UUID);
        DateRangeParam lastUpdated = createDateRangeParam(LAST_UPDATED_DATE, LAST_UPDATED_DATE);
        HashSet<Include> includes = new HashSet<>();
        includes.add(new Include("ServiceRequest:patient"));

        // Call the service method with all parameters
        IBundleProvider results = serviceRequestService.searchForServiceRequests(
                patientReference, code, encounterReference, participantReference,
                occurrence, uuid, lastUpdated, includes);

        // Capture the actual SearchParameterMap passed to searchQuery.getQueryResults
        ArgumentCaptor<SearchParameterMap> mapCaptor = ArgumentCaptor.forClass(SearchParameterMap.class);
        verify(searchQuery).getQueryResults(mapCaptor.capture(), eq(dao), eq(translator), eq(searchQueryInclude));

        // Get the captured parameter map
        SearchParameterMap actualMap = mapCaptor.getValue();

        // Verify the map contains all expected parameters
        assertBasicSearchParams(patientReference, actualMap, code, encounterReference, participantReference, occurrence, uuid, lastUpdated, includes);
    }
	
	@Test
    public void searchForServiceRequestsWithCategory_shouldInvokeDaoWithProperSearchParamMap() {
        // Create test parameters for all search parameters
        ReferenceAndListParam patientReference = createReferenceParam(PATIENT_GIVEN_NAME, SP_GIVEN);
        TokenAndListParam code = createTokenParam("CIEL", "1234567898");
        ReferenceAndListParam encounterReference = createReferenceParam(ENCOUNTER_UUID, null);
        ReferenceAndListParam participantReference = createReferenceParam(PARTICIPANT_IDENTIFIER, SP_IDENTIFIER);
        ReferenceAndListParam category = createReferenceParam("lab", null);
        DateRangeParam occurrence = createDateRangeParam(OCCURRENCE, OCCURRENCE);
        TokenAndListParam uuid = createTokenParam(null, SERVICE_REQUEST_UUID);
        DateRangeParam lastUpdated = createDateRangeParam(LAST_UPDATED_DATE, LAST_UPDATED_DATE);
        HashSet<Include> includes = new HashSet<>();
        includes.add(new Include("ServiceRequest:patient"));

        // Call the service method with all parameters including category
        IBundleProvider results = serviceRequestService.searchForServiceRequestsWithCategory(
                patientReference, code, encounterReference, participantReference,
                category, occurrence, uuid, lastUpdated, includes);

        // Capture the actual SearchParameterMap passed to searchQuery.getQueryResults
        ArgumentCaptor<SearchParameterMap> mapCaptor = ArgumentCaptor.forClass(SearchParameterMap.class);
        verify(searchQuery).getQueryResults(mapCaptor.capture(), eq(dao), eq(translator), eq(searchQueryInclude));

        // Get the captured parameter map
        SearchParameterMap actualMap = mapCaptor.getValue();

        assertBasicSearchParams(patientReference, actualMap, code, encounterReference, participantReference, occurrence, uuid, lastUpdated, includes);
        assertEquals(category, actualMap.getParameters(CATEGORY_SEARCH_HANDLER).get(0).getParam());
    }
	
	private void assertBasicSearchParams(ReferenceAndListParam patientReference, SearchParameterMap actualMap,
	        TokenAndListParam code, ReferenceAndListParam encounterReference, ReferenceAndListParam participantReference,
	        DateRangeParam occurrence, TokenAndListParam uuid, DateRangeParam lastUpdated, HashSet<Include> includes) {
		assertEquals(patientReference, actualMap.getParameters(PATIENT_REFERENCE_SEARCH_HANDLER).get(0).getParam());
		assertEquals(code, actualMap.getParameters(CODED_SEARCH_HANDLER).get(0).getParam());
		assertEquals(encounterReference, actualMap.getParameters(ENCOUNTER_REFERENCE_SEARCH_HANDLER).get(0).getParam());
		assertEquals(participantReference, actualMap.getParameters(PARTICIPANT_REFERENCE_SEARCH_HANDLER).get(0).getParam());
		assertEquals(occurrence, actualMap.getParameters(DATE_RANGE_SEARCH_HANDLER).get(0).getParam());
		
		// For common search handler parameters, we need to check both the property and value
		List<PropParam<?>> commonParams = actualMap.getParameters(COMMON_SEARCH_HANDLER);
		
		for (PropParam entry : commonParams) {
			if (entry.getPropertyName().equals(ID_PROPERTY)) {
				assertEquals(uuid, entry.getParam());
			} else if (entry.getPropertyName().equals(LAST_UPDATED_PROPERTY)) {
				assertEquals(lastUpdated, entry.getParam());
			}
		}
		
		// Verify includes parameter
		assertEquals(includes, actualMap.getParameters(INCLUDE_SEARCH_HANDLER).get(0).getParam());
	}
	
	@Test
	public void searchForServiceRequestsByNumberOfVisits_shouldReturnServiceRequestsForGivenNumberOfVisits() {
		// Setup test parameters
		ReferenceParam patientReference = new ReferenceParam().setValue(PATIENT_GIVEN_NAME);
		NumberParam numberOfVisits = new NumberParam(3);
		ReferenceAndListParam category = createReferenceParam("lab", null);
		HashSet<Include> includes = new HashSet<>();
		includes.add(new Include("ServiceRequest:patient"));
		
		// Setup mock DAO response for encounter references
		ReferenceAndListParam encounterReferences = new ReferenceAndListParam()
				.addAnd(new ReferenceOrListParam().add(new ReferenceParam().setValue(ENCOUNTER_UUID)));
		when(dao.createEncounterReferencesByNumberOfVisit(eq(numberOfVisits), eq(patientReference)))
				.thenReturn(encounterReferences);
		
		// Create expected search parameter map
		SearchParameterMap expectedParams = new SearchParameterMap()
				.addParameter(ENCOUNTER_REFERENCE_SEARCH_HANDLER, encounterReferences)
				.addParameter(CATEGORY_SEARCH_HANDLER, category)
				.addParameter(INCLUDE_SEARCH_HANDLER, includes);
				

		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
				new SearchQueryBundleProvider<>(expectedParams, dao, translator, globalPropertyService, searchQueryInclude));
		
		// Call the method under test
		IBundleProvider results = serviceRequestService.searchForServiceRequestsByNumberOfVisits(
				patientReference, numberOfVisits, category, includes);
		
		// Verify results
		assertThat(results, notNullValue());
		
		// Capture and verify the search parameter map
		ArgumentCaptor<SearchParameterMap> mapCaptor = ArgumentCaptor.forClass(SearchParameterMap.class);
		verify(searchQuery).getQueryResults(mapCaptor.capture(), eq(dao), eq(translator), eq(searchQueryInclude));
		
		SearchParameterMap actualMap = mapCaptor.getValue();
		assertEquals(encounterReferences, actualMap.getParameters(ENCOUNTER_REFERENCE_SEARCH_HANDLER).get(0).getParam());
		assertEquals(category, actualMap.getParameters(CATEGORY_SEARCH_HANDLER).get(0).getParam());
		assertEquals(includes, actualMap.getParameters(INCLUDE_SEARCH_HANDLER).get(0).getParam());
	}
	
	@Test(expected = InvalidRequestException.class)
	public void searchForServiceRequestsByNumberOfVisits_shouldThrowExceptionWhenPatientReferenceIsNull() {
		// Call the method with null patient reference
		serviceRequestService.searchForServiceRequestsByNumberOfVisits(null, new NumberParam(3), null, null);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void searchForServiceRequestsByNumberOfVisits_shouldThrowExceptionWhenNumberOfVisitsIsNull() {
		// Call the method with null number of visits
		serviceRequestService.searchForServiceRequestsByNumberOfVisits(new ReferenceParam().setValue(PATIENT_GIVEN_NAME),
		    null, null, null);
	}
	
	@Test
	public void searchForServiceRequestsByNumberOfVisits_shouldReturnNullWhenNoEncountersFound() {
		// Setup test parameters
		ReferenceParam patientReference = new ReferenceParam().setValue(PATIENT_GIVEN_NAME);
		NumberParam numberOfVisits = new NumberParam(3);
		
		// Mock DAO to return null for encounter references
		when(dao.createEncounterReferencesByNumberOfVisit(eq(numberOfVisits), eq(patientReference))).thenReturn(null);
		
		// Call the method under test
		IBundleProvider results = serviceRequestService.searchForServiceRequestsByNumberOfVisits(patientReference,
		    numberOfVisits, null, null);
		
		// Verify results
		assertThat(results, nullValue());
		
		// Verify DAO was called with correct parameters
		verify(dao).createEncounterReferencesByNumberOfVisit(eq(numberOfVisits), eq(patientReference));
		
		// Verify searchQuery was not called
		verify(searchQuery, times(0)).getQueryResults(any(), any(), any(), any());
	}
}
