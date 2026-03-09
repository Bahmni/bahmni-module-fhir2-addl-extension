package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirAppointmentDao;
import org.bahmni.module.fhir2AddlExtension.api.search.param.BahmniAppointmentSearchParams;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniFhirAppointmentTranslator;
import org.hl7.fhir.r4.model.Appointment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirAppointmentServiceImplTest {
	
	@Mock
	private BahmniFhirAppointmentDao appointmentDao;
	
	@Mock
	private BahmniFhirAppointmentTranslator appointmentTranslator;
	
	@Mock
	private SearchQueryInclude<Appointment> searchQueryInclude;
	
	@Mock
	private SearchQuery<org.openmrs.module.appointments.model.Appointment, Appointment, BahmniFhirAppointmentDao, BahmniFhirAppointmentTranslator, SearchQueryInclude<Appointment>> searchQuery;
	
	private BahmniFhirAppointmentServiceImpl appointmentService;
	
	@Before
	public void setUp() {
		appointmentService = new BahmniFhirAppointmentServiceImpl(appointmentDao, appointmentTranslator, searchQueryInclude,
		        searchQuery);
	}
	
	@Test
	public void shouldThrowExceptionWhenNoSearchParametersProvided() {
		BahmniAppointmentSearchParams emptySearchParams = new BahmniAppointmentSearchParams();

		assertThrows(UnsupportedOperationException.class, () -> {
			appointmentService.searchAppointments(emptySearchParams);
		});
	}
	
	@Test
	public void shouldThrowExceptionOnCreateAttempt() {
		assertThrows(UnsupportedOperationException.class, this::callCreate);
	}
	
	private void callCreate() {
		appointmentService.create(new Appointment());
	}
	
	@Test
	public void shouldThrowExceptionOnUpdateAttempt() {
		assertThrows(UnsupportedOperationException.class, this::callUpdate);
	}
	
	private void callUpdate() {
		appointmentService.update("test-uuid", new Appointment());
	}
	
	@Test
	public void shouldThrowExceptionOnDeleteAttempt() {
		assertThrows(UnsupportedOperationException.class, this::callDelete);
	}
	
	private void callDelete() {
		appointmentService.delete("test-uuid");
	}
	
	@Test
	public void shouldThrowExceptionOnPatchAttempt() {
		assertThrows(UnsupportedOperationException.class, this::callPatch);
	}
	
	private void callPatch() {
		appointmentService.patch("test-uuid", null, "", null);
	}
	
	@Test
	public void shouldSearchAppointmentsWithValidPatientReference() {
		BahmniAppointmentSearchParams validSearchParams = new BahmniAppointmentSearchParams();
		ReferenceAndListParam patientRef = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam("Patient", "patient-uuid"));
		patientRef.addAnd(orListParam);
		validSearchParams.setPatientReference(patientRef);
		
		IBundleProvider expectedResult = null;
		
		when(
		    searchQuery.getQueryResults(validSearchParams.toSearchParameterMap(), appointmentDao, appointmentTranslator,
		        searchQueryInclude)).thenReturn(expectedResult);
		
		IBundleProvider result = appointmentService.searchAppointments(validSearchParams);
		
		assertEquals("Should return result from searchQuery", expectedResult, result);
	}
	
	@Test
	public void shouldSearchAppointmentsWithValidStatus() {
		BahmniAppointmentSearchParams validSearchParams = new BahmniAppointmentSearchParams();
		TokenAndListParam status = new TokenAndListParam();
		TokenOrListParam orListParam = new TokenOrListParam();
		orListParam.add(new TokenParam("booked"));
		status.addAnd(orListParam);
		validSearchParams.setStatus(status);
		
		IBundleProvider expectedResult = null;
		
		when(
		    searchQuery.getQueryResults(validSearchParams.toSearchParameterMap(), appointmentDao, appointmentTranslator,
		        searchQueryInclude)).thenReturn(expectedResult);
		
		IBundleProvider result = appointmentService.searchAppointments(validSearchParams);
		
		assertEquals("Should return result from searchQuery", expectedResult, result);
	}
	
	@Test
	public void shouldSearchAppointmentsWithValidDateRange() {
		BahmniAppointmentSearchParams validSearchParams = new BahmniAppointmentSearchParams();
		validSearchParams.setDate(new DateRangeParam("2026-01-01", "2026-12-31"));
		
		IBundleProvider expectedResult = null;
		
		when(
		    searchQuery.getQueryResults(validSearchParams.toSearchParameterMap(), appointmentDao, appointmentTranslator,
		        searchQueryInclude)).thenReturn(expectedResult);
		
		IBundleProvider result = appointmentService.searchAppointments(validSearchParams);
		
		assertEquals("Should return result from searchQuery", expectedResult, result);
	}
}
