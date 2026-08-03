package org.bahmni.module.fhir2addlextension.advice;

import org.hl7.fhir.r4.model.Encounter;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.api.util.FhirUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Optional;

@Component
public class FhirEncounterSaveAdvice extends BaseAdvice {
	
	public static final String TITLE = "Encounter";
	
	public static final String CATEGORY = "Encounter";
	
	@Override
	public void afterReturning(Object returnValue, Method method, Object[] args, Object emrEncounterService)
	        throws Throwable {
		Encounter encounter = (Encounter) returnValue;
		if (!shouldRaiseEvent(encounter)) {
			return;
		}
		String encounterUuid = encounter.getId();
		String encounterFeedUrl = getEncounterFeedUrl();
		String url = String.format(encounterFeedUrl, encounterUuid);
		raiseEvent(TITLE, CATEGORY, null, url);
	}
	
	private String getEncounterFeedUrl() {
		return Context.getAdministrationService().getGlobalProperty("encounter.feed.publish.url");
	}
	
	private boolean shouldRaiseEvent(Encounter encounter) {
		if (encounter == null) {
			return false;
		}
		Optional<FhirUtils.OpenmrsEncounterType> encounterResourceType = FhirUtils.getOpenmrsEncounterType(encounter);
		return encounterResourceType.isPresent() && encounterResourceType.get() == FhirUtils.OpenmrsEncounterType.ENCOUNTER;
	}
}
