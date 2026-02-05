package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2AddlExtension.api.context.AppContext;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirServiceRequestDao;
import org.bahmni.module.fhir2AddlExtension.api.dao.OrderAttributeTypeDao;
import org.bahmni.module.fhir2AddlExtension.api.service.OpenMRSOrderServiceExtension;
import org.hamcrest.Matchers;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
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
import org.openmrs.LocationAttribute;
import org.openmrs.LocationAttributeType;
import org.openmrs.Order;
import org.openmrs.OrderAttributeType;
import org.openmrs.OrderType;
import org.openmrs.PersonName;
import org.openmrs.User;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.FhirGlobalPropertyService;
import org.openmrs.module.fhir2.api.dao.FhirServiceRequestDao;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryBundleProvider;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.search.param.PropParam;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.translators.LocationReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.ServiceRequestTranslator;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hl7.fhir.r4.model.Patient.SP_GIVEN;
import static org.hl7.fhir.r4.model.Practitioner.SP_IDENTIFIER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openmrs.module.fhir2.FhirConstants.CATEGORY_SEARCH_HANDLER;
import static org.openmrs.module.fhir2.FhirConstants.CODED_SEARCH_HANDLER;
import static org.openmrs.module.fhir2.FhirConstants.COMMON_SEARCH_HANDLER;
import static org.openmrs.module.fhir2.FhirConstants.DATE_RANGE_SEARCH_HANDLER;
import static org.openmrs.module.fhir2.FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER;
import static org.openmrs.module.fhir2.FhirConstants.ID_PROPERTY;
import static org.openmrs.module.fhir2.FhirConstants.INCLUDE_SEARCH_HANDLER;
import static org.openmrs.module.fhir2.FhirConstants.LAST_UPDATED_PROPERTY;
import static org.openmrs.module.fhir2.FhirConstants.PARTICIPANT_REFERENCE_SEARCH_HANDLER;
import static org.openmrs.module.fhir2.FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirServiceRequestServiceImplTest {
	
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
	
	@Mock
	OrderAttributeTypeDao orderAttributeTypeDao;
	
	@Mock
	LocationReferenceTranslator locationReferenceTranslator;

    @Mock
    AppContext appContext;

    @Mock
    OpenMRSOrderServiceExtension openMRSOrderServiceExtension;
	
	private BahmniFhirServiceRequestServiceImpl serviceRequestService;
	
	private ServiceRequest fhirServiceRequest;
	
	private Order order;
	
	private User user;

    private Map<String, String> orderTypeToLocationAttributeNameMap = Stream.of(new Object[][] {
            { "RADIOLOGY ORDER", "REFERRAL_RADIOLOGY_CENTER" },
            { "TEST ORDER", "REFERRAL_LABORATORY_CENTER" },
            { "LAB ORDER", "REFERRAL_LABORATORY_CENTER" }
    }).collect(Collectors.toMap(
            data -> (String) data[0],
            data -> (String) data[1]
    ));


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
        serviceRequestService.setOpenMRSOrderServiceExtension(openMRSOrderServiceExtension);
		user = exampleUser();

        when(appContext.getCurrentUser()).thenReturn(user);
        when(appContext.getOrderTypeToLocationAttributeNameMap()).thenReturn(orderTypeToLocationAttributeNameMap);
		ServiceRequestLocationReferenceResolverImpl orderLocationReferenceResolver =
            new ServiceRequestLocationReferenceResolverImpl(locationReferenceTranslator, orderAttributeTypeDao,appContext);
		serviceRequestService.setLocationReferenceResolver(orderLocationReferenceResolver);
		
		order = new Order();
		order.setUuid(SERVICE_REQUEST_UUID);
		
		fhirServiceRequest = new ServiceRequest();
		fhirServiceRequest.setId(SERVICE_REQUEST_UUID);
		fhirServiceRequest.setStatus(STATUS);
		fhirServiceRequest.setPriority(PRIORITY);
        when(openMRSOrderServiceExtension.validateAndSetMissingFields(any(Order.class), ArgumentMatchers.isNull()))
                .thenAnswer(returnsFirstArg());
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
                category, occurrence, uuid, lastUpdated, includes, null);

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
		SortSpec sort = new SortSpec("_lastUpdated");
		HashSet<Include> includes = new HashSet<>();
		includes.add(new Include("ServiceRequest:patient"));
		
		// Setup mock DAO response for encounter references
		ReferenceAndListParam encounterReferences = new ReferenceAndListParam()
				.addAnd(new ReferenceOrListParam().add(new ReferenceParam().setValue(ENCOUNTER_UUID)));
		when(dao.getEncounterReferencesByNumberOfVisit(eq(numberOfVisits), eq(patientReference)))
				.thenReturn(encounterReferences);
		
		// Create expected search parameter map
		SearchParameterMap expectedParams = new SearchParameterMap()
				.addParameter(ENCOUNTER_REFERENCE_SEARCH_HANDLER, encounterReferences)
				.addParameter(CATEGORY_SEARCH_HANDLER, category)
				.addParameter(INCLUDE_SEARCH_HANDLER, includes);
		if (sort != null) {
			expectedParams.setSortSpec(sort);
		}
				

		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
				new SearchQueryBundleProvider<>(expectedParams, dao, translator, globalPropertyService, searchQueryInclude));
		
		// Call the method under test
		IBundleProvider results = serviceRequestService.searchForServiceRequestsByNumberOfVisits(
				patientReference, numberOfVisits, category, sort, includes, null);
		
		// Verify results
		assertThat(results, notNullValue());
		
		// Capture and verify the search parameter map
		ArgumentCaptor<SearchParameterMap> mapCaptor = ArgumentCaptor.forClass(SearchParameterMap.class);
		verify(searchQuery).getQueryResults(mapCaptor.capture(), eq(dao), eq(translator), eq(searchQueryInclude));
		
		SearchParameterMap actualMap = mapCaptor.getValue();
		assertEquals(encounterReferences, actualMap.getParameters(ENCOUNTER_REFERENCE_SEARCH_HANDLER).get(0).getParam());
		assertEquals(category, actualMap.getParameters(CATEGORY_SEARCH_HANDLER).get(0).getParam());
		assertEquals(sort, actualMap.getSortSpec());
		assertEquals(includes, actualMap.getParameters(INCLUDE_SEARCH_HANDLER).get(0).getParam());
	}
	
	@Test(expected = InvalidRequestException.class)
	public void searchForServiceRequestsByNumberOfVisits_shouldThrowExceptionWhenPatientReferenceIsNull() {
		// Call the method with null patient reference
		serviceRequestService.searchForServiceRequestsByNumberOfVisits(null, new NumberParam(3), null, null, null, null);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void searchForServiceRequestsByNumberOfVisits_shouldThrowExceptionWhenNumberOfVisitsIsNull() {
		// Call the method with null number of visits
		serviceRequestService.searchForServiceRequestsByNumberOfVisits(new ReferenceParam().setValue(PATIENT_GIVEN_NAME),
		    null, null, null, null, null);
	}
	
	@Test
	public void searchForServiceRequestsByNumberOfVisits_shouldReturnNullWhenNoEncountersFound() {
		// Setup test parameters
		ReferenceParam patientReference = new ReferenceParam().setValue(PATIENT_GIVEN_NAME);
		NumberParam numberOfVisits = new NumberParam(3);
		
		// Mock DAO to return null for encounter references
		when(dao.getEncounterReferencesByNumberOfVisit(eq(numberOfVisits), eq(patientReference))).thenReturn(null);
		
		// Call the method under test
		IBundleProvider results = serviceRequestService.searchForServiceRequestsByNumberOfVisits(patientReference,
		    numberOfVisits, null, null, null, null);
		
		// Verify results
		assertThat(results, nullValue());
		
		// Verify DAO was called with correct parameters
		verify(dao).getEncounterReferencesByNumberOfVisit(eq(numberOfVisits), eq(patientReference));
		
		// Verify searchQuery was not called
		verify(searchQuery, times(0)).getQueryResults(any(), any(), any(), any());
	}
	
	@Test
    public void createServiceRequestShouldSetLocationReferenceOnOrder() {
        // Create different concept for different order type
        OrderType labOrderType = new OrderType();
        labOrderType.setUuid("lab-order-type-uuid");
        labOrderType.setName("Lab Order");

        Location pathLab = new Location();
        pathLab.setName("Path lab");
        pathLab.setUuid(UUID.randomUUID().toString());
        LocationAttributeType locationAttributeType = new LocationAttributeType();
        locationAttributeType.setName("REFERRAL_LABORATORY_CENTER");
        locationAttributeType.setDatatypeClassname(ServiceRequestLocationReferenceResolverImpl.LOCATION_DATA_TYPE);

        Location clinic = new Location();
        clinic.setName("Clinic");
        clinic.setUuid(UUID.randomUUID().toString());
        LocationAttribute referLocation = new LocationAttribute();
        referLocation.setAttributeType(locationAttributeType);
        referLocation.setValue(pathLab);
        clinic.addAttribute(referLocation);

        Encounter encounter = new Encounter();
        encounter.setLocation(clinic);

        OrderType orderType = new OrderType();
        orderType.setName("LAB ORDER");
        Order order = new Order();
        order.setOrderType(orderType);
        order.setEncounter(encounter);

        OrderAttributeType orderLocationType = new OrderAttributeType();
        orderLocationType.setName(ServiceRequestLocationReferenceResolverImpl.REQUESTED_LOCATION_FOR_ORDER);
        orderLocationType.setDatatypeClassname(ServiceRequestLocationReferenceResolverImpl.LOCATION_DATA_TYPE);
        when(orderAttributeTypeDao.getOrderAttributeTypes(false)).thenReturn(Collections.singletonList(orderLocationType));

        ServiceRequest serviceRequest = exampleServiceRequest();
        when(translator.toOpenmrsType(serviceRequest)).thenReturn(order);
        when(locationReferenceTranslator.toOpenmrsType(
                ArgumentMatchers.argThat(reference -> reference.getReference().equals("Location/" + pathLab.getUuid()))))
                .thenReturn(pathLab);

        ServiceRequest updatedResource = serviceRequestService.create(serviceRequest);
        Assert.assertEquals(1, order.getActiveAttributes().size());
        Assert.assertEquals(pathLab, order.getActiveAttributes().iterator().next().getValue());
    }
	
	@Test
	public void createServiceRequestShouldNotSetLocationReferenceOnOrderIfOrderAttributeIsNotDefined() {
		// Create different concept for different order type
		OrderType labOrderType = new OrderType();
		labOrderType.setUuid("lab-order-type-uuid");
		labOrderType.setName("Lab Order");
		
		Location pathLab = new Location();
		pathLab.setName("Path lab");
		pathLab.setUuid(UUID.randomUUID().toString());
		LocationAttributeType locationAttributeType = new LocationAttributeType();
		locationAttributeType.setName("REFERRAL_LABORATORY_CENTER");
		locationAttributeType.setDatatypeClassname(ServiceRequestLocationReferenceResolverImpl.LOCATION_DATA_TYPE);
		
		Location clinic = new Location();
		clinic.setName("Clinic");
		clinic.setUuid(UUID.randomUUID().toString());
		LocationAttribute referLocation = new LocationAttribute();
		referLocation.setAttributeType(locationAttributeType);
		referLocation.setValue(pathLab);
		clinic.addAttribute(referLocation);
		
		Encounter encounter = new Encounter();
		encounter.setLocation(clinic);
		
		OrderType orderType = new OrderType();
		orderType.setName("Lab Order");
		Order order = new Order();
		order.setOrderType(orderType);
		order.setEncounter(encounter);
		
		when(orderAttributeTypeDao.getOrderAttributeTypes(false)).thenReturn(Collections.emptyList());
		
		ServiceRequest serviceRequest = exampleServiceRequest();
		when(translator.toOpenmrsType(serviceRequest)).thenReturn(order);
		ServiceRequest updatedResource = serviceRequestService.create(serviceRequest);
		Assert.assertEquals(0, order.getActiveAttributes().size());
	}
	
	@Test
    public void createServiceRequestShouldSetVisitLocationOnOrderIfNoPreferredLocationIsDefined() {
        // Create different concept for different order type
        OrderType labOrderType = new OrderType();
        labOrderType.setUuid("lab-order-type-uuid");
        labOrderType.setName("Lab Order");

        LocationAttributeType otherAttributeType = new LocationAttributeType();
        otherAttributeType.setName("External Lab");
        otherAttributeType.setDatatypeClassname(ServiceRequestLocationReferenceResolverImpl.LOCATION_DATA_TYPE);

        Location clinic = new Location();
        clinic.setName("Clinic");
        clinic.setUuid(UUID.randomUUID().toString());
        LocationAttribute referLocation = new LocationAttribute();
        referLocation.setAttributeType(otherAttributeType);
        clinic.addAttribute(referLocation);

        Location labRoom = new Location();
        labRoom.setName("Lab Room");
        labRoom.setUuid(UUID.randomUUID().toString());
        labRoom.setParentLocation(clinic);

        Encounter encounter = new Encounter();
        encounter.setLocation(clinic);

        OrderType orderType = new OrderType();
        orderType.setName("Lab Order");
        Order order = new Order();
        order.setOrderType(orderType);
        order.setEncounter(encounter);

        OrderAttributeType orderLocationType = new OrderAttributeType();
        orderLocationType.setName(ServiceRequestLocationReferenceResolverImpl.REQUESTED_LOCATION_FOR_ORDER);
        orderLocationType.setDatatypeClassname(ServiceRequestLocationReferenceResolverImpl.LOCATION_DATA_TYPE);
        when(orderAttributeTypeDao.getOrderAttributeTypes(false)).thenReturn(Collections.singletonList(orderLocationType));

        ServiceRequest serviceRequest = exampleServiceRequest();
        when(translator.toOpenmrsType(serviceRequest)).thenReturn(order);
        when(locationReferenceTranslator.toOpenmrsType(
                ArgumentMatchers.argThat(reference -> reference.getReference().equals("Location/" + clinic.getUuid()))))
                .thenReturn(clinic);

        ServiceRequest updatedResource = serviceRequestService.create(serviceRequest);
        Assert.assertEquals(1, order.getActiveAttributes().size());
        Assert.assertEquals(clinic, order.getActiveAttributes().iterator().next().getValue());
    }

    @Test
    public void shouldInvokeOpenMRSOrderServiceExtensionValidationWhenCreatingServiceRequest() {
        ServiceRequest serviceRequest = exampleServiceRequest();

        OrderType orderType = new OrderType();
        orderType.setName("Lab Order");
        Order order = new Order();
        order.setOrderType(orderType);

        when(translator.toOpenmrsType(serviceRequest)).thenReturn(order);

        serviceRequestService.create(serviceRequest);

        verify(openMRSOrderServiceExtension).validateAndSetMissingFields(eq(order), ArgumentMatchers.isNull());
    }
	
	private ServiceRequest exampleServiceRequest() {
		// Create test ServiceRequest
		ServiceRequest serviceRequest = new ServiceRequest();
		serviceRequest.setId("test-service-request-id");
		serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);
		serviceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
		serviceRequest.setPriority(ServiceRequest.ServiceRequestPriority.ROUTINE);
		
		// Create test code
		CodeableConcept code = new CodeableConcept();
		Coding coding = new Coding();
		coding.setSystem("http://loinc.org");
		coding.setCode("12345-6");
		coding.setDisplay("Test Lab Order");
		code.addCoding(coding);
		serviceRequest.setCode(code);
		
		// Create test subject reference
		Reference subjectRef = new Reference();
		subjectRef.setReference("Patient/test-patient-uuid");
		serviceRequest.setSubject(subjectRef);
		
		// Create test encounter reference
		Reference encounterRef = new Reference();
		encounterRef.setReference("Encounter/test-encounter-uuid");
		serviceRequest.setEncounter(encounterRef);
		
		// Create test requester reference
		Reference requesterRef = new Reference();
		requesterRef.setReference("Practitioner/test-provider-uuid");
		serviceRequest.setRequester(requesterRef);
		return serviceRequest;
	}
	
	private User exampleUser() {
		User user = new User();
		PersonName personName = new PersonName();
		personName.setFamilyName("Beth");
		personName.setGivenName("Bethany");
		user.addName(personName);
		return user;
	}

	@Test
	public void searchForServiceRequestsWithCategory_shouldIncludeRevIncludeParamForImagingStudy() {
		ReferenceAndListParam patientReference = createReferenceParam(PATIENT_GIVEN_NAME, SP_GIVEN);
		ReferenceAndListParam category = createReferenceParam("radiology", null);
		HashSet<Include> revIncludes = new HashSet<>();
		revIncludes.add(new Include("ImagingStudy:basedon", true));

		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		        new SearchQueryBundleProvider<>(new SearchParameterMap(), dao, translator, globalPropertyService,
		                searchQueryInclude));

		IBundleProvider results = serviceRequestService.searchForServiceRequestsWithCategory(patientReference, null, null,
		    null, category, null, null, null, null, revIncludes);

		assertThat(results, notNullValue());

		ArgumentCaptor<SearchParameterMap> mapCaptor = ArgumentCaptor.forClass(SearchParameterMap.class);
		verify(searchQuery).getQueryResults(mapCaptor.capture(), eq(dao), eq(translator), eq(searchQueryInclude));

		SearchParameterMap actualMap = mapCaptor.getValue();
		assertThat(actualMap.getParameters(FhirConstants.REVERSE_INCLUDE_SEARCH_HANDLER), notNullValue());
		assertEquals(revIncludes,
		    actualMap.getParameters(FhirConstants.REVERSE_INCLUDE_SEARCH_HANDLER).get(0).getParam());
	}

	@Test
	public void searchForServiceRequestsWithCategory_shouldHandleNullRevIncludes() {
		ReferenceAndListParam patientReference = createReferenceParam(PATIENT_GIVEN_NAME, SP_GIVEN);
		ReferenceAndListParam category = createReferenceParam("radiology", null);

		when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(
		        new SearchQueryBundleProvider<>(new SearchParameterMap(), dao, translator, globalPropertyService,
		                searchQueryInclude));

		IBundleProvider results = serviceRequestService.searchForServiceRequestsWithCategory(patientReference, null, null,
		    null, category, null, null, null, null, null);

		assertThat(results, notNullValue());

		ArgumentCaptor<SearchParameterMap> mapCaptor = ArgumentCaptor.forClass(SearchParameterMap.class);
		verify(searchQuery).getQueryResults(mapCaptor.capture(), eq(dao), eq(translator), eq(searchQueryInclude));

		SearchParameterMap actualMap = mapCaptor.getValue();
		Object revIncludesParam = actualMap.getParameters(FhirConstants.REVERSE_INCLUDE_SEARCH_HANDLER);
		assertThat(revIncludesParam, anyOf(nullValue(), equalTo(Collections.emptyList())));
	}
}
