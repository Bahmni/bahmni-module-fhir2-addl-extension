package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
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
import org.openmrs.module.fhir2.api.translators.impl.ObservationTranslatorImpl;

import java.lang.reflect.Field;

import static org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants.FHIR_EXT_OBSERVATiON_FORM_NAMESPACE_PATH;
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

    @Mock
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
        observationTranslator = new BahmniObservationTranslatorImpl();

        setPropertyOnSuperClass(observationTranslator, "observationStatusTranslator", observationStatusTranslator);
        setPropertyOnSuperClass(observationTranslator, "observationReferenceTranslator", observationReferenceTranslator);
        setPropertyOnSuperClass(observationTranslator, "observationValueTranslator", observationValueTranslator);
        setPropertyOnSuperClass(observationTranslator, "conceptTranslator", conceptTranslator);
        setPropertyOnSuperClass(observationTranslator, "categoryTranslator", categoryTranslator);
        setPropertyOnSuperClass(observationTranslator, "encounterReferenceTranslator", encounterReferenceTranslator);
        setPropertyOnSuperClass(observationTranslator, "patientReferenceTranslator", patientReferenceTranslator);
        setPropertyOnSuperClass(observationTranslator, "interpretationTranslator", interpretationTranslator);
        setPropertyOnSuperClass(observationTranslator, "referenceRangeTranslator", referenceRangeTranslator);
        setPropertyOnSuperClass(observationTranslator, "basedOnReferenceTranslator", basedOnReferenceTranslator);
        setPropertyOnSuperClass(observationTranslator, "datetimeTranslator", datetimeTranslator);
    }

    @Test
    public void shouldMapToOpenmrsObsFormNamespacePath() {
        String CONCEPT_UUID = "obs_concept_uuid";
        Concept concept = new Concept();
        concept.setUuid(CONCEPT_UUID);

        Observation resource = new Observation();
        CodeableConcept codeableConcept = new CodeableConcept();
        codeableConcept.setId(CONCEPT_UUID);
        resource.setCode(codeableConcept);

        resource.addExtension(FHIR_EXT_OBSERVATiON_FORM_NAMESPACE_PATH, new StringType(EXAMPLE_OBS_FORM_NAMESPACE_PATH));

        when(conceptTranslator.toOpenmrsType(codeableConcept)).thenReturn(concept);
        Obs result = observationTranslator.toOpenmrsType(new Obs(), resource);
        Assert.assertNotNull(result);
        Assert.assertEquals(EXAMPLE_OBS_FORM_NAMESPACE_PATH, result.getFormNamespaceAndPath());
    }

    @Test
    public void shouldReturnFormNameSpaceAsObservationExtension() {
        Obs observation = new Obs();
        observation.setFormNamespaceAndPath(EXAMPLE_OBS_FORM_NAMESPACE_PATH);
        Observation result = observationTranslator.toFhirResource(observation);
        Assert.assertEquals(EXAMPLE_OBS_FORM_NAMESPACE_PATH,
                result.getExtensionByUrl(FHIR_EXT_OBSERVATiON_FORM_NAMESPACE_PATH).getValue().primitiveValue());
    }

    private void setPropertyOnSuperClass(ObservationTranslatorImpl translator, String attributeName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        //TBD: unfortunately, the way the fhir2 ObservationTranslatorImpl beans are declared (package private),
        //and since BahmniObservationTranslatorImpl is from different package, hence usage of reflection
        Class<?> clazz = translator.getClass().getSuperclass();
        Field field = clazz.getDeclaredField(attributeName);
        field.setAccessible(true);
        field.set(translator, value);
    }
}