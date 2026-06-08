package org.bahmni.module.fhir2addlextension.api.service.impl;

import javax.annotation.Nonnull;

import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.api.impl.FhirAllergyIntoleranceServiceImpl;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class BahmniFhirAllergyIntoleranceServiceImpl extends FhirAllergyIntoleranceServiceImpl {
	
	@Override
	public void delete(@Nonnull String uuid) {
		super.delete(uuid);
		// Flush so the voided allergy is visible to the allergyapi duplicate-allergen
		// DB check when a replacement POST follows in the same transaction (e.g. bundle update).
		Context.flushSession();
	}
}
