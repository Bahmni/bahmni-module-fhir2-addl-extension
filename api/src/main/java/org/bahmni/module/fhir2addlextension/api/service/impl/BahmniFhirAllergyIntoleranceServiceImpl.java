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
		// Context.flushSession() flushes ALL pending Hibernate operations in the current
		// persistence context to the DB (without committing the transaction), ensuring the
		// voided AllergyIntolerance is persisted before the allergyapi duplicate-allergen
		// DB check runs on a subsequent POST within the same transaction.
		Context.flushSession();
	}
}
