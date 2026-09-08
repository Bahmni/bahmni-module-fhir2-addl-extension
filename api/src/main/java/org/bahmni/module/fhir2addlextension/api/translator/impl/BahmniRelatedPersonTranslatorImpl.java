package org.bahmni.module.fhir2addlextension.api.translator.impl;

import org.bahmni.module.fhir2addlextension.api.translator.BahmniRelatedPersonTranslator;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.RelatedPerson;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.PersonAddress;
import org.openmrs.PersonName;
import org.openmrs.Relationship;
import org.openmrs.RelationshipType;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.module.fhir2.api.translators.BirthDateTranslator;
import org.openmrs.module.fhir2.api.translators.GenderTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PersonAddressTranslator;
import org.openmrs.module.fhir2.api.translators.PersonNameTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Date;

import static org.apache.commons.lang3.Validate.notNull;

@Component
@Primary
public class BahmniRelatedPersonTranslatorImpl implements BahmniRelatedPersonTranslator {
	
	static final String RELATED_PATIENT_EXT_URL = "http://fhir.bahmni.org/ext/relatedPatient";
	
	static final String RELATIONSHIP_TYPE_SYSTEM = "http://fhir.bahmni.org/RelationshipType";
	
	private final PersonNameTranslator nameTranslator;
	
	private final GenderTranslator genderTranslator;
	
	private final BirthDateTranslator birthDateTranslator;
	
	private final PersonAddressTranslator addressTranslator;
	
	private final PatientReferenceTranslator patientReferenceTranslator;
	
	private final PersonService personService;
	
	private final PatientService patientService;
	
	@Autowired
	public BahmniRelatedPersonTranslatorImpl(PersonNameTranslator nameTranslator, GenderTranslator genderTranslator,
	    BirthDateTranslator birthDateTranslator, PersonAddressTranslator addressTranslator,
	    PatientReferenceTranslator patientReferenceTranslator, PersonService personService, PatientService patientService) {
		this.nameTranslator = nameTranslator;
		this.genderTranslator = genderTranslator;
		this.birthDateTranslator = birthDateTranslator;
		this.addressTranslator = addressTranslator;
		this.patientReferenceTranslator = patientReferenceTranslator;
		this.personService = personService;
		this.patientService = patientService;
	}
	
	@Override
	public RelatedPerson toFhirResource(@Nonnull Relationship relationship) {
		// Default: personB is the focal patient, personA is the related person
		return translate(relationship, relationship.getPersonB(), relationship.getPersonA(), relationship
		        .getRelationshipType().getaIsToB());
	}
	
	@Override
	public RelatedPerson toFhirResource(@Nonnull Relationship relationship, String subjectPatientUuid) {
		if (subjectPatientUuid != null && relationship.getPersonA().getUuid().equals(subjectPatientUuid)) {
			// personA is the focal patient → personB is the related person
			return translate(relationship, relationship.getPersonA(), relationship.getPersonB(), relationship
			        .getRelationshipType().getbIsToA());
		}
		return toFhirResource(relationship);
	}
	
	private RelatedPerson translate(Relationship relationship, Person focalPatient, Person relatedPerson, String roleDisplay) {
		RelatedPerson fhirRelatedPerson = new RelatedPerson();
		fhirRelatedPerson.setId(relationship.getUuid());
		
		if (focalPatient.getIsPatient()) {
			org.openmrs.Patient patient = patientService.getPatient(focalPatient.getPersonId());
			if (patient != null) {
				fhirRelatedPerson.setPatient(patientReferenceTranslator.toFhirResource(patient));
			}
		}
		
		for (PersonName name : relatedPerson.getNames()) {
			fhirRelatedPerson.addName(nameTranslator.toFhirResource(name));
		}
		
		fhirRelatedPerson.setGender(genderTranslator.toFhirResource(relatedPerson.getGender()));
		fhirRelatedPerson.setBirthDateElement(birthDateTranslator.toFhirResource(relatedPerson));
		
		for (PersonAddress address : relatedPerson.getAddresses()) {
			fhirRelatedPerson.addAddress(addressTranslator.toFhirResource(address));
		}
		
		fhirRelatedPerson.setActive(isActive(relationship));
		Period period = buildPeriod(relationship);
		if (period != null) {
			fhirRelatedPerson.setPeriod(period);
		}
		fhirRelatedPerson.setRelationship(Collections.singletonList(buildCodeableConcept(roleDisplay,
		    relationship.getRelationshipType())));
		
		if (relatedPerson.getIsPatient()) {
			org.openmrs.Patient relatedPatient = patientService.getPatient(relatedPerson.getPersonId());
			if (relatedPatient != null) {
				Reference ref = patientReferenceTranslator.toFhirResource(relatedPatient);
				fhirRelatedPerson.addExtension(new Extension(RELATED_PATIENT_EXT_URL, ref));
			}
		}
		
		return fhirRelatedPerson;
	}
	
