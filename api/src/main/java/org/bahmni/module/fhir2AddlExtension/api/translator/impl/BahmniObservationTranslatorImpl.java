package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import lombok.AccessLevel;
import lombok.Setter;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniOrderReferenceTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.ComplexObsDataTranslator;
import org.hibernate.proxy.HibernateProxy;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptNumeric;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.api.db.hibernate.HibernateUtil;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationCategoryTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationEffectiveDatetimeTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationInterpretationTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationReferenceRangeTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationStatusTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationValueTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.impl.ObservationQuantityCodingTranslatorImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.Validate.notNull;
import static org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants.FHIR_EXT_OBSERVATION_FORM_NAMESPACE_PATH;
import static org.openmrs.module.fhir2.FhirConstants.UCUM_SYSTEM_URI;
import static org.openmrs.module.fhir2.api.translators.impl.FhirTranslatorUtils.getLastUpdated;
import static org.openmrs.module.fhir2.api.translators.impl.FhirTranslatorUtils.getVersionId;

/**
 * Note, besides mapping the formNamespaceAndPath extension, this overloaded class fixes the obs
 * group translation to openmrs, where instead of using obs.addGroupMember(member) it uses
 * obs.setGroupMember(obs set). Otherwise, Hibernate throws error when the persisted obs are dirtied
 * when obs.addGroupMember() tries to set the member obs' group, causing Hibernate to throw for
 * attached transient group. Once we resolve the issue with OpenMRS, then the overridden
 * toOpenmrsType(Obs obs, Observation resource, Supplier<Obs> groupedObsFactory) should be removed.
 * Other improvements - overridden behavior of sending obs.valueText as reference to location if
 * comment is org.openmrs.location - set obs.comments as notes - handle complex obs
 */
@Component
@Primary
@Setter(AccessLevel.PROTECTED)
public class BahmniObservationTranslatorImpl implements ObservationTranslator {
	
	private static final List<ComplexObsDataTranslator> complexDataTranslators = Arrays.asList(
	    new AttachmentObsDataTranslator(), new LocationObsDataTranslator());
	
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
	private BahmniOrderReferenceTranslator basedOnReferenceTranslator;
	
	@Autowired
	private ObservationEffectiveDatetimeTranslator datetimeTranslator;
	
	@Autowired
	private ObservationQuantityCodingTranslatorImpl quantityCodingTranslator;
	
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
		if (isComplexData(concept)) {
			mapExtensionToOpenmrsData(concept, resource).ifPresent(value -> {
				obs.setValueText(null);
				obs.setValueComplex(value);
			});
		}

		if (resource.hasNote()) {
			String notes = resource.getNote().stream()
					.map(Annotation::getText)
					.filter(Objects::nonNull)
					.collect(Collectors.joining(" | "));
			obs.setComment(notes);
		}

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
		return toOpenmrsType(new Obs(), resource);
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
		resource.addInterpretation(interpretationTranslator.toFhirResource(obs));

		resource.setValue(observationValueTranslator.toFhirResource(obs));
		mapObservationValueQuantity(obs, resource);


		if (isComplexData(obs.getConcept())) {
			mapComplexDataToFhirExtension(obs).ifPresent(resource::addExtension);
		}
        if (obs.getValueNumeric() != null) {
			mapObservationReferenceRange(obs, resource);
        }

		if (obs.isObsGrouping()) {
			for (Obs member : obs.getGroupMembers()) {
				if (!member.getVoided()) {
					resource.addHasMember(observationReferenceTranslator.toFhirResource(member));
				}
			}
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
		if (!StringUtils.isEmpty(obs.getComment())) {
			resource.addNote().setText(obs.getComment());
		}
        return resource;
    }
	
	private void mapObservationReferenceRange(Obs obs, Observation resource) {
		if (!obs.getConcept().isNumeric()) {
			return;
		}
		
		if (resource.hasReferenceRange()) {
			return;
		}
		
		Concept concept = deProxyConcept(obs.getConcept());
		if (concept instanceof ConceptNumeric) {
			NumericObs numericObs = new NumericObs((ConceptNumeric) concept);
			resource.setReferenceRange(referenceRangeTranslator.toFhirResource(numericObs));
		}
	}
	
