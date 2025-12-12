package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.openmrs.Obs;
import org.openmrs.module.fhir2.api.translators.impl.ObservationTranslatorImpl;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

import static org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants.FHIR_EXT_OBSERVATION_FORM_NAMESPACE_PATH;

@Component
@Primary
public class BahmniObservationTranslatorImpl extends ObservationTranslatorImpl {
    @Override
    public Obs toOpenmrsType(@Nonnull Observation fhirObservation) {
        Obs obs = super.toOpenmrsType(fhirObservation);
        obs.setFormNamespaceAndPath(mapFormNamespacePathExtension(fhirObservation));
        return obs;
    }

    private String mapFormNamespacePathExtension(Observation fhirObservation) {
        List<Extension> extensions = fhirObservation.getExtensionsByUrl(FHIR_EXT_OBSERVATION_FORM_NAMESPACE_PATH);
        if (!extensions.isEmpty()) {
            Extension formNameSpacePathExtn = extensions.get(0);
            Type formNameSpacePathExtnValue = formNameSpacePathExtn.getValue();
            return formNameSpacePathExtnValue.primitiveValue();
        }
        return null;
    }

    @Override
    public Observation toFhirResource(@Nonnull Obs obs) {
        Observation fhirResource = super.toFhirResource(obs);
        if (obs.getFormNamespaceAndPath() != null) {
            Optional.ofNullable(obs.getFormNamespaceAndPath())
               .ifPresent(value -> fhirResource.addExtension(FHIR_EXT_OBSERVATION_FORM_NAMESPACE_PATH, new StringType(value)));
        }
        return fhirResource;
    }

    @Override
    public Obs toOpenmrsType(@Nonnull Obs existingObs, @Nonnull Observation observation) {
        Obs obs = super.toOpenmrsType(existingObs, observation);
        obs.setFormNamespaceAndPath(mapFormNamespacePathExtension(observation));
        return obs;
    }
}
