package org.bahmni.module.fhir2AddlExtension.api.service.impl;

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
		assertThrows(UnsupportedOperationException.class, () -> {
			appointmentService.create(new Appointment());
		});
	}
	
	@Test
	public void shouldThrowExceptionOnUpdateAttempt() {
		assertThrows(UnsupportedOperationException.class, () -> {
			appointmentService.update("test-uuid", new Appointment());
		});
	}
	
	@Test
	public void shouldThrowExceptionOnDeleteAttempt() {
		assertThrows(UnsupportedOperationException.class, () -> {
			appointmentService.delete("test-uuid");
		});
	}
	
	@Test
	public void shouldThrowExceptionOnPatchAttempt() {
		assertThrows(UnsupportedOperationException.class, () -> {
			appointmentService.patch("test-uuid", null, "", null);
		});
	}
}
