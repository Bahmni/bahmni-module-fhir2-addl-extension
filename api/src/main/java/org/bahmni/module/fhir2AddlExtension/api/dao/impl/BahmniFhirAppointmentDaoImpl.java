package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirAppointmentDao;
import org.bahmni.module.fhir2AddlExtension.api.translator.AppointmentStatusTranslator;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hl7.fhir.r4.model.Appointment;
import org.openmrs.module.appointments.model.AppointmentStatus;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.api.SortOrderEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;

import java.util.Locale;
import java.util.Optional;

@Component
public class BahmniFhirAppointmentDaoImpl extends BaseFhirDao<org.openmrs.module.appointments.model.Appointment> implements BahmniFhirAppointmentDao {
	
	private final AppointmentStatusTranslator statusTranslator;
	
	@Autowired
	public BahmniFhirAppointmentDaoImpl(AppointmentStatusTranslator statusTranslator) {
		this.statusTranslator = statusTranslator;
	}
	
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
					try {
						// Parse FHIR status string to enum and use translator to convert to Bahmni status
						Appointment.AppointmentStatus fhirStatus =
							Appointment.AppointmentStatus.fromCode(token.getValue().toLowerCase(Locale.ROOT));
						AppointmentStatus bahmniStatus = statusTranslator.toOpenmrsType(fhirStatus);
						if (bahmniStatus != null) {
							return Optional.of(Restrictions.eq("status", bahmniStatus));
						}
					} catch (Exception e) {
						// Invalid status code, skip this filter
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
	
	@Override
	protected void handleSort(Criteria criteria, SortSpec sortSpec) {
		if (sortSpec == null) {
			return;
		}
		
		// FHIR Sort Convention:
		// _sort=date     → Ascending order (earliest first)
		// _sort=-date    → Descending order (latest first)
		// Prefix "-" indicates descending/reverse order
		
		for (SortSpec sort = sortSpec; sort != null; sort = sort.getChain()) {
			String paramName = sort.getParamName();
			
			// Map FHIR sort parameter name to Appointment property name
			String propertyName = mapSortParamToProperty(paramName);
			
			if (propertyName != null) {
				if (SortOrderEnum.DESC.equals(sort.getOrder())) {
					// "-" prefix means descending order
					criteria.addOrder(Order.desc(propertyName));
				} else {
					// Default to ascending order (no prefix or "+" prefix)
					criteria.addOrder(Order.asc(propertyName));
				}
			}
		}
	}
	
	private String mapSortParamToProperty(String paramName) {
		// Map FHIR sort parameter names to Appointment entity property names
		if (paramName == null) {
			return null;
		}
		
		switch (paramName) {
			case "date":
				return "startDateTime";
			case "status":
				return "status";
			case "patient":
				return "patient";
			default:
				return null;
		}
	}
}
