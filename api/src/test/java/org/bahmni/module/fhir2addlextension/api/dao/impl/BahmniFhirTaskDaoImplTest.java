package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

import java.util.List;
import java.util.stream.Collectors;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2addlextension.api.BaseFhir2AddlContextSensitiveTest;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirTaskDao;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.model.FhirReference;
import org.openmrs.module.fhir2.model.FhirTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class BahmniFhirTaskDaoImplTest extends BaseFhir2AddlContextSensitiveTest {
	
	private static final String VISIT_1 = "visit-0001";
	
	private static final String VISIT_2 = "visit-0002";
	
	private static final String ENC_1 = "enc-0001";
	
	private static final String ENC_2 = "enc-0002";
	
	private static final String ENC_3 = "enc-0003";
	
	private static final String TASK_RADIOLOGY = "task-radiology-v1-e1";
	
	private static final String TASK_LAB_V1 = "task-lab-v1-e2";
	
	private static final String TASK_LAB_V2 = "task-lab-v2-e3";
	
	private static final String TASK_UNLINKED = "task-unlinked";
	
	@Autowired
	private BahmniFhirTaskDao dao;
	
	@Autowired
	@Qualifier("sessionFactory")
	private SessionFactory sessionFactory;
	
	/**
	 * <pre>
	 * radiology : forReference Visit v1, encounterReference e1, status REQUESTED
	 * lab (v1)  : forReference Visit v1, encounterReference e2, status COMPLETED
	 * lab (v2)  : forReference Visit v2, encounterReference e3, status REQUESTED
	 * unlinked  : no forReference / encounterReference,           status REQUESTED
	 * </pre>
	 */
	@Before
	public void createTasks() {
		createTask(TASK_RADIOLOGY, "Radiology Order", VISIT_1, ENC_1, FhirTask.TaskStatus.REQUESTED);
		createTask(TASK_LAB_V1, "Lab Order", VISIT_1, ENC_2, FhirTask.TaskStatus.COMPLETED);
		createTask(TASK_LAB_V2, "Lab Order", VISIT_2, ENC_3, FhirTask.TaskStatus.REQUESTED);
		createTask(TASK_UNLINKED, "Unlinked", null, null, FhirTask.TaskStatus.REQUESTED);
		sessionFactory.getCurrentSession().flush();
		sessionFactory.getCurrentSession().clear();
	}
	
	@Test
	public void search_withoutParameters_returnsAllTasks() {
		assertThat(uuids(new SearchParameterMap()),
		    containsInAnyOrder(TASK_RADIOLOGY, TASK_LAB_V1, TASK_LAB_V2, TASK_UNLINKED));
	}
	
	@Test
	public void search_byForReference_returnsTasksStoredForThatVisit() {
		assertThat(uuids(forSearch(VISIT_1)), containsInAnyOrder(TASK_RADIOLOGY, TASK_LAB_V1));
		assertThat(uuids(forSearch(VISIT_2)), contains(TASK_LAB_V2));
	}
	
	@Test
	public void search_byForReferenceMatchesOnUuidRegardlessOfResourceType() {
		SearchParameterMap params = new SearchParameterMap().addParameter(FhirConstants.FOR_REFERENCE_SEARCH_HANDLER,
		    new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam("Patient", VISIT_2))));
		assertThat(uuids(params), contains(TASK_LAB_V2));
	}
	
	@Test
	public void search_byUnknownForReference_returnsNothing() {
		assertThat(uuids(forSearch("visit-unknown")), empty());
	}
	
	@Test
	public void search_byForReferenceOrList_returnsTasksOfEitherVisit() {
		SearchParameterMap params = new SearchParameterMap().addParameter(
		    FhirConstants.FOR_REFERENCE_SEARCH_HANDLER,
		    new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam("Visit", VISIT_1)).add(
		        new ReferenceParam("Visit", VISIT_2))));
		assertThat(uuids(params), containsInAnyOrder(TASK_RADIOLOGY, TASK_LAB_V1, TASK_LAB_V2));
	}
	
	@Test
	public void search_byForReferenceWithoutIdPart_addsNoValuePredicateButStillRequiresAForReference() {
		SearchParameterMap params = new SearchParameterMap().addParameter(FhirConstants.FOR_REFERENCE_SEARCH_HANDLER,
		    new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam())));
		// the join on forReference is an inner join, so the task without any forReference is excluded
		assertThat(uuids(params), containsInAnyOrder(TASK_RADIOLOGY, TASK_LAB_V1, TASK_LAB_V2));
	}
	
	@Test
	public void search_byEncounterReference_returnsTaskOfThatEncounter() {
		assertThat(uuids(encounterSearch(ENC_2)), contains(TASK_LAB_V1));
		assertThat(uuids(encounterSearch(ENC_3)), contains(TASK_LAB_V2));
	}
	
	@Test
	public void search_byEncounterReferenceOrList_returnsTasksOfEitherEncounter() {
		SearchParameterMap params = new SearchParameterMap().addParameter(
		    FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER,
		    new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam("Encounter", ENC_1)).add(
		        new ReferenceParam("Encounter", ENC_3))));
		assertThat(uuids(params), containsInAnyOrder(TASK_RADIOLOGY, TASK_LAB_V2));
	}
	
	@Test
	public void search_byUnknownEncounterReference_returnsNothing() {
		assertThat(uuids(encounterSearch("enc-unknown")), empty());
	}
	
	@Test
	public void search_byEncounterReferenceWithoutIdPart_addsNoValuePredicateButStillRequiresAnEncounterReference() {
		SearchParameterMap params = new SearchParameterMap().addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER,
		    new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam())));
		// the join on encounterReference is an inner join, so the task without an encounterReference is excluded
		assertThat(uuids(params), containsInAnyOrder(TASK_RADIOLOGY, TASK_LAB_V1, TASK_LAB_V2));
	}
	
	@Test
	public void search_byName_returnsTasksWithExactName() {
		assertThat(uuids(nameSearch("Lab Order")), containsInAnyOrder(TASK_LAB_V1, TASK_LAB_V2));
		assertThat(uuids(nameSearch("Unlinked")), contains(TASK_UNLINKED));
	}
	
	@Test
	public void search_byNameIsExactNotPartial() {
		assertThat(uuids(nameSearch("Lab")), empty());
	}
	
	@Test
	public void search_byNameOrList_returnsTasksMatchingEitherName() {
		SearchParameterMap params = new SearchParameterMap().addParameter(
		    BahmniFhirConstants.NAME_SEARCH_HANDLER,
		    new StringAndListParam().addAnd(new StringOrListParam().addOr(new StringParam("Radiology Order")).addOr(
		        new StringParam("Unlinked"))));
		assertThat(uuids(params), containsInAnyOrder(TASK_RADIOLOGY, TASK_UNLINKED));
	}
	
	@Test
	public void search_byForReferenceAndName_returnsIntersection() {
		SearchParameterMap params = forSearch(VISIT_1).addParameter(BahmniFhirConstants.NAME_SEARCH_HANDLER,
		    new StringAndListParam().addAnd(new StringOrListParam().addOr(new StringParam("Lab Order"))));
		assertThat(uuids(params), contains(TASK_LAB_V1));
	}
	
	@Test
	public void search_byForReferenceAndEncounterReference_returnsIntersection() {
		SearchParameterMap params = forSearch(VISIT_1).addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER,
		    new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam("Encounter", ENC_3))));
		assertThat(uuids(params), empty());
		
		params = forSearch(VISIT_1).addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER,
		    new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam("Encounter", ENC_1))));
		assertThat(uuids(params), contains(TASK_RADIOLOGY));
	}
	
	@Test
	public void search_byStatus_isHandledBySuperclassAlongsideBahmniParams() {
		SearchParameterMap params = new SearchParameterMap().addParameter(FhirConstants.STATUS_SEARCH_HANDLER,
		    new TokenAndListParam().addAnd(new TokenParam("requested")));
		assertThat(uuids(params), containsInAnyOrder(TASK_RADIOLOGY, TASK_LAB_V2, TASK_UNLINKED));
		
		params = forSearch(VISIT_1).addParameter(FhirConstants.STATUS_SEARCH_HANDLER,
		    new TokenAndListParam().addAnd(new TokenParam("requested")));
		assertThat(uuids(params), contains(TASK_RADIOLOGY));
	}
	
	@Test
	public void search_withNullBahmniParameterValues_areIgnored() {
		SearchParameterMap params = new SearchParameterMap();
		params.addParameter(FhirConstants.FOR_REFERENCE_SEARCH_HANDLER, null);
		params.addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, null);
		params.addParameter(BahmniFhirConstants.NAME_SEARCH_HANDLER, null);
		assertThat(uuids(params), containsInAnyOrder(TASK_RADIOLOGY, TASK_LAB_V1, TASK_LAB_V2, TASK_UNLINKED));
	}
	
	@Test
	public void search_byNameWithoutValue_doesNotRestrictResults() {
		SearchParameterMap params = new SearchParameterMap().addParameter(BahmniFhirConstants.NAME_SEARCH_HANDLER,
		    new StringAndListParam().addAnd(new StringOrListParam().addOr(new StringParam())));
		assertThat(uuids(params), containsInAnyOrder(TASK_RADIOLOGY, TASK_LAB_V1, TASK_LAB_V2, TASK_UNLINKED));
	}
	
	@Test
	public void search_byCommonIdParameter_isPassedToSuperclass() {
		SearchParameterMap params = new SearchParameterMap().addParameter(FhirConstants.COMMON_SEARCH_HANDLER,
		    FhirConstants.ID_PROPERTY, new TokenAndListParam().addAnd(new TokenParam(TASK_LAB_V2)));
		assertThat(uuids(params), contains(TASK_LAB_V2));
	}
	
	private void createTask(String uuid, String name, String visitUuid, String encounterUuid, FhirTask.TaskStatus status) {
		FhirTask task = new FhirTask();
		task.setUuid(uuid);
		task.setName(name);
		task.setStatus(status);
		task.setIntent(FhirTask.TaskIntent.ORDER);
		if (visitUuid != null) {
			task.setForReference(createReference("Visit", visitUuid));
		}
		if (encounterUuid != null) {
			task.setEncounterReference(createReference("Encounter", encounterUuid));
		}
		dao.createOrUpdate(task);
	}
	
	private FhirReference createReference(String type, String targetUuid) {
		FhirReference ref = new FhirReference();
		ref.setType(type);
		ref.setTargetUuid(targetUuid);
		ref.setReference(type + "/" + targetUuid);
		ref.setName(type + " reference");
		ref.setCreator(org.openmrs.api.context.Context.getAuthenticatedUser());
		ref.setDateCreated(new java.util.Date());
		sessionFactory.getCurrentSession().save(ref);
		return ref;
	}
	
	private SearchParameterMap forSearch(String visitUuid) {
		return new SearchParameterMap().addParameter(FhirConstants.FOR_REFERENCE_SEARCH_HANDLER,
		    new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam("Visit", visitUuid))));
	}
	
	private SearchParameterMap encounterSearch(String encounterUuid) {
		return new SearchParameterMap().addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER,
		    new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		            .add(new ReferenceParam("Encounter", encounterUuid))));
	}
	
	private SearchParameterMap nameSearch(String name) {
		return new SearchParameterMap().addParameter(BahmniFhirConstants.NAME_SEARCH_HANDLER,
		    new StringAndListParam().addAnd(new StringOrListParam().addOr(new StringParam(name))));
	}
	
	private List<String> uuids(SearchParameterMap params) {
		return dao.getSearchResults(params).stream().map(FhirTask::getUuid).collect(Collectors.toList());
	}
}
