package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2addlextension.api.BaseFhir2AddlContextSensitiveTest;
import org.bahmni.module.fhir2addlextension.api.context.impl.OpenmrsAppContext;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirServiceRequestDao;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.DrugOrder;
import org.openmrs.GlobalProperty;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.TestOrder;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class BahmniFhirServiceRequestDaoImplTest extends BaseFhir2AddlContextSensitiveTest {
	
	private static final String DATASET = "org/bahmni/module/fhir2addlextension/api/dao/impl/BahmniFhirServiceRequestDaoImplTest_data.xml";
	
	// orders defined in this test's dataset
	private static final String SR1 = "sr-9001-0000-0000-0000-000000000001"; // lab, patient 7, enc 4 (loc 1), concept 5089
	
	private static final String SR2 = "sr-9002-0000-0000-0000-000000000002"; // lab, patient 2, enc 6 (loc 2), concept 5497, provider 9002
	
	private static final String SR3 = "sr-9003-0000-0000-0000-000000000003"; // x-ray, patient 7, enc 5 (loc 2), concept 5089, revises SR1
	
	private static final String SR4_VOIDED = "sr-9004-0000-0000-0000-000000000004";
	
	private static final String SR5 = "sr-9005-0000-0000-0000-000000000005"; // scheduled lab, patient 2, enc 6
	
	// non-drug orders of the standard OpenMRS test dataset (type 2 "Test order", patient 2, encounter 6, concept 5497)
	private static final String STD_6 = "1c96f25c-4949-4f72-9931-d808fbc226de";
	
	private static final String STD_7 = "2c96f25c-4949-4f72-9931-d808fbc226df";
	
	private static final String STD_8_VOIDED = "3c96f25c-4949-4f72-9931-d808fbc226dg";
	
	private static final String STD_9 = "4c96f25c-4949-4f72-9931-d808fbc226dh";
	
	private static final String DRUG_ORDER = "921de0a3-05c4-444a-be03-e01b4c4b9142";
	
	private static final String PATIENT_2 = "da7f524f-27ce-4bb2-86d6-6d1d05312bd5";
	
	private static final String PATIENT_7 = "5946f880-b197-400b-9caa-a3c661d23041";
	
	private static final String ENCOUNTER_4 = "eec646cb-c847-45a7-98bc-91c8c4f70add";
	
	private static final String ENCOUNTER_6 = "y403fafb-e5e4-42d0-9d11-4f52e89d123r";
	
	private static final String LOCATION_1 = "8d6c993e-c2cc-11de-8d13-0010c6dffd0f";
	
	private static final String LOCATION_2 = "9356400c-a5a2-4532-8f2b-2361b3446eb8";
	
	private static final String PROVIDER_1 = "c2299800-cca9-11e0-9572-0800200c9a66";
	
	private static final String PROVIDER_2 = "prov-9002-0000-0000-0000-000000000002";
	
	private static final String LAB_TYPE_UUID = "87f82f50-ac05-11e3-a5e2-0800200c9a66";
	
	private static final String XRAY_TYPE_UUID = "4db73380-ac06-11e3-a5e2-0800200c9a66";
	
	private static final String TEST_ORDER_TYPE_UUID = "52a447d3-a64a-11e3-9aeb-50e549534c5e";
	
	private static final String CODE_SYSTEM = "urn:test:sstrm";
	
	private static final String SNOMED_SYSTEM = "http://snomed.info/sct";
	
	@Autowired
	private BahmniFhirServiceRequestDao<Order> dao;
	
	@Autowired
	private AdministrationService administrationService;
	
	@Before
	public void loadDataset() throws Exception {
		executeDataSet(DATASET);
	}
	
	private List<String> uuids(SearchParameterMap params) {
		return dao.getSearchResults(params).stream().map(Order::getUuid).collect(Collectors.toList());
	}
	
	private static ReferenceAndListParam ref(String... values) {
		ReferenceOrListParam or = new ReferenceOrListParam();
		Arrays.stream(values).forEach(v -> or.add(new ReferenceParam(v)));
		return new ReferenceAndListParam().addAnd(or);
	}
	
	private static TokenAndListParam token(String system, String code) {
		return new TokenAndListParam().addAnd(new TokenOrListParam().add(new TokenParam(system, code)));
	}
	
	private static SearchParameterMap param(String handler, Object value) {
		return new SearchParameterMap().addParameter(handler, value);
	}
	
	// ---- baseline / drug-order exclusion / paging ----
	
	@Test
	public void search_withNoParameters_returnsAllNonDrugOrdersAndNeverDrugOrders() {
		List<String> result = uuids(new SearchParameterMap());
		
		assertThat(result, containsInAnyOrder(SR1, SR2, SR3, SR5, STD_6, STD_7, STD_9));
		assertThat(result, not(hasItem(DRUG_ORDER)));
	}
	
	@Test
	public void search_neverReturnsVoidedOrders() {
		List<String> result = uuids(new SearchParameterMap());
		
		assertThat(result, not(hasItem(SR4_VOIDED)));
		assertThat(result, not(hasItem(STD_8_VOIDED)));
	}
	
	@Test
	public void searchCount_matchesTheNumberOfFilteredResults() {
		SearchParameterMap params = param(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, ref(PATIENT_7));
		
		assertThat(dao.getSearchResultsCount(params), equalTo(2));
	}
	
	@Test
	public void search_withPagingWindow_returnsOnlyRequestedNumberOfRows() {
		SearchParameterMap params = new SearchParameterMap();
		params.setFromIndex(0);
		params.setToIndex(2);
		
		assertThat(dao.getSearchResults(params), hasSize(2));
	}
	
	// ---- patient / encounter ----
	
	@Test
	public void search_byPatient_returnsOnlyThatPatientsNonDrugOrders() {
		assertThat(uuids(param(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, ref(PATIENT_7))),
		    containsInAnyOrder(SR1, SR3));
		assertThat(uuids(param(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, ref(PATIENT_2))),
		    containsInAnyOrder(SR2, SR5, STD_6, STD_7, STD_9));
	}
	
	@Test
	public void search_byUnknownPatient_returnsNothing() {
		assertThat(uuids(param(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, ref("no-such-patient"))), empty());
	}
	
	@Test
	public void search_byEncounter_returnsOrdersOfThatEncounterOnly() {
		assertThat(uuids(param(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, ref(ENCOUNTER_4))), containsInAnyOrder(SR1));
	}
	
	@Test
	public void search_byEncounterAndPatientOfAnotherEncounter_isAnAndAndReturnsNothing() {
		SearchParameterMap params = param(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, ref(ENCOUNTER_6)).addParameter(
		    FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, ref(PATIENT_7));
		
		assertThat(uuids(params), empty());
	}
	
	// ---- code ----
	
	@Test
	public void search_byCodeOfConceptMapping_matchesOnlyOrdersWithThatConcept() {
		assertThat(uuids(param(FhirConstants.CODED_SEARCH_HANDLER, token(CODE_SYSTEM, "WGT234"))),
		    containsInAnyOrder(SR1, SR3));
		assertThat(uuids(param(FhirConstants.CODED_SEARCH_HANDLER, token(SNOMED_SYSTEM, "7345693"))),
		    containsInAnyOrder(SR2, SR5, STD_6, STD_7, STD_9));
	}
	
	@Test
	public void search_byCodeWithSystemThatDoesNotOwnTheCode_returnsNothing() {
		assertThat(uuids(param(FhirConstants.CODED_SEARCH_HANDLER, token(SNOMED_SYSTEM, "WGT234"))), empty());
	}
	
	@Test
	public void search_byConceptUuid_matchesOrdersWithThatConcept() {
		assertThat(uuids(param(FhirConstants.CODED_SEARCH_HANDLER, token(null, "c607c80f-1ea9-4da3-bb88-6276ce8868dd"))),
		    containsInAnyOrder(SR1, SR3));
	}
	
	@Test
	public void search_byCodeAndPatientWithoutSuchOrder_returnsNothing() {
		SearchParameterMap params = param(FhirConstants.CODED_SEARCH_HANDLER, token(CODE_SYSTEM, "WGT234")).addParameter(
		    FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, ref(PATIENT_2));
		
		assertThat(uuids(params), empty());
	}
	
	// ---- participant (orderer) ----
	
	@Test
	public void search_byParticipant_returnsOrdersPlacedByThatProviderOnly() {
		List<String> result = uuids(param(FhirConstants.PARTICIPANT_REFERENCE_SEARCH_HANDLER, ref(PROVIDER_2)));
		
		assertThat(result, containsInAnyOrder(SR2));
	}
	
	@Test
	public void search_byParticipantOtherThanOrderer_excludesThoseOrders() {
		List<String> result = uuids(param(FhirConstants.PARTICIPANT_REFERENCE_SEARCH_HANDLER, ref(PROVIDER_1)));
		
		assertThat(result, hasItem(SR1));
		assertThat(result, not(hasItem(SR2)));
	}
	
	// ---- date range ----
	
	private static SearchParameterMap dateRange(String lower, String upper) {
		DateRangeParam range = new DateRangeParam();
		if (lower != null) {
			range.setLowerBound(lower);
		}
		if (upper != null) {
			range.setUpperBound(upper);
		}
		return param(FhirConstants.DATE_RANGE_SEARCH_HANDLER, range);
	}
	
	@Test
	public void search_dateRangeLowerBoundOnly_matchesOrdersActivatedOrScheduledOnOrAfterBound() {
		assertThat(uuids(dateRange("2024-02-15", null)), containsInAnyOrder(SR2, SR5));
	}
	
	@Test
	public void search_dateRangeLowerBoundOnly_matchesOrdersByScheduledDateEvenWhenActivatedEarlier() {
		// SR5 activated 2024-05-01 but scheduled 2024-06-01: only the scheduled date is >= 2024-05-15
		assertThat(uuids(dateRange("2024-05-15", null)), containsInAnyOrder(SR5));
	}
	
	@Test
	public void search_dateRangeUpperBoundOnly_matchesOrdersStoppedOrExpiringOnOrBeforeBound() {
		List<String> result = uuids(dateRange(null, "2024-02-15"));
		
		// SR1 auto-expires 2024-01-20, SR3 stopped 2024-02-10
		assertThat(result, hasItem(SR1));
		assertThat(result, hasItem(SR3));
		assertThat(result, not(hasItem(SR5)));
		assertThat(result, not(hasItem(SR2)));
	}
	
	@Test
	public void search_dateRangeUpperBound_usesStopDateNotActivationDate() {
		// SR3 was activated 2024-02-01 (before the bound) but only stopped 2024-02-10 (after it)
		List<String> result = uuids(dateRange(null, "2024-02-05"));
		
		assertThat(result, hasItem(SR1));
		assertThat(result, not(hasItem(SR3)));
	}
	
	@Test
	public void search_dateRangeWithBothBounds_requiresStartAfterLowerAndEndBeforeUpper() {
		assertThat(uuids(dateRange("2024-04-01", "2024-12-31")), containsInAnyOrder(SR5));
		assertThat(uuids(dateRange("2024-01-25", "2024-02-28")), containsInAnyOrder(SR3));
	}
	
	@Test
	public void search_dateRangeInTheFuture_returnsNothing() {
		assertThat(uuids(dateRange("2030-01-01", "2030-12-31")), empty());
	}
	
	@Test
	public void search_emptyDateRange_isIgnoredAndReturnsEverything() {
		assertThat(uuids(param(FhirConstants.DATE_RANGE_SEARCH_HANDLER, new DateRangeParam())),
		    containsInAnyOrder(SR1, SR2, SR3, SR5, STD_6, STD_7, STD_9));
	}
	
	// ---- category ----
	
	@Test
	public void search_byCategoryMappedInGlobalProperty_resolvesToOrderTypeByName() {
		administrationService.saveGlobalProperty(new GlobalProperty(OpenmrsAppContext.PROP_ORDER_TYPE_TO_CATEGORY_MAP,
		        "Lab test:laboratory;X-Ray:imaging"));
		
		assertThat(uuids(param(FhirConstants.CATEGORY_SEARCH_HANDLER, ref("imaging"))), containsInAnyOrder(SR3));
		assertThat(uuids(param(FhirConstants.CATEGORY_SEARCH_HANDLER, ref("laboratory"))), containsInAnyOrder(SR1, SR2, SR5));
	}
	
	@Test
	public void search_byCategoryWithoutMapping_treatsValueAsOrderTypeUuid() {
		assertThat(uuids(param(FhirConstants.CATEGORY_SEARCH_HANDLER, ref(XRAY_TYPE_UUID))), containsInAnyOrder(SR3));
	}
	
	@Test
	public void search_byCategoryMappedToUnknownOrderTypeName_fallsBackToRawValueAndMatchesNothing() {
		administrationService.saveGlobalProperty(new GlobalProperty(OpenmrsAppContext.PROP_ORDER_TYPE_TO_CATEGORY_MAP,
		        "No Such Type:ghost"));
		
		assertThat(uuids(param(FhirConstants.CATEGORY_SEARCH_HANDLER, ref("ghost"))), empty());
	}
	
	@Test
	public void search_byCategoryOrList_matchesEitherOrderType() {
		assertThat(uuids(param(FhirConstants.CATEGORY_SEARCH_HANDLER, ref(XRAY_TYPE_UUID, LAB_TYPE_UUID))),
		    containsInAnyOrder(SR1, SR2, SR3, SR5));
	}
	
	@Test
	public void search_byDrugOrderTypeCategory_stillNeverReturnsDrugOrders() {
		assertThat(uuids(param(FhirConstants.CATEGORY_SEARCH_HANDLER, ref(OrderType.DRUG_ORDER_TYPE_UUID))), empty());
	}
	
	// ---- basedOn (previousOrder) ----
	
	@Test
	public void search_byBasedOn_returnsOrdersThatReviseThePreviousOrder() {
		assertThat(uuids(param(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, ref(SR1))), containsInAnyOrder(SR3));
	}
	
	@Test
	public void search_byBasedOnOnOrderWithNoRevisions_returnsNothing() {
		assertThat(uuids(param(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, ref(SR2))), empty());
	}
	
	// ---- location (via the order's encounter) ----
	
	@Test
	public void search_byLocation_returnsOrdersWhoseEncounterIsAtThatLocation() {
		assertThat(uuids(param(BahmniFhirConstants.ORDER_LOCATION_SEARCH_HANDLER, ref(LOCATION_1))), containsInAnyOrder(SR1));
		assertThat(uuids(param(BahmniFhirConstants.ORDER_LOCATION_SEARCH_HANDLER, ref(LOCATION_2))),
		    containsInAnyOrder(SR2, SR3, SR5, STD_6, STD_7, STD_9));
	}
	
	@Test
	public void search_byLocationAndEncounterAtAnotherLocation_intersectsAndReturnsNothing() {
		SearchParameterMap params = param(BahmniFhirConstants.ORDER_LOCATION_SEARCH_HANDLER, ref(LOCATION_2)).addParameter(
		    FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, ref(ENCOUNTER_4));
		
		assertThat(uuids(params), empty());
	}
	
	@Test
	public void search_withNullValuedCodeCategoryBasedOnLocationAndDateParameters_ignoresThemAndReturnsEverything() {
		List<String> unfiltered = uuids(new SearchParameterMap());
		
		for (String handler : Arrays.asList(FhirConstants.CODED_SEARCH_HANDLER, FhirConstants.CATEGORY_SEARCH_HANDLER,
		    FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, BahmniFhirConstants.ORDER_LOCATION_SEARCH_HANDLER,
		    FhirConstants.DATE_RANGE_SEARCH_HANDLER)) {
			assertThat(handler, uuids(param(handler, null)), containsInAnyOrder(unfiltered.toArray()));
		}
	}
	
	// ---- common parameters ----
	
	@Test
	public void search_byIdCommonParameter_returnsThatOrderOnly() {
		SearchParameterMap params = new SearchParameterMap().addParameter(FhirConstants.COMMON_SEARCH_HANDLER,
		    FhirConstants.ID_PROPERTY, new TokenAndListParam().addAnd(new TokenParam(SR3)));
		
		assertThat(uuids(params), containsInAnyOrder(SR3));
	}
	
	// ---- get ----
	
	@Test
	public void get_byUuid_returnsServiceRequestOrder() {
		Order order = dao.get(SR1);
		
		assertThat(order, notNullValue());
		assertThat(order.getUuid(), equalTo(SR1));
	}
	
	@Test
	public void get_byUuidOfDrugOrder_returnsNull() {
		assertThat(dao.get(DRUG_ORDER), nullValue());
	}
	
	@Test
	public void get_byUnknownUuid_returnsNull() {
		assertThat(dao.get("does-not-exist"), nullValue());
	}
	
	@Test
	public void get_byUuidCollection_skipsDrugOrdersVoidedOrdersAndUnknownUuids() {
		List<Order> orders = dao.get(Arrays.asList(SR1, SR3, SR4_VOIDED, DRUG_ORDER, "unknown"));

		assertThat(orders.stream().map(Order::getUuid).collect(Collectors.toList()), containsInAnyOrder(SR1, SR3));
	}
	
	@Test
	public void get_byEmptyOrNullUuidCollection_returnsEmptyList() {
		assertThat(dao.get(Collections.<String> emptyList()), empty());
		assertThat(dao.get((Collection<String>) null), empty());
	}
	
	// ---- createOrUpdate ----
	
	@Test
	public void createOrUpdate_withDrugOrderType_isRejected() {
		DrugOrder drugOrder = new DrugOrder();
		drugOrder.setOrderType(Context.getOrderService().getOrderTypeByUuid(OrderType.DRUG_ORDER_TYPE_UUID));

		InvalidRequestException ex = assertThrows(InvalidRequestException.class, () -> dao.createOrUpdate(drugOrder));

		assertThat(ex.getMessage(), is("Drug Orders cannot be submitted through ServiceRequest "));
	}
	
	private TestOrder newTestOrder() {
		TestOrder order = new TestOrder();
		order.setPatient(Context.getPatientService().getPatientByUuid(PATIENT_2));
		order.setConcept(Context.getConceptService().getConcept(5497));
		order.setEncounter(Context.getEncounterService().getEncounterByUuid(ENCOUNTER_6));
		order.setOrderer(Context.getProviderService().getProvider(1));
		order.setCareSetting(Context.getOrderService().getCareSetting(1));
		order.setOrderType(Context.getOrderService().getOrderTypeByUuid(TEST_ORDER_TYPE_UUID));
		order.setUrgency(Order.Urgency.ROUTINE);
		order.setAction(Order.Action.NEW);
		order.setDateActivated(new Date(System.currentTimeMillis() - 24L * 3600 * 1000));
		return order;
	}
	
	@Test
	public void createOrUpdate_withTestOrder_persistsAndMakesOrderRetrievableAndSearchable() {
		Order saved = dao.createOrUpdate(newTestOrder());
		Context.flushSession();
		
		assertThat(saved.getOrderId(), notNullValue());
		assertThat(dao.get(saved.getUuid()).getOrderId(), equalTo(saved.getOrderId()));
		assertThat(uuids(param(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, ref(ENCOUNTER_6))),
		    hasItem(saved.getUuid()));
	}
	
	@Test
	public void createOrUpdate_withDiscontinueOrder_stopsThePreviousOrderAndLinksBackToIt() {
		Order original = dao.createOrUpdate(newTestOrder());
		Context.flushSession();
		
		TestOrder revoke = newTestOrder();
		revoke.setAction(Order.Action.DISCONTINUE);
		revoke.setPreviousOrder(original);
		revoke.setDateActivated(new Date());
		Order saved = dao.createOrUpdate(revoke);
		Context.flushSession();
		
		assertThat(saved.getAction(), equalTo(Order.Action.DISCONTINUE));
		assertThat(dao.get(original.getUuid()).getDateStopped(), notNullValue());
		assertThat(uuids(param(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, ref(original.getUuid()))),
		    containsInAnyOrder(saved.getUuid()));
	}
}
