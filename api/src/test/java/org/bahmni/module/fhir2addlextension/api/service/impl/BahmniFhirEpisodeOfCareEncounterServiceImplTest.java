package org.bahmni.module.fhir2addlextension.api.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniEpisodeOfCareEncounterDao;
import org.hl7.fhir.r4.model.Encounter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Visit;
import org.openmrs.module.fhir2.api.translators.EncounterTranslator;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirEpisodeOfCareEncounterServiceImplTest {
	
	@Mock
	private BahmniEpisodeOfCareEncounterDao dao;
	
	@Mock
	private EncounterTranslator<org.openmrs.Encounter> encounterTranslator;
	
	@Mock
	private EncounterTranslator<Visit> visitTranslator;
	
	private BahmniFhirEpisodeOfCareEncounterServiceImpl service;
	
	@Before
	public void setUp() {
		service = new BahmniFhirEpisodeOfCareEncounterServiceImpl(dao, encounterTranslator, visitTranslator);
	}
	
	private static ReferenceAndListParam episodes(String... uuids) {
		ReferenceOrListParam or = new ReferenceOrListParam();
		for (String uuid : uuids) {
			or.add(new ReferenceParam(uuid));
		}
		return new ReferenceAndListParam().addAnd(or);
	}
	
	private static Encounter fhirEncounter(String id) {
		Encounter e = new Encounter();
		e.setId(id);
		return e;
	}
	
	private static List<String> ids(IBundleProvider provider) {
		return provider.getResources(0, provider.size()).stream().map(r -> r.getIdElement().getIdPart())
		        .collect(Collectors.toList());
	}
	
	@Test
	public void encountersForEpisodes_withNoAndParams_shouldReturnEmptyWithoutQueryingDao() {
		IBundleProvider result = service.encountersForEpisodes(new ReferenceAndListParam());
		
		assertThat(result.size(), equalTo(0));
		verify(dao, never()).getEncountersForEpisodes(anyList());
		verify(dao, never()).getVisitsForEpisodes(anyList());
	}
	
	@Test
	public void encountersForEpisodes_withEmptyOrList_shouldReturnEmptyWithoutQueryingDao() {
		IBundleProvider result = service.encountersForEpisodes(new ReferenceAndListParam()
		        .addAnd(new ReferenceOrListParam()));
		
		assertThat(result.size(), equalTo(0));
		verify(dao, never()).getEncountersForEpisodes(anyList());
	}
	
	@Test
	public void encountersForEpisodes_shouldPassAllEpisodeUuidsToDaoForEncountersAndVisits() {
		when(dao.getEncountersForEpisodes(anyList())).thenReturn(Collections.emptyMap());
		when(dao.getVisitsForEpisodes(anyList())).thenReturn(Collections.emptyMap());
		
		IBundleProvider result = service.encountersForEpisodes(episodes("eoc-1", "eoc-2"));
		
		ArgumentCaptor<List<String>> encCaptor = ArgumentCaptor.forClass(List.class);
		ArgumentCaptor<List<String>> visitCaptor = ArgumentCaptor.forClass(List.class);
		verify(dao).getEncountersForEpisodes(encCaptor.capture());
		verify(dao).getVisitsForEpisodes(visitCaptor.capture());
		assertThat(encCaptor.getValue(), contains("eoc-1", "eoc-2"));
		assertThat(visitCaptor.getValue(), contains("eoc-1", "eoc-2"));
		assertThat(result.size(), equalTo(0));
	}
	
	@Test
	public void encountersForEpisodes_shouldTranslateEncountersAndLinkEpisodeOfCare() {
		org.openmrs.Encounter enc = new org.openmrs.Encounter();
		Map<String, List<org.openmrs.Encounter>> byEpisode = new LinkedHashMap<>();
		byEpisode.put("eoc-1", Collections.singletonList(enc));
		when(dao.getEncountersForEpisodes(anyList())).thenReturn(byEpisode);
		when(dao.getVisitsForEpisodes(anyList())).thenReturn(Collections.emptyMap());
		when(encounterTranslator.toFhirResources(Collections.singletonList(enc))).thenReturn(
		    Arrays.asList(fhirEncounter("e1"), fhirEncounter("e2")));

		IBundleProvider result = service.encountersForEpisodes(episodes("eoc-1"));

		assertThat(result.size(), equalTo(2));
		assertThat(result.preferredPageSize(), equalTo(2));
		result.getResources(0, 2).forEach(r -> {
			Encounter e = (Encounter) r;
			assertThat(e.getEpisodeOfCare(), hasSize(1));
			assertThat(e.getEpisodeOfCareFirstRep().getReference(), equalTo("EpisodeOfCare/eoc-1"));
		});
		verify(visitTranslator, never()).toFhirResources(anyList());
	}
	
	@Test
	public void encountersForEpisodes_shouldTranslateVisitsViaVisitTranslatorAndLinkEpisodeOfCare() {
		Visit visit = new Visit();
		Map<String, List<Visit>> byEpisode = new LinkedHashMap<>();
		byEpisode.put("eoc-2", Collections.singletonList(visit));
		when(dao.getEncountersForEpisodes(anyList())).thenReturn(Collections.emptyMap());
		when(dao.getVisitsForEpisodes(anyList())).thenReturn(byEpisode);
		when(visitTranslator.toFhirResources(Collections.singletonList(visit))).thenReturn(
		    Collections.singletonList(fhirEncounter("v1")));

		IBundleProvider result = service.encountersForEpisodes(episodes("eoc-2"));

		assertThat(ids(result), contains("v1"));
		Encounter e = (Encounter) result.getResources(0, 1).get(0);
		assertThat(e.getEpisodeOfCareFirstRep().getReference(), equalTo("EpisodeOfCare/eoc-2"));
		verify(encounterTranslator, never()).toFhirResources(anyList());
	}
	
	@Test
	public void encountersForEpisodes_shouldCombineEncountersAndVisitsAcrossEpisodes() {
		org.openmrs.Encounter enc1 = new org.openmrs.Encounter();
		org.openmrs.Encounter enc2 = new org.openmrs.Encounter();
		Visit visit = new Visit();
		Map<String, List<org.openmrs.Encounter>> encMap = new LinkedHashMap<>();
		encMap.put("eoc-1", Collections.singletonList(enc1));
		encMap.put("eoc-2", Collections.singletonList(enc2));
		Map<String, List<Visit>> visitMap = new LinkedHashMap<>();
		visitMap.put("eoc-1", Collections.singletonList(visit));
		when(dao.getEncountersForEpisodes(anyList())).thenReturn(encMap);
		when(dao.getVisitsForEpisodes(anyList())).thenReturn(visitMap);
		when(encounterTranslator.toFhirResources(Collections.singletonList(enc1))).thenReturn(
		    Collections.singletonList(fhirEncounter("e1")));
		when(encounterTranslator.toFhirResources(Collections.singletonList(enc2))).thenReturn(
		    Collections.singletonList(fhirEncounter("e2")));
		when(visitTranslator.toFhirResources(Collections.singletonList(visit))).thenReturn(
		    Collections.singletonList(fhirEncounter("v1")));

		IBundleProvider result = service.encountersForEpisodes(episodes("eoc-1", "eoc-2"));

		assertThat(ids(result), containsInAnyOrder("e1", "e2", "v1"));
		Map<String, String> episodeById = result.getResources(0, 3).stream().map(r -> (Encounter) r)
		        .collect(Collectors.toMap(e -> e.getIdElement().getIdPart(), e -> e.getEpisodeOfCareFirstRep()
		                .getReference()));
		assertThat(episodeById.get("e1"), equalTo("EpisodeOfCare/eoc-1"));
		assertThat(episodeById.get("e2"), equalTo("EpisodeOfCare/eoc-2"));
		assertThat(episodeById.get("v1"), equalTo("EpisodeOfCare/eoc-1"));
	}
	
	@Test
	public void encountersForEpisodes_withNegativeToIndex_shouldReturnWholeList() {
		org.openmrs.Encounter enc = new org.openmrs.Encounter();
		when(dao.getEncountersForEpisodes(anyList())).thenReturn(
		    Collections.singletonMap("eoc-1", Collections.singletonList(enc)));
		when(dao.getVisitsForEpisodes(anyList())).thenReturn(Collections.emptyMap());
		when(encounterTranslator.toFhirResources(anyList())).thenReturn(Collections.singletonList(fhirEncounter("e1")));
		
		IBundleProvider result = service.encountersForEpisodes(episodes("eoc-1"));
		
		assertThat(result.getResources(0, -1), hasSize(1));
	}
}
