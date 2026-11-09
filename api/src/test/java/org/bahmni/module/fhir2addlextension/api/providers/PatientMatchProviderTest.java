package org.bahmni.module.fhir2addlextension.api.providers;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2addlextension.api.service.PatientMatchService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.fhir2.api.translators.PatientTranslator;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PatientMatchProviderTest {
	
	@Mock
	private PatientMatchService matchService;
	
	@Mock
	private PatientTranslator patientTranslator;
	
	@Mock
	private RequestDetails requestDetails;
	
	private PatientMatchProvider provider;
	
	@Before
	public void setUp() {
		when(requestDetails.getFhirServerBase()).thenReturn("http://localhost/openmrs/ws/fhir2/R4");
		provider = new PatientMatchProvider(matchService, patientTranslator);
	}
	
	@Test
	public void shouldReturnSearchsetBundleWithMatchingPatients() {
		Patient request = patient("Sasirekha", "P");
		request.setGender(Enumerations.AdministrativeGender.FEMALE);
		request.setBirthDateElement(new DateType("2002-02-11"));
		org.openmrs.Patient openmrsPatient = new org.openmrs.Patient();
		Patient fhirPatient = new Patient();
		fhirPatient.setId("uuid-1");
		when(matchService.findMatches(request)).thenReturn(Collections.singletonList(openmrsPatient));
		when(patientTranslator.toFhirResource(openmrsPatient)).thenReturn(fhirPatient);
		
		Bundle bundle = provider.match(request, requestDetails);
		
		assertThat(bundle.getType(), equalTo(Bundle.BundleType.SEARCHSET));
		assertThat(bundle.getTotal(), equalTo(1));
		assertThat(bundle.getEntry(), hasSize(1));
		assertThat(bundle.getEntryFirstRep().getFullUrl(), equalTo("http://localhost/openmrs/ws/fhir2/R4/Patient/uuid-1"));
		assertThat(bundle.getEntryFirstRep().getResource(), equalTo(fhirPatient));
		assertThat(bundle.getEntryFirstRep().getSearch().getMode(), equalTo(Bundle.SearchEntryMode.MATCH));
	}
	
	@Test
	public void shouldReturnEmptyBundleWhenNoMatches() {
		when(matchService.findMatches(any())).thenReturn(Collections.emptyList());
		
		Bundle bundle = provider.match(patient("Nobody", "Here"), requestDetails);
		
		assertThat(bundle.getTotal(), equalTo(0));
		assertThat(bundle.getEntry(), hasSize(0));
	}
	
	@Test(expected = InvalidRequestException.class)
	public void shouldRejectMissingResource() {
		provider.match(null, requestDetails);
	}
	
	@Test
	public void shouldDeclareResourceTypeAsPatient() {
		assertThat(provider.getResourceType().equals(Patient.class), equalTo(true));
	}
	
	private Patient patient(String given, String family) {
		Patient patient = new Patient();
		patient.addName().addGiven(given).setFamily(family);
		patient.setGender(Enumerations.AdministrativeGender.MALE);
		return patient;
	}
}
