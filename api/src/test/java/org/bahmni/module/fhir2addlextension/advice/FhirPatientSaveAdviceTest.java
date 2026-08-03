package org.bahmni.module.fhir2addlextension.advice;

import org.hl7.fhir.r4.model.Patient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.atomfeed.transaction.support.AtomFeedSpringTransactionManager;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class FhirPatientSaveAdviceTest {
	
	@Mock
	private AtomFeedSpringTransactionManager atomFeedSpringTransactionManager;
	
	private FhirPatientSaveAdvice advice;
	
	@Before
	public void setUp() throws Exception {
		advice = new FhirPatientSaveAdvice();
		setField(advice, "atomFeedSpringTransactionManager", atomFeedSpringTransactionManager);
	}
	
	@Test
	public void shouldNotRaiseEventWhenPatientIsNull() throws Throwable {
		advice.afterReturning(null, null, null, null);
		
		verify(atomFeedSpringTransactionManager, never()).executeWithTransaction(any());
	}
	
	@Test
	public void shouldRaiseEventWhenPatientIsReturned() throws Throwable {
		Patient patient = new Patient();
		patient.setId("test-uuid");
		
		advice.afterReturning(patient, null, null, null);
		
		verify(atomFeedSpringTransactionManager).executeWithTransaction(any());
	}
	
	private void setField(Object target, String fieldName, Object value) throws Exception {
		Class<?> type = target.getClass();
		while (type != null) {
			try {
				Field field = type.getDeclaredField(fieldName);
				field.setAccessible(true);
				field.set(target, value);
				return;
			}
			catch (NoSuchFieldException e) {
				type = type.getSuperclass();
			}
		}
		throw new NoSuchFieldException(fieldName);
	}
}
