package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bahmni.module.fhir2addlextension.api.BaseFhir2AddlContextSensitiveTest;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniEpisodeOfCareEncounterDao;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Visit;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Uses the episode dataset of {@link BahmniFhirEpisodeOfCareDaoImplTest}: episode 9001 has
 * encounter 6 and visits 1,2; episode 9002 has encounters 3,4,5 and visit 8; episode 9003 has
 * neither.
 */
public class BahmniEpisodeOfCareEncounterDaoImplTest extends BaseFhir2AddlContextSensitiveTest {
	
	private static final String DATASET = "org/bahmni/module/fhir2addlextension/api/dao/impl/BahmniFhirEpisodeOfCareDaoImplTest_data.xml";
	
	private static final String EOC_1 = "eoc-9001-0000-0000-0000-000000000001";
	
	private static final String EOC_2 = "eoc-9002-0000-0000-0000-000000000002";
	
	private static final String EOC_3_NO_LINKS = "eoc-9003-0000-0000-0000-000000000003";
	
	@Autowired
	private BahmniEpisodeOfCareEncounterDao dao;
	
	@Before
	public void loadDataset() throws Exception {
		executeDataSet(DATASET);
	}
	
	@Test
	public void getEncountersForEpisodes_groupsEncountersByTheirOwnEpisode() {
		Map<String, List<Encounter>> result = dao.getEncountersForEpisodes(Arrays.asList(EOC_1, EOC_2));
		
		assertThat(result.keySet(), containsInAnyOrder(EOC_1, EOC_2));
		assertThat(encounterIds(result.get(EOC_1)), containsInAnyOrder(6));
		assertThat(encounterIds(result.get(EOC_2)), containsInAnyOrder(3, 4, 5));
	}
	
	@Test
	public void getEncountersForEpisodes_onlyReturnsRequestedEpisodes() {
		Map<String, List<Encounter>> result = dao.getEncountersForEpisodes(Collections.singletonList(EOC_2));
		
		assertThat(result.keySet(), containsInAnyOrder(EOC_2));
		assertThat(result.get(EOC_2), hasSize(3));
	}
	
	@Test
	public void getEncountersForEpisodes_omitsEpisodesWithoutEncounters() {
		Map<String, List<Encounter>> result = dao.getEncountersForEpisodes(Arrays.asList(EOC_1, EOC_3_NO_LINKS));
		
		assertThat(result.keySet(), containsInAnyOrder(EOC_1));
		assertThat(result, not(hasKey(EOC_3_NO_LINKS)));
	}
	
	@Test
	public void getEncountersForEpisodes_withUnknownEpisodeUuid_returnsEmptyMap() {
		assertThat(dao.getEncountersForEpisodes(Collections.singletonList("does-not-exist")), anEmptyMap());
	}
	
	@Test
	public void getVisitsForEpisodes_groupsVisitsByTheirOwnEpisode() {
		Map<String, List<Visit>> result = dao.getVisitsForEpisodes(Arrays.asList(EOC_1, EOC_2));
		
		assertThat(result.keySet(), containsInAnyOrder(EOC_1, EOC_2));
		assertThat(visitIds(result.get(EOC_1)), containsInAnyOrder(1, 2));
		assertThat(visitIds(result.get(EOC_2)), containsInAnyOrder(8));
	}
	
	@Test
	public void getVisitsForEpisodes_onlyReturnsRequestedEpisodes() {
		Map<String, List<Visit>> result = dao.getVisitsForEpisodes(Collections.singletonList(EOC_1));
		
		assertThat(result.keySet(), containsInAnyOrder(EOC_1));
		assertThat(visitIds(result.get(EOC_1)), containsInAnyOrder(1, 2));
	}
	
	@Test
	public void getVisitsForEpisodes_omitsEpisodesWithoutVisits() {
		Map<String, List<Visit>> result = dao.getVisitsForEpisodes(Arrays.asList(EOC_2, EOC_3_NO_LINKS));
		
		assertThat(result.keySet(), containsInAnyOrder(EOC_2));
	}
	
	@Test
	public void getVisitsForEpisodes_withUnknownEpisodeUuid_returnsEmptyMap() {
		assertThat(dao.getVisitsForEpisodes(Collections.singletonList("does-not-exist")), anEmptyMap());
	}
	
	private List<Integer> encounterIds(List<Encounter> encounters) {
		return encounters.stream().map(Encounter::getEncounterId).collect(Collectors.toList());
	}
	
	private List<Integer> visitIds(List<Visit> visits) {
		return visits.stream().map(Visit::getVisitId).collect(Collectors.toList());
	}
}
