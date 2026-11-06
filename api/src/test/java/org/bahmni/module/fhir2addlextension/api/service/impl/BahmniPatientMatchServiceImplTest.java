package org.bahmni.module.fhir2addlextension.api.service.impl;

import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.HumanName;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.api.translators.impl.GenderTranslatorImpl;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BahmniPatientMatchServiceImplTest {
	
	@Mock
	private PersonService personService;
	
	@Mock
	private PatientService patientService;
	
	private MockedStatic<Context> contextMock;
	
	private final BahmniPatientMatchServiceImpl service = new BahmniPatientMatchServiceImpl(new GenderTranslatorImpl());
	
	@Before
	public void setUp() {
		contextMock = Mockito.mockStatic(Context.class);
		contextMock.when(Context::getPersonService).thenReturn(personService);
		contextMock.when(Context::getPatientService).thenReturn(patientService);
	}
	
	@After
	public void tearDown() {
		contextMock.close();
	}
	
	@Test
	public void shouldJoinNamesAndConvertGenderAndYear() throws Exception {
		when(personService.getSimilarPeople(any(), any(), any())).thenReturn(Collections.emptySet());
		
		service.findMatches(fhirPatient(Arrays.asList("Sasirekha", "Devi"), "P", Enumerations.AdministrativeGender.FEMALE,
		    new SimpleDateFormat("yyyy-MM-dd").parse("2002-02-11")));
		
		verify(personService).getSimilarPeople("Sasirekha Devi P", 2002, "F");
	}
	
	@Test
	public void shouldConvertMaleAndOtherGender() {
		when(personService.getSimilarPeople(any(), any(), any())).thenReturn(Collections.emptySet());
		
		service.findMatches(fhirPatient(Collections.singletonList("Akash"), null, Enumerations.AdministrativeGender.MALE,
		    null));
		service.findMatches(fhirPatient(Collections.singletonList("A"), null, Enumerations.AdministrativeGender.OTHER, null));
		
		verify(personService).getSimilarPeople("Akash", null, "M");
		verify(personService).getSimilarPeople("A", null, "O");
	}
	
	@Test
	public void shouldPassNullYearAndGenderWhenAbsent() {
		when(personService.getSimilarPeople(any(), any(), any())).thenReturn(Collections.emptySet());
		
		service.findMatches(fhirPatient(Collections.singletonList("A"), "B", null, null));
		
		verify(personService).getSimilarPeople("A B", null, null);
	}
	
	@Test
	public void shouldSkipPeopleWhoAreNotPatientsAndReturnPatients() {
		Person patientPerson = person(1, true);
		Person nonPatientPerson = person(2, false);
		Patient patient = new Patient();
		when(personService.getSimilarPeople(any(), any(), any())).thenReturn(
		    new LinkedHashSet<>(Arrays.asList(patientPerson, nonPatientPerson)));
		when(patientService.getPatient(1)).thenReturn(patient);

		List<Patient> result = service.findMatches(fhirPatient(Collections.singletonList("A"), "B", null, null));

		assertThat(result, contains(patient));
		verify(patientService, never()).getPatient(2);
	}
	
	@Test
	public void shouldReturnEmptyWhenNoSimilarPersonIsAPatient() {
		Person first = person(1, false);
		Person second = person(2, false);
		when(personService.getSimilarPeople(any(), any(), any())).thenReturn(
		    new LinkedHashSet<>(Arrays.asList(first, second)));

		List<Patient> result = service.findMatches(fhirPatient(Collections.singletonList("A"), "B", null, null));

		assertThat(result, empty());
		verify(patientService, never()).getPatient(any());
	}
	
	@Test
	public void shouldReturnEmptyWhenNoSimilarPeople() {
		when(personService.getSimilarPeople(any(), any(), any())).thenReturn(Collections.emptySet());
		
		assertThat(service.findMatches(fhirPatient(Collections.singletonList("A"), "B", null, null)), empty());
	}
	
	private org.hl7.fhir.r4.model.Patient fhirPatient(List<String> givenNames, String familyName,
	        Enumerations.AdministrativeGender gender, Date birthDate) {
		org.hl7.fhir.r4.model.Patient patient = new org.hl7.fhir.r4.model.Patient();
		HumanName name = patient.addName();
		givenNames.forEach(name::addGiven);
		name.setFamily(familyName);
		patient.setGender(gender);
		patient.setBirthDate(birthDate);
		return patient;
	}
	
	private Person person(int id, boolean isPatient) {
		Person person = Mockito.mock(Person.class);
		when(person.getPersonId()).thenReturn(id);
		when(person.getIsPatient()).thenReturn(isPatient);
		return person;
	}
}
