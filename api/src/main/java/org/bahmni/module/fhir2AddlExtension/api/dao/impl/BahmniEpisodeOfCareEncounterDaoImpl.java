package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniEpisodeOfCareEncounterDao;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openmrs.Encounter;
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
}
