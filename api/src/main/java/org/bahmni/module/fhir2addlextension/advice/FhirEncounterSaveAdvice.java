package org.bahmni.module.fhir2addlextension.advice;

import org.hl7.fhir.r4.model.Encounter;
import org.bahmni.module.eventoutbox.EMREvent;
import org.openmrs.api.context.Context;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
public class FhirEncounterSaveAdvice implements AfterReturningAdvice, ApplicationEventPublisherAware {
	
	public static final String TITLE = "Encounter";
	
	public static final String CATEGORY = "Encounter";
	
	private ApplicationEventPublisher eventPublisher;
	
	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}
	
	@Override
    public void afterReturning(Object returnValue, Method method, Object[] args, Object emrEncounterService)
            throws Throwable {

        if (!(returnValue instanceof Encounter)) {
            return;
        }
        Encounter encounter = (Encounter) returnValue;
        String encounterUuid = encounter.getId();
        String encounterFeedUrl = getEncounterFeedUrl();
        String url = String.format(encounterFeedUrl, encounterUuid);

        EMREvent<Encounter> event = new EMREvent<>(encounter, CATEGORY, TITLE, null, url);
        eventPublisher.publishEvent(event);
    }
	
	private String getEncounterFeedUrl() {
		return Context.getAdministrationService().getGlobalProperty("bahmnievents.encounter.feed.publish.url");
	}
}
