package org.bahmni.module.fhir2addlextension.api.dao.impl;

import javax.annotation.Nonnull;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirServiceRequestDao;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirTaskDao;
import org.bahmni.module.fhir2addlextension.api.utils.TaskStatusToFulfillerStatusMapper;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Order;
import org.openmrs.api.db.DAOException;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.impl.FhirTaskDaoImpl;
import org.openmrs.module.fhir2.api.search.param.PropParam;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.model.FhirReference;
import org.openmrs.module.fhir2.model.FhirTask;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Primary
@Slf4j
public class BahmniFhirTaskDaoImpl extends FhirTaskDaoImpl implements BahmniFhirTaskDao {
	
	private final BahmniFhirServiceRequestDao<Order> serviceRequestDao;
	
	@Autowired
	public BahmniFhirTaskDaoImpl(BahmniFhirServiceRequestDao<Order> serviceRequestDao) {
		this.serviceRequestDao = serviceRequestDao;
	}
	
	@Override
	@Transactional
	public FhirTask createOrUpdate(@Nonnull FhirTask task) throws DAOException {
		boolean isNewTask = task.getId() == null;
		FhirTask savedTask = super.createOrUpdate(task);
		if (isNewTask) {
			updateFulfillerStatusFromTask(savedTask);
		}
		return savedTask;
	}
	
	@Override
	protected void setupSearchParams(Criteria criteria, SearchParameterMap theParams) {
		List<PropParam<?>> forReferenceParams = theParams.getParameters(FhirConstants.FOR_REFERENCE_SEARCH_HANDLER);

		SearchParameterMap paramsCopy = new SearchParameterMap();
		theParams.getParameters().forEach(entry -> {
			if (!FhirConstants.FOR_REFERENCE_SEARCH_HANDLER.equals(entry.getKey())) {
				entry.getValue().forEach(param -> paramsCopy.addParameter(entry.getKey(), param.getParam()));
			}
		});

		super.setupSearchParams(criteria, paramsCopy);

		if (forReferenceParams != null) {
			forReferenceParams.forEach(
			    param -> handleForReference(criteria, (ReferenceAndListParam) param.getParam()));
		}

		theParams.getParameters().forEach(entry -> {
			switch (entry.getKey()) {
				case FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER:
					entry.getValue().forEach(
					    param -> handleEncounterForTask(criteria, (ReferenceAndListParam) param.getParam()));
					break;
				case BahmniFhirConstants.NAME_SEARCH_HANDLER:
					entry.getValue().forEach(
					    param -> handleName(criteria, (StringAndListParam) param.getParam()));
					break;
			}
		});
	}
	
	private void handleEncounterForTask(Criteria criteria, ReferenceAndListParam encounterReference) {
		if (encounterReference == null) {
			return;
		}
		if (lacksAlias(criteria, "er")) {
			criteria.createAlias("encounterReference", "er");
		}
		handleAndListParam(encounterReference,
		    ref -> ref.getIdPart() != null ? Optional.of(Restrictions.eq("er.targetUuid", ref.getIdPart()))
		            : Optional.empty()).ifPresent(criteria::add);
	}
	
	private void handleName(Criteria criteria, StringAndListParam name) {
		if (name == null) {
			return;
		}
		handleAndListParam(name,
		    param -> param.getValue() != null ? Optional.of(Restrictions.eq("name", param.getValue()))
		            : Optional.empty()).ifPresent(criteria::add);
	}
	
	private void handleForReference(Criteria criteria, ReferenceAndListParam forReference) {
		if (forReference == null) {
			return;
		}
		if (lacksAlias(criteria, "fr")) {
			criteria.createAlias("forReference", "fr");
		}
		handleAndListParam(forReference,
		    ref -> ref.getIdPart() != null ? Optional.of(Restrictions.eq("fr.targetUuid", ref.getIdPart()))
		            : Optional.empty()).ifPresent(criteria::add);
	}
	
	@Override
	public FhirTask getTaskByOrderUuid(String orderUuid) {
		Criteria criteria = getSessionFactory().getCurrentSession().createCriteria(FhirTask.class)
		        .createAlias("basedOnReferences", "bor").add(Restrictions.eq("bor.targetUuid", orderUuid))
		        .addOrder(org.hibernate.criterion.Order.desc("dateCreated")).setMaxResults(1);
		return (FhirTask) criteria.uniqueResult();
	}
	
	private void updateFulfillerStatusFromTask(FhirTask fhirTask) {
		String orderUuid = extractOrderUuidFromBasedOn(fhirTask);
		if (orderUuid == null) {
			return;
		}
		
		Order.FulfillerStatus fulfillerStatus = TaskStatusToFulfillerStatusMapper.toFulfillerStatus(fhirTask.getStatus());
		if (fulfillerStatus == null) {
			log.warn("No fulfiller status mapping for task status {}, skipping update", fhirTask.getStatus());
			return;
		}
		
		try {
			Order order = serviceRequestDao.get(orderUuid);
			if (order == null) {
				log.warn("Order not found for uuid {}, skipping fulfiller status update", orderUuid);
				return;
			}
			order.setFulfillerStatus(fulfillerStatus);
			String taskComment = fhirTask.getComment();
			if (taskComment != null && !taskComment.trim().isEmpty()) {
				order.setFulfillerComment(taskComment);
			}
			serviceRequestDao.updateOrder(order);
			log.info("Updated fulfiller status to {} and comment for order {}", fulfillerStatus, orderUuid);
		}
		catch (Exception e) {
			log.warn("Could not update fulfiller status for order {}: {}", orderUuid, e.getMessage());
		}
	}
	
	private String extractOrderUuidFromBasedOn(FhirTask fhirTask) {
		Set<FhirReference> basedOnRefs = fhirTask.getBasedOnReferences();
		if (basedOnRefs == null || basedOnRefs.isEmpty()) {
			return null;
		}
		for (FhirReference ref : basedOnRefs) {
			if ("ServiceRequest".equals(ref.getType())) {
				return ref.getTargetUuid();
			}
		}
		return null;
	}
}
