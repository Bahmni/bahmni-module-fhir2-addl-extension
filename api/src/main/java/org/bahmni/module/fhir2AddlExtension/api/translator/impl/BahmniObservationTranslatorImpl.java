package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import lombok.AccessLevel;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.proxy.HibernateProxy;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.openmrs.Concept;
import org.openmrs.ConceptNumeric;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.api.db.hibernate.HibernateUtil;
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
import org.openmrs.module.fhir2.api.translators.ObservationTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationValueTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.apache.commons.lang3.Validate.notNull;
import static org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants.FHIR_EXT_OBSERVATION_FORM_NAMESPACE_PATH;
import static org.openmrs.module.fhir2.api.translators.impl.FhirTranslatorUtils.getLastUpdated;
import static org.openmrs.module.fhir2.api.translators.impl.FhirTranslatorUtils.getVersionId;
import static org.openmrs.module.fhir2.api.translators.impl.ReferenceHandlingTranslator.createLocationReferenceByUuid;

/**
 * Note, besides mapping the formNamespaceAndPath extension, this overloaded class fixes the obs
 * group translation to openmrs, where instead of using obs.addGroupMember(member) it uses
 * obs.setGroupMember(obs set). Otherwise, Hibernate throws error when the persisted obs are dirtied
 * when obs.addGroupMember() tries to set the member obs' group, causing Hibernate to throw for
 * attached transient group. Once we resolve the issue with OpenMRS, then the overridden
 * toOpenmrsType(Obs obs, Observation resource, Supplier<Obs> groupedObsFactory) should be removed.
 */
@Component
@Primary
@Setter(AccessLevel.PROTECTED)
public class BahmniObservationTranslatorImpl implements ObservationTranslator {
	
	@Autowired
	private ObservationStatusTranslator observationStatusTranslator;
	
	@Autowired
	private ObservationReferenceTranslator observationReferenceTranslator;
	
	@Autowired
	private ObservationValueTranslator observationValueTranslator;
	
	@Autowired
	private ConceptTranslator conceptTranslator;
	
	@Autowired
	private ObservationCategoryTranslator categoryTranslator;
	
	@Autowired
	private EncounterReferenceTranslator<Encounter> encounterReferenceTranslator;
	
	@Autowired
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Autowired
	private ObservationInterpretationTranslator interpretationTranslator;
	
	@Autowired
	private ObservationReferenceRangeTranslator referenceRangeTranslator;
	
	@Autowired
	private ObservationBasedOnReferenceTranslator basedOnReferenceTranslator;
	
	@Autowired
	private ObservationEffectiveDatetimeTranslator datetimeTranslator;
	
	@Override
	public Obs toOpenmrsType(Obs obs, Observation resource, Supplier<Obs> groupedObsFactory) {
		notNull(obs, "The existing Obs object should not be null");
		notNull(resource, "The Observation object should not be null");
		
		observationStatusTranslator.toOpenmrsType(obs, resource.getStatus());
		
		obs.setEncounter(encounterReferenceTranslator.toOpenmrsType(resource.getEncounter()));
		obs.setPerson(patientReferenceTranslator.toOpenmrsType(resource.getSubject()));
		
		Concept concept = conceptTranslator.toOpenmrsType(resource.getCode());
		obs.setConcept(concept);
		observationValueTranslator.toOpenmrsType(obs, resource.getValue());

		Set<Obs> members = new HashSet<>();
		for (Reference reference : resource.getHasMember()) {
			Obs member = observationReferenceTranslator.toOpenmrsType(reference);
			members.add(member);
		}
		obs.setGroupMembers(members);
		
		if (!resource.getInterpretation().isEmpty()) {
			interpretationTranslator.toOpenmrsType(obs, resource.getInterpretation().get(0));
		}
		
		datetimeTranslator.toOpenmrsType(obs, resource.getEffectiveDateTimeType());
		
		if (resource.hasBasedOn()) {
			obs.setOrder(basedOnReferenceTranslator.toOpenmrsType(resource.getBasedOn().get(0)));
		}
		
		obs.setFormNamespaceAndPath(mapFormNamespacePathExtension(resource));
		return obs;
	}
	
	@Override
	public Obs toOpenmrsType(@Nonnull Observation resource) {
		notNull(resource, "The Observation object should not be null");
		Obs obs = toOpenmrsType(new Obs(), resource);
		obs.setFormNamespaceAndPath(mapFormNamespacePathExtension(resource));
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
        notNull(obs, "The Obs object should not be null");

        Observation resource = new Observation();
        resource.setId(obs.getUuid());
        resource.setStatus(observationStatusTranslator.toFhirResource(obs));

        resource.setEncounter(encounterReferenceTranslator.toFhirResource(obs.getEncounter()));

        Person obsPerson = obs.getPerson();
        if (obsPerson != null) {
            if (obsPerson instanceof HibernateProxy) {
                obsPerson = HibernateUtil.getRealObjectFromProxy(obsPerson);
            }

            if (obsPerson instanceof Patient) {
                resource.setSubject(patientReferenceTranslator.toFhirResource((Patient) obsPerson));
            }
        }

        resource.setCode(conceptTranslator.toFhirResource(obs.getConcept()));
        resource.addCategory(categoryTranslator.toFhirResource(obs.getConcept()));

        if (obs.isObsGrouping()) {
            for (Obs groupObs : obs.getGroupMembers()) {
                if (!groupObs.getVoided()) {
                    resource.addHasMember(observationReferenceTranslator.toFhirResource(groupObs));
                }
            }
        }

        resource.setValue(observationValueTranslator.toFhirResource(obs));

        resource.addInterpretation(interpretationTranslator.toFhirResource(obs));

        if (obs.getValueNumeric() != null) {
            Concept concept = obs.getConcept();
            if (concept instanceof ConceptNumeric) {
                resource.setReferenceRange(referenceRangeTranslator.toFhirResource(obs));
            }
        }

        if (obs.getValueText() != null && StringUtils.equals(obs.getComment(), "org.openmrs.Location")) {
            resource.addExtension(FhirConstants.OPENMRS_FHIR_EXT_OBS_LOCATION_VALUE,
                    createLocationReferenceByUuid(obs.getValueText()));
        }

        resource.setIssued(obs.getDateCreated());
        resource.setEffective(datetimeTranslator.toFhirResource(obs));
        resource.addBasedOn(basedOnReferenceTranslator.toFhirResource(obs.getOrder()));

        resource.getMeta().setLastUpdated(getLastUpdated(obs));
        resource.getMeta().setVersionId(getVersionId(obs));

        if (obs.getFormNamespaceAndPath() != null) {
            Optional.ofNullable(obs.getFormNamespaceAndPath())
               .ifPresent(value -> resource.addExtension(FHIR_EXT_OBSERVATION_FORM_NAMESPACE_PATH, new StringType(value)));
        }
        return resource;
    }
}
