package org.bahmni.module.fhir2addlextension.advice;

import org.hl7.fhir.r4.model.Encounter;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.api.util.FhirUtils;

import java.util.Optional;

public class FhirEncounterSaveAdvice extends BaseAdvice {
	
	public static final String TITLE = "Encounter";
	
	public static final String CATEGORY = "Encounter";
	
	@Override
	protected boolean shouldRaiseEvent(Object returnValue) {
		Encounter encounter = (Encounter) returnValue;
		if (encounter == null) {
			return false;
		}
		Optional<FhirUtils.OpenmrsEncounterType> encounterResourceType = FhirUtils.getOpenmrsEncounterType(encounter);
		return encounterResourceType.isPresent() && encounterResourceType.get() == FhirUtils.OpenmrsEncounterType.ENCOUNTER;
	}
	
	@Override
	protected String getUrl(Object returnValue) {
		Encounter encounter = (Encounter) returnValue;
		String encounterFeedUrl = getEncounterFeedUrl();
		return String.format(encounterFeedUrl, encounter.getId());
	}
	
	@Override
	protected String getTitle() {
		return TITLE;
	}
	
	@Override
	protected String getCategory() {
		return CATEGORY;
	}
	
	private String getEncounterFeedUrl() {
		return Context.getAdministrationService().getGlobalProperty("encounter.feed.publish.url");
	}
}
