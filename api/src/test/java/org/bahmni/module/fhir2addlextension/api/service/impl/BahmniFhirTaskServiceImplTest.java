package org.bahmni.module.fhir2addlextension.api.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirTaskDao;
import org.hl7.fhir.r4.model.Task;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.fhir2.api.translators.TaskTranslator;
import org.openmrs.module.fhir2.model.FhirTask;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirTaskServiceImplTest {
	
	private static final String TASK_UUID = "task-uuid-456";
	
	@Mock
	private BahmniFhirTaskDao dao;
	
	@Mock
	private TaskTranslator translator;
	
	private BahmniFhirTaskServiceImpl taskService;
	
	@Before
	public void setup() {
		taskService = new BahmniFhirTaskServiceImpl() {
			
			@Override
			protected void validateObject(FhirTask object) {
			}
		};
		taskService.setDao(dao);
		taskService.setTranslator(translator);
	}
	
	@Test
	public void create_shouldTranslateAndDelegateToDao() {
		Task inputTask = new Task();
		FhirTask openmrsTask = new FhirTask();
		openmrsTask.setUuid(TASK_UUID);
		FhirTask savedTask = new FhirTask();
		savedTask.setUuid(TASK_UUID);
		Task expectedResult = new Task();
		expectedResult.setId(TASK_UUID);
		
		when(translator.toOpenmrsType(inputTask)).thenReturn(openmrsTask);
		when(dao.createOrUpdate(openmrsTask)).thenReturn(savedTask);
		when(translator.toFhirResource(savedTask)).thenReturn(expectedResult);
		
		Task result = taskService.create(inputTask);
		
		assertThat(result, notNullValue());
		assertThat(result.getId(), equalTo(TASK_UUID));
		verify(translator).toOpenmrsType(inputTask);
		verify(dao).createOrUpdate(openmrsTask);
		verify(translator).toFhirResource(savedTask);
	}
	
	@Test
	public void get_shouldDelegateToDao() {
		FhirTask openmrsTask = new FhirTask();
		openmrsTask.setUuid(TASK_UUID);
		Task expectedTask = new Task();
		expectedTask.setId(TASK_UUID);
		
		when(dao.get(TASK_UUID)).thenReturn(openmrsTask);
		when(translator.toFhirResource(openmrsTask)).thenReturn(expectedTask);
		
		Task result = taskService.get(TASK_UUID);
		
		assertThat(result, notNullValue());
		assertThat(result.getId(), equalTo(TASK_UUID));
		verify(dao).get(TASK_UUID);
	}
}
