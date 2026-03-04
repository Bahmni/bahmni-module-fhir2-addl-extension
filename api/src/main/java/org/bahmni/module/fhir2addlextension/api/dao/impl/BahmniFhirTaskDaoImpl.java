package org.bahmni.module.fhir2addlextension.api.dao.impl;

import javax.annotation.Nonnull;

import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirServiceRequestDao;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirTaskDao;
import org.bahmni.module.fhir2addlextension.api.utils.TaskStatusToFulfillerStatusMapper;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Order;
import org.openmrs.api.db.DAOException;
import org.openmrs.module.fhir2.api.dao.impl.FhirTaskDaoImpl;
import org.openmrs.module.fhir2.model.FhirReference;
import org.openmrs.module.fhir2.model.FhirTask;
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
