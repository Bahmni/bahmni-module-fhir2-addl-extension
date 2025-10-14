package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniEpisodeOfCareTranslator;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.EpisodeOfCare;
import org.hl7.fhir.r4.model.Extension;
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
import org.openmrs.module.episodes.EpisodeReason;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniEpisodeOfCareTranslatorImplTest {
	
	private BahmniEpisodeOfCareTranslator episodeOfCareTranslator;
	
	@Mock
	private ConceptTranslator conceptTranslator;
	
	@Mock
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Mock
	PractitionerReferenceTranslator<Provider> providerReferenceTranslator;
	
	@Mock
	private ContextDAO contextDAO;
	
	@Mock
	private UserContext userContext;
	
	@Mock
	private User user;
	
	@Before
	public void setup() {
		// Create a sample ConceptSource
		when(userContext.getAuthenticatedUser()).thenReturn(user);
		Context.setDAO(contextDAO);
		Context.openSession();
		Context.setUserContext(userContext);
		episodeOfCareTranslator = new BahmniEpisodeOfCareTranslatorImpl(patientReferenceTranslator, conceptTranslator,
		        providerReferenceTranslator);
	}
	
	@Test
	public void toFhirResource_shouldTranslateTypeForEpisodeOfCare() {
		String uuid = UUID.randomUUID().toString();
		Concept concept = exampleConcept();
		CodeableConcept episodeType = getCodeableConceptForConcept(concept);
		
		Patient patient = exampleEpisodePatient();
		Episode episode = prepareTestEpisode(uuid, concept, patient);
		when(conceptTranslator.toFhirResource(concept)).thenReturn(episodeType);
		
		EpisodeOfCare episodeOfCare = episodeOfCareTranslator.toFhirResource(episode);
		assertThat(episodeOfCare, notNullValue());
		assertThat(episodeOfCare.getId(), notNullValue());
		assertThat(episodeOfCare.getType().get(0).getCoding().get(0).getCode(), equalTo(concept.getUuid()));
	}
	
	@Test
    public void toFhirResource_shouldTranslateEpisodeReasonExtension() {
        String uuid = UUID.randomUUID().toString();
        Concept concept = exampleConcept();
        CodeableConcept episodeType = getCodeableConceptForConcept(concept);

        Patient patient = exampleEpisodePatient();
        Episode episode = prepareTestEpisode(uuid, concept, patient);
        Concept reason = exampleConcept();
        setupEpisodeReason(episode, reason);

        when(conceptTranslator.toFhirResource(concept)).thenReturn(episodeType);
        when(conceptTranslator.toFhirResource(reason)).thenReturn(getCodeableConceptForConcept(reason));

        EpisodeOfCare episodeOfCare = episodeOfCareTranslator.toFhirResource(episode);
        assertThat(episodeOfCare, notNullValue());
        assertThat(episodeOfCare.getId(), notNullValue());


        List<CodeableConcept> episodeReasonCodeableConcepts = episodeOfCare
                .getExtensionsByUrl(BahmniFhirConstants.FHIR_EXT_EPISODE_OF_CARE_REASON)
                .stream().map(extension -> extension.getExtensionsByUrl("value"))
                .flatMap(List::stream)
                .map(extension -> (CodeableConcept) extension.getValue())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        assertThat(1, equalTo(episodeReasonCodeableConcepts.size()));
        assertThat(reason.getUuid(), equalTo(episodeReasonCodeableConcepts.get(0).getCoding().get(0).getCode()));
    }
	
	@Test
	public void toOpenmrsType_shouldTranslateEpisodeType() {
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
		
		Episode openmrsEpisode = episodeOfCareTranslator.toOpenmrsType(episodeOfCare);
		assertThat(episodeOfCare.getId(), equalTo(openmrsEpisode.getUuid()));
		assertThat(episodeOfCare.getId(), equalTo(openmrsEpisode.getUuid()));
		assertThat(Episode.Status.ACTIVE, equalTo(openmrsEpisode.getStatus()));
		assertThat(episodeOfCare.getPeriod().getStart(), equalTo(openmrsEpisode.getDateStarted()));
		assertThat(episodeOfCare.getType().get(0).getCoding().get(0).getCode(), equalTo(openmrsEpisode.getConcept()
		        .getUuid()));
	}
	
	@Test
	public void toOpenmrsType_shouldTranslateEpisodeReasonExtensions() {
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
		
		Concept openmrsEpisodeReasonConcept1 = exampleConcept();
		CodeableConcept episodeReasonCodeableConcept1 = getCodeableConceptForConcept(openmrsEpisodeReasonConcept1);
		Extension episodeReasonExtension1 = episodeOfCare.addExtension();
		episodeReasonExtension1.setUrl(BahmniFhirConstants.FHIR_EXT_EPISODE_OF_CARE_REASON);
		episodeReasonExtension1.addExtension("value", episodeReasonCodeableConcept1);
		when(conceptTranslator.toOpenmrsType(episodeReasonCodeableConcept1)).thenReturn(openmrsEpisodeReasonConcept1);
		
		Episode openmrsEpisode = episodeOfCareTranslator.toOpenmrsType(episodeOfCare);
		assertThat(episodeOfCare.getId(), equalTo(openmrsEpisode.getUuid()));
		assertThat(episodeOfCare.getId(), equalTo(openmrsEpisode.getUuid()));
		assertThat(Episode.Status.ACTIVE, equalTo(openmrsEpisode.getStatus()));
		assertThat(episodeOfCare.getPeriod().getStart(), equalTo(openmrsEpisode.getDateStarted()));
		assertThat(episodeOfCare.getType().get(0).getCoding().get(0).getCode(), equalTo(openmrsEpisode.getConcept()
		        .getUuid()));
		
		Set<EpisodeReason> episodeReasons = openmrsEpisode.getEpisodeReason();
		assertThat(1, equalTo(episodeReasons.size()));
		assertThat(openmrsEpisodeReasonConcept1, equalTo(episodeReasons.iterator().next().getReason()));
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
	
	private void setupEpisodeReason(Episode episode, Concept reason) {
		EpisodeReason episodeReason = new EpisodeReason();
		episodeReason.setReason(reason);
		episodeReason.setEpisode(episode);
		episode.setEpisodeReason(Collections.singleton(episodeReason));
	}
	
	private CodeableConcept getCodeableConceptForConcept(Concept concept) {
		CodeableConcept codeableConcept = new CodeableConcept();
		Coding coding = codeableConcept.addCoding();
		coding.setCode(concept.getUuid());
		return codeableConcept;
	}
	
	private Patient exampleEpisodePatient() {
		Patient patient = new Patient();
		patient.setUuid(UUID.randomUUID().toString());
		return patient;
	}
	
	private Concept exampleConcept() {
		Concept concept = new Concept();
		concept.setUuid(UUID.randomUUID().toString());
		return concept;
	}
	
	private Episode prepareTestEpisode(String uuid, Concept concept, Patient patient) {
		Episode episode = new Episode();
		episode.setUuid(uuid);
		episode.setPatient(patient);
		episode.setDateStarted(new Date());
		episode.setConcept(concept);
		return episode;
	}
	
}
