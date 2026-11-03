package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirAppointmentDao;
import org.bahmni.module.fhir2addlextension.api.translator.AppointmentStatusTranslator;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentStatus;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;
import org.openmrs.module.fhir2.api.dao.internals.OpenmrsFhirCriteriaContext;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.Path;
import java.util.Locale;
import java.util.Optional;

@Component
public class BahmniFhirAppointmentDaoImpl extends BaseFhirDao<org.openmrs.module.appointments.model.Appointment> implements BahmniFhirAppointmentDao {
	
	private static final Logger log = LoggerFactory.getLogger(BahmniFhirAppointmentDaoImpl.class);
	
	private static final String STATUS_PROPERTY = "status";
	
	private static final String DATE_PARAM = "date";
	
	private static final String START_DATE_TIME_PROPERTY = "startDateTime";
	
	private static final String PATIENT_PARAM = "patient";
	
	private final AppointmentStatusTranslator statusTranslator;
	
	@Autowired
	public BahmniFhirAppointmentDaoImpl(AppointmentStatusTranslator statusTranslator) {
		this.statusTranslator = statusTranslator;
	}
	
	@Override
	protected <U> void setupSearchParams(
	        OpenmrsFhirCriteriaContext<org.openmrs.module.appointments.model.Appointment, U> ctx,
	        SearchParameterMap theParams) {
		super.setupSearchParams(ctx, theParams);
		ctx.getCriteriaQuery().distinct(true);

		theParams.getParameters().forEach(param -> {
			switch (param.getKey()) {
				case FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER:
					param.getValue().forEach(patientReference -> getSearchQueryHelper().handlePatientReference(ctx,
					    (ReferenceAndListParam) patientReference.getParam(), PATIENT_PARAM));
					break;
				case FhirConstants.STATUS_SEARCH_HANDLER:
					param.getValue().forEach(status -> handleStatus(ctx, (TokenAndListParam) status.getParam()));
					break;
				case FhirConstants.DATE_RANGE_SEARCH_HANDLER:
					param.getValue()
					        .forEach(dateRange -> handleDateRange(ctx, (DateRangeParam) dateRange.getParam()));
					break;
				case FhirConstants.COMMON_SEARCH_HANDLER:
					handleCommonSearchParameters(ctx, param.getValue()).ifPresent(ctx::addPredicate);
					break;
			}
		});
	}
	
	private <U> void handleStatus(
	        OpenmrsFhirCriteriaContext<org.openmrs.module.appointments.model.Appointment, U> ctx,
	        TokenAndListParam status) {
		if (status == null) {
			return;
		}
		handleAndListParam(ctx.getCriteriaBuilder(), status, token -> {
			if (token.getValue() != null) {
				try {
					Appointment.AppointmentStatus fhirStatus = Appointment.AppointmentStatus
					        .fromCode(token.getValue().toLowerCase(Locale.ROOT));
					AppointmentStatus bahmniStatus = statusTranslator.toOpenmrsType(fhirStatus);
					if (bahmniStatus != null) {
						return Optional.of(
						    ctx.getCriteriaBuilder().equal(ctx.getRoot().get(STATUS_PROPERTY), bahmniStatus));
					}
				}
				catch (FHIRException | IllegalArgumentException e) {
					log.debug("Invalid appointment status code: {}", token.getValue(), e);
				}
			}
			return Optional.empty();
		}).ifPresent(ctx::addPredicate);
	}
	
	private <U> void handleDateRange(
	        OpenmrsFhirCriteriaContext<org.openmrs.module.appointments.model.Appointment, U> ctx,
	        DateRangeParam dateRange) {
		if (dateRange != null) {
			getSearchQueryHelper().handleDateRange(ctx, START_DATE_TIME_PROPERTY, dateRange)
			        .ifPresent(ctx::addPredicate);
		}
	}
	
	@Override
	protected <V, U> Path<?> paramToProp(OpenmrsFhirCriteriaContext<V, U> ctx, String paramName) {
		if (paramName == null) {
			return null;
		}
		switch (paramName) {
			case DATE_PARAM:
				return ctx.getRoot().get(START_DATE_TIME_PROPERTY);
			case STATUS_PROPERTY:
				return ctx.getRoot().get(STATUS_PROPERTY);
			case PATIENT_PARAM:
				return ctx.getRoot().get(PATIENT_PARAM);
			default:
				return null;
		}
	}
}
