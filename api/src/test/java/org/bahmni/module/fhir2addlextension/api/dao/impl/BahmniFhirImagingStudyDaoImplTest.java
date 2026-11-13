package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.bahmni.module.fhir2addlextension.api.BaseFhir2AddlContextSensitiveTest;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirImagingStudyDao;
import org.bahmni.module.fhir2addlextension.api.model.FhirImagingStudy;
import org.bahmni.module.fhir2addlextension.api.model.FhirImagingStudy.FhirImagingStudyStatus;
import org.bahmni.module.fhir2addlextension.api.model.FhirImagingStudyNote;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Obs;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class BahmniFhirImagingStudyDaoImplTest extends BaseFhir2AddlContextSensitiveTest {
	
	private static final String DATASET = "org/bahmni/module/fhir2addlextension/api/dao/impl/BahmniFhirImagingStudyDaoImplTest_data.xml";
	
	private static final String IS_1 = "img-9001-0000-0000-0000-000000000001";
	
	private static final String IS_2 = "img-9002-0000-0000-0000-000000000002";
	
	private static final String IS_3 = "img-9003-0000-0000-0000-000000000003";
	
	private static final String IS_4 = "img-9004-0000-0000-0000-000000000004";
	
	private static final String PATIENT_2 = "da7f524f-27ce-4bb2-86d6-6d1d05312bd5";
	
	private static final String PATIENT_7 = "5946f880-b197-400b-9caa-a3c661d23041";
	
	private static final String ORDER_1 = "921de0a3-05c4-444a-be03-e01b4c4b9142";
	
	private static final String ORDER_2 = "dfca4077-493c-496b-8312-856ee5d1cc26";
	
	private static final String ORDER_222 = "2662e6c2-697b-11e3-bd76-0800271c1b75";
	
	@Autowired
	private BahmniFhirImagingStudyDao dao;
	
	@Before
	public void loadDataset() throws Exception {
		executeDataSet(DATASET);
	}
	
	@Test
	public void search_withoutParameters_returnsAllNonVoidedStudies() {
		assertThat(uuids(map()), containsInAnyOrder(IS_1, IS_2, IS_3, IS_4));
	}
	
	@Test
	public void search_byPatient_returnsOnlyThatPatientsStudiesAndSkipsVoided() {
		assertThat(uuids(map().addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, reference(PATIENT_7))),
		    containsInAnyOrder(IS_1, IS_2));
		assertThat(uuids(map().addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, reference(PATIENT_2))),
		    containsInAnyOrder(IS_3, IS_4));
	}
	
	@Test
	public void search_byUnknownPatient_returnsNothing() {
		assertThat(
		    uuids(map().addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER,
		        reference("00000000-0000-0000-0000-000000000000"))), empty());
	}
	
	@Test
	public void search_byBasedOnOrder_returnsStudiesOfThatOrderOnly() {
		assertThat(uuids(basedOn(ORDER_1)), contains(IS_1));
		assertThat(uuids(basedOn(ORDER_222)), contains(IS_4));
	}
	
	@Test
	public void search_byBasedOnOrder_doesNotReturnVoidedStudyOfThatOrder() {
		assertThat(uuids(basedOn(ORDER_2)), contains(IS_3));
	}
	
	@Test
	public void search_byBasedOnOrList_returnsUnion() {
		ReferenceOrListParam or = new ReferenceOrListParam().add(new ReferenceParam(ORDER_1)).add(
		    new ReferenceParam(ORDER_222));
		SearchParameterMap params = map().addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER,
		    new ReferenceAndListParam().addAnd(or));
		assertThat(uuids(params), containsInAnyOrder(IS_1, IS_4));
	}
	
	@Test
	public void search_byUnknownBasedOn_returnsNothing() {
		assertThat(uuids(basedOn("00000000-0000-0000-0000-000000000000")), empty());
	}
	
	@Test
	public void search_withUnsupportedSearchHandler_ignoresItAndReturnsUnfilteredResults() {
		SearchParameterMap params = map().addParameter(FhirConstants.GENDER_SEARCH_HANDLER,
		    new TokenAndListParam().addAnd(new TokenParam("F")));
		assertThat(uuids(params), containsInAnyOrder(IS_1, IS_2, IS_3, IS_4));
	}
	
	@Test
	public void search_byPatientAndBasedOn_combinesWithAnd() {
		SearchParameterMap matching = basedOn(ORDER_222).addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER,
		    reference(PATIENT_2));
		assertThat(uuids(matching), contains(IS_4));
		
		SearchParameterMap contradicting = basedOn(ORDER_222).addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER,
		    reference(PATIENT_7));
		assertThat(uuids(contradicting), empty());
	}
	
	@Test
	public void search_byIdProperty_returnsStudyWithThatUuid() {
		SearchParameterMap params = map().addParameter(FhirConstants.COMMON_SEARCH_HANDLER, FhirConstants.ID_PROPERTY,
		    new TokenAndListParam().addAnd(new TokenParam(IS_2)));
		assertThat(uuids(params), contains(IS_2));
	}
	
	@Test
	public void count_matchesSearchResultsAndIgnoresPaging() {
		assertThat(dao.getSearchResultsCount(map()), equalTo(4));
		assertThat(dao.getSearchResultsCount(basedOn(ORDER_1)), equalTo(1));
		
		SearchParameterMap paged = map();
		paged.setFromIndex(0);
		paged.setToIndex(2);
		assertThat(dao.getSearchResults(paged), hasSize(2));
	}
	
	@Test
	public void get_byUuid_returnsFullyMappedStudy() {
		FhirImagingStudy study = dao.get(IS_1);

		assertThat(study, notNullValue());
		assertThat(study.getSubject().getUuid(), equalTo(PATIENT_7));
		assertThat(study.getOrder().getUuid(), equalTo(ORDER_1));
		assertThat(study.getEncounter().getEncounterId(), equalTo(3));
		assertThat(study.getStatus(), equalTo(FhirImagingStudyStatus.AVAILABLE));
		assertThat(study.getStudyInstanceUuid(), equalTo("1.2.840.1"));
		assertThat(study.getDescription(), equalTo("chest xray"));
		assertThat(study.getPerformer().getProviderId(), equalTo(1));
		assertThat(study.getLocation().getLocationId(), equalTo(2));
		assertThat(study.getDateStarted(), notNullValue());
		assertThat(study.getDateCompleted(), notNullValue());
		assertThat(study.getNotes(), hasSize(1));
		assertThat(study.getNotes().iterator().next().getNote(), equalTo("looks fine"));
		assertThat(study.getAssessment().stream().map(Obs::getObsId).collect(Collectors.toList()), contains(7));
	}
	
	@Test
	public void get_unknownUuid_returnsNull() {
		assertThat(dao.get("does-not-exist"), nullValue());
	}
	
	@Test
	public void get_byUuidCollection_returnsOnlyMatchingStudies() {
		List<FhirImagingStudy> studies = dao.get(java.util.Arrays.asList(IS_1, IS_3, "does-not-exist"));
		assertThat(studies.stream().map(FhirImagingStudy::getUuid).collect(Collectors.toList()),
		    containsInAnyOrder(IS_1, IS_3));
	}
	
	@Test
	public void createOrUpdate_persistsScalarsEnumNotesAndAssessment() {
		FhirImagingStudy study = new FhirImagingStudy();
		study.setSubject(Context.getPatientService().getPatient(6));
		study.setOrder(Context.getOrderService().getOrder(222));
		study.setEncounter(Context.getEncounterService().getEncounter(6));
		study.setStatus(FhirImagingStudyStatus.REGISTERED);
		study.setStudyInstanceUuid("9.9.9");
		study.setDescription("new study");
		study.setDateStarted(new Date(1_000_000_000_000L));
		study.setPerformer(Context.getProviderService().getProvider(1));
		study.setLocation(Context.getLocationService().getLocation(2));
		FhirImagingStudyNote note = new FhirImagingStudyNote();
		note.setImagingStudy(study);
		note.setNote("first note");
		note.setPerformer(Context.getProviderService().getProvider(1));
		study.getNotes().add(note);
		study.getAssessment().add(Context.getObsService().getObs(10));

		String uuid = dao.createOrUpdate(study).getUuid();
		flushAndClear();

		FhirImagingStudy reloaded = dao.get(uuid);
		assertThat(reloaded.getImagingStudyId(), notNullValue());
		assertThat(reloaded.getCreator(), notNullValue());
		assertThat(reloaded.getSubject().getPatientId(), equalTo(6));
		assertThat(reloaded.getOrder().getUuid(), equalTo(ORDER_222));
		assertThat(reloaded.getStatus(), equalTo(FhirImagingStudyStatus.REGISTERED));
		assertThat(reloaded.getStudyInstanceUuid(), equalTo("9.9.9"));
		assertThat(reloaded.getDateStarted().getTime(), equalTo(1_000_000_000_000L));
		assertThat(reloaded.getNotes(), hasSize(1));
		assertThat(reloaded.getNotes().iterator().next().getNote(), equalTo("first note"));
		assertThat(reloaded.getNotes().iterator().next().getImagingStudy().getUuid(), equalTo(uuid));
		assertThat(reloaded.getAssessment().stream().map(Obs::getObsId).collect(Collectors.toList()), contains(10));
		assertThat(uuids(basedOn(ORDER_222)), containsInAnyOrder(IS_4, uuid));
	}
	
	@Test
	public void createOrUpdate_onLoadedEntity_updatesStatusAndAddsNote() {
		FhirImagingStudy study = dao.get(IS_2);
		study.setStatus(FhirImagingStudyStatus.AVAILABLE);
		FhirImagingStudyNote note = new FhirImagingStudyNote();
		note.setImagingStudy(study);
		note.setNote("added later");
		study.getNotes().add(note);
		dao.createOrUpdate(study);
		flushAndClear();

		FhirImagingStudy reloaded = dao.get(IS_2);
		assertThat(reloaded.getStatus(), equalTo(FhirImagingStudyStatus.AVAILABLE));
		assertThat(reloaded.getChangedBy(), notNullValue());
		assertThat(reloaded.getNotes().stream().map(FhirImagingStudyNote::getNote).collect(Collectors.toList()),
		    contains("added later"));
	}
	
	@Test
	public void delete_isSoftDelete_studyDisappearsFromSearch() {
		assertThat(dao.delete(IS_1), notNullValue());
		flushAndClear();
		
		assertThat(uuids(map()), not(hasItem(IS_1)));
		assertThat(dao.get(IS_1).getVoided(), equalTo(true));
	}
	
	@Test
	public void delete_unknownUuid_returnsNull() {
		assertThat(dao.delete("does-not-exist"), nullValue());
	}
	
	@Test
	public void unauthenticatedUser_cannotSearchGetOrModify() {
		Context.logout();

		assertThrows(APIAuthenticationException.class, () -> dao.getSearchResults(map()));
		assertThrows(APIAuthenticationException.class, () -> dao.getSearchResultsCount(map()));
		assertThrows(APIAuthenticationException.class, () -> dao.get(IS_1));
		assertThrows(APIAuthenticationException.class, () -> dao.createOrUpdate(new FhirImagingStudy()));
		assertThrows(APIAuthenticationException.class, () -> dao.delete(IS_1));
	}
	
	private SearchParameterMap map() {
		return new SearchParameterMap();
	}
	
	private SearchParameterMap basedOn(String orderUuid) {
		return map().addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, reference(orderUuid));
	}
	
	private ReferenceAndListParam reference(String uuid) {
		return new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam(uuid)));
	}
	
	private List<String> uuids(SearchParameterMap params) {
		return dao.getSearchResults(params).stream().map(FhirImagingStudy::getUuid).collect(Collectors.toList());
	}
	
	private void flushAndClear() {
		Context.flushSession();
		Context.clearSession();
	}
}
