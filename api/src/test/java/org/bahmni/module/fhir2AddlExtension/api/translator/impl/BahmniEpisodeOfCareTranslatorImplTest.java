package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniEpisodeOfCareTranslator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Element;
import org.hl7.fhir.r4.model.EpisodeOfCare;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
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

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
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
	
	public static IParser r4ResourceParser = FhirContext.forR4().newJsonParser();
	
	@Before
	public void setup() {
		// Create a sample ConceptSource
		BahmniEpisodeOfCareStatusTranslatorImpl statusTranslator = new BahmniEpisodeOfCareStatusTranslatorImpl();
		statusTranslator.initialize();
		when(userContext.getAuthenticatedUser()).thenReturn(user);
		Context.setDAO(contextDAO);
		Context.openSession();
		Context.setUserContext(userContext);
		episodeOfCareTranslator = new BahmniEpisodeOfCareTranslatorImpl(patientReferenceTranslator, conceptTranslator,
		        providerReferenceTranslator, statusTranslator);
	}
	
	@Test
	public void toFhirResource_shouldTranslateTypeForEpisodeOfCare() {
		String uuid = UUID.randomUUID().toString();
		Concept episodeType = exampleConcept("Home and Community Care");
		CodeableConcept episodeTypeCodeableConcept = getCodeableConceptForConcept(episodeType);
		
		Patient patient = exampleEpisodePatient();
		Episode episode = prepareTestEpisode(uuid, episodeType, patient);
		when(conceptTranslator.toFhirResource(episodeType)).thenReturn(episodeTypeCodeableConcept);
		
		EpisodeOfCare episodeOfCare = episodeOfCareTranslator.toFhirResource(episode);
		assertThat(episodeOfCare, notNullValue());
		assertThat(episodeOfCare.getId(), notNullValue());
		assertThat(episodeOfCare.getType().get(0).getCoding().get(0).getCode(), equalTo(episodeType.getUuid()));
	}
	
	@Test
    public void toFhirResource_shouldTranslateToEpisodeReasonExtension() {
        String uuid = UUID.randomUUID().toString();
        Concept episodeType = exampleConcept("Home and Community Care");

		Patient patient = exampleEpisodePatient();
        Episode episode = prepareTestEpisode(uuid, episodeType, patient);
        Concept reasonUseConcept = exampleConcept("Health Screening");
		Concept reasonValueConcept = exampleConcept("Covid 19");

		setupEpisodeReason(episode, reasonUseConcept, Collections.singletonList(new ReasonValuePair(reasonValueConcept, null)));

        when(conceptTranslator.toFhirResource(episodeType)).thenReturn(getCodeableConceptForConcept(episodeType));
        when(conceptTranslator.toFhirResource(reasonUseConcept)).thenReturn(getCodeableConceptForConcept(reasonUseConcept));
		CodeableConcept reasonValue = getCodeableConceptForConcept(reasonValueConcept);
		when(conceptTranslator.toFhirResource(reasonValueConcept)).thenReturn(reasonValue);

        EpisodeOfCare episodeOfCare = episodeOfCareTranslator.toFhirResource(episode);
		//prettyPrint(episodeOfCare);
		assertThat(episodeOfCare, notNullValue());
        assertThat(episodeOfCare.getId(), notNullValue());


        List<CodeableConcept> episodeReasonCodeableConcepts = episodeOfCare
                .getExtensionsByUrl(BahmniFhirConstants.FHIR_EXT_EPISODE_OF_CARE_REASON)
                .stream().map(extension -> extension.getExtensionsByUrl("use"))
                .flatMap(List::stream)
                .map(extension -> (CodeableConcept) extension.getValue())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        assertThat(1, equalTo(episodeReasonCodeableConcepts.size()));
        assertThat(reasonUseConcept.getUuid(), equalTo(episodeReasonCodeableConcepts.get(0).getCoding().get(0).getCode()));


		List<CodeableConcept> episodeReasonvalues = episodeOfCare
				.getExtensionsByUrl(BahmniFhirConstants.FHIR_EXT_EPISODE_OF_CARE_REASON)
				.stream().map(extension -> extension.getExtensionsByUrl("value"))
				.flatMap(List::stream)
				.map(Element::getExtension)
				.flatMap((List::stream))
				.filter(extension -> extension.getUrl().equals("concept"))
				.map(extension -> (CodeableConcept) extension.getValue())
				.collect(Collectors.toList());

		assertThat(1, equalTo(episodeReasonvalues.size()));
		assertThat(reasonValue, equalTo(episodeReasonvalues.get(0)));
    }
	
	@Test
	public void toFhirResource_shouldTranslateToMultipleEpisodeReasonExtension() {
		String uuid = UUID.randomUUID().toString();
		Concept episodeType = exampleConcept("Home and Community Care");

		Patient patient = exampleEpisodePatient();
		Episode episode = prepareTestEpisode(uuid, episodeType, patient);
		Concept reasonUseConcept = exampleConcept("Health Screening");
		Concept reasonValue1 = exampleConcept("Covid 19");
		Concept reasonValue2 = exampleConcept("H1N1");
		setupEpisodeReason(episode, reasonUseConcept, Arrays.asList(new ReasonValuePair(reasonValue1, "HealthCareService/1"), new ReasonValuePair(reasonValue2, "")));

		when(conceptTranslator.toFhirResource(episodeType)).thenReturn(getCodeableConceptForConcept(episodeType));
		when(conceptTranslator.toFhirResource(reasonUseConcept)).thenReturn(getCodeableConceptForConcept(reasonUseConcept));
		CodeableConcept valueConcept1 = getCodeableConceptForConcept(reasonValue1);
		when(conceptTranslator.toFhirResource(reasonValue1)).thenReturn(valueConcept1);

		CodeableConcept valueConcept2 = getCodeableConceptForConcept(reasonValue2);
		when(conceptTranslator.toFhirResource(reasonValue2)).thenReturn(valueConcept2);

		EpisodeOfCare episodeOfCare = episodeOfCareTranslator.toFhirResource(episode);
		//prettyPrint(episodeOfCare);
		assertThat(episodeOfCare, notNullValue());
		assertThat(episodeOfCare.getId(), notNullValue());


		List<CodeableConcept> episodeReasonCodeableConcepts = episodeOfCare
				.getExtensionsByUrl(BahmniFhirConstants.FHIR_EXT_EPISODE_OF_CARE_REASON)
				.stream().map(extension -> extension.getExtensionsByUrl("use"))
				.flatMap(List::stream)
				.map(extension -> (CodeableConcept) extension.getValue())
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		assertThat(2, equalTo(episodeReasonCodeableConcepts.size()));
		assertThat(reasonUseConcept.getUuid(), equalTo(episodeReasonCodeableConcepts.get(0).getCoding().get(0).getCode()));
		assertThat(reasonUseConcept.getUuid(), equalTo(episodeReasonCodeableConcepts.get(1).getCoding().get(0).getCode()));


		List<CodeableConcept> reasonValueConceptList = episodeOfCare
				.getExtensionsByUrl(BahmniFhirConstants.FHIR_EXT_EPISODE_OF_CARE_REASON)
				.stream().map(extension -> extension.getExtensionsByUrl("value"))
				.flatMap(List::stream)
				.map(Element::getExtension)
				.flatMap((List::stream))
				.filter(extension -> extension.getUrl().equals("concept"))
				.map(extension -> (CodeableConcept) extension.getValue())
				.collect(Collectors.toList());

		assertThat(2, equalTo(reasonValueConceptList.size()));
		Assert.assertTrue("Didn't find episode reason: Covid 19",
				reasonValueConceptList.stream().anyMatch(codeableConcept -> reasonValue1.getUuid().equals(codeableConcept.getCoding().get(0).getCode())));
		Assert.assertTrue("Didn't find episode reason: H1N1",
				reasonValueConceptList.stream().anyMatch(codeableConcept -> reasonValue2.getUuid().equals(codeableConcept.getCoding().get(0).getCode())));
	}
	
	@Test
	public void toOpenmrsType_shouldTranslateEpisodeType() {
		String episodeOfCareUuid = UUID.randomUUID().toString();
		String patientUuid = UUID.randomUUID().toString();
		Concept episodeType = exampleConcept("Home and Community Care");
		
		EpisodeOfCare episodeOfCare = new EpisodeOfCare();
		episodeOfCare.setId(episodeOfCareUuid);
		episodeOfCare.setPatient(patientReference(patientUuid));
		episodeOfCare.setPeriod(exampleEpisodePeriod());
		episodeOfCare.setStatus(EpisodeOfCare.EpisodeOfCareStatus.ACTIVE);
		CodeableConcept episodeTypeCodeableConcept = getCodeableConceptForConcept(episodeType);
		episodeOfCare.setType(Collections.singletonList(episodeTypeCodeableConcept));
		
		when(conceptTranslator.toOpenmrsType(episodeTypeCodeableConcept)).thenReturn(episodeType);
		
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
		Concept episodeType = exampleConcept("Home and Community Care");
		
		EpisodeOfCare episodeOfCare = new EpisodeOfCare();
		episodeOfCare.setId(episodeOfCareUuid);
		episodeOfCare.setPatient(patientReference(patientUuid));
		episodeOfCare.setPeriod(exampleEpisodePeriod());
		episodeOfCare.setStatus(EpisodeOfCare.EpisodeOfCareStatus.ACTIVE);
		CodeableConcept episodeTypeCodeableConcept = getCodeableConceptForConcept(episodeType);
		episodeOfCare.setType(Collections.singletonList(episodeTypeCodeableConcept));
		when(conceptTranslator.toOpenmrsType(episodeTypeCodeableConcept)).thenReturn(episodeType);
		
		Concept openmrsEpisodeReasonConcept1 = exampleConcept("Health Screening");
		CodeableConcept episodeReasonCodeableConcept1 = getCodeableConceptForConcept(openmrsEpisodeReasonConcept1);
		Extension episodeReasonExtension1 = episodeOfCare.addExtension();
		episodeReasonExtension1.setUrl(BahmniFhirConstants.FHIR_EXT_EPISODE_OF_CARE_REASON);
		episodeReasonExtension1.addExtension("use", episodeReasonCodeableConcept1);
		when(conceptTranslator.toOpenmrsType(episodeReasonCodeableConcept1)).thenReturn(openmrsEpisodeReasonConcept1);
		
		Extension reasonValueExtension1 = episodeReasonExtension1.addExtension();
		reasonValueExtension1.setUrl("value");
		Concept reasonValue1 = exampleConcept("Covid 19");
		
		CodeableConcept reasonCodeableConcept1 = getCodeableConceptForConcept(reasonValue1);
		when(conceptTranslator.toOpenmrsType(reasonCodeableConcept1)).thenReturn(reasonValue1);
		reasonValueExtension1.addExtension("concept", reasonCodeableConcept1);
		Reference reasonValueRef1 = new Reference();
		reasonValueRef1.setReference("Condition/wheezing-uuid");
		reasonValueExtension1.addExtension("valueReference", reasonValueRef1);
		
		Episode openmrsEpisode = episodeOfCareTranslator.toOpenmrsType(episodeOfCare);
		assertThat(episodeOfCare.getId(), equalTo(openmrsEpisode.getUuid()));
		assertThat(episodeOfCare.getId(), equalTo(openmrsEpisode.getUuid()));
		assertThat(Episode.Status.ACTIVE, equalTo(openmrsEpisode.getStatus()));
		assertThat(episodeOfCare.getPeriod().getStart(), equalTo(openmrsEpisode.getDateStarted()));
		assertThat(episodeOfCare.getType().get(0).getCoding().get(0).getCode(), equalTo(openmrsEpisode.getConcept()
		        .getUuid()));
		
		Set<EpisodeReason> episodeReasons = openmrsEpisode.getEpisodeReason();
		assertThat(1, equalTo(episodeReasons.size()));
		assertThat(openmrsEpisodeReasonConcept1, equalTo(episodeReasons.iterator().next().getReasonUse()));
		assertThat(reasonValue1, equalTo(episodeReasons.iterator().next().getValueConcept()));
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
	
	private void setupEpisodeReason(Episode episode, Concept reasonUse, @NotNull List<ReasonValuePair> reasonValues) {
		if (reasonValues.isEmpty()) {
			episode.setEpisodeReason(Collections.singleton(createEpisodeReason(episode, reasonUse, null, null)));
		} else {
			Set<EpisodeReason> reasons = reasonValues.stream()
					.map(rv-> createEpisodeReason(episode, reasonUse, rv.concept, rv.valueReference))
					.collect(Collectors.toSet());
			episode.setEpisodeReason(reasons);
		}
	}
	
	private EpisodeReason createEpisodeReason(Episode episode, Concept reasonUse, Concept reasonValueConcept, String valueReference) {
		EpisodeReason episodeReason = new EpisodeReason();
		episodeReason.setEpisode(episode);
		Optional.ofNullable(reasonUse).ifPresent(episodeReason::setReasonUse);
		Optional.ofNullable(reasonValueConcept).ifPresent(episodeReason::setValueConcept);
		Optional.ofNullable(valueReference).ifPresent(episodeReason::setValueReference);
		return episodeReason;
	}
	
	private CodeableConcept getCodeableConceptForConcept(Concept concept) {
		CodeableConcept codeableConcept = new CodeableConcept();
		Coding coding = codeableConcept.addCoding();
		coding.setCode(concept.getUuid());
		concept.getNames().stream().findFirst().ifPresent(conceptName -> coding.setDisplay(conceptName.getName()));
		return codeableConcept;
	}
	
	private Patient exampleEpisodePatient() {
		Patient patient = new Patient();
		patient.setUuid(UUID.randomUUID().toString());
		return patient;
	}
	
	private Concept exampleConcept(String name) {
		Concept concept = new Concept();
		ConceptName cn = new ConceptName();
		cn.setName(name);
		cn.setLocale(Locale.ENGLISH);
		concept.addName(cn);
		concept.setUuid(UUID.randomUUID().toString());
		return concept;
	}
	
	private Episode prepareTestEpisode(String uuid, Concept episodeType, Patient patient) {
		Episode episode = new Episode();
		episode.setUuid(uuid);
		episode.setPatient(patient);
		episode.setDateStarted(new Date());
		episode.setConcept(episodeType);
		return episode;
	}
	
	private void prettyPrint(IBaseResource resource) {
		System.out.println(r4ResourceParser.encodeResourceToString(resource));
	}
	
	private static class ReasonValuePair {
		
		Concept concept;
		
		String valueReference;
		
		public ReasonValuePair(Concept concept, String valueReference) {
			this.concept = concept;
			this.valueReference = valueReference;
		}
	}
	
}
