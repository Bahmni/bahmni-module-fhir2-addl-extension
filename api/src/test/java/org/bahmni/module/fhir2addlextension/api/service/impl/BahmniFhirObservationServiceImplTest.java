package org.bahmni.module.fhir2addlextension.api.service.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.bahmni.module.fhir2addlextension.api.context.RequestContextHolder;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniObsDao;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Obs;
import org.openmrs.module.fhir2.api.dao.FhirObservationDao;
import org.openmrs.module.fhir2.api.search.param.ObservationSearchParams;
import org.openmrs.module.fhir2.api.translators.ObservationTranslator;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirObservationServiceImplTest {
	
	private static final String ENCOUNTER_UUID = "f9df3ec8-fda0-4c8a-9957-cbcdf02de89f";
	
	private static final String SERVER_BASE = "https://localhost/openmrs/ws/fhir2/R4";
	
	@Spy
	private BahmniFhirObservationServiceImpl observationService;
	
	// Separate service instance for create() tests — uses anonymous subclass to bypass
	// validateObject() which requires an OpenMRS Spring context.
	private BahmniFhirObservationServiceImpl createTestService;
	
	private BahmniObsDao mockBahmniObsDao;
	
	private FhirObservationDao mockDao;
	
	private ObservationTranslator mockTranslator;
	
	@Before
	public void setUpCreateTestService() throws NoSuchFieldException, IllegalAccessException {
		mockBahmniObsDao = mock(BahmniObsDao.class);
		mockDao = mock(FhirObservationDao.class);
		mockTranslator = mock(ObservationTranslator.class);
		
		createTestService = new BahmniFhirObservationServiceImpl() {
			
			@Override
			protected void validateObject(Obs object) {
				// no-op — bypasses OpenMRS ValidateUtil static context requirement
			}
		};
		createTestService.setBahmniObsDao(mockBahmniObsDao);
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
	// Tests for create() — UUID handling and existing parent obs detection
	// ──────────────────────────────────────────────────────────────────────────────
	
	@Test
	public void create_shouldSetUuidFromResourceIdSoClientUuidIsStoredInDb() {
		String resourceId = "client-obs-uuid-123";
		Observation fhirObs = new Observation();
		fhirObs.setId(resourceId);
		
		Obs obsFromTranslator = new Obs();
		obsFromTranslator.setGroupMembers(new HashSet<>()); // translator always initialises groupMembers
		when(mockTranslator.toOpenmrsType(fhirObs)).thenReturn(obsFromTranslator);
		// no getDao().get() stub needed — groupMembers is empty so the existing-obs check is skipped
		when(mockDao.createOrUpdate(obsFromTranslator)).thenReturn(obsFromTranslator);
		when(mockTranslator.toFhirResource(obsFromTranslator)).thenReturn(new Observation());
		
		createTestService.create(fhirObs);
		
		assertEquals("UUID from resource.id must be set on the obs before createOrUpdate", resourceId,
		    obsFromTranslator.getUuid());
	}
	
	@Test
	public void create_shouldReturnExistingObsAndLinkNewChildrenWhenParentUuidExistsInDb() {
		String parentUuid = "existing-parent-group-obs-uuid";
		Observation fhirParentObs = new Observation();
		fhirParentObs.setId(parentUuid);

		Obs childObs = new Obs();
		Set<Obs> groupMembers = new HashSet<>();
		groupMembers.add(childObs);

		Obs translatedObs = new Obs();
		translatedObs.setGroupMembers(groupMembers);

		Obs existingParentObs = new Obs();
		existingParentObs.setUuid(parentUuid);

		Observation returnedFhirObs = new Observation();
		returnedFhirObs.setId(parentUuid);

		when(mockTranslator.toOpenmrsType(fhirParentObs)).thenReturn(translatedObs);
		when(mockDao.get((String) parentUuid)).thenReturn(existingParentObs);
		when(mockTranslator.toFhirResource(existingParentObs)).thenReturn(returnedFhirObs);

		Observation result = createTestService.create(fhirParentObs);

		assertEquals(parentUuid, result.getId());
		verify(mockBahmniObsDao).updateObsMember(existingParentObs, groupMembers);
		verify(mockDao, never()).createOrUpdate(any());
	}
	
	@Test
	public void create_shouldCreateNewObsWhenResourceUuidNotFoundInDb() {
		String newObsUuid = "brand-new-obs-uuid";
		Observation fhirObs = new Observation();
		fhirObs.setId(newObsUuid);

		Obs obsFromTranslator = new Obs();
		Set<Obs> groupMembers = new HashSet<>();
		groupMembers.add(new Obs());
		obsFromTranslator.setGroupMembers(groupMembers);

		Observation createdFhirObs = new Observation();

		when(mockTranslator.toOpenmrsType(fhirObs)).thenReturn(obsFromTranslator);
		when(mockDao.get((String) newObsUuid)).thenReturn(null);  // UUID not in DB
		when(mockDao.createOrUpdate(obsFromTranslator)).thenReturn(obsFromTranslator);
		when(mockTranslator.toFhirResource(obsFromTranslator)).thenReturn(createdFhirObs);

		Observation result = createTestService.create(fhirObs);

		assertEquals(createdFhirObs, result);
		verify(mockDao).createOrUpdate(obsFromTranslator);
		verify(mockBahmniObsDao).updateObsMember(obsFromTranslator, groupMembers);
	}
	
	@Test
	public void create_shouldCreateNewObsNormallyWhenResourceHasNoId() {
		Observation fhirObs = new Observation();
		// no id set
		
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
	
}
