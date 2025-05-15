package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirServiceRequestDao;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirServiceRequestService;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.Order;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.FhirServiceRequestDao;
import org.openmrs.module.fhir2.api.impl.BaseFhirService;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.translators.ServiceRequestTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
@Primary
public class BahmniFhirServiceRequestServiceImpl extends BaseFhirService<ServiceRequest, Order> implements BahmniFhirServiceRequestService {
	
	@Getter(value = AccessLevel.PROTECTED)
	@Setter(value = AccessLevel.PACKAGE, onMethod_ = @Autowired)
	private BahmniFhirServiceRequestDao<Order> dao;
	
	@Getter(value = AccessLevel.PROTECTED)
	@Setter(value = AccessLevel.PACKAGE, onMethod_ = @Autowired)
	private ServiceRequestTranslator<Order> translator;
	
	@Setter(value = AccessLevel.PACKAGE, onMethod_ = @Autowired)
	private SearchQueryInclude<ServiceRequest> searchQueryInclude;
	
	@Setter(value = AccessLevel.PACKAGE, onMethod_ = @Autowired)
	private SearchQuery<Order, ServiceRequest, FhirServiceRequestDao<Order>, ServiceRequestTranslator<Order>, SearchQueryInclude<ServiceRequest>> searchQuery;
	
	@Override
	public IBundleProvider searchForServiceRequests(ReferenceAndListParam patientReference, TokenAndListParam code,
	        ReferenceAndListParam encounterReference, ReferenceAndListParam participantReference, DateRangeParam occurrence,
	        TokenAndListParam uuid, DateRangeParam lastUpdated, HashSet<Include> includes) {
		
		SearchParameterMap theParams = getSearchParameterMap(patientReference, code, encounterReference,
		    participantReference, occurrence, uuid, lastUpdated, includes);
		
		return searchQuery.getQueryResults(theParams, dao, translator, searchQueryInclude);
	}
	
	@Override
	public IBundleProvider searchForServiceRequestsWithCategory(ReferenceAndListParam patientReference,
	        TokenAndListParam code, ReferenceAndListParam encounterReference, ReferenceAndListParam participantReference,
	        ReferenceAndListParam category, DateRangeParam occurrence, TokenAndListParam uuid, DateRangeParam lastUpdated,
	        HashSet<Include> includes) {
		SearchParameterMap theParams = getSearchParameterMap(patientReference, code, encounterReference,
		    participantReference, occurrence, uuid, lastUpdated, includes);
		theParams.addParameter(FhirConstants.CATEGORY_SEARCH_HANDLER, category);
		return searchQuery.getQueryResults(theParams, dao, translator, searchQueryInclude);
	}
	
	@Override
	public IBundleProvider searchForServiceRequestsByNumberOfVisits(ReferenceParam patientReference,
	        NumberParam numberOfVisits, ReferenceAndListParam category, HashSet<Include> includes) {
		if (patientReference == null) {
			throw new InvalidRequestException("Patient reference is required for searching by number of visits");
		}
		
		if (numberOfVisits == null) {
			throw new InvalidRequestException("Number of visits parameter is required");
		}
		
		ReferenceAndListParam encounterReferencesByNumberOfVisit = dao.createEncounterReferencesByNumberOfVisit(
		    numberOfVisits, patientReference);
		if (encounterReferencesByNumberOfVisit == null) {
			return null;
		}
		SearchParameterMap theParams = new SearchParameterMap()
		        .addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, encounterReferencesByNumberOfVisit)
		        .addParameter(FhirConstants.CATEGORY_SEARCH_HANDLER, category)
		        .addParameter(FhirConstants.INCLUDE_SEARCH_HANDLER, includes);
		return searchQuery.getQueryResults(theParams, dao, translator, searchQueryInclude);
	}
	
	private SearchParameterMap getSearchParameterMap(ReferenceAndListParam patientReference, TokenAndListParam code,
	        ReferenceAndListParam encounterReference, ReferenceAndListParam participantReference, DateRangeParam occurrence,
	        TokenAndListParam uuid, DateRangeParam lastUpdated, HashSet<Include> includes) {
		return new SearchParameterMap().addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, patientReference)
		        .addParameter(FhirConstants.CODED_SEARCH_HANDLER, code)
		        .addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, encounterReference)
		        .addParameter(FhirConstants.PARTICIPANT_REFERENCE_SEARCH_HANDLER, participantReference)
		        .addParameter(FhirConstants.DATE_RANGE_SEARCH_HANDLER, occurrence)
		        .addParameter(FhirConstants.COMMON_SEARCH_HANDLER, FhirConstants.ID_PROPERTY, uuid)
		        .addParameter(FhirConstants.COMMON_SEARCH_HANDLER, FhirConstants.LAST_UPDATED_PROPERTY, lastUpdated)
		        .addParameter(FhirConstants.INCLUDE_SEARCH_HANDLER, includes);
	}
}
