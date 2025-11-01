package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirEpisodeOfCareDao;
import org.bahmni.module.fhir2AddlExtension.api.search.param.BahmniEpisodeOfCareSearchParams;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirEpisodeOfCareService;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniEpisodeOfCareTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.EpisodeOfCareStatusTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.impl.BahmniEpisodeOfCareStatusTranslatorImpl;
import org.bahmni.module.fhir2AddlExtension.api.translator.impl.BahmniEpisodeOfCareTranslatorImpl;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.EpisodeOfCare;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.api.db.ContextDAO;
import org.openmrs.module.episodes.Episode;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirEpisodeOfCareServiceImplTest {
	
	private BahmniFhirEpisodeOfCareService episodeOfCareService;
	
	@Mock
	private BahmniFhirEpisodeOfCareDao fhirEpisodeOfCareDao;
	
	@Mock
	private SearchQueryInclude<EpisodeOfCare> searchQueryInclude;
	
	@Mock
	private SearchQuery<Episode, EpisodeOfCare, BahmniFhirEpisodeOfCareDao, BahmniEpisodeOfCareTranslator, SearchQueryInclude<EpisodeOfCare>> searchQuery;
	
	@Mock
	private User user;
	
	@Mock
	private UserContext userContext;
	
	@Mock
	private ContextDAO contextDAO;
	
	@Mock
	private ConceptTranslator conceptTranslator;
	
	@Mock
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Mock
	PractitionerReferenceTranslator<Provider> providerReferenceTranslator;
	
	@Before
	public void setup() {
		when(userContext.getAuthenticatedUser()).thenReturn(user);
		Context.setDAO(contextDAO);
		Context.openSession();
		Context.setUserContext(userContext);
		BahmniEpisodeOfCareStatusTranslatorImpl statusTranslator = new BahmniEpisodeOfCareStatusTranslatorImpl();
		statusTranslator.initialize();
		
		BahmniEpisodeOfCareTranslator episodeTranslator = new BahmniEpisodeOfCareTranslatorImpl(patientReferenceTranslator,
		        conceptTranslator, providerReferenceTranslator, statusTranslator);
		
		episodeOfCareService = new TestBahmniFhirEpisodeOfCareServiceImpl(fhirEpisodeOfCareDao, episodeTranslator,
		        searchQueryInclude, statusTranslator, searchQuery);
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void shouldThrowErrorIfPatientReferenceIsNotProvided() {
		BahmniEpisodeOfCareSearchParams searchParams = new BahmniEpisodeOfCareSearchParams(new ReferenceAndListParam(), new TokenAndListParam(), null, new HashSet<>(), null);
		episodeOfCareService.episodesForPatient(searchParams);
	}
	
	@Test(expected = UnsupportedOperationException.class)
	public void shouldThrowErrorIfPatientReferenceValueIsNotProvided() {
		ReferenceAndListParam listParam = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue("").setChain(EpisodeOfCare.SP_PATIENT)));
		BahmniEpisodeOfCareSearchParams searchParams = new BahmniEpisodeOfCareSearchParams(listParam, null, null, new HashSet<>(), null);
		episodeOfCareService.episodesForPatient(searchParams);
	}
	
	@Test
	public void shouldUpdateEpisodeStatusHistory() {
		String episodeOfCareUuid = UUID.randomUUID().toString();
		String patientUuid = UUID.randomUUID().toString();
		Concept openmrsConcept = exampleConcept();
		
		EpisodeOfCare episodeOfCare = new EpisodeOfCare();
		episodeOfCare.setId(episodeOfCareUuid);
		episodeOfCare.setPatient(patientReference(patientUuid));
		episodeOfCare.setPeriod(exampleEpisodePeriod());
		episodeOfCare.setStatus(EpisodeOfCare.EpisodeOfCareStatus.ACTIVE);
		CodeableConcept episodeTypeCodeableConcept = getCodeableConceptForConcept(openmrsConcept);
		episodeOfCare.setType(Collections.singletonList(episodeTypeCodeableConcept));
		when(conceptTranslator.toOpenmrsType(episodeTypeCodeableConcept)).thenReturn(openmrsConcept);
		
		Concept concept = exampleConcept();
		Patient patient = new Patient();
		patient.setUuid(UUID.randomUUID().toString());
		Episode episode = prepareTestEpisode(UUID.randomUUID().toString(), concept, patient);
		
		when(fhirEpisodeOfCareDao.get(episodeOfCareUuid)).thenReturn(episode);
		when(fhirEpisodeOfCareDao.createOrUpdate(episode)).thenReturn(episode);
		EpisodeOfCare updatedEOC = episodeOfCareService.update(episodeOfCareUuid, episodeOfCare);
		assertThat(1, equalTo(updatedEOC.getStatusHistory().size()));
	}
	
	private Concept exampleConcept() {
		Concept concept = new Concept();
		concept.setUuid(UUID.randomUUID().toString());
		return concept;
	}
	
	private Period exampleEpisodePeriod() {
		Period period = new Period();
		period.setStart(new Date());
		return period;
	}
	
	private Reference patientReference(String patientUuid) {
		Reference reference = new Reference();
		reference.setType("Patient");
		reference.setReference(patientUuid);
		return reference;
	}
	
	private CodeableConcept getCodeableConceptForConcept(Concept concept) {
		CodeableConcept codeableConcept = new CodeableConcept();
		Coding coding = codeableConcept.addCoding();
		coding.setCode(concept.getUuid());
		return codeableConcept;
	}
	
	private Episode prepareTestEpisode(String uuid, Concept concept, Patient patient) {
		Episode episode = new Episode();
		episode.setUuid(uuid);
		episode.setPatient(patient);
		episode.setDateStarted(new Date());
		episode.setConcept(concept);
		return episode;
	}
	
	/**
	 * Done to avoid mocking of the AdministrativeService called during validateObject TODO: write
	 * integration test?
	 */
	private static class TestBahmniFhirEpisodeOfCareServiceImpl extends BahmniFhirEpisodeOfCareServiceImpl {
		
		public TestBahmniFhirEpisodeOfCareServiceImpl(
		    BahmniFhirEpisodeOfCareDao fhirEpisodeOfCareDao,
		    BahmniEpisodeOfCareTranslator episodeTranslator,
		    SearchQueryInclude<EpisodeOfCare> searchQueryInclude,
		    EpisodeOfCareStatusTranslator statusTranslator,
		    SearchQuery<Episode, EpisodeOfCare, BahmniFhirEpisodeOfCareDao, BahmniEpisodeOfCareTranslator, SearchQueryInclude<EpisodeOfCare>> searchQuery) {
			super(fhirEpisodeOfCareDao, episodeTranslator, searchQueryInclude, statusTranslator, searchQuery);
		}
		
		@Override
		protected void validateObject(Episode object) {
			//do nothing
		}
	}
	
}
