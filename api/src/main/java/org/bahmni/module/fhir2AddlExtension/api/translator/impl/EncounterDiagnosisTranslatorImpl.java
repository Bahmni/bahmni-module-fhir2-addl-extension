package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import lombok.AccessLevel;
import lombok.Setter;
import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Condition;
import org.openmrs.*;
import org.openmrs.Encounter;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.translators.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

import java.util.Collections;
import java.util.Optional;

import static org.apache.commons.lang3.Validate.notNull;
import static org.openmrs.module.fhir2.api.translators.impl.FhirTranslatorUtils.getLastUpdated;
import static org.openmrs.module.fhir2.api.translators.impl.FhirTranslatorUtils.getVersionId;

@Component
@Setter(AccessLevel.PACKAGE)
public class EncounterDiagnosisTranslatorImpl implements ConditionTranslator<Diagnosis> {
	
	@Autowired
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Autowired
	private ConditionVerificationStatusTranslator<ConditionVerificationStatus> verificationStatusTranslator;
	
	@Autowired
	private PractitionerReferenceTranslator<User> practitionerReferenceTranslator;
	
	@Autowired
	private ConceptTranslator conceptTranslator;
	
	@Autowired
	private EncounterReferenceTranslator<Encounter> encounterReferenceTranslator;
	
	@Override
	public Condition toFhirResource(@Nonnull Diagnosis diagnosis) {
		notNull(diagnosis, "The Openmrs diagnosis object should not be null");
		
		Condition fhirCondition = new Condition();
		fhirCondition.setCategory(Collections.singletonList(createCategoryCodeableConcept()));
		fhirCondition.setId(diagnosis.getUuid());
		fhirCondition.setSubject(patientReferenceTranslator.toFhirResource(diagnosis.getPatient()));
		fhirCondition.setVerificationStatus(verificationStatusTranslator.toFhirResource(diagnosis.getCertainty()));
		
		CodedOrFreeText codedOrFreeTextCondition = diagnosis.getDiagnosis();
		if (codedOrFreeTextCondition != null) {
			fhirCondition.setCode(conceptTranslator.toFhirResource(codedOrFreeTextCondition.getCoded()));
			if (codedOrFreeTextCondition.getNonCoded() != null) {
				Extension extension = new Extension();
				extension.setUrl(FhirConstants.OPENMRS_FHIR_EXT_NON_CODED_CONDITION);
				extension.setValue(new StringType(codedOrFreeTextCondition.getNonCoded()));
				fhirCondition.addExtension(extension);
			}
		}
		fhirCondition.setEncounter(encounterReferenceTranslator.toFhirResource(diagnosis.getEncounter()));
		
		fhirCondition.setRecorder(practitionerReferenceTranslator.toFhirResource(diagnosis.getCreator()));
		fhirCondition.setRecordedDate(diagnosis.getDateCreated());
		
		fhirCondition.getMeta().setLastUpdated(getLastUpdated(diagnosis));
		fhirCondition.getMeta().setVersionId(getVersionId(diagnosis));
		
		return fhirCondition;
	}
	
	@Override
	public Diagnosis toOpenmrsType(@Nonnull Condition condition) {
		notNull(condition, "The Condition object should not be null");
		return this.toOpenmrsType(new Diagnosis(), condition);
	}
	
	@Override
	public Diagnosis toOpenmrsType(@Nonnull Diagnosis existingDiagnosis, @Nonnull Condition condition) {
		notNull(existingDiagnosis, "The existing Openmrs Condition object should not be null");
		notNull(condition, "The Condition object should not be null");

		if (condition.hasId()) {
			existingDiagnosis.setUuid(condition.getIdElement().getIdPart());
		}

		//TODO: Implement an extension to capture rank
		existingDiagnosis.setRank(1);

		existingDiagnosis.setPatient(patientReferenceTranslator.toOpenmrsType(condition.getSubject()));
		existingDiagnosis
				.setCertainty(verificationStatusTranslator.toOpenmrsType(condition.getVerificationStatus()));

		CodeableConcept codeableConcept = condition.getCode();
		CodedOrFreeText conditionCodedOrText = new CodedOrFreeText();
		Optional<Extension> extension = Optional
				.ofNullable(condition.getExtensionByUrl(FhirConstants.OPENMRS_FHIR_EXT_NON_CODED_CONDITION));
		extension.ifPresent(value -> conditionCodedOrText.setNonCoded(String.valueOf(value.getValue())));
		conditionCodedOrText.setCoded(conceptTranslator.toOpenmrsType(codeableConcept));
		existingDiagnosis.setDiagnosis(conditionCodedOrText);
		existingDiagnosis.setEncounter(encounterReferenceTranslator.toOpenmrsType(condition.getEncounter()));
		existingDiagnosis.setCreator(practitionerReferenceTranslator.toOpenmrsType(condition.getRecorder()));

		return existingDiagnosis;
	}
	
	private CodeableConcept createCategoryCodeableConcept() {
		CodeableConcept codeableConcept = new CodeableConcept();
		Coding coding = new Coding();
		coding.setSystem(BahmniFhirConstants.HL7_CONDITION_CATEGORY_CODE_SYSTEM);
		coding.setCode(BahmniFhirConstants.HL7_CONDITION_CATEGORY_DIAGNOSIS_CODE);
		codeableConcept.addCoding(coding);
		return codeableConcept;
	}
}
