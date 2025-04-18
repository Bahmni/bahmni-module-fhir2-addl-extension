package org.bahmni.module.fhir2AddlExtension.api.translator;

import lombok.AccessLevel;
import lombok.Setter;
import org.bahmni.module.fhir2AddlExtension.api.validators.BahmniEncounterValidator;
import org.openmrs.*;
import org.openmrs.module.fhir2.api.translators.impl.EncounterTranslatorImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component
@Primary
@Setter(AccessLevel.PACKAGE)
public class BahmniEncounterTranslatorImpl extends EncounterTranslatorImpl {

    @Autowired
    private BahmniEncounterValidator bahmniEncounterValidator;

    @Override
    public Encounter toOpenmrsType(@Nonnull Encounter existingEncounter, @Nonnull org.hl7.fhir.r4.model.Encounter encounter) {

        bahmniEncounterValidator.validate(existingEncounter, encounter);
        return super.toOpenmrsType(existingEncounter, encounter);
    }

}
