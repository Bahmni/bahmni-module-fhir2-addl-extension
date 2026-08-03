package org.bahmni.module.fhir2addlextension.advice;

import org.ict4h.atomfeed.server.service.EventService;
import org.ict4h.atomfeed.transaction.AFTransactionWork;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.atomfeed.transaction.support.AtomFeedSpringTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class BaseAdviceTest {
	
	@Mock
	private AtomFeedSpringTransactionManager atomFeedSpringTransactionManager;
	
	@Mock
	private EventService eventService;
	
	@Mock
	private PlatformTransactionManager platformTransactionManager;
	
	private TestFhirSaveAdvice advice;
	
	@Before
	public void setUp() {
		advice = new TestFhirSaveAdvice();
	}
	
	@Test
	public void shouldBuildAtomfeedDependenciesOnInit() throws Exception {
		setField(advice, "platformTransactionManager", platformTransactionManager);
		
		advice.init();
		
		assertNotNull(getField(advice, "atomFeedSpringTransactionManager"));
		assertNotNull(getField(advice, "eventService"));
	}
	
	@Test
	public void shouldNotifyEventServiceWithinATransaction() throws Exception {
		setField(advice, "atomFeedSpringTransactionManager", atomFeedSpringTransactionManager);
		setField(advice, "eventService", eventService);
		doAnswer(invocation -> {
			AFTransactionWork<?> work = invocation.getArgument(0);
			return work.execute();
		}).when(atomFeedSpringTransactionManager).executeWithTransaction(any());

		advice.raiseEvent("Patient", "patient", null, "/openmrs/ws/rest/v1/patient/test-uuid?v=full");

		verify(eventService).notify(any());
	}
	
	private static class TestFhirSaveAdvice extends BaseAdvice {
		
		@Override
		public void afterReturning(Object returnValue, Method method, Object[] args, Object target) {
		}
	}
	
	private void setField(Object target, String fieldName, Object value) throws Exception {
		findField(target.getClass(), fieldName).set(target, value);
	}
	
	private Object getField(Object target, String fieldName) throws Exception {
		return findField(target.getClass(), fieldName).get(target);
	}
	
	private Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
		while (type != null) {
			try {
				Field field = type.getDeclaredField(fieldName);
				field.setAccessible(true);
				return field;
			}
			catch (NoSuchFieldException e) {
				type = type.getSuperclass();
			}
		}
		throw new NoSuchFieldException(fieldName);
	}
}
