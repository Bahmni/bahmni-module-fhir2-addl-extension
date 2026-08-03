package org.bahmni.module.fhir2addlextension.advice;

import org.hl7.fhir.r4.model.Patient;
import org.ict4h.atomfeed.server.repository.AllEventRecordsQueue;
import org.ict4h.atomfeed.server.repository.jdbc.AllEventRecordsQueueJdbcImpl;
import org.ict4h.atomfeed.server.service.Event;
import org.ict4h.atomfeed.server.service.EventService;
import org.ict4h.atomfeed.server.service.EventServiceImpl;
import org.ict4h.atomfeed.transaction.AFTransactionWorkWithoutResult;
import org.openmrs.module.atomfeed.transaction.support.AtomFeedSpringTransactionManager;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class FhirPatientSaveAdvice implements AfterReturningAdvice {
	
	private static final String TEMPLATE = "/openmrs/ws/rest/v1/patient/%s?v=full";
	
	public static final String TITLE = "Patient";
	
	public static final String CATEGORY = "patient";
	
	@Autowired
	private PlatformTransactionManager platformTransactionManager;
	
	private AtomFeedSpringTransactionManager atomFeedSpringTransactionManager;
	
	private EventService eventService;
	
	@PostConstruct
	public void init() {
		atomFeedSpringTransactionManager = new AtomFeedSpringTransactionManager(platformTransactionManager);
		AllEventRecordsQueue allEventRecordsQueue = new AllEventRecordsQueueJdbcImpl(atomFeedSpringTransactionManager);
		this.eventService = new EventServiceImpl(allEventRecordsQueue);
	}
	
	@Override
	public void afterReturning(Object returnValue, Method method, Object[] args, Object emrPatientService) throws Throwable {
		Patient patient = (Patient) returnValue;
		if (patient == null) {
			return;
		}
		String contents = String.format(TEMPLATE, patient.getIdElement().getIdPart());
		final Event event = new Event(UUID.randomUUID().toString(), TITLE, LocalDateTime.now(), (URI) null, contents,
		        CATEGORY);
		
		atomFeedSpringTransactionManager.executeWithTransaction(new AFTransactionWorkWithoutResult() {
			
			@Override
			protected void doInTransaction() {
				eventService.notify(event);
			}
			
			@Override
			public PropagationDefinition getTxPropagationDefinition() {
				return PropagationDefinition.PROPAGATION_REQUIRED;
			}
		});
	}
}
