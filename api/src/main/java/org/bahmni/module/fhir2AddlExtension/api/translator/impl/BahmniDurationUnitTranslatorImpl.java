package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.hl7.fhir.r4.model.Timing;
import org.openmrs.Concept;
import org.openmrs.ConceptSource;
import org.openmrs.Duration;
import org.openmrs.module.fhir2.api.FhirConceptService;
import org.openmrs.module.fhir2.api.FhirConceptSourceService;
import org.openmrs.module.fhir2.api.translators.impl.DurationUnitTranslatorImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Primary
public class BahmniDurationUnitTranslatorImpl extends DurationUnitTranslatorImpl {
	
	@Autowired
	FhirConceptService fhirConceptService;
	
	@Autowired
	FhirConceptSourceService fhirConceptSourceService;
	
	private static Map<String, Timing.UnitsOfTime> codeMap;
	
	static {
		codeMap = new HashMap<>();
		codeMap.put(Duration.SNOMED_CT_SECONDS_CODE, Timing.UnitsOfTime.S);
		codeMap.put(Duration.SNOMED_CT_MINUTES_CODE, Timing.UnitsOfTime.MIN);
		codeMap.put(Duration.SNOMED_CT_HOURS_CODE, Timing.UnitsOfTime.H);
		codeMap.put(Duration.SNOMED_CT_DAYS_CODE, Timing.UnitsOfTime.D);
		codeMap.put(Duration.SNOMED_CT_WEEKS_CODE, Timing.UnitsOfTime.WK);
		codeMap.put(Duration.SNOMED_CT_MONTHS_CODE, Timing.UnitsOfTime.MO);
		codeMap.put(Duration.SNOMED_CT_YEARS_CODE, Timing.UnitsOfTime.A);
	}
	
	@Override
	public Concept toOpenmrsType(@Nonnull Timing.UnitsOfTime unitsOfTime) {
		Optional<ConceptSource> conceptSource = fhirConceptSourceService
		        .getConceptSourceByHl7Code(Duration.SNOMED_CT_CONCEPT_SOURCE_HL7_CODE);
		if (!conceptSource.isPresent()) {
			return null;
		}
		
		for (String durationCode : codeMap.keySet()) {
			Timing.UnitsOfTime units = codeMap.get(durationCode);
			if (units == unitsOfTime) {
				return fhirConceptService.getConceptWithSameAsMappingInSource(conceptSource.get(), durationCode)
				        .orElse(null);
			}
		}
		return null;
	}
}
