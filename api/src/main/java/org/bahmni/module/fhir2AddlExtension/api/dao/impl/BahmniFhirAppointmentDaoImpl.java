package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirAppointmentDao;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.openmrs.module.appointments.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentStatus;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;

import java.util.Optional;

@Component
public class BahmniFhirAppointmentDaoImpl extends BaseFhirDao<Appointment> implements BahmniFhirAppointmentDao {
	
	@Override
	protected void setupSearchParams(Criteria criteria, SearchParameterMap theParams) {
		super.setupSearchParams(criteria, theParams);
		theParams.getParameters().forEach(param -> {
			switch (param.getKey()) {
				case FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER:
					param.getValue().forEach(patientReference -> handlePatientReference(criteria,
					    (ReferenceAndListParam) patientReference.getParam(), "patient"));
					break;
				case FhirConstants.STATUS_SEARCH_HANDLER:
					param.getValue()
					    .forEach(status -> handleStatus(criteria, (TokenAndListParam) status.getParam()));
					break;
				case FhirConstants.DATE_RANGE_SEARCH_HANDLER:
					param.getValue()
					    .forEach(dateRange -> handleDateRange(criteria, (DateRangeParam) dateRange.getParam()));
					break;
				case FhirConstants.COMMON_SEARCH_HANDLER:
					handleCommonSearchParameters(param.getValue()).ifPresent(criteria::add);
					break;
			}
		});
	}
	
	private void handleStatus(Criteria criteria, TokenAndListParam status) {
		if (status != null) {
			handleAndListParam(status, token -> {
				if (token.getValue() != null) {
					// Map FHIR status string to Bahmni AppointmentStatus enum
					AppointmentStatus bahmniStatus = mapFhirStatusToBahmni(token.getValue());
					if (bahmniStatus != null) {
						return Optional.of(Restrictions.eq("status", bahmniStatus));
					}
				}
				return Optional.empty();
			}).ifPresent(criteria::add);
		}
	}
	
	private void handleDateRange(Criteria criteria, DateRangeParam dateRange) {
		if (dateRange != null) {
			handleDateRange("startDateTime", dateRange).ifPresent(criteria::add);
		}
	}
	
	private AppointmentStatus mapFhirStatusToBahmni(String fhirStatus) {
		// Map FHIR AppointmentStatus codes to Bahmni AppointmentStatus enum
		switch (fhirStatus.toLowerCase()) {
			case "booked":
				return AppointmentStatus.Scheduled;
			case "fulfilled":
				return AppointmentStatus.Completed;
			case "cancelled":
				return AppointmentStatus.Cancelled;
			case "noshow":
				return AppointmentStatus.Missed;
			case "checkedin":
				return AppointmentStatus.CheckedIn;
			case "pending":
				return AppointmentStatus.Requested;
			default:
				// Try to parse directly as enum name
				try {
					return AppointmentStatus.valueOf(fhirStatus);
				}
				catch (IllegalArgumentException e) {
					return null;
				}
		}
	}
}
