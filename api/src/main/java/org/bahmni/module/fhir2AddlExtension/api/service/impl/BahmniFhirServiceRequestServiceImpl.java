package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirServiceRequestDao;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirServiceRequestService;
import org.bahmni.module.fhir2AddlExtension.api.service.OpenMRSOrderServiceExtension;
import org.bahmni.module.fhir2AddlExtension.api.service.ServiceRequestLocationReferenceResolver;
import org.bahmni.module.fhir2AddlExtension.api.utils.ModuleUtils;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.Location;
import org.openmrs.Order;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.FhirServiceRequestDao;
import org.openmrs.module.fhir2.api.impl.BaseFhirService;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.translators.ServiceRequestTranslator;
import org.openmrs.module.fhir2.api.util.FhirUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
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
	private ServiceRequestLocationReferenceResolver locationReferenceResolver;
	
	@Setter(value = AccessLevel.PACKAGE, onMethod_ = @Autowired)
	private SearchQuery<Order, ServiceRequest, FhirServiceRequestDao<Order>, ServiceRequestTranslator<Order>, SearchQueryInclude<ServiceRequest>> searchQuery;
	
	@Setter(value = AccessLevel.PACKAGE, onMethod_ = @Autowired)
	private OpenMRSOrderServiceExtension openMRSOrderServiceExtension;
	
	@Override
	public IBundleProvider searchForServiceRequests(ReferenceAndListParam patientReference, TokenAndListParam code,
	        ReferenceAndListParam encounterReference, ReferenceAndListParam participantReference, DateRangeParam occurrence,
	        TokenAndListParam uuid, DateRangeParam lastUpdated, HashSet<Include> includes) {
		
		SearchParameterMap theParams = getSearchParameterMap(patientReference, code, encounterReference,
		    participantReference, occurrence, uuid, lastUpdated, includes, null);
		
		return searchQuery.getQueryResults(theParams, dao, translator, searchQueryInclude);
	}
	
	@Override
	public IBundleProvider searchForServiceRequestsWithCategory(ReferenceAndListParam patientReference,
	        TokenAndListParam code, ReferenceAndListParam encounterReference, ReferenceAndListParam participantReference,
	        ReferenceAndListParam category, DateRangeParam occurrence, TokenAndListParam uuid, DateRangeParam lastUpdated,
	        HashSet<Include> includes, HashSet<Include> revIncludes) {
		SearchParameterMap theParams = getSearchParameterMap(patientReference, code, encounterReference,
		    participantReference, occurrence, uuid, lastUpdated, includes, revIncludes);
		theParams.addParameter(FhirConstants.CATEGORY_SEARCH_HANDLER, category);
		return searchQuery.getQueryResults(theParams, dao, translator, searchQueryInclude);
	}
	
	@Override
	public IBundleProvider searchForServiceRequestsByNumberOfVisits(ReferenceParam patientReference,
	        NumberParam numberOfVisits, ReferenceAndListParam category, SortSpec sort, HashSet<Include> includes,
	        HashSet<Include> revIncludes) {
		if (patientReference == null) {
			throw new InvalidRequestException("Patient reference is required for searching by number of visits");
		}
		
		if (numberOfVisits == null) {
			throw new InvalidRequestException("Number of visits parameter is required");
		}
		
		ReferenceAndListParam encounterReferencesByNumberOfVisit = dao.getEncounterReferencesByNumberOfVisit(numberOfVisits,
		    patientReference);
		if (encounterReferencesByNumberOfVisit == null) {
			return null;
		}
		SearchParameterMap theParams = new SearchParameterMap()
		        .addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, encounterReferencesByNumberOfVisit)
		        .addParameter(FhirConstants.CATEGORY_SEARCH_HANDLER, category)
		        .addParameter(FhirConstants.INCLUDE_SEARCH_HANDLER, includes)
		        .addParameter(FhirConstants.REVERSE_INCLUDE_SEARCH_HANDLER, revIncludes);
		
		if (sort != null) {
			theParams.setSortSpec(sort);
		}
		return searchQuery.getQueryResults(theParams, dao, translator, searchQueryInclude);
	}
	
	@Override
	public ServiceRequest create(@Nonnull ServiceRequest newResource) {
		if (newResource == null) {
			throw new InvalidRequestException("A resource of type " + resourceClass.getSimpleName() + " must be supplied");
		}
		
		Order order = getTranslator().toOpenmrsType(newResource);
		
		/*
		 * TODO: Remove the below extension method once this JIRA changes are backported into 2.5.x and 2.6.x on OpenMRS Core
		 * JIRA: https://openmrs.atlassian.net/browse/TRUNK-6534
		 * Core PR: https://github.com/openmrs/openmrs-core/pull/5736
		 * This has been done to support creation of linked orders
		 */
		order = openMRSOrderServiceExtension.validateAndSetMissingFields(order, null);
		
		if (newResource.hasLocationReference()) {
			setRequestedLocationOnOrder(newResource.getLocationReference().get(0), order);
		} else {
			setDefaultOrderLocation(order);
		}
		
		validateObject(order);
		
		if (order.getUuid() == null) {
			order.setUuid(FhirUtils.newUuid());
		}
		
		return getTranslator().toFhirResource(getDao().createOrUpdate(order));
	}
	
	@Override
	protected ServiceRequest applyUpdate(Order existingObject, ServiceRequest updatedResource) {
		if (updatedResource.hasLocationReference()) {
			setRequestedLocationOnOrder(updatedResource.getLocationReference().get(0), existingObject);
		}
		return super.applyUpdate(existingObject, updatedResource);
	}
	
	private void setDefaultOrderLocation(Order order) {
		if (order.getEncounter() == null) {
			return;
		}
		Location preferredLocationForOrder = locationReferenceResolver.getPreferredLocation(order);
		Location orderLocation = preferredLocationForOrder != null ? preferredLocationForOrder : ModuleUtils
		        .getVisitLocation(order.getEncounter().getLocation());
		if (orderLocation == null) {
			return;
		}
		
		log.info("No preferred location is set for the order. System will attempt to default to the ordering location");
		if (locationReferenceResolver.hasRequestedLocation(order)) {
			locationReferenceResolver.updateOrderRequestLocation(orderLocation, order);
		} else {
			Reference reference = new Reference("Location/" + orderLocation.getUuid());
			locationReferenceResolver.setOrderRequestLocation(reference, order);
		}
	}
	
	protected void setRequestedLocationOnOrder(Reference reference, Order order) {
		if (locationReferenceResolver.hasRequestedLocation(order)) {
			locationReferenceResolver.updateOrderRequestLocation(reference, order);
		} else {
			locationReferenceResolver.setOrderRequestLocation(reference, order);
		}
	}
	
	private SearchParameterMap getSearchParameterMap(ReferenceAndListParam patientReference, TokenAndListParam code,
	        ReferenceAndListParam encounterReference, ReferenceAndListParam participantReference, DateRangeParam occurrence,
	        TokenAndListParam uuid, DateRangeParam lastUpdated, HashSet<Include> includes, HashSet<Include> revIncludes) {
		return new SearchParameterMap().addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, patientReference)
		        .addParameter(FhirConstants.CODED_SEARCH_HANDLER, code)
		        .addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, encounterReference)
		        .addParameter(FhirConstants.PARTICIPANT_REFERENCE_SEARCH_HANDLER, participantReference)
		        .addParameter(FhirConstants.DATE_RANGE_SEARCH_HANDLER, occurrence)
		        .addParameter(FhirConstants.COMMON_SEARCH_HANDLER, FhirConstants.ID_PROPERTY, uuid)
		        .addParameter(FhirConstants.COMMON_SEARCH_HANDLER, FhirConstants.LAST_UPDATED_PROPERTY, lastUpdated)
		        .addParameter(FhirConstants.INCLUDE_SEARCH_HANDLER, includes)
		        .addParameter(FhirConstants.REVERSE_INCLUDE_SEARCH_HANDLER, revIncludes);
	}
}
