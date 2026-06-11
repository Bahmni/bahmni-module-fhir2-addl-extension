package org.bahmni.module.fhir2addlextension.api.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Test;
import org.openmrs.Order;
import org.openmrs.module.fhir2.model.FhirTask;

public class TaskStatusToFulfillerStatusMapperTest {
	
	@Test
	public void shouldMapRequestedToReceived() {
		assertThat(TaskStatusToFulfillerStatusMapper.toFulfillerStatus(FhirTask.TaskStatus.REQUESTED),
		    equalTo(Order.FulfillerStatus.RECEIVED));
	}
	
	@Test
	public void shouldMapAcceptedToInProgress() {
		assertThat(TaskStatusToFulfillerStatusMapper.toFulfillerStatus(FhirTask.TaskStatus.ACCEPTED),
		    equalTo(Order.FulfillerStatus.IN_PROGRESS));
	}
	
	@Test
	public void shouldMapCompletedToCompleted() {
		assertThat(TaskStatusToFulfillerStatusMapper.toFulfillerStatus(FhirTask.TaskStatus.COMPLETED),
		    equalTo(Order.FulfillerStatus.COMPLETED));
	}
	
	@Test
	public void shouldMapRejectedToException() {
		assertThat(TaskStatusToFulfillerStatusMapper.toFulfillerStatus(FhirTask.TaskStatus.REJECTED),
		    equalTo(Order.FulfillerStatus.EXCEPTION));
	}
	
	@Test
	public void shouldReturnNullForUnknownStatus() {
		assertThat(TaskStatusToFulfillerStatusMapper.toFulfillerStatus(FhirTask.TaskStatus.UNKNOWN), nullValue());
	}
	
	@Test
	public void shouldReturnNullForDraftStatus() {
		assertThat(TaskStatusToFulfillerStatusMapper.toFulfillerStatus(FhirTask.TaskStatus.DRAFT), nullValue());
	}
	
	@Test
	public void shouldReturnNullForOnHoldStatus() {
		assertThat(TaskStatusToFulfillerStatusMapper.toFulfillerStatus(FhirTask.TaskStatus.ONHOLD), nullValue());
	}
	
	@Test
	public void shouldReturnNullForNullStatus() {
		assertThat(TaskStatusToFulfillerStatusMapper.toFulfillerStatus(null), nullValue());
	}
	
	@Test
	public void shouldReturnNullForReadyStatus() {
		// READY has no Order.FulfillerStatus equivalent — fulfillerStatus stays null intentionally
		assertThat(TaskStatusToFulfillerStatusMapper.toFulfillerStatus(FhirTask.TaskStatus.READY), nullValue());
	}
}