	/**
	 * This is done because of Obs.concept are not deproxied, and as a result checks done in Openmrs
	 * fhir2 ObservationTranslatorImpl for value and reference ranges, where it checks the type of
	 * obs.concept - e.g. concept instanceof ConceptNumeric, ConceptComplex While the Openmrs
	 * translator implementation works fine with Default ObservationValueTranslator when queries
	 * through the obs root, and Hibernate seems to have enough context to instantiate the specific
	 * subclass for obs.concept into Concept. But when navigating through a Set in Observations,
	 * loaded through a different root, e.g. Diagnostic Report, because of lazy-loading proxy for
	 * the collection elements, obs.concept does not "transform" into the subclass (ConceptNumeric),
	 * which breaks the instanceof check.
	 * 
	 * @param obs
	 * @param resource
	 */
	private void mapObservationValueQuantity(Obs obs, Observation resource) {
		if (!obs.getConcept().isNumeric()) {
			return;
		}
		
		if (resource.hasValue()) {
			if (resource.getValue() instanceof Quantity) {
				Quantity value = (Quantity) resource.getValue();
				if (value.hasUnit() || value.hasCode()) {
					return;
				}
			}
		}
		
		Concept concept = deProxyConcept(obs.getConcept());
		if (concept instanceof ConceptNumeric) {
			Double value = obs.getValueNumeric();
			Quantity result = resource.hasValue() ? (Quantity) resource.getValue() : new Quantity();
			ConceptNumeric cn = (ConceptNumeric) concept;
			if (cn.getAllowDecimal()) {
				result.setValue(value);
			} else {
				result.setValue(value.longValue());
			}
			result.setUnit(cn.getUnits());
			// only set the coding system if unit conforms to UCUM standard
			Coding coding = quantityCodingTranslator.toFhirResource(cn);
			if (coding != null && coding.hasSystem() && coding.getSystem().equals(UCUM_SYSTEM_URI)) {
				result.setCode(coding.getCode());
				result.setSystem(coding.getSystem());
			}
			resource.setValue(result);
		}
	}
	
	private Concept deProxyConcept(Concept concept) {
		if (concept instanceof HibernateProxy) {
			return (Concept) ((HibernateProxy) concept).getHibernateLazyInitializer().getImplementation();
		} else {
			return concept;
		}
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
	
	private Optional<String> mapExtensionToOpenmrsData(Concept concept, Observation observation) {
		for (ComplexObsDataTranslator complexDataTranslator : complexDataTranslators) {
			if (complexDataTranslator.supports(concept)) {
				String complexData = complexDataTranslator.toOpenmrsType(observation);
				if (complexData != null) {
					return Optional.of(complexData);
				}
			}
		}
		return Optional.empty();
	}
	
	/**
	 * Default OMRS ObservationTranslatorImpl does not handle complex obs. The following also
	 * overrides openmrs default ObservationTranslatorImpl of supporting location captured as value
	 * in Obs. > return hasTextValue && StringUtils.equals(obs.getComment(), "org.openmrs.Location")
	 * ? Optional.of(obs.getValueText()) : Optional.empty(); The above is presumably a temporary
	 * hack, as it does not work as obs.comments should really capture Obervation.note, and also how
	 * Bahmni interprets Bahmni does allow storing observation as obs, but it's done through concept
	 * type = complex, which allows custom datatypes to be stored against observation and also
	 * leverage support of data type handlers to get/save data. TODO: propose change of
	 * interpretation implementation or provide a hook for impl to OMRS The following code uses a
	 * simple strategy (ComplexDataTranslator) below and for now these are internal In future, the
	 * may allow exposing ComplexDataTranslator and allowing implementations to provide custom
	 * implementations, without having to completely implement obsValueTranslators and also
	 * observation translator
	 */
	private Optional<Extension> mapComplexDataToFhirExtension(Obs obs) {
		if (obs.getValueComplex() == null) {
			return Optional.empty();
		}
		
		for (ComplexObsDataTranslator complexDataTranslator : complexDataTranslators) {
			if (complexDataTranslator.supports(obs.getConcept())) {
				Extension result = complexDataTranslator.toFhirResource(obs);
				if (result != null) {
					return Optional.of(result);
				}
			}
		}
		return Optional.empty();
	}
	
	private boolean isComplexData(Concept concept) {
		return concept.getDatatype().getUuid().equals(ConceptDatatype.COMPLEX_UUID);
	}
	
	private class NumericObs extends Obs {
		
		private ConceptNumeric concept;
		
		public NumericObs(ConceptNumeric concept) {
			this.concept = concept;
		}
		
		@Override
		public Concept getConcept() {
			return this.concept;
		}
	}
	
}
