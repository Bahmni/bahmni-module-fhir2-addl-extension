package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;

import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.bahmni.module.fhir2addlextension.api.BaseFhir2AddlContextSensitiveTest;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirServiceRequestDao;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Order;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link BahmniBaseFhirDao} is abstract; its only concrete user is the service-request DAO, which
 * exposes getEncounterReferencesByNumberOfVisit on its interface, so the shared logic is exercised
 * through it.
 */
public class BahmniBaseFhirDaoTest extends BaseFhir2AddlContextSensitiveTest {
	
	private static final String DATASET = "org/bahmni/module/fhir2addlextension/api/dao/impl/BahmniBaseFhirDaoTest_data.xml";
	
	private static final String PATIENT_6 = "a7e04421-525f-442f-8138-05b619d16def";
	
	private static final String PATIENT_8 = "8adf539e-4b5a-47aa-80c0-ba1025c957fa";
	
	private static final String E1 = "enc-9101-0000-0000-0000-000000000001";
	
	private static final String E2 = "enc-9102-0000-0000-0000-000000000002";
	
	private static final String E3 = "enc-9103-0000-0000-0000-000000000003";
	
	private static final String E4 = "enc-9104-0000-0000-0000-000000000004";
	
	@Autowired
	private BahmniFhirServiceRequestDao<Order> dao;
	
	@Before
	public void loadDataset() throws Exception {
		executeDataSet(DATASET);
	}
	
	@Test
	public void getEncounterReferencesByNumberOfVisit_lastVisit_returnsAllEncountersOfNewestVisitOnly() {
		assertThat(encounterUuids(dao.getEncounterReferencesByNumberOfVisit(new NumberParam(1), patient(PATIENT_6))),
		    containsInAnyOrder(E3, E4));
	}
	
	@Test
	public void getEncounterReferencesByNumberOfVisit_lastTwoVisits_includesNextOlderVisit() {
		assertThat(encounterUuids(dao.getEncounterReferencesByNumberOfVisit(new NumberParam(2), patient(PATIENT_6))),
		    containsInAnyOrder(E2, E3, E4));
	}
	
	@Test
	public void getEncounterReferencesByNumberOfVisit_moreVisitsThanExist_returnsEveryEncounterOfThePatient() {
		assertThat(encounterUuids(dao.getEncounterReferencesByNumberOfVisit(new NumberParam(10), patient(PATIENT_6))),
		    containsInAnyOrder(E1, E2, E3, E4));
	}
	
	@Test
	public void getEncounterReferencesByNumberOfVisit_patientWithoutVisits_returnsNull() {
		assertThat(
		    dao.getEncounterReferencesByNumberOfVisit(new NumberParam(1), patient("00000000-0000-0000-0000-000000000000")),
		    nullValue());
	}
	
	@Test
	public void getEncounterReferencesByNumberOfVisit_visitsWithoutEncounters_returnsNull() {
		assertThat(dao.getEncounterReferencesByNumberOfVisit(new NumberParam(1), patient(PATIENT_8)), nullValue());
	}
	
	@Test
	public void getEncounterReferencesByNumberOfVisit_missingPatientId_returnsNull() {
		assertThat(dao.getEncounterReferencesByNumberOfVisit(new NumberParam(1), new ReferenceParam()), nullValue());
		assertThat(dao.getEncounterReferencesByNumberOfVisit(new NumberParam(1), new ReferenceParam("")), nullValue());
	}
	
	private ReferenceParam patient(String uuid) {
		return new ReferenceParam(uuid);
	}
	
	private List<String> encounterUuids(ReferenceAndListParam param) {
		return param.getValuesAsQueryTokens().stream().flatMap(or -> or.getValuesAsQueryTokens().stream())
		        .map(ReferenceParam::getIdPart).collect(Collectors.toList());
	}
}
