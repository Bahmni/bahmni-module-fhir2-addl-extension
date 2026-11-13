package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.bahmni.module.fhir2addlextension.api.BaseFhir2AddlContextSensitiveTest;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniObsDao;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Obs;
import org.openmrs.api.ObsService;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BahmniObsDaoImplTest extends BaseFhir2AddlContextSensitiveTest {
	
	private static final String DATASET = "org/bahmni/module/fhir2addlextension/api/dao/impl/BahmniObsDaoImplTest_data.xml";
	
	private static final String OBS_FOR_ORDER_6 = "obs-9201-0000-0000-0000-000000000001";
	
	private static final String OBS_FOR_ORDER_7 = "obs-9202-0000-0000-0000-000000000002";
	
	private static final String OBS_WITHOUT_ORDER = "obs-9203-0000-0000-0000-000000000003";
	
	private static final String VOIDED_OBS_FOR_ORDER_6 = "obs-9204-0000-0000-0000-000000000004";
	
	private static final String ORDER_6 = "1c96f25c-4949-4f72-9931-d808fbc226de";
	
	private static final String ORDER_7 = "2c96f25c-4949-4f72-9931-d808fbc226df";
	
	private static final String ORDER_WITHOUT_OBS = "4c96f25c-4949-4f72-9931-d808fbc226dh";
	
	@Autowired
	private BahmniObsDao dao;
	
	@Autowired
	private ObsService obsService;
	
	@Before
	public void loadDataset() throws Exception {
		executeDataSet(DATASET);
	}
	
	private List<String> uuids(SearchParameterMap params) {
		return dao.getSearchResults(params).stream().map(Obs::getUuid).collect(Collectors.toList());
	}
	
	private static SearchParameterMap basedOn(String... orderUuids) {
		ReferenceOrListParam or = new ReferenceOrListParam();
		for (String uuid : orderUuids) {
			or.add(new ReferenceParam("ServiceRequest", uuid));
		}
		return new SearchParameterMap().addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER,
		    new ReferenceAndListParam().addAnd(or));
	}
	
	// ---- basedOn ----
	
	@Test
	public void search_byBasedOnOrder_returnsOnlyObsRecordedAgainstThatOrder() {
		assertThat(uuids(basedOn(ORDER_6)), hasItem(OBS_FOR_ORDER_6));
		assertThat(uuids(basedOn(ORDER_6)), not(hasItem(OBS_FOR_ORDER_7)));
		assertThat(uuids(basedOn(ORDER_6)), not(hasItem(OBS_WITHOUT_ORDER)));
	}
	
	@Test
	public void search_byBasedOnOrder_excludesVoidedObs() {
		assertThat(uuids(basedOn(ORDER_6)), containsInAnyOrder(OBS_FOR_ORDER_6));
	}
	
	@Test
	public void search_byBasedOnOrderList_matchesAnyOfTheOrders() {
		assertThat(uuids(basedOn(ORDER_6, ORDER_7)), containsInAnyOrder(OBS_FOR_ORDER_6, OBS_FOR_ORDER_7));
	}
	
	@Test
	public void search_byBasedOnOrderWithoutObs_returnsNothing() {
		assertThat(uuids(basedOn(ORDER_WITHOUT_OBS)), empty());
	}
	
	@Test
	public void search_byUnknownBasedOnOrder_returnsNothing() {
		assertThat(uuids(basedOn("no-such-order")), empty());
	}
	
	@Test
	public void search_withoutBasedOn_doesNotRestrictByOrder() {
		List<String> result = uuids(new SearchParameterMap());
		
		assertThat(result, hasItem(OBS_FOR_ORDER_6));
		assertThat(result, hasItem(OBS_WITHOUT_ORDER));
	}
	
	// ---- lastN: voided obs must be excluded ----
	
	@Test
	public void searchLastN_ignoresVoidedObsWhenPickingTheLatestPerConcept() {
		// the voided obs (2024-01-13) is the newest for orders 6/7; lastN=1 must still pick the newest non-voided one
		SearchParameterMap params = basedOn(ORDER_6, ORDER_7).addParameter(FhirConstants.LASTN_OBSERVATION_SEARCH_HANDLER,
		    new NumberParam(1)).addParameter(FhirConstants.MAX_SEARCH_HANDLER, new NumberParam(1));
		
		List<String> result = uuids(params);
		
		assertThat(result, containsInAnyOrder(OBS_FOR_ORDER_7));
		assertThat(result, not(hasItem(VOIDED_OBS_FOR_ORDER_6)));
	}
	
	// ---- updateObsMember ----
	
	@Test
	public void updateObsMember_attachesGivenObsToTheGroupAndLeavesOthersUntouched() {
		Obs group = obsService.getObs(9205);
		Set<Obs> members = new HashSet<>();
		members.add(obsService.getObs(9201));
		members.add(obsService.getObs(9202));

		dao.updateObsMember(group, members);
		Context.flushSession();
		Context.clearSession();

		assertThat(obsService.getObs(9201).getObsGroup(), notNullValue());
		assertThat(obsService.getObs(9201).getObsGroup().getObsId(), equalTo(9205));
		assertThat(obsService.getObs(9202).getObsGroup().getObsId(), equalTo(9205));
		assertThat(obsService.getObs(9203).getObsGroup(), nullValue());
	}
	
	@Test
	public void updateObsMember_withNullMembers_changesNothing() {
		Obs group = obsService.getObs(9205);
		
		dao.updateObsMember(group, null);
		Context.flushSession();
		Context.clearSession();
		
		assertThat(obsService.getObs(9201).getObsGroup(), nullValue());
	}
	
	@Test
	public void updateObsMember_withEmptyMembers_changesNothing() {
		Obs group = obsService.getObs(9205);
		
		dao.updateObsMember(group, Collections.emptySet());
		Context.flushSession();
		Context.clearSession();
		
		assertThat(obsService.getObs(9201).getObsGroup(), nullValue());
	}
}
