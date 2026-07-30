package org.bahmni.module.fhir2addlextension.api.service.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.bahmni.module.fhir2addlextension.api.context.RequestContextHolder;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniObsDao;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniObservationSearchParams;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Obs;
import org.openmrs.api.ObsService;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.dao.FhirObservationDao;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.search.param.ObservationSearchParams;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.translators.ObservationTranslator;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirObservationServiceImplTest {
	
	private static final String ENCOUNTER_UUID = "f9df3ec8-fda0-4c8a-9957-cbcdf02de89f";
	
	private static final String PATIENT_UUID = "patient-uuid-123";
	
	private static final String SERVICE_REQUEST_UUID = "service-request-uuid-456";
	
	private static final String SERVER_BASE = "https://localhost/openmrs/ws/fhir2/R4";
	
	@Mock
	private BahmniObsDao bahmniObsDao;
	
	@Mock
	private SearchQueryInclude<Observation> searchQueryInclude;
	
	@Mock
	private SearchQuery<Obs, Observation, FhirDao<Obs>, OpenmrsFhirTranslator<Obs, Observation>, SearchQueryInclude<Observation>> searchQuery;
	
	private BahmniFhirObservationServiceImpl observationService;
	
	@Mock
	private OpenmrsFhirTranslator<Obs, Observation> translator;
	
	// Separate service instance for create() tests — uses anonymous subclass to bypass
	// validateObject() which requires an OpenMRS Spring context.
	private BahmniFhirObservationServiceImpl createTestService;
	
	private BahmniObsDao mockBahmniObsDao;
	
	private FhirObservationDao mockDao;
	
	private ObservationTranslator mockTranslator;
	
	@Before
	public void setUp() {
		observationService = org.mockito.Mockito.spy(new BahmniFhirObservationServiceImpl(bahmniObsDao, searchQueryInclude,
		        searchQuery));
	}
	
	@Before
	public void setUpCreateTestService() throws NoSuchFieldException, IllegalAccessException {
		mockBahmniObsDao = mock(BahmniObsDao.class);
		mockDao = mock(FhirObservationDao.class);
		mockTranslator = mock(ObservationTranslator.class);
		
		createTestService = new BahmniFhirObservationServiceImpl(
		                                                         mockBahmniObsDao, searchQueryInclude, searchQuery) {
			
			@Override
			protected void validateObject(Obs object) {
				// no-op — bypasses OpenMRS ValidateUtil static context requirement
			}
		};
		setFieldOnSuperClass(createTestService, "dao", mockDao);
		setFieldOnSuperClass(createTestService, "translator", mockTranslator);
	}
	
	@After
	public void tearDown() {
		RequestContextHolder.clear();
	}
	
	private void setFieldOnSuperClass(BahmniFhirObservationServiceImpl service, String fieldName, Object value)
	        throws NoSuchFieldException, IllegalAccessException {
		// Anonymous subclass → BahmniFhirObservationServiceImpl → FhirObservationServiceImpl
		// dao and translator fields are declared in FhirObservationServiceImpl
		Class<?> clazz = service.getClass().getSuperclass().getSuperclass();
		Field field = clazz.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(service, value);
	}
	
	@Test
	public void fetchAllByEncounter_shouldReturnBundleWithAllObservations() {
		Observation obs1 = new Observation();
		obs1.setId("obs-uuid-1");
		Observation obs2 = new Observation();
		obs2.setId("obs-uuid-2");
		Observation obs3 = new Observation();
		obs3.setId("obs-uuid-3");
		List<IBaseResource> observations = Arrays.asList(obs1, obs2, obs3);
		
		ReferenceAndListParam encounterReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam(ENCOUNTER_UUID)));
		
		IBundleProvider bundleProvider = mock(IBundleProvider.class);
		doReturn(bundleProvider).when(observationService).searchForObservations(any(ObservationSearchParams.class));
		when(bundleProvider.getResources(0, Integer.MAX_VALUE)).thenReturn(observations);
		
		RequestContextHolder.setValue(SERVER_BASE);
		
		Bundle result = observationService.fetchAllByEncounter(encounterReference);
		
		assertNotNull(result);
		assertEquals(Bundle.BundleType.SEARCHSET, result.getType());
		assertEquals(3, result.getTotal());
		assertEquals(3, result.getEntry().size());
		assertEquals(SERVER_BASE + "/Observation/obs-uuid-1", result.getEntry().get(0).getFullUrl());
		assertEquals(SERVER_BASE + "/Observation/obs-uuid-2", result.getEntry().get(1).getFullUrl());
		assertEquals(SERVER_BASE + "/Observation/obs-uuid-3", result.getEntry().get(2).getFullUrl());
	}
	
	@Test
	public void fetchAllByEncounter_shouldReturnEmptyBundleWhenNoObservationsFound() {
		ReferenceAndListParam encounterReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam(ENCOUNTER_UUID)));
		
		IBundleProvider bundleProvider = mock(IBundleProvider.class);
		doReturn(bundleProvider).when(observationService).searchForObservations(any(ObservationSearchParams.class));
		when(bundleProvider.getResources(0, Integer.MAX_VALUE)).thenReturn(Collections.emptyList());
		
		RequestContextHolder.setValue(SERVER_BASE);
		
		Bundle result = observationService.fetchAllByEncounter(encounterReference);
		
		assertNotNull(result);
		assertEquals(Bundle.BundleType.SEARCHSET, result.getType());
		assertEquals(0, result.getTotal());
		assertEquals(0, result.getEntry().size());
	}
	
	// ──────────────────────────────────────────────────────────────────────────────
	// Tests for create() — brand-new observations only. Editing an existing
	// obsGroup no longer routes through create() (it uses a real PUT, handled by
	// applyUpdate() below), so create() no longer reads/honours a client-supplied
	// resource id at all.
	// ──────────────────────────────────────────────────────────────────────────────
	
	@Test
	public void create_shouldCreateNewLeafObsAndNotTouchDaoGet() {
		Observation fhirObs = new Observation();
		// no id set — create() always treats the incoming resource as brand new

		Obs obsFromTranslator = new Obs();
		obsFromTranslator.setGroupMembers(new HashSet<>()); // translator always initialises groupMembers
		Observation createdFhirObs = new Observation();

		when(mockTranslator.toOpenmrsType(fhirObs)).thenReturn(obsFromTranslator);
		when(mockDao.createOrUpdate(obsFromTranslator)).thenReturn(obsFromTranslator);
		when(mockTranslator.toFhirResource(obsFromTranslator)).thenReturn(createdFhirObs);

		Observation result = createTestService.create(fhirObs);

		assertEquals(createdFhirObs, result);
		verify(mockDao).createOrUpdate(obsFromTranslator);
		verify(mockDao, never()).get(any(String.class));
	}
	
	@Test
	public void create_shouldCreateNewGroupAndLinkItsMembers() {
		Observation fhirObs = new Observation();

		Obs obsFromTranslator = new Obs();
		Set<Obs> groupMembers = new HashSet<>();
		groupMembers.add(new Obs());
		obsFromTranslator.setGroupMembers(groupMembers);

		Observation createdFhirObs = new Observation();

		when(mockTranslator.toOpenmrsType(fhirObs)).thenReturn(obsFromTranslator);
		when(mockDao.createOrUpdate(obsFromTranslator)).thenReturn(obsFromTranslator);
		when(mockTranslator.toFhirResource(obsFromTranslator)).thenReturn(createdFhirObs);

		Observation result = createTestService.create(fhirObs);

		assertEquals(createdFhirObs, result);
		verify(mockDao).createOrUpdate(obsFromTranslator);
		verify(mockBahmniObsDao).updateObsMember(obsFromTranslator, groupMembers);
		verify(mockDao, never()).get(any(String.class));
	}
	
	// ──────────────────────────────────────────────────────────────────────────────
	// Tests for applyUpdate() — PUT should behave the same as the POST-with-id
	// workaround for obsGroup parents, instead of falling through to core's
	// default (non-additive) update behaviour.
	// ──────────────────────────────────────────────────────────────────────────────
	
	@Test
	public void applyUpdate_shouldReuseExistingParentAndLinkChildrenWhenExistingObsIsAGroup() {
		Obs existingParentObs = new Obs();
		existingParentObs.setUuid("existing-parent-group-obs-uuid");
		// Marks this Obs as an obsGroup in the DB — this, not the incoming
		// resource's hasMember, is what applyUpdate() branches on.
		Set<Obs> existingGroupMembers = new HashSet<>();
		existingGroupMembers.add(new Obs());
		existingParentObs.setGroupMembers(existingGroupMembers);

		Observation incomingFhirObs = new Observation();
		incomingFhirObs.setId("existing-parent-group-obs-uuid");

		Obs childObs = new Obs();
		Set<Obs> incomingGroupMembers = new HashSet<>();
		incomingGroupMembers.add(childObs);

		Obs translatedObs = new Obs();
		translatedObs.setGroupMembers(incomingGroupMembers);

		Observation returnedFhirObs = new Observation();
		returnedFhirObs.setId("existing-parent-group-obs-uuid");

		when(mockTranslator.toOpenmrsType(incomingFhirObs)).thenReturn(translatedObs);
		when(mockTranslator.toFhirResource(existingParentObs)).thenReturn(returnedFhirObs);

		Observation result = createTestService.applyUpdate(existingParentObs, incomingFhirObs);

		assertEquals(returnedFhirObs, result);
		verify(mockBahmniObsDao).updateObsMember(existingParentObs, incomingGroupMembers);
		// Must NOT go through the default create/replace path — the existing
		// parent obs is kept as-is, only its children are re-linked.
		verify(mockDao, never()).createOrUpdate(any());
	}
	
	@Test
	public void applyUpdate_shouldStillTakeSafePathWhenExistingGroupHasNoNewOrChangedMembersToLink() {
		// Regression: if every remaining member of an existing group is
		// unchanged, the frontend legitimately sends an EMPTY hasMember
		// (see observationResourceCreator.ts). Branching on the EXISTING
		// obs's group-ness (not on whether the incoming resource's
		// hasMember happens to be non-empty) means this still takes the
		// safe, no-op updateObsMember path instead of falling through to
		// core's default update — which would try to persist the group
		// parent as a valueless, member-less Obs and fail OpenMRS's
		// ObsValidator with "error.noValue".
		Obs existingParentObs = new Obs();
		existingParentObs.setUuid("existing-parent-group-obs-uuid");
		Set<Obs> existingGroupMembers = new HashSet<>();
		existingGroupMembers.add(new Obs());
		existingParentObs.setGroupMembers(existingGroupMembers);

		Observation incomingFhirObs = new Observation();
		incomingFhirObs.setId("existing-parent-group-obs-uuid");

		Obs translatedObs = new Obs();
		translatedObs.setGroupMembers(new HashSet<>()); // no new/changed members this save

		Observation returnedFhirObs = new Observation();
		returnedFhirObs.setId("existing-parent-group-obs-uuid");

		when(mockTranslator.toOpenmrsType(incomingFhirObs)).thenReturn(translatedObs);
		when(mockTranslator.toFhirResource(existingParentObs)).thenReturn(returnedFhirObs);

		Observation result = createTestService.applyUpdate(existingParentObs, incomingFhirObs);

		assertEquals(returnedFhirObs, result);
		verify(mockBahmniObsDao).updateObsMember(existingParentObs, new HashSet<>());
		verify(mockDao, never()).createOrUpdate(any());
	}
	
	@Test
	public void applyUpdate_shouldDelegateToCoreUpdateWhenExistingObsIsAPlainLeaf() {
		Obs existingLeafObs = new Obs();
		existingLeafObs.setUuid("existing-leaf-obs-uuid");
		// No group members set — isObsGrouping() is false, so this must fall
		// through to the core (non-additive) update path instead of the
		// obsGroup re-linking shortcut above.

		Observation incomingFhirObs = new Observation();
		incomingFhirObs.setId("existing-leaf-obs-uuid");

		Obs translatedObs = new Obs();
		Obs savedObs = new Obs();
		Observation returnedFhirObs = new Observation();
		returnedFhirObs.setId("existing-leaf-obs-uuid");

		// ObservationTranslator is an UpdatableOpenmrsTranslator, so core applyUpdate()
		// calls the two-arg overload rather than the plain toOpenmrsType(resource).
		when(mockTranslator.toOpenmrsType(existingLeafObs, incomingFhirObs)).thenReturn(translatedObs);
		when(mockTranslator.toFhirResource(savedObs)).thenReturn(returnedFhirObs);

		ObsService mockObsService = mock(ObsService.class);
		when(mockObsService.saveObs(translatedObs, "Updated via the FHIR2 API")).thenReturn(savedObs);

		try (MockedStatic<Context> mockedContext = org.mockito.Mockito.mockStatic(Context.class)) {
			mockedContext.when(Context::getObsService).thenReturn(mockObsService);

			Observation result = createTestService.applyUpdate(existingLeafObs, incomingFhirObs);

			assertEquals(returnedFhirObs, result);
		}
		verify(mockBahmniObsDao, never()).updateObsMember(any(), any());
	}
	
	// ──────────────────────────────────────────────────────────────────────────────
	
	@Test
	public void fetchAllByEncounter_shouldPopulateBundleMetadata() {
		Observation obs = new Observation();
		obs.setId("obs-uuid-1");
		List<IBaseResource> observations = Arrays.asList(obs);
		
		ReferenceAndListParam encounterReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam(ENCOUNTER_UUID)));
		
		IBundleProvider bundleProvider = mock(IBundleProvider.class);
		doReturn(bundleProvider).when(observationService).searchForObservations(any(ObservationSearchParams.class));
		when(bundleProvider.getResources(0, Integer.MAX_VALUE)).thenReturn(observations);
		
		RequestContextHolder.setValue(SERVER_BASE);
		
		Bundle result = observationService.fetchAllByEncounter(encounterReference);
		
		assertNotNull(result);
		assertNotNull(result.getId());
		assertNotNull(result.getMeta());
		assertNotNull(result.getMeta().getLastUpdated());
	}
	
	@Test
	public void searchObservations_shouldReturnResultsWhenBasedOnReferenceProvided() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam(SERVICE_REQUEST_UUID)));
		
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(null, basedOnReference, null, null);
		
		IBundleProvider expectedResults = mock(IBundleProvider.class);
		when(searchQuery.getQueryResults(any(SearchParameterMap.class), eq(bahmniObsDao), any(), eq(searchQueryInclude)))
		        .thenReturn(expectedResults);
		
		IBundleProvider result = observationService.searchObservations(searchParams);
		
		assertNotNull(result);
		assertEquals(expectedResults, result);
		verify(searchQuery).getQueryResults(any(SearchParameterMap.class), eq(bahmniObsDao), any(), eq(searchQueryInclude));
	}
	
	@Test
	public void searchObservations_shouldReturnResultsWhenPatientReferenceProvided() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(PATIENT_UUID)));
		
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(patientReference, null, null, null);
		
		IBundleProvider expectedResults = mock(IBundleProvider.class);
		when(searchQuery.getQueryResults(any(SearchParameterMap.class), eq(bahmniObsDao), any(), eq(searchQueryInclude)))
		        .thenReturn(expectedResults);
		
		IBundleProvider result = observationService.searchObservations(searchParams);
		
		assertNotNull(result);
		assertEquals(expectedResults, result);
	}
	
	@Test
	public void searchObservations_shouldReturnResultsWhenBothPatientAndBasedOnProvided() {
		ReferenceAndListParam patientReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(PATIENT_UUID)));
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam(SERVICE_REQUEST_UUID)));
		
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(patientReference, basedOnReference,
		        null, null);
		
		IBundleProvider expectedResults = mock(IBundleProvider.class);
		when(searchQuery.getQueryResults(any(SearchParameterMap.class), eq(bahmniObsDao), any(), eq(searchQueryInclude)))
		        .thenReturn(expectedResults);
		
		IBundleProvider result = observationService.searchObservations(searchParams);
		
		assertNotNull(result);
		assertEquals(expectedResults, result);
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void searchObservations_shouldThrowExceptionWhenNoSearchParametersProvided() {
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(null, null, null, null);
		
		observationService.searchObservations(searchParams);
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void searchObservations_shouldThrowExceptionWhenOnlyEmptyParametersProvided() {
		ReferenceAndListParam emptyPatient = new ReferenceAndListParam();
		ReferenceAndListParam emptyBasedOn = new ReferenceAndListParam();
		
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(emptyPatient, emptyBasedOn, null,
		        null);
		
		observationService.searchObservations(searchParams);
	}
	
}
