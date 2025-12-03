package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirEpisodeOfCareDao;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Encounter;
import org.openmrs.User;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.api.db.ContextDAO;
import org.openmrs.module.episodes.Episode;
import org.openmrs.module.fhir2.api.FhirVisitService;
import org.openmrs.module.fhir2.api.dao.FhirEncounterDao;
import org.openmrs.module.fhir2.api.dao.FhirVisitDao;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.translators.EncounterTranslator;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.bahmni.module.fhir2AddlExtension.api.TestDataFactory.loadResourceFromFile;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirEncounterServiceImplTest {
	
	@Mock
	private ContextDAO contextDAO;
	
	@Mock
	private UserContext userContext;
	
	@Mock
	private User user;
	
	@Mock
	private FhirEncounterDao dao;
	
	@Mock
	EncounterTranslator<Encounter> translator;
	
	@Mock
	SearchQueryInclude<org.hl7.fhir.r4.model.Encounter> searchQueryInclude;
	
	@Mock
	private SearchQuery<Encounter, org.hl7.fhir.r4.model.Encounter, FhirEncounterDao, EncounterTranslator<Encounter>, SearchQueryInclude<org.hl7.fhir.r4.model.Encounter>> searchQuery;
	
	@Mock
	private FhirVisitService visitService;
	
	@Mock
	private FhirVisitDao visitDao;
	
	@Mock
	private BahmniFhirEpisodeOfCareDao episodeOfCareDao;
	
	@Mock
	private AdministrationService administrationService;
	
	private BahmniFhirEncounterServiceImpl encounterService;
	
	@Before
	public void setUp() throws NoSuchFieldException, IllegalAccessException {
		//		when(userContext.getAuthenticatedUser()).thenReturn(user);
		//		Context.setDAO(contextDAO);
		//		Context.openSession();
		//		Context.setUserContext(userContext);
		//requires a different mock maker. not introducing additional lib as of now
		//mockStatic(Context.class).when(Context::getAdministrationService).thenReturn(administrationService);
		
		encounterService = new BahmniFhirEncounterServiceImpl() {
			
			@Override
			protected void validateObject(Encounter object) {
				//Done so that static context service methods calls are escaped
			}
		};
		encounterService.setVisitDao(visitDao);
		encounterService.setEpisodeOfCareDao(episodeOfCareDao);
		setPropertyOnSuperClass(encounterService, "dao", dao);
		setPropertyOnSuperClass(encounterService, "translator", translator);
		setPropertyOnSuperClass(encounterService, "searchQueryInclude", searchQueryInclude);
		setPropertyOnSuperClass(encounterService, "searchQuery", searchQuery);
		setPropertyOnSuperClass(encounterService, "visitService", visitService);
		
	}
	
	@Test
	public void shouldAssociateEncounterIfEpisodeReferenceIsMentioned() throws IOException {
		String existingEpisodeUuid = "12b8dec4-28f4-4b17-ac62-099510a35f35";
		Episode episode = new Episode();
		episode.setUuid(existingEpisodeUuid);
		org.hl7.fhir.r4.model.Encounter fhirEncounter = (org.hl7.fhir.r4.model.Encounter) loadResourceFromFile("example-encounter-resource.json");
		when(episodeOfCareDao.get(existingEpisodeUuid)).thenReturn(episode);
		Encounter emrEncounter = new Encounter();
		when(dao.createOrUpdate(emrEncounter)).thenReturn(emrEncounter);
		when(dao.get(fhirEncounter.getId())).thenReturn(emrEncounter);
		when(translator.toOpenmrsType(fhirEncounter)).thenReturn(emrEncounter);
		when(translator.toFhirResource(emrEncounter)).thenReturn(fhirEncounter);
		org.hl7.fhir.r4.model.Encounter encounter = encounterService.create(fhirEncounter);
		verify(episodeOfCareDao).createOrUpdate(any(Episode.class));
	}
	
	@Test(expected = InvalidRequestException.class)
	public void shouldErrorOutForInvalidEpisodeReference() throws IOException {
		org.hl7.fhir.r4.model.Encounter fhirEncounter = (org.hl7.fhir.r4.model.Encounter) loadResourceFromFile("example-encounter-resource.json");
		Encounter emrEncounter = new Encounter();
		when(dao.createOrUpdate(emrEncounter)).thenReturn(emrEncounter);
		when(translator.toOpenmrsType(fhirEncounter)).thenReturn(emrEncounter);
		when(translator.toFhirResource(emrEncounter)).thenReturn(fhirEncounter);
		encounterService.create(fhirEncounter);
	}
	
	private void setPropertyOnSuperClass(BahmniFhirEncounterServiceImpl service, String attributeName, Object value)
	        throws NoSuchFieldException, IllegalAccessException {
		//TBD: unfortunately, the way the fhir2 EncounterServiceImpl beans are declared (package private)
		//additional level of getSuperClass() due to overrding of validateObject in the setup method
		Class<?> clazz = service.getClass().getSuperclass().getSuperclass();
		Field field = clazz.getDeclaredField(attributeName);
		field.setAccessible(true);
		field.set(service, value);
	}
}
