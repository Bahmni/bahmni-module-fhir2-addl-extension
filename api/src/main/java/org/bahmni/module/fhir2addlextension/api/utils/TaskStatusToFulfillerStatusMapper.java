package org.bahmni.module.fhir2addlextension.api.utils;

import org.openmrs.Order;
import org.openmrs.module.fhir2.model.FhirTask;

public final class TaskStatusToFulfillerStatusMapper {
	
	private TaskStatusToFulfillerStatusMapper() {
	}
	
	public static Order.FulfillerStatus toFulfillerStatus(FhirTask.TaskStatus taskStatus) {
		if (taskStatus == null) {
			return null;
		}
		switch (taskStatus) {
			case REQUESTED:
				return Order.FulfillerStatus.RECEIVED;
			case ACCEPTED:
				return Order.FulfillerStatus.IN_PROGRESS;
			case COMPLETED:
				return Order.FulfillerStatus.COMPLETED;
			case REJECTED:
				return Order.FulfillerStatus.EXCEPTION;
			default:
				return null;
		}
	}
}
