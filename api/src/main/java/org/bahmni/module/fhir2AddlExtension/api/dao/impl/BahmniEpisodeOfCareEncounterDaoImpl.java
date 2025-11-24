package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniEpisodeOfCareEncounterDao;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.Encounter;
import org.openmrs.Visit;
import org.openmrs.module.episodes.Episode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class BahmniEpisodeOfCareEncounterDaoImpl implements BahmniEpisodeOfCareEncounterDao {
	
	@Getter(AccessLevel.PUBLIC)
	@Setter(value = AccessLevel.PROTECTED, onMethod = @__({ @Autowired, @Qualifier("sessionFactory") }))
	private SessionFactory sessionFactory;
	
	@Override
	public Map<String, List<Encounter>> getEncountersForEpisodes(@Nonnull List<String> episodeUuids) {
		Session currentSession = sessionFactory.getCurrentSession();
		CriteriaBuilder cb = currentSession.getCriteriaBuilder();
		CriteriaQuery<Object[]> ecq = cb.createQuery(Object[].class);
		Root<Episode> eocRoot = ecq.from(Episode.class);
		Join<Episode, Encounter> joinEncounter = eocRoot.join("encounters", JoinType.INNER);
		ecq.select(
			cb.array(
				eocRoot.get("uuid").alias("episodeUuid"), // Select Episode's uuid
				joinEncounter.get("encounterId").alias("encounterId")  // Select Encounter's id
			)
		);
		ecq.where(eocRoot.get("uuid").in(episodeUuids));
		List<Object[]> encounterResults = currentSession.createQuery(ecq).getResultList();

		Map<String, List<Integer>> episodeUuidToEncounterIdMap = new HashMap<>();
		for (Object[] row : encounterResults) {
			String rowEpisodeUuid = (String) row[0];
			Integer rowEncounterId = (Integer) row[1];
			Optional.ofNullable(episodeUuidToEncounterIdMap.get(rowEpisodeUuid))
				.orElseGet(() -> {
					ArrayList<Integer> aList = new ArrayList<>();
					episodeUuidToEncounterIdMap.put(rowEpisodeUuid, aList);
					return aList;
				}).add(rowEncounterId);
		}

		List<Integer> encounterIds = episodeUuidToEncounterIdMap.values()
			.stream()
			.flatMap(List::stream)
			.collect(Collectors.toList());

		List<Encounter> encounters = currentSession
			.createQuery("FROM Encounter e WHERE e.encounterId IN (:encounterIds)", Encounter.class)
			.setParameterList("encounterIds", encounterIds)
			.list();

		Map<String, List<Encounter>> mappedResults = new HashMap<>();
		episodeUuidToEncounterIdMap
			.forEach((episodeUuid, mappedEncounterIds)
				-> mappedResults
					.put(episodeUuid,
						encounters.stream()
						.filter(encounter -> mappedEncounterIds.contains(encounter.getEncounterId()))
						.collect(Collectors.toList())));

		return mappedResults;
	}
	
	@Override
	public Map<String, List<Visit>> getVisitsForEpisodes(@Nonnull List<String> episodeUuids) {
		Session currentSession = sessionFactory.getCurrentSession();
		CriteriaBuilder cb = currentSession.getCriteriaBuilder();
		CriteriaQuery<Object[]> vcq = cb.createQuery(Object[].class);
		Root<Episode> eocRoot = vcq.from(Episode.class);
		Join<Episode, Encounter> joinEncounter = eocRoot.join("visits", JoinType.INNER);
		vcq.select(
				cb.array(
						eocRoot.get("uuid").alias("episodeUuid"), // Select Episode's uuid
						joinEncounter.get("visitId").alias("visitId")  // Select Visit's id
				)
		);
		vcq.where(eocRoot.get("uuid").in(episodeUuids));
		List<Object[]> visitsForEpisode = currentSession.createQuery(vcq).getResultList();

		Map<String, List<Integer>> episodeUuidToVisitIdMap = new HashMap<>();
		for (Object[] row : visitsForEpisode) {
			String rowEpisodeUuid = (String) row[0];
			Integer rowVisitId = (Integer) row[1];
			Optional.ofNullable(episodeUuidToVisitIdMap.get(rowEpisodeUuid))
					.orElseGet(() -> {
						ArrayList<Integer> aList = new ArrayList<>();
						episodeUuidToVisitIdMap.put(rowEpisodeUuid, aList);
						return aList;
					}).add(rowVisitId);
		}

		List<Integer> visitIds = episodeUuidToVisitIdMap.values()
				.stream()
				.flatMap(List::stream)
				.collect(Collectors.toList());

		List<Visit> visits = currentSession
				.createQuery("FROM Visit e WHERE e.visitId IN (:visitIds)", Visit.class)
				.setParameterList("visitIds", visitIds)
				.list();

		Map<String, List<Visit>> mappedResults = new HashMap<>();
		episodeUuidToVisitIdMap
			.forEach((episodeUuid, mappedVisitIds)
				-> mappedResults
					.put(episodeUuid,
						visits.stream()
							.filter(visit -> mappedVisitIds.contains(visit.getVisitId()))
							.collect(Collectors.toList())));

		return mappedResults;
	}
}
