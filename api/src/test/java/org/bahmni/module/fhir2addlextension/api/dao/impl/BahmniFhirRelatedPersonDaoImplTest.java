package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.stream.Collectors;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.bahmni.module.fhir2addlextension.api.BaseFhir2AddlContextSensitiveTest;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirRelatedPersonDao;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Relationship;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Relationships (A -> B): standard dataset has REL_1 (502 -> 2) and REL_2 (502 -> 7); the test
 * dataset adds REL_3 (501 -> 6), REL_4 (7 -> 8) and a voided relationship (6 -> 9).
 */
public class BahmniFhirRelatedPersonDaoImplTest extends BaseFhir2AddlContextSensitiveTest {
	
	private static final String DATASET = "org/bahmni/module/fhir2addlextension/api/dao/impl/BahmniFhirRelatedPersonDaoImplTest_data.xml";
	
	private static final String REL_1 = "c18717dd-5d78-4a0e-84fc-ee62c5f0676a";
	
	private static final String REL_2 = "8c0a3d9c-bf61-496b-95b0-72e6bc4a2b83";
	
	private static final String REL_3 = "rel-9101-0000-0000-0000-000000000001";
	
	private static final String REL_4 = "rel-9102-0000-0000-0000-000000000002";
	
	private static final String PERSON_2 = "da7f524f-27ce-4bb2-86d6-6d1d05312bd5";
	
	private static final String PERSON_6 = "a7e04421-525f-442f-8138-05b619d16def";
	
	private static final String PERSON_7 = "5946f880-b197-400b-9caa-a3c661d23041";
	
	private static final String PERSON_8 = "8adf539e-4b5a-47aa-80c0-ba1025c957fa";
	
	private static final String PERSON_9 = "c04ee3c8-b68f-43cc-bff3-5a831ee7225f";
	
	private static final String PERSON_502 = "341b4e41-790c-484f-b6ed-71dc8da222de";
	
	@Autowired
	private BahmniFhirRelatedPersonDao dao;
	
	@Before
	public void loadDataset() throws Exception {
		executeDataSet(DATASET);
	}
	
	@Test
	public void search_withoutParameters_returnsAllNonVoidedRelationships() {
		assertThat(uuids(new SearchParameterMap()), containsInAnyOrder(REL_1, REL_2, REL_3, REL_4));
	}
	
	@Test
	public void search_byPatientOnSideB_returnsRelationshipWherePatientIsPersonB() {
		assertThat(uuids(patientSearch(PERSON_2)), contains(REL_1));
		assertThat(uuids(patientSearch(PERSON_6)), contains(REL_3));
	}
	
	@Test
	public void search_byPatientOnSideA_returnsRelationshipWherePatientIsPersonA() {
		assertThat(uuids(patientSearch(PERSON_502)), containsInAnyOrder(REL_1, REL_2));
	}
	
	@Test
	public void search_byPatientOnBothSidesOfDifferentRelationships_returnsBoth() {
		assertThat(uuids(patientSearch(PERSON_7)), containsInAnyOrder(REL_2, REL_4));
	}
	
	@Test
	public void search_byPatientWithOnlyVoidedRelationship_returnsNothing() {
		assertThat(uuids(patientSearch(PERSON_9)), empty());
	}
	
	@Test
	public void search_byUnknownPatient_returnsNothing() {
		assertThat(uuids(patientSearch("00000000-0000-0000-0000-000000000000")), empty());
	}
	
	@Test
	public void search_byMultiplePatientsInOrList_returnsRelationshipsOfEither() {
		assertThat(uuids(patientOrSearch(PERSON_2, PERSON_8)), containsInAnyOrder(REL_1, REL_4));
	}
	
	@Test
	public void search_byOrListWithUnknownAndKnownPatient_returnsRelationshipsOfTheKnownPatient() {
		assertThat(uuids(patientOrSearch("00000000-0000-0000-0000-000000000000", PERSON_6)), contains(REL_3));
	}
	
	@Test
	public void search_byPatientsInAndList_returnsOnlyRelationshipsMatchingEveryGroup() {
		// REL_2 is the only relationship that involves both 502 and 7
		SearchParameterMap params = andSearch(PERSON_502, PERSON_7);
		assertThat(uuids(params), contains(REL_2));
	}
	
	@Test
	public void search_byPatientsInAndListWithNoCommonRelationship_returnsNothing() {
		assertThat(uuids(andSearch(PERSON_2, PERSON_7)), empty());
	}
	
	@Test
	public void search_byPatientReferenceWithoutIdPart_doesNotRestrictResults() {
		SearchParameterMap params = new SearchParameterMap().addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER,
		    new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam())));
		assertThat(uuids(params), containsInAnyOrder(REL_1, REL_2, REL_3, REL_4));
	}
	
	@Test
	public void search_byPatientReferenceWithEmptyIdPart_doesNotRestrictResults() {
		SearchParameterMap params = new SearchParameterMap().addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER,
		    new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam(""))));
		assertThat(uuids(params), containsInAnyOrder(REL_1, REL_2, REL_3, REL_4));
	}
	
	@Test
	public void search_byOrListWithBlankAndRealPatient_onlyRestrictsByTheRealPatient() {
		assertThat(uuids(patientOrSearch("", PERSON_8)), contains(REL_4));
	}
	
	@Test
	public void search_byCommonIdParameter_returnsOnlyMatchingRelationship() {
		SearchParameterMap params = new SearchParameterMap().addParameter(FhirConstants.COMMON_SEARCH_HANDLER,
		    FhirConstants.ID_PROPERTY, new TokenAndListParam().addAnd(new TokenParam(REL_3)));
		assertThat(uuids(params), contains(REL_3));
	}
	
	@Test
	public void search_byPatientAndCommonId_appliesBothFilters() {
		SearchParameterMap params = patientSearch(PERSON_502).addParameter(FhirConstants.COMMON_SEARCH_HANDLER,
		    FhirConstants.ID_PROPERTY, new TokenAndListParam().addAnd(new TokenParam(REL_2)));
		assertThat(uuids(params), contains(REL_2));
		
		params = patientSearch(PERSON_502).addParameter(FhirConstants.COMMON_SEARCH_HANDLER, FhirConstants.ID_PROPERTY,
		    new TokenAndListParam().addAnd(new TokenParam(REL_3)));
		assertThat(uuids(params), empty());
	}
	
	@Test
	public void get_byUuid_returnsRelationshipWithBothPersons() {
		Relationship relationship = dao.get(REL_4);
		assertThat(relationship.getPersonA().getUuid(), equalTo(PERSON_7));
		assertThat(relationship.getPersonB().getUuid(), equalTo(PERSON_8));
	}
	
	private SearchParameterMap patientSearch(String uuid) {
		return patientOrSearch(uuid);
	}
	
	private SearchParameterMap patientOrSearch(String... uuids) {
		ReferenceOrListParam or = new ReferenceOrListParam();
		for (String uuid : uuids) {
			or.add(new ReferenceParam(uuid));
		}
		return new SearchParameterMap().addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER,
		    new ReferenceAndListParam().addAnd(or));
	}
	
	private SearchParameterMap andSearch(String first, String second) {
		return new SearchParameterMap().addParameter(
		    FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER,
		    new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam(first))).addAnd(
		        new ReferenceOrListParam().add(new ReferenceParam(second))));
	}
	
	private List<String> uuids(SearchParameterMap params) {
		return dao.getSearchResults(params).stream().map(Relationship::getUuid).collect(Collectors.toList());
	}
}
