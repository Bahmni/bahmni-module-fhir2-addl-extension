package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.openmrs.Encounter;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.impl.ConditionTranslatorImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Collections;

@Component
@Primary
public class BahmniConditionTranslatorImpl extends ConditionTranslatorImpl {
	
	@Autowired
	EncounterReferenceTranslator<Encounter> encounterReferenceTranslator;
	
	@Override
	public Condition toFhirResource(@Nonnull org.openmrs.Condition condition) {
		Condition fhirCondition = super.toFhirResource(condition);
		fhirCondition.setCategory(Collections.singletonList(createCategoryCodeableConcept()));
		return fhirCondition;
	}
	
	@Override
	public org.openmrs.Condition toOpenmrsType(@Nonnull org.openmrs.Condition existingCondition,
	        @Nonnull Condition fhirCondition) {
		existingCondition = super.toOpenmrsType(existingCondition, fhirCondition);
		if (fhirCondition.hasEncounter())
			existingCondition.setEncounter(encounterReferenceTranslator.toOpenmrsType(fhirCondition.getEncounter()));
		return existingCondition;
	}
	
	private CodeableConcept createCategoryCodeableConcept() {
		CodeableConcept codeableConcept = new CodeableConcept();
		Coding coding = new Coding();
		coding.setSystem(BahmniFhirConstants.HL7_CONDITION_CATEGORY_CODE_SYSTEM);
		coding.setCode(BahmniFhirConstants.HL7_CONDITION_CATEGORY_CONDITION_CODE);
		codeableConcept.addCoding(coding);
		return codeableConcept;
	}
}
