package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import lombok.AccessLevel;
import lombok.Setter;
import org.bahmni.module.fhir2AddlExtension.api.validators.BahmniEncounterValidator;
import org.openmrs.*;
import org.openmrs.module.fhir2.api.translators.*;
import org.openmrs.module.fhir2.api.translators.impl.EncounterTranslatorImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.Validate.notNull;

@Component
@Primary
@Setter(AccessLevel.PROTECTED)
public class BahmniEncounterTranslatorImpl extends EncounterTranslatorImpl {
	
	@Autowired
	private EncounterParticipantTranslator participantTranslator;
	
	@Autowired
	private EncounterLocationTranslator encounterLocationTranslator;
	
	@Autowired
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Autowired
	private EncounterReferenceTranslator<Visit> visitReferenceTranlator;
	
	@Autowired
	private EncounterTypeTranslator<EncounterType> encounterTypeTranslator;
	
	@Autowired
	private EncounterPeriodTranslator<Encounter> encounterPeriodTranslator;
	
	@Autowired
	private BahmniEncounterValidator bahmniEncounterValidator;
	
	@Override
	public Encounter toOpenmrsType(@Nonnull Encounter existingEncounter, @Nonnull org.hl7.fhir.r4.model.Encounter encounter) {
		
		bahmniEncounterValidator.validate(existingEncounter, encounter);
		
		/* TODO
		 * The following lines has been copied over from OpenMRS EncounterTranslatorImpl to fix issues with
		 * EncounterProviderDuplication. This can be removed once the https://openmrs.atlassian.net/browse/FM2-660
		 * is fixed in OpenMRS and super.toOpenMRStype() should be invoked.
		 */
		notNull(existingEncounter, "The existing Openmrs Encounter object should not be null");
		notNull(encounter, "The Encounter object should not be null");
		
		if (encounter.hasId()) {
			existingEncounter.setUuid(encounter.getIdElement().getIdPart());
		}
		
		EncounterType encounterType = encounterTypeTranslator.toOpenmrsType(encounter.getType());
		if (encounterType != null) {
			existingEncounter.setEncounterType(encounterType);
		}
		
		existingEncounter.setPatient(patientReferenceTranslator.toOpenmrsType(encounter.getSubject()));
		existingEncounter.setLocation(encounterLocationTranslator.toOpenmrsType(encounter.getLocationFirstRep()));
		existingEncounter.setVisit(visitReferenceTranlator.toOpenmrsType(encounter.getPartOf()));
		setEncounterProviders(existingEncounter, encounter);
		encounterPeriodTranslator.toOpenmrsType(existingEncounter, encounter.getPeriod());
		
		return existingEncounter;
	}
	
	private void setEncounterProviders(Encounter existingEncounter, org.hl7.fhir.r4.model.Encounter encounter) {
		Set<EncounterProvider> existingProviders = existingEncounter.getEncounterProviders();
		if (existingProviders == null) {
			existingProviders = new LinkedHashSet<>(encounter.getParticipant().size());
		}

		Set<EncounterProvider> updatedProviders = encounter.getParticipant().stream().map(p -> {
			EncounterProvider ep = participantTranslator.toOpenmrsType(new EncounterProvider(), p);
			ep.setEncounter(existingEncounter);
			return ep;
		}).collect(Collectors.toCollection(LinkedHashSet::new));


		if (existingProviders.isEmpty() && !updatedProviders.isEmpty()) {
			existingProviders.addAll(updatedProviders);
		} else {
			for (EncounterProvider updated : updatedProviders) {
				boolean isNew = existingProviders.stream()
						.noneMatch(existing -> existing.getProvider().getUuid().equals(updated.getProvider().getUuid()));
				if (isNew) {
					existingProviders.add(updated);
				}
			}

			existingEncounter.setEncounterProviders(existingProviders);
		}
	}
}