	@Override
	public Relationship toOpenmrsType(@Nonnull RelatedPerson relatedPerson) {
		Relationship relationship = new Relationship();

		if (relatedPerson.hasPatient()) {
			String focalUuid = extractUuid(relatedPerson.getPatient().getReference());
			relationship.setPersonB(personService.getPersonByUuid(focalUuid));
		}

		relatedPerson.getExtensionsByUrl(RELATED_PATIENT_EXT_URL).stream()
		        .findFirst()
		        .ifPresent(ext -> {
			        String relatedUuid = extractUuid(((Reference) ext.getValue()).getReference());
			        relationship.setPersonA(personService.getPersonByUuid(relatedUuid));
		        });

		resolveRelationshipType(relatedPerson, relationship);

		if (relatedPerson.hasPeriod()) {
			relationship.setStartDate(relatedPerson.getPeriod().getStart());
			relationship.setEndDate(relatedPerson.getPeriod().getEnd());
		}

		return relationship;
	}
	
	@Override
	public Relationship toOpenmrsType(@Nonnull Relationship existing, @Nonnull RelatedPerson relatedPerson) {
		notNull(existing, "existing Relationship must not be null");
		
		if (relatedPerson.hasPeriod()) {
			existing.setStartDate(relatedPerson.getPeriod().getStart());
			existing.setEndDate(relatedPerson.getPeriod().getEnd());
		}
		
		resolveRelationshipType(relatedPerson, existing);
		
		return existing;
	}
	
	private void resolveRelationshipType(RelatedPerson relatedPerson, Relationship relationship) {
		if (relatedPerson.hasRelationship()) {
			relatedPerson.getRelationship().stream()
			        .flatMap(cc -> cc.getCoding().stream())
			        .filter(c -> RELATIONSHIP_TYPE_SYSTEM.equals(c.getSystem()))
			        .findFirst()
			        .ifPresent(coding -> {
				        RelationshipType type = personService.getRelationshipTypeByUuid(coding.getCode());
				        if (type != null) {
					        relationship.setRelationshipType(type);
				        }
			        });
		}
	}
	
	private CodeableConcept buildCodeableConcept(String display, RelationshipType type) {
		CodeableConcept concept = new CodeableConcept();
		concept.addCoding(new Coding().setSystem(RELATIONSHIP_TYPE_SYSTEM).setCode(type.getUuid()).setDisplay(display));
		concept.setText(display);
		return concept;
	}
	
	private boolean isActive(Relationship relationship) {
		Date now = new Date();
		Date start = relationship.getStartDate();
		Date end = relationship.getEndDate();
		boolean started = start == null || !start.after(now);
		boolean notEnded = end == null || end.after(now);
		return started && notEnded;
	}
	
	private Period buildPeriod(Relationship relationship) {
		if (relationship.getStartDate() == null && relationship.getEndDate() == null) {
			return null;
		}
		Period period = new Period();
		period.setStart(relationship.getStartDate());
		period.setEnd(relationship.getEndDate());
		return period;
	}
	
	private String extractUuid(String reference) {
		if (reference == null)
			return null;
		int slash = reference.lastIndexOf('/');
		return slash >= 0 ? reference.substring(slash + 1) : reference;
	}
}
