package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptComplex;
import org.openmrs.ConceptDatatype;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationBasedOnReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationCategoryTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationEffectiveDatetimeTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationInterpretationTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationReferenceRangeTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationStatusTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationValueTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.impl.ObservationInterpretationTranslatorImpl;

import static org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants.FHIR_EXT_OBSERVATION_FORM_NAMESPACE_PATH;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniObservationTranslatorImplTest {
	
	public static final String EXAMPLE_OBS_FORM_NAMESPACE_PATH = "Bahmni^Vitals.1/14-0";
	
	@Mock
	private ObservationStatusTranslator observationStatusTranslator;
	
	@Mock
	private ObservationReferenceTranslator observationReferenceTranslator;
	
	@Mock
	private ObservationValueTranslator observationValueTranslator;
	
	@Mock
	private ConceptTranslator conceptTranslator;
	
	@Mock
	private ObservationCategoryTranslator categoryTranslator;
	
	@Mock
	private EncounterReferenceTranslator<Encounter> encounterReferenceTranslator;
	
	@Mock
	private PatientReferenceTranslator patientReferenceTranslator;
	
	private ObservationInterpretationTranslator interpretationTranslator;
	
	@Mock
	private ObservationReferenceRangeTranslator referenceRangeTranslator;
	
	@Mock
	private ObservationBasedOnReferenceTranslator basedOnReferenceTranslator;
	
	@Mock
	private ObservationEffectiveDatetimeTranslator datetimeTranslator;
	
	private BahmniObservationTranslatorImpl observationTranslator;
	
	@Before
	public void setUp() throws Exception {
		interpretationTranslator = new ObservationInterpretationTranslatorImpl();
		observationTranslator = new BahmniObservationTranslatorImpl();
		observationTranslator.setObservationStatusTranslator(observationStatusTranslator);
		observationTranslator.setObservationReferenceTranslator(observationReferenceTranslator);
		observationTranslator.setObservationValueTranslator(observationValueTranslator);
		observationTranslator.setConceptTranslator(conceptTranslator);
		observationTranslator.setCategoryTranslator(categoryTranslator);
		observationTranslator.setEncounterReferenceTranslator(encounterReferenceTranslator);
		observationTranslator.setPatientReferenceTranslator(patientReferenceTranslator);
		observationTranslator.setInterpretationTranslator(interpretationTranslator);
		observationTranslator.setReferenceRangeTranslator(referenceRangeTranslator);
		observationTranslator.setBasedOnReferenceTranslator(basedOnReferenceTranslator);
		observationTranslator.setDatetimeTranslator(datetimeTranslator);
	}
	
	@Test
	public void shouldMapToOpenmrsObsFormNamespacePath() {
		String CONCEPT_UUID = "obs_concept_uuid";
		Concept concept = new Concept();
		ConceptDatatype conceptDatatype = new ConceptDatatype();
		concept.setUuid(CONCEPT_UUID);
		concept.setDatatype(conceptDatatype);
		
		Observation resource = new Observation();
		CodeableConcept codeableConcept = new CodeableConcept();
		codeableConcept.setId(CONCEPT_UUID);
		resource.setCode(codeableConcept);
		
		resource.addExtension(FHIR_EXT_OBSERVATION_FORM_NAMESPACE_PATH, new StringType(EXAMPLE_OBS_FORM_NAMESPACE_PATH));
		
		when(conceptTranslator.toOpenmrsType(codeableConcept)).thenReturn(concept);
		Obs result = observationTranslator.toOpenmrsType(new Obs(), resource);
		Assert.assertNotNull(result);
		Assert.assertEquals(EXAMPLE_OBS_FORM_NAMESPACE_PATH, result.getFormNamespaceAndPath());
	}
	
	@Test
	public void shouldMapToOpenmrsObsComments() {
		String CONCEPT_UUID = "obs_concept_uuid";
		Concept concept = new Concept();
		ConceptDatatype conceptDatatype = new ConceptDatatype();
		concept.setUuid(CONCEPT_UUID);
		concept.setDatatype(conceptDatatype);
		
		Observation resource = new Observation();
		CodeableConcept codeableConcept = new CodeableConcept();
		codeableConcept.setId(CONCEPT_UUID);
		resource.setCode(codeableConcept);
		resource.addNote().setText("Note 1");
		resource.addNote().setText("Note 2");
		when(conceptTranslator.toOpenmrsType(codeableConcept)).thenReturn(concept);
		Obs result = observationTranslator.toOpenmrsType(new Obs(), resource);
		Assert.assertNotNull(result);
		Assert.assertEquals("Note 1 | Note 2", result.getComment());
	}
	
	@Test
	public void shouldReturnFormNameSpaceAsObservationExtension() {
		Obs obs = new Obs();
		ConceptClass conceptClass = new ConceptClass();
		conceptClass.setName("Misc");
		ConceptComplex concept = new ConceptComplex();
		concept.setConceptClass(conceptClass);
		ConceptDatatype conceptDatatype = new ConceptDatatype();
		conceptDatatype.setUuid(ConceptDatatype.TEXT_UUID);
		concept.setDatatype(conceptDatatype);
		obs.setConcept(concept);
		obs.setValueText("Example Text");
		obs.setFormNamespaceAndPath(EXAMPLE_OBS_FORM_NAMESPACE_PATH);
		obs.setComment("Note 1");
		when(observationValueTranslator.toFhirResource(obs)).thenReturn(new StringType().setValue(obs.getValueText()));
		Observation result = observationTranslator.toFhirResource(obs);
		Assert.assertEquals(EXAMPLE_OBS_FORM_NAMESPACE_PATH,
		    result.getExtensionByUrl(FHIR_EXT_OBSERVATION_FORM_NAMESPACE_PATH).getValue().primitiveValue());
		Assert.assertEquals("Example Text", result.getValueStringType().getValue());
		Assert.assertEquals(1, result.getNote().size());
		Assert.assertEquals("Note 1", result.getNote().get(0).getText());
	}
	
	@Test
	public void shouldReturnLocationAsObservationExtension() {
		Obs observation = new Obs();
		ConceptClass conceptClass = new ConceptClass();
		conceptClass.setName("Misc");
		ConceptComplex concept = new ConceptComplex();
		concept.setConceptClass(conceptClass);
		ConceptDatatype conceptDatatype = new ConceptDatatype();
		conceptDatatype.setUuid(ConceptDatatype.COMPLEX_UUID);
		concept.setDatatype(conceptDatatype);
		concept.setHandler("LocationObsHandler");
		observation.setConcept(concept);
		observation.setInterpretation(Obs.Interpretation.NORMAL);
		observation.setValueComplex("example-location-uuid");
		observation.setFormNamespaceAndPath(EXAMPLE_OBS_FORM_NAMESPACE_PATH);
		Observation result = observationTranslator.toFhirResource(observation);
		Assert.assertEquals(EXAMPLE_OBS_FORM_NAMESPACE_PATH,
		    result.getExtensionByUrl(FHIR_EXT_OBSERVATION_FORM_NAMESPACE_PATH).getValue().primitiveValue());
		Type refLocationValue = result.getExtensionByUrl(FhirConstants.OPENMRS_FHIR_EXT_OBS_LOCATION_VALUE).getValue();
		Assert.assertTrue(refLocationValue instanceof Reference);
		Assert.assertEquals("Location/example-location-uuid", ((Reference) refLocationValue).getReference());
	}
	
	@Test
	public void shouldConvertLocationExtensionToObsComplexData() {
		Obs observation = new Obs();
		ConceptClass conceptClass = new ConceptClass();
		conceptClass.setName("Misc");
		ConceptComplex concept = new ConceptComplex();
		concept.setConceptClass(conceptClass);
		ConceptDatatype conceptDatatype = new ConceptDatatype();
		conceptDatatype.setUuid(ConceptDatatype.COMPLEX_UUID);
		concept.setDatatype(conceptDatatype);
		concept.setHandler("LocationObsHandler");
		observation.setConcept(concept);
		observation.setInterpretation(Obs.Interpretation.NORMAL);
		observation.setValueComplex("example-location-uuid");
		observation.setFormNamespaceAndPath(EXAMPLE_OBS_FORM_NAMESPACE_PATH);
		Observation resource = observationTranslator.toFhirResource(observation);
		Reference refLocationValue = (Reference) resource.getExtensionByUrl(
		    FhirConstants.OPENMRS_FHIR_EXT_OBS_LOCATION_VALUE).getValue();
		Assert.assertNotNull(refLocationValue);
		when(conceptTranslator.toOpenmrsType(resource.getCode())).thenReturn(concept);
		Obs obs = observationTranslator.toOpenmrsType(resource);
		Assert.assertEquals("example-location-uuid", obs.getValueComplex());
	}
	
	@Test
	public void shouldReturnAttachmentAsObservationExtension() {
		Obs observation = new Obs();
		ConceptClass conceptClass = new ConceptClass();
		conceptClass.setName("Image");
		ConceptComplex concept = new ConceptComplex();
		concept.setConceptClass(conceptClass);
		ConceptDatatype conceptDatatype = new ConceptDatatype();
		conceptDatatype.setUuid(ConceptDatatype.COMPLEX_UUID);
		concept.setDatatype(conceptDatatype);
		concept.setHandler("ImageUrlHandler");
		observation.setConcept(concept);
		observation.setValueComplex("/path-to-file.jpg");
		observation.setFormNamespaceAndPath(EXAMPLE_OBS_FORM_NAMESPACE_PATH);
		Observation result = observationTranslator.toFhirResource(observation);
		Assert.assertEquals(EXAMPLE_OBS_FORM_NAMESPACE_PATH,
		    result.getExtensionByUrl(FHIR_EXT_OBSERVATION_FORM_NAMESPACE_PATH).getValue().primitiveValue());
		Type attachmentValue = result.getExtensionByUrl(BahmniFhirConstants.FHIR_EXT_OBSERVATION_ATTACHMENT_VALUE)
		        .getValue();
		Assert.assertTrue(attachmentValue instanceof Attachment);
		Assert.assertEquals("/path-to-file.jpg", ((Attachment) attachmentValue).getUrl());
	}
	
	@Test
	public void shouldConvertAttachmentExtensionToObsComplexData() {
		Obs observation = new Obs();
		ConceptClass conceptClass = new ConceptClass();
		conceptClass.setName("Image");
		ConceptComplex concept = new ConceptComplex();
		concept.setConceptClass(conceptClass);
		ConceptDatatype conceptDatatype = new ConceptDatatype();
		conceptDatatype.setUuid(ConceptDatatype.COMPLEX_UUID);
		concept.setDatatype(conceptDatatype);
		concept.setHandler("ImageUrlHandler");
		observation.setConcept(concept);
		observation.setValueComplex("/path-to-file.jpg");
		observation.setFormNamespaceAndPath(EXAMPLE_OBS_FORM_NAMESPACE_PATH);
		Observation resource = observationTranslator.toFhirResource(observation);
		Type attachment = resource.getExtensionByUrl(BahmniFhirConstants.FHIR_EXT_OBSERVATION_ATTACHMENT_VALUE).getValue();
		Assert.assertTrue(attachment instanceof Attachment);
		
		when(conceptTranslator.toOpenmrsType(resource.getCode())).thenReturn(concept);
		
		Obs openmrsObs = observationTranslator.toOpenmrsType(resource);
		Assert.assertEquals("/path-to-file.jpg", openmrsObs.getValueComplex());
	}
	
	@Test
	public void shouldReturnObservationInterpretation() {
		Obs observation = new Obs();
		ConceptClass conceptClass = new ConceptClass();
		conceptClass.setName("Misc");
		ConceptComplex concept = new ConceptComplex();
		concept.setConceptClass(conceptClass);
		ConceptDatatype conceptDatatype = new ConceptDatatype();
		conceptDatatype.setUuid("example-content-data=type");
		concept.setDatatype(conceptDatatype);
		concept.setHandler("example-handler");
		observation.setConcept(concept);
		observation.setInterpretation(Obs.Interpretation.NORMAL);
		observation.setValueComplex("example-location-uuid");
		observation.setFormNamespaceAndPath(EXAMPLE_OBS_FORM_NAMESPACE_PATH);
		Observation result = observationTranslator.toFhirResource(observation);
		Assert.assertEquals("Normal", result.getInterpretationFirstRep().getCodingFirstRep().getDisplay());
	}
	
	/**
	 * TBD: Commented off till we resolve the obsGroup issue in the translator.
	 */
	//	private void setPropertyOnSuperClass(ObservationTranslatorImpl translator, String attributeName, Object value)
	//			throws NoSuchFieldException, IllegalAccessException {
	//		//TBD: unfortunately, the way the fhir2 ObservationTranslatorImpl beans are declared (package private),
	//		//and since BahmniObservationTranslatorImpl is from different package, hence usage of reflection
	//		Class<?> clazz = translator.getClass().getSuperclass();
	//		Field field = clazz.getDeclaredField(attributeName);
	//		field.setAccessible(true);
	//		field.set(translator, value);
	//  	}
	
}
