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

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.bahmni.module.fhir2addlextension.api.BaseFhir2AddlContextSensitiveTest;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirDiagnosticReportDao;
import org.bahmni.module.fhir2addlextension.api.model.Attachment;
import org.bahmni.module.fhir2addlextension.api.model.FhirDiagnosticReportExt;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Provider;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.model.FhirDiagnosticReport.DiagnosticReportStatus;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class BahmniFhirDiagnosticReportDaoImplTest extends BaseFhir2AddlContextSensitiveTest {
	
	private static final String DATASET = "org/bahmni/module/fhir2addlextension/api/dao/impl/BahmniFhirDiagnosticReportDaoImplTest_data.xml";
	
	private static final String DR_1 = "dgr-9001-0000-0000-0000-000000000001";
	
	private static final String DR_2 = "dgr-9002-0000-0000-0000-000000000002";
	
	private static final String DR_3 = "dgr-9003-0000-0000-0000-000000000003";
	
	private static final String DR_4_VOIDED = "dgr-9004-0000-0000-0000-000000000004";
	
	private static final String PATIENT_2 = "da7f524f-27ce-4bb2-86d6-6d1d05312bd5";
	
	private static final String PATIENT_7 = "5946f880-b197-400b-9caa-a3c661d23041";
	
	private static final String ENCOUNTER_3 = "6519d653-393b-4118-9c83-a3715b82d4ac";
	
	private static final String ENCOUNTER_4 = "eec646cb-c847-45a7-98bc-91c8c4f70add";
	
	private static final String ENCOUNTER_6 = "y403fafb-e5e4-42d0-9d11-4f52e89d123r";
	
	private static final String ORDER_1 = "921de0a3-05c4-444a-be03-e01b4c4b9142";
	
	private static final String ORDER_2 = "dfca4077-493c-496b-8312-856ee5d1cc26";
	
	private static final String ORDER_222 = "2662e6c2-697b-11e3-bd76-0800271c1b75";
	
	private static final String OBS_7 = "39fb7f47-e80a-4056-9285-bd798be13c63";
	
	private static final String OBS_9 = "be48cdcb-6a76-47e3-9f2e-2635032f3a9a";
	
	private static final String OBS_10 = "2f616900-5e7c-4667-9a7f-dcb260abf1de";
	
	private static final String SSTRM = "urn:test:sstrm";
	
	private static final String SNOMED = "http://snomed.info/sct";
	
	@Autowired
	private BahmniFhirDiagnosticReportDao dao;
	
	@Before
	public void loadDataset() throws Exception {
		executeDataSet(DATASET);
	}
	
	@Test
	public void search_withoutParameters_returnsAllNonVoidedReports() {
		assertThat(uuids(map()), containsInAnyOrder(DR_1, DR_2, DR_3));
	}
	
	@Test
	public void search_byPatient_returnsOnlyThatPatientsReportsAndSkipsVoided() {
		assertThat(uuids(map().addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, reference(PATIENT_7))),
		    containsInAnyOrder(DR_1, DR_2));
		assertThat(uuids(map().addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, reference(PATIENT_2))),
		    contains(DR_3));
	}
	
	@Test
	public void search_byUnknownPatient_returnsNothing() {
		assertThat(
		    uuids(map().addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER,
		        reference("00000000-0000-0000-0000-000000000000"))), empty());
	}
	
	@Test
	public void search_byEncounter_returnsOnlyReportsOfThatEncounter() {
		assertThat(uuids(map().addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, reference(ENCOUNTER_3))),
		    contains(DR_1));
		assertThat(uuids(map().addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, reference(ENCOUNTER_6))),
		    contains(DR_3));
	}
	
	@Test
	public void search_byEncounterOrList_returnsUnion() {
		ReferenceOrListParam or = new ReferenceOrListParam().add(new ReferenceParam(ENCOUNTER_3)).add(
		    new ReferenceParam(ENCOUNTER_4));
		assertThat(
		    uuids(map().addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER,
		        new ReferenceAndListParam().addAnd(or))), containsInAnyOrder(DR_1, DR_2));
	}
	
	@Test
	public void search_byCodeSystemAndValue_returnsReportsWhoseCodeHasThatMapping() {
		assertThat(uuids(code(new TokenParam(SSTRM, "WGT234"))), containsInAnyOrder(DR_1, DR_3));
		assertThat(uuids(code(new TokenParam(SSTRM, "CD41003"))), contains(DR_2));
		assertThat(uuids(code(new TokenParam(SNOMED, "7345693"))), contains(DR_2));
	}
	
	@Test
	public void search_byCodeWithWrongSystem_returnsNothing() {
		assertThat(uuids(code(new TokenParam(SNOMED, "WGT234"))), empty());
	}
	
	@Test
	public void search_byUnknownCode_returnsNothing() {
		assertThat(uuids(code(new TokenParam(SSTRM, "NOPE"))), empty());
	}
	
	@Test
	public void search_byConceptUuid_matchesTheCodeConcept() {
		assertThat(uuids(code(new TokenParam(null, "a09ab2c5-878e-4905-b25d-5784167d0216"))), contains(DR_2));
	}
	
	@Test
	public void search_byCodeOrList_returnsUnion() {
		TokenAndListParam params = new TokenAndListParam().addAnd(new TokenOrListParam()
		        .add(new TokenParam(SSTRM, "WGT234")).add(new TokenParam(SSTRM, "CD41003")));
		assertThat(uuids(map().addParameter(FhirConstants.CODED_SEARCH_HANDLER, params)),
		    containsInAnyOrder(DR_1, DR_2, DR_3));
	}
	
	@Test
	public void search_byCodeAndList_requiresEveryCodeToMatch() {
		TokenAndListParam differentConcepts = new TokenAndListParam().addAnd(new TokenParam(SSTRM, "WGT234")).addAnd(
		    new TokenParam(SSTRM, "CD41003"));
		assertThat(uuids(map().addParameter(FhirConstants.CODED_SEARCH_HANDLER, differentConcepts)), empty());
	}
	
	@Test
	public void search_withUnsupportedSearchHandler_ignoresItAndReturnsUnfilteredResults() {
		SearchParameterMap params = map().addParameter(FhirConstants.GENDER_SEARCH_HANDLER,
		    new TokenAndListParam().addAnd(new TokenParam("F")));
		assertThat(uuids(params), containsInAnyOrder(DR_1, DR_2, DR_3));
	}
	
	@Test
	public void search_byIssuedDateRange_filtersOnIssuedDate() {
		DateRangeParam from = new DateRangeParam(new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, "2024-02-01"), null);
		assertThat(uuids(map().addParameter(FhirConstants.DATE_RANGE_SEARCH_HANDLER, from)), containsInAnyOrder(DR_2, DR_3));
		
		DateRangeParam window = new DateRangeParam(new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, "2024-01-01"),
		        new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, "2024-01-31"));
		assertThat(uuids(map().addParameter(FhirConstants.DATE_RANGE_SEARCH_HANDLER, window)), contains(DR_1));
	}
	
	@Test
	public void search_byResult_returnsReportsContainingThatObservation() {
		assertThat(uuids(result(OBS_7)), contains(DR_1));
		assertThat(uuids(result(OBS_9)), contains(DR_1));
		assertThat(uuids(result(OBS_10)), contains(DR_2));
	}
	
	@Test
	public void search_byResultOrList_returnsUnion() {
		ReferenceAndListParam or = new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(
		    new ReferenceParam(OBS_7)).add(new ReferenceParam(OBS_10)));
		assertThat(uuids(map().addParameter(FhirConstants.RESULT_SEARCH_HANDLER, or)), containsInAnyOrder(DR_1, DR_2));
	}
	
	@Test
	public void search_byBasedOnOrder_returnsReportsLinkedToThatOrder() {
		assertThat(uuids(basedOn(ORDER_1)), contains(DR_1));
		assertThat(uuids(basedOn(ORDER_2)), contains(DR_3));
		assertThat(uuids(basedOn(ORDER_222)), empty());
	}
	
	@Test
	public void search_byBasedOnOrList_returnsUnion() {
		ReferenceAndListParam or = new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(
		    new ReferenceParam(ORDER_1)).add(new ReferenceParam(ORDER_2)));
		assertThat(uuids(map().addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, or)),
		    containsInAnyOrder(DR_1, DR_3));
	}
	
	@Test
	public void search_withPatientCodeAndResultTogether_appliesAllFiltersWithAnd() {
		SearchParameterMap matching = code(new TokenParam(SSTRM, "WGT234")).addParameter(
		    FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, reference(PATIENT_7)).addParameter(
		    FhirConstants.RESULT_SEARCH_HANDLER, referenceAnd(OBS_9));
		assertThat(uuids(matching), contains(DR_1));
		
		SearchParameterMap contradicting = code(new TokenParam(SSTRM, "WGT234")).addParameter(
		    FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, reference(PATIENT_7)).addParameter(
		    FhirConstants.RESULT_SEARCH_HANDLER, referenceAnd(OBS_10));
		assertThat(uuids(contradicting), empty());
	}
	
	@Test
	public void search_byIdProperty_returnsReportWithThatUuid() {
		SearchParameterMap params = map().addParameter(FhirConstants.COMMON_SEARCH_HANDLER, FhirConstants.ID_PROPERTY,
		    new TokenAndListParam().addAnd(new TokenParam(DR_2)));
		assertThat(uuids(params), contains(DR_2));
	}
	
	@Test
	public void count_matchesSearchResultsAndIgnoresPaging() {
		assertThat(dao.getSearchResultsCount(map()), equalTo(3));
		assertThat(dao.getSearchResultsCount(code(new TokenParam(SSTRM, "WGT234"))), equalTo(2));
		
		SearchParameterMap paged = map();
		paged.setFromIndex(0);
		paged.setToIndex(2);
		assertThat(dao.getSearchResults(paged), hasSize(2));
	}
	
	@Test
	public void findByOrderUuid_returnsTheNonVoidedReportLinkedToTheOrder() {
		FhirDiagnosticReportExt report = dao.findByOrderUuid(ORDER_1);
		
		assertThat(report, notNullValue());
		assertThat(report.getUuid(), equalTo(DR_1));
		assertThat(report.getCode().getConceptId(), equalTo(5089));
	}
	
	@Test
	public void findByOrderUuid_returnsNullWhenNoReportLinkedToOrder() {
		assertThat(dao.findByOrderUuid(ORDER_222), nullValue());
		assertThat(dao.findByOrderUuid("does-not-exist"), nullValue());
	}
	
	@Test
	public void findByOrderUuid_returnsNullWhenOnlyVoidedReportExists() {
		dao.delete(DR_3);
		flushAndClear();
		
		assertThat(dao.findByOrderUuid(ORDER_2), nullValue());
	}
	
	@Test
	public void get_byUuid_returnsFullyMappedReport() {
		FhirDiagnosticReportExt report = dao.get(DR_1);

		assertThat(report, notNullValue());
		assertThat(report.getStatus(), equalTo(DiagnosticReportStatus.FINAL));
		assertThat(report.getSubject().getUuid(), equalTo(PATIENT_7));
		assertThat(report.getEncounter().getUuid(), equalTo(ENCOUNTER_3));
		assertThat(report.getIssued(), notNullValue());
		assertThat(report.getConclusion(), equalTo("all good"));
		assertThat(report.getResults().stream().map(Obs::getUuid).collect(Collectors.toList()),
		    containsInAnyOrder(OBS_7, OBS_9));
		assertThat(report.getOrders().stream().map(Order::getUuid).collect(Collectors.toList()), contains(ORDER_1));
		assertThat(report.getPerformers().stream().map(p -> p.getProviderId()).collect(Collectors.toList()), contains(1));
		assertThat(report.getPresentedForms().stream().map(Attachment::getTitle).collect(Collectors.toList()),
		    contains("report.pdf"));
	}
	
	@Test
	public void get_unknownUuid_returnsNull() {
		assertThat(dao.get("does-not-exist"), nullValue());
	}
	
	@Test
	public void createOrUpdate_persistsScalarsEnumAndAllAssociations() {
		FhirDiagnosticReportExt report = new FhirDiagnosticReportExt();
		report.setStatus(DiagnosticReportStatus.AMENDED);
		report.setCode(Context.getConceptService().getConcept(5497));
		report.setSubject(Context.getPatientService().getPatient(6));
		report.setEncounter(Context.getEncounterService().getEncounter(5));
		report.setIssued(new Date(1_000_000_000_000L));
		report.setConclusion("new conclusion");
		report.setResults(new HashSet<>(Arrays.asList(Context.getObsService().getObs(11))));
		report.setOrders(new HashSet<>(Arrays.asList(Context.getOrderService().getOrder(222))));
		Provider performer = new Provider();
		performer.setPerson(Context.getPersonService().getPerson(2));
		performer.setIdentifier("DR-PERFORMER");
		Context.getProviderService().saveProvider(performer);
		report.setPerformers(new HashSet<>(Arrays.asList(performer)));
		Attachment attachment = new Attachment();
		attachment.setTitle("scan.png");
		attachment.setContentType("image/png");
		attachment.setContentUrl("/scan.png");
		report.getPresentedForms().add(attachment);

		String uuid = dao.createOrUpdate(report).getUuid();
		flushAndClear();

		FhirDiagnosticReportExt reloaded = dao.get(uuid);
		assertThat(reloaded.getId(), notNullValue());
		assertThat(reloaded.getCreator(), notNullValue());
		assertThat(reloaded.getStatus(), equalTo(DiagnosticReportStatus.AMENDED));
		assertThat(reloaded.getCode().getConceptId(), equalTo(5497));
		assertThat(reloaded.getSubject().getPatientId(), equalTo(6));
		assertThat(reloaded.getEncounter().getEncounterId(), equalTo(5));
		assertThat(reloaded.getIssued().getTime(), equalTo(1_000_000_000_000L));
		assertThat(reloaded.getConclusion(), equalTo("new conclusion"));
		assertThat(reloaded.getResults().stream().map(Obs::getObsId).collect(Collectors.toList()), contains(11));
		assertThat(reloaded.getOrders().stream().map(Order::getUuid).collect(Collectors.toList()), contains(ORDER_222));
		assertThat(reloaded.getPerformers().stream().map(Provider::getIdentifier).collect(Collectors.toList()),
		    contains("DR-PERFORMER"));
		assertThat(reloaded.getPresentedForms(), hasSize(1));
		Attachment reloadedAttachment = reloaded.getPresentedForms().iterator().next();
		assertThat(reloadedAttachment.getAttachmentId(), notNullValue());
		assertThat(reloadedAttachment.getContentUrl(), equalTo("/scan.png"));
		assertThat(dao.findByOrderUuid(ORDER_222).getUuid(), equalTo(uuid));
	}
	
	@Test
	public void createOrUpdate_onLoadedEntity_updatesConclusionAndStatus() {
		FhirDiagnosticReportExt report = dao.get(DR_2);
		report.setConclusion("revised");
		report.setStatus(DiagnosticReportStatus.FINAL);
		dao.createOrUpdate(report);
		flushAndClear();
		
		FhirDiagnosticReportExt reloaded = dao.get(DR_2);
		assertThat(reloaded.getConclusion(), equalTo("revised"));
		assertThat(reloaded.getStatus(), equalTo(DiagnosticReportStatus.FINAL));
		assertThat(reloaded.getChangedBy(), notNullValue());
	}
	
	@Test
	public void delete_isSoftDelete_reportDisappearsFromSearch() {
		assertThat(dao.delete(DR_1), notNullValue());
		flushAndClear();
		
		assertThat(uuids(map()), not(hasItem(DR_1)));
		assertThat(dao.get(DR_1).getVoided(), equalTo(true));
	}
	
	@Test
	public void delete_unknownUuid_returnsNull() {
		assertThat(dao.delete("does-not-exist"), nullValue());
	}
	
	@Test
	public void unauthenticatedUser_cannotSearchGetOrFindByOrder() {
		Context.logout();

		assertThrows(APIAuthenticationException.class, () -> dao.getSearchResults(map()));
		assertThrows(APIAuthenticationException.class, () -> dao.get(DR_1));
		assertThrows(APIAuthenticationException.class, () -> dao.findByOrderUuid(ORDER_1));
		assertThrows(APIAuthenticationException.class, () -> dao.delete(DR_1));
	}
	
	private SearchParameterMap map() {
		return new SearchParameterMap();
	}
	
	private SearchParameterMap code(TokenParam token) {
		return map().addParameter(FhirConstants.CODED_SEARCH_HANDLER, new TokenAndListParam().addAnd(token));
	}
	
	private SearchParameterMap result(String obsUuid) {
		return map().addParameter(FhirConstants.RESULT_SEARCH_HANDLER, referenceAnd(obsUuid));
	}
	
	private SearchParameterMap basedOn(String orderUuid) {
		return map().addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, referenceAnd(orderUuid));
	}
	
	private ReferenceAndListParam reference(String uuid) {
		return referenceAnd(uuid);
	}
	
	private ReferenceAndListParam referenceAnd(String uuid) {
		return new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam(uuid)));
	}
	
	private List<String> uuids(SearchParameterMap params) {
		return dao.getSearchResults(params).stream().map(FhirDiagnosticReportExt::getUuid).collect(Collectors.toList());
	}
	
	private void flushAndClear() {
		Context.flushSession();
		Context.clearSession();
	}
}
