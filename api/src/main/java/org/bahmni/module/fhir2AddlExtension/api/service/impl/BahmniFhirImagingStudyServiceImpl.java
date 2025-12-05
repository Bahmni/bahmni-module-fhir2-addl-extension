package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2AddlExtension.api.PrivilegeConstants;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirImagingStudyDao;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirServiceRequestDao;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirImagingStudy;
import org.bahmni.module.fhir2AddlExtension.api.search.param.BahmniImagingStudySearchParams;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirImagingStudyService;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniFhirImagingStudyTranslator;
import org.hl7.fhir.r4.model.ImagingStudy;
import org.openmrs.Order;
import org.openmrs.annotation.Authorized;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.impl.BaseFhirService;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;

import static org.bahmni.module.fhir2AddlExtension.api.PrivilegeConstants.*;

@Component
@Transactional
@Slf4j
public class BahmniFhirImagingStudyServiceImpl extends BaseFhirService<ImagingStudy, FhirImagingStudy> implements BahmniFhirImagingStudyService {
	
	private final BahmniFhirImagingStudyDao imagingStudyDao;
	
	private final BahmniFhirImagingStudyTranslator imagingStudyTranslator;
	
	private final BahmniFhirServiceRequestDao<Order> serviceRequestDao;
	
	private final SearchQueryInclude<ImagingStudy> searchQueryInclude;
	
	private final SearchQuery<FhirImagingStudy, ImagingStudy, BahmniFhirImagingStudyDao, BahmniFhirImagingStudyTranslator, SearchQueryInclude<ImagingStudy>> searchQuery;
	
	@Autowired
	public BahmniFhirImagingStudyServiceImpl(
	    BahmniFhirImagingStudyDao imagingStudyDao,
	    BahmniFhirImagingStudyTranslator imagingStudyTranslator,
	    BahmniFhirServiceRequestDao<Order> serviceRequestDao,
	    SearchQueryInclude<ImagingStudy> searchQueryInclude,
	    SearchQuery<FhirImagingStudy, ImagingStudy, BahmniFhirImagingStudyDao, BahmniFhirImagingStudyTranslator, SearchQueryInclude<ImagingStudy>> searchQuery) {
		this.imagingStudyDao = imagingStudyDao;
		this.imagingStudyTranslator = imagingStudyTranslator;
		this.serviceRequestDao = serviceRequestDao;
		this.searchQueryInclude = searchQueryInclude;
		this.searchQuery = searchQuery;
	}
	
	@Override
	protected FhirDao<FhirImagingStudy> getDao() {
		return imagingStudyDao;
	}
	
	@Override
	protected OpenmrsFhirTranslator<FhirImagingStudy, ImagingStudy> getTranslator() {
		return imagingStudyTranslator;
	}
	
	@Override
	@Transactional(readOnly = true)
	@Authorized(GET_IMAGING_STUDY)
	public ImagingStudy get(@NotNull String uuid) {
		return super.get(uuid);
	}
	
	@Override
	@Transactional
	@Authorized(CREATE_IMAGING_STUDY)
	public ImagingStudy create(@NotNull ImagingStudy newResource) {
		return super.create(newResource);
	}
	
	@Override
	@Transactional
	@Authorized(EDIT_IMAGING_STUDY)
	public ImagingStudy applyUpdate(FhirImagingStudy existingObject, ImagingStudy updatedResource) {
		ImagingStudy imagingStudy = super.applyUpdate(existingObject, updatedResource);
		updateOrderFulFillerStatus(existingObject, imagingStudy);
		return imagingStudy;
	}
	
	@Override
	@Transactional(readOnly = true)
	@Authorized(GET_IMAGING_STUDY)
	public IBundleProvider searchImagingStudy(BahmniImagingStudySearchParams searchParams) {
		if (!searchParams.hasPatientReference() && !searchParams.hasId() && !searchParams.hasBasedOnReference()) {
			log.error("Missing patient reference, resource id or basedOn reference for ImagingStudy search");
			throw new UnsupportedOperationException(
			        "You must specify patient reference or resource _id or basedOn reference!");
		}
		return searchQuery.getQueryResults(searchParams.toSearchParameterMap(), imagingStudyDao, imagingStudyTranslator,
		    searchQueryInclude);
	}
	
	private void updateOrderFulFillerStatus(FhirImagingStudy existingObject, ImagingStudy imagingStudy) {
		Order order = existingObject.getOrder();
		if (order != null) {
			Order.FulfillerStatus fulfillerStatus = mapFulfillerStatus(imagingStudy.getStatus());
			if (fulfillerStatus != null) {
				order.setFulfillerStatus(fulfillerStatus);
				serviceRequestDao.updateOrder(order);
				log.info("Fulfiller status updated for order: {}", order.getUuid());
			}
		}
	}
	
	private Order.FulfillerStatus mapFulfillerStatus(ImagingStudy.ImagingStudyStatus status) {
		switch (status) {
			case REGISTERED:
				return Order.FulfillerStatus.RECEIVED;
			case ENTEREDINERROR:
				return Order.FulfillerStatus.EXCEPTION;
			case UNKNOWN:
			case NULL:
			default:
				return null;
		}
	}
}
