package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2addlextension.api.BaseFhir2AddlContextSensitiveTest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.DrugOrder;
import org.openmrs.FreeTextDosingInstructions;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.FhirMedicationRequestDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class BahmniFhirMedicationRequestDaoImplTest extends BaseFhir2AddlContextSensitiveTest {
	
	private static final String DATASET = "org/bahmni/module/fhir2addlextension/api/dao/impl/BahmniFhirMedicationRequestDaoImplTest_data.xml";
	
	// drug orders of the standard OpenMRS test dataset. Concept 88 maps to SSTRM:WGT234, concept 792 to SNOMED:7345693
	// (see this test's dataset); concept 3 has no mapping.
	private static final String ORD_1 = "921de0a3-05c4-444a-be03-e01b4c4b9142"; // patient 7, enc 3 (loc 1), concept 88
	
	private static final String ORD_111 = "e1f95924-697a-11e3-bd76-0800271c1b75"; // patient 7, enc 3 (loc 1), concept 88
	
	private static final String ORD_2 = "dfca4077-493c-496b-8312-856ee5d1cc26"; // patient 2, enc 6 (loc 2), concept 792
	
	private static final String ORD_22 = "efca4077-493c-496b-8312-856ee5d1cc27"; // DISCONTINUE of ORD_2
	
	private static final String ORD_222 = "2662e6c2-697b-11e3-bd76-0800271c1b75"; // concept 792
	
	private static final String ORD_3 = "e3d621f0-a4d5-47d1-a4e1-5ace3f66d43a"; // concept 792
	
	private static final String ORD_4 = "047b7424-6f33-4357-823c-420f316bb039"; // concept 88
	
	private static final String ORD_44 = "147b7424-6f33-4357-823c-420f316bb038"; // DISCONTINUE of ORD_4
	
	private static final String ORD_444 = "9c21e407-697b-11e3-bd76-0800271c1b75"; // concept 88
	
	private static final String ORD_5 = "0c96f25c-4949-4f72-9931-d808fbc226db"; // concept 3 (unmapped)
	
	private static final String TEST_ORDER = "1c96f25c-4949-4f72-9931-d808fbc226de"; // not a drug order
	
	private static final String CONCEPT_792_UUID = "d144d24f-6913-4b63-9660-a9108c2bebef";
	
	private static final String PATIENT_7 = "5946f880-b197-400b-9caa-a3c661d23041";
	
	private static final String LOCATION_1 = "8d6c993e-c2cc-11de-8d13-0010c6dffd0f";
	
	private static final String LOCATION_2 = "9356400c-a5a2-4532-8f2b-2361b3446eb8";
	
	private static final String SSTRM = "urn:test:sstrm";
	
	private static final String SNOMED = "http://snomed.info/sct";
	
	@Autowired
	private FhirMedicationRequestDao dao;
	
	@Before
	public void loadDataset() throws Exception {
		executeDataSet(DATASET);
	}
	
	private List<String> uuids(SearchParameterMap params) {
		return dao.getSearchResults(params).stream().map(DrugOrder::getUuid).collect(Collectors.toList());
	}
	
	private static TokenAndListParam code(String system, String code) {
		return new TokenAndListParam().addAnd(new TokenOrListParam().add(new TokenParam(system, code)));
	}
	
	private static SearchParameterMap codeSearch(TokenAndListParam code) {
		return new SearchParameterMap().addParameter(FhirConstants.CODED_SEARCH_HANDLER, code);
	}
	
	private static ReferenceAndListParam ref(String value) {
		return new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam(value)));
	}
	
	@Test
	public void search_withoutParameters_returnsAllDrugOrdersAndNoServiceRequests() {
		List<String> result = uuids(new SearchParameterMap());
		
		assertThat(result, containsInAnyOrder(ORD_1, ORD_111, ORD_2, ORD_222, ORD_3, ORD_4, ORD_444, ORD_5));
		assertThat(result, not(hasItem(TEST_ORDER)));
	}
	
	@Test
	public void search_excludesDiscontinuationOrders() {
		assertThat(uuids(new SearchParameterMap()), not(hasItem(ORD_22)));
		assertThat(uuids(new SearchParameterMap()), not(hasItem(ORD_44)));
	}
	
	// ---- coded search: system-only tokens (Bahmni override) ----
	
	@Test
	@Ignore("BUG: system-only search (?code=system|) returns nothing - fhir2 handleQueryForSystem with an empty code list "
	        + "renders 'crt.code in ()', which matches no rows on H2 and is invalid SQL on MySQL")
	public void search_bySystemOnlyToken_matchesEveryOrderWhoseConceptHasAMappingInThatSystem() {
		assertThat(uuids(codeSearch(code(SSTRM, ""))), containsInAnyOrder(ORD_1, ORD_111, ORD_4, ORD_444));
		assertThat(uuids(codeSearch(code(SNOMED, ""))), containsInAnyOrder(ORD_2, ORD_222, ORD_3));
	}
	
	@Test
	public void search_bySystemOnlyTokenForUnmappedSystem_returnsNothing() {
		assertThat(uuids(codeSearch(code("urn:unknown:system", ""))), empty());
	}
	
	@Test
	public void search_bySystemOnlyTokenAndPatient_neverReturnsOrdersOutsideThePatient() {
		SearchParameterMap params = codeSearch(code(SSTRM, "")).addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER,
		    ref(PATIENT_7));
		
		assertThat(uuids(params), not(hasItem(ORD_4)));
	}
	
	@Test
	public void search_bySystemOnlyTokenOrdWithSystemAndCode_stillMatchesTheSystemAndCodeAlternative() {
		TokenAndListParam or = new TokenAndListParam().addAnd(new TokenOrListParam().add(new TokenParam(SSTRM, "")).add(
		    new TokenParam(SNOMED, "7345693")));
		
		List<String> result = uuids(codeSearch(or));
		
		assertThat(result, hasItem(ORD_2));
		assertThat(result, hasItem(ORD_3));
		assertThat(result, not(hasItem(ORD_5)));
	}
	
	@Test
	public void search_bySystemOnlyTokenOrdWithCodeWithoutSystem_dropsTheSystemlessAlternative() {
		TokenAndListParam or = new TokenAndListParam().addAnd(new TokenOrListParam().add(new TokenParam(SSTRM, "")).add(
		    new TokenParam(null, CONCEPT_792_UUID)));
		
		// current behaviour: once a system-only token is present, alternatives lacking a system are ignored
		assertThat(uuids(codeSearch(or)), not(hasItem(ORD_2)));
	}
	
	// ---- coded search: delegated to the fhir2 implementation ----
	
	@Test
	public void search_bySystemAndCode_matchesOnlyOrdersWithThatMapping() {
		assertThat(uuids(codeSearch(code(SSTRM, "WGT234"))), containsInAnyOrder(ORD_1, ORD_111, ORD_4, ORD_444));
		assertThat(uuids(codeSearch(code(SNOMED, "7345693"))), containsInAnyOrder(ORD_2, ORD_222, ORD_3));
	}
	
	@Test
	public void search_bySystemAndCodeNotInThatSystem_returnsNothing() {
		assertThat(uuids(codeSearch(code(SNOMED, "WGT234"))), empty());
	}
	
	@Test
	public void search_byConceptUuidWithoutSystem_matchesOrdersOfThatConcept() {
		assertThat(uuids(codeSearch(code(null, CONCEPT_792_UUID))), containsInAnyOrder(ORD_2, ORD_222, ORD_3));
	}
	
	// ---- location (Bahmni extension) ----
	
	private static SearchParameterMap locationSearch(String locationUuid) {
		return new SearchParameterMap().addParameter(BahmniFhirConstants.ORDER_LOCATION_SEARCH_HANDLER, ref(locationUuid));
	}
	
	@Test
	public void search_byLocation_returnsOrdersWhoseEncounterIsAtThatLocation() {
		assertThat(uuids(locationSearch(LOCATION_1)), containsInAnyOrder(ORD_1, ORD_111));
		assertThat(uuids(locationSearch(LOCATION_2)), containsInAnyOrder(ORD_2, ORD_222, ORD_3, ORD_4, ORD_444, ORD_5));
	}
	
	@Test
	public void search_byUnknownLocation_returnsNothing() {
		assertThat(uuids(locationSearch("no-such-location")), empty());
	}
	
	@Test
	public void search_byLocationAndConcept_intersectsBothFilters() {
		SearchParameterMap params = locationSearch(LOCATION_2).addParameter(FhirConstants.CODED_SEARCH_HANDLER,
		    code(SSTRM, "WGT234"));
		
		assertThat(uuids(params), containsInAnyOrder(ORD_4, ORD_444));
	}
	
	// ---- createOrUpdate ----
	
	@Test
	public void createOrUpdate_persistsDrugOrderThroughOrderServiceAndMakesItSearchable() {
		DrugOrder order = new DrugOrder();
		order.setPatient(Context.getPatientService().getPatient(7));
		order.setEncounter(Context.getEncounterService().getEncounter(3));
		order.setOrderer(Context.getProviderService().getProvider(1));
		order.setCareSetting(Context.getOrderService().getCareSetting(1));
		order.setOrderType(Context.getOrderService().getOrderTypeByUuid(OrderType.DRUG_ORDER_TYPE_UUID));
		order.setDrug(Context.getConceptService().getDrug(2));
		order.setConcept(order.getDrug().getConcept());
		order.setUrgency(Order.Urgency.ROUTINE);
		order.setAction(Order.Action.NEW);
		order.setDateActivated(new Date(System.currentTimeMillis() - 60_000));
		order.setDosingType(FreeTextDosingInstructions.class);
		order.setDosingInstructions("one tablet daily");
		order.setQuantity(10.0);
		order.setQuantityUnits(Context.getConceptService().getConcept(51));
		order.setNumRefills(0);

		DrugOrder saved = dao.createOrUpdate(order);
		Context.flushSession();

		assertThat(saved.getOrderId(), notNullValue());
		assertThat(dao.get(saved.getUuid()).getDrug().getDrugId(), equalTo(2));
		assertThat(uuids(locationSearch(LOCATION_1)), hasItem(saved.getUuid()));
	}
}
