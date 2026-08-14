package org.bahmni.module.fhir2addlextension.advice;

import org.ict4h.atomfeed.server.repository.AllEventRecordsQueue;
import org.ict4h.atomfeed.server.repository.jdbc.AllEventRecordsQueueJdbcImpl;
import org.ict4h.atomfeed.server.service.Event;
import org.ict4h.atomfeed.server.service.EventService;
import org.ict4h.atomfeed.server.service.EventServiceImpl;
import org.ict4h.atomfeed.transaction.AFTransactionWorkWithoutResult;
import org.openmrs.module.atomfeed.transaction.support.AtomFeedSpringTransactionManager;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;

public abstract class BaseAdvice implements AfterReturningAdvice {
	
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
	public final void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
		if (!shouldRaiseEvent(returnValue)) {
			return;
		}
		raiseEvent(getTitle(), getCategory(), getUri(returnValue), getUrl(returnValue));
	}
	
	protected abstract boolean shouldRaiseEvent(Object returnValue);
	
	protected abstract String getUrl(Object returnValue);
	
	protected abstract String getTitle();
	
	protected abstract String getCategory();
	
	protected URI getUri(Object returnValue) {
		return null;
	}
	
	protected void raiseEvent(String title, String category, URI uri, String url) {
		final Event event = new Event(UUID.randomUUID().toString(), title, LocalDateTime.now(), uri, url, category);
		
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
