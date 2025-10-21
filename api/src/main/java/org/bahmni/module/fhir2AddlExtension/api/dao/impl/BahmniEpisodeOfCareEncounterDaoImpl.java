package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniEpisodeOfCareEncounterDao;
import org.hibernate.SessionFactory;
import org.openmrs.Encounter;
import org.openmrs.module.fhir2.api.search.param.PropParam;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class BahmniEpisodeOfCareEncounterDaoImpl implements BahmniEpisodeOfCareEncounterDao {
	
	@Getter(AccessLevel.PUBLIC)
	@Setter(value = AccessLevel.PROTECTED, onMethod = @__({ @Autowired, @Qualifier("sessionFactory") }))
	private SessionFactory sessionFactory;
	
	@Override
	public List<Encounter> getSearchResults(@Nonnull SearchParameterMap theParams) {
		List<PropParam<?>> eocSearchParams = theParams.getParameters(BahmniFhirConstants.EPISODE_OF_CARE_REFERENCE_SEARCH_PARAM);
		if (eocSearchParams.isEmpty()) return Collections.emptyList();
		ReferenceAndListParam listParam = (ReferenceAndListParam) eocSearchParams.get(0).getParam();
		List<String> episodeUuids = new ArrayList<>();
		listParam.getValuesAsQueryTokens().forEach(referenceOrListParam -> {
			referenceOrListParam.getValuesAsQueryTokens().forEach(referenceParam -> {
				episodeUuids.add(referenceParam.getValue());
			});
		});
		//TODO - now that we have all episode UUIDs, we can query and find the encounters using IN clause
		//		CriteriaBuilder criteriaBuilder = sessionFactory.getCurrentSession().getCriteriaBuilder();
		//		CriteriaQuery<Integer> cq = criteriaBuilder.createQuery(Integer.class);
		//		Root<Episode> rootEpisode = cq.from(Episode.class);
		//		Join<Episode, Encounter> joinB = rootEpisode.join("encounters", JoinType.INNER);
		//

		return Collections.emptyList();
	}
}
