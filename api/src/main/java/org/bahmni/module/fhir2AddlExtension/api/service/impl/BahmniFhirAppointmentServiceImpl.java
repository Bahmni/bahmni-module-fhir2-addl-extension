package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.api.PatchTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirAppointmentDao;
import org.bahmni.module.fhir2AddlExtension.api.search.param.BahmniAppointmentSearchParams;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirAppointmentService;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniFhirAppointmentTranslator;
import org.hl7.fhir.r4.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentStatus;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.impl.BaseFhirService;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

@Component
@Primary
@Transactional
@Slf4j
public class BahmniFhirAppointmentServiceImpl extends BaseFhirService<Appointment, org.openmrs.module.appointments.model.Appointment> implements BahmniFhirAppointmentService {
	
	private static final String PATIENT_REFERENCE_OR_RES_ID_MUST_BE_SPECIFIED = "You must specify patient reference, resource id, or status!";
	
	private final BahmniFhirAppointmentDao appointmentDao;
	
	private final BahmniFhirAppointmentTranslator appointmentTranslator;
	
	private final SearchQueryInclude<Appointment> searchQueryInclude;
	
	private final SearchQuery<org.openmrs.module.appointments.model.Appointment, Appointment, BahmniFhirAppointmentDao, BahmniFhirAppointmentTranslator, SearchQueryInclude<Appointment>> searchQuery;
	
	@Autowired
	public BahmniFhirAppointmentServiceImpl(
	    BahmniFhirAppointmentDao appointmentDao,
	    BahmniFhirAppointmentTranslator appointmentTranslator,
	    SearchQueryInclude<Appointment> searchQueryInclude,
	    SearchQuery<org.openmrs.module.appointments.model.Appointment, Appointment, BahmniFhirAppointmentDao, BahmniFhirAppointmentTranslator, SearchQueryInclude<Appointment>> searchQuery) {
		this.appointmentDao = appointmentDao;
		this.appointmentTranslator = appointmentTranslator;
		this.searchQueryInclude = searchQueryInclude;
		this.searchQuery = searchQuery;
	}
	
	@Override
	public Appointment get(@Nonnull String uuid) {
		return super.get(uuid);
	}
	
	@Override
	public List<Appointment> get(@Nonnull Collection<String> uuids) {
		return super.get(uuids);
	}
	
	@Override
	@Transactional(readOnly = true)
	public IBundleProvider searchAppointments(BahmniAppointmentSearchParams searchParams) {
		// Validation: at least one search parameter required
		if (!searchParams.hasPatientReference() && !searchParams.hasId() && !searchParams.hasStatus()
		        && !searchParams.hasDate()) {
			log.error("Missing search parameters for appointment search");
			throw new UnsupportedOperationException(PATIENT_REFERENCE_OR_RES_ID_MUST_BE_SPECIFIED);
		}
		return searchQuery.getQueryResults(searchParams.toSearchParameterMap(), appointmentDao, appointmentTranslator,
		    searchQueryInclude);
	}
	
	@Override
	protected FhirDao<org.openmrs.module.appointments.model.Appointment> getDao() {
		return appointmentDao;
	}
	
	@Override
	protected OpenmrsFhirTranslator<org.openmrs.module.appointments.model.Appointment, Appointment> getTranslator() {
		return appointmentTranslator;
	}
	
	@Override
	public Appointment create(@Nonnull Appointment appointment) {
		throw new UnsupportedOperationException("Appointment resource is read-only via FHIR API");
	}
	
	@Override
	public Appointment update(@Nonnull String uuid, @Nonnull Appointment appointment) {
		throw new UnsupportedOperationException("Appointment resource is read-only via FHIR API");
	}
	
	@Override
	public Appointment patch(@Nonnull String uuid, @Nonnull PatchTypeEnum patchType, @Nonnull String body,
	        RequestDetails requestDetails) {
		throw new UnsupportedOperationException("Appointment resource is read-only via FHIR API");
	}
	
	@Override
	public void delete(@Nonnull String uuid) {
		throw new UnsupportedOperationException("Appointment resource is read-only via FHIR API");
	}
}
