package org.bahmni.module.fhir2addlextension.advice;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.atomfeed.transaction.support.AtomFeedSpringTransactionManager;
import org.openmrs.module.fhir2.FhirConstants;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FhirEncounterSaveAdviceTest {
	
	@Mock
	private AtomFeedSpringTransactionManager atomFeedSpringTransactionManager;
	
	@Mock
	private AdministrationService administrationService;
	
	private FhirEncounterSaveAdvice advice;
	
	@Before
	public void setUp() throws Exception {
		advice = new FhirEncounterSaveAdvice();
		setField(advice, "atomFeedSpringTransactionManager", atomFeedSpringTransactionManager);
	}
	
	@Test
	public void shouldNotRaiseEventWhenEncounterIsNull() throws Throwable {
		advice.afterReturning(null, null, null, null);
		verify(atomFeedSpringTransactionManager, never()).executeWithTransaction(any());
	}
	
	@Test
	public void shouldNotRaiseEventWhenEncounterHasNoTypeCoding() throws Throwable {
		advice.afterReturning(new Encounter(), null, null, null);
		verify(atomFeedSpringTransactionManager, never()).executeWithTransaction(any());
	}
	
	@Test
	public void shouldNotRaiseEventWhenEncounterIsVisitType() throws Throwable {
		advice.afterReturning(encounterWithCoding(FhirConstants.VISIT_TYPE_SYSTEM_URI), null, null, null);
		verify(atomFeedSpringTransactionManager, never()).executeWithTransaction(any());
	}
	
	@Test
	public void shouldNotRaiseEventWhenEncounterTypeIsAmbiguous() throws Throwable {
		Encounter encounter = new Encounter();
		encounter.setId("test-uuid");
		CodeableConcept type = new CodeableConcept();
		type.addCoding(new Coding(FhirConstants.ENCOUNTER_TYPE_SYSTEM_URI, "enc-code", "Encounter Type"));
		type.addCoding(new Coding(FhirConstants.VISIT_TYPE_SYSTEM_URI, "vis-code", "Visit Type"));
		encounter.addType(type);
		
		advice.afterReturning(encounter, null, null, null);
		
		verify(atomFeedSpringTransactionManager, never()).executeWithTransaction(any());
	}
	
	@Test
	public void shouldRaiseEventWhenEncounterIsEncounterType() throws Throwable {
		Encounter encounter = encounterWithCoding(FhirConstants.ENCOUNTER_TYPE_SYSTEM_URI);
		try (MockedStatic<Context> mockedContext = Mockito.mockStatic(Context.class)) {
			mockedContext.when(Context::getAdministrationService).thenReturn(administrationService);
			when(administrationService.getGlobalProperty("encounter.feed.publish.url")).thenReturn("http://example.com/%s");

			advice.afterReturning(encounter, null, null, null);

			verify(atomFeedSpringTransactionManager).executeWithTransaction(any());
		}
	}
	
	private Encounter encounterWithCoding(String systemUri) {
		Encounter encounter = new Encounter();
		encounter.setId("test-uuid");
		CodeableConcept type = new CodeableConcept();
		type.addCoding(new Coding(systemUri, "some-code", "Some Type"));
		encounter.addType(type);
		return encounter;
	}
	
	private void setField(Object target, String fieldName, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}
}
