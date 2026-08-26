package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniObsDao;
import org.hibernate.Criteria;
import org.hibernate.query.NativeQuery;
import org.openmrs.Obs;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.impl.FhirObservationDaoImpl;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Primary
public class BahmniObsDaoImpl extends FhirObservationDaoImpl implements BahmniObsDao {
	
	@Override
	protected void setupSearchParams(Criteria criteria, SearchParameterMap theParams) {
		super.setupSearchParams(criteria, theParams);
		theParams.getParameters().forEach(param -> {
			switch (param.getKey()) {
				case FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER:
					param.getValue().forEach(basedOnReference -> handleBasedOnReference(criteria,
					    basedOnReference.getParam()));
					break;
			}
		});
	}
	
	private void handleBasedOnReference(Criteria criteria, Object basedOnReference) {
		if (basedOnReference != null) {
			if (lacksAlias(criteria, "o")) {
				criteria.createAlias("order", "o");
			}
		handleAndListParam((ReferenceAndListParam) basedOnReference, param -> {
			String uuid = param.getIdPart();
			return propertyLike("o.uuid", uuid);
		}).ifPresent(criteria::add);
		}
	}
	
	@Override
	public void updateObsMember(Obs obsGroup, Set<Obs> groupMembers) {
		if (groupMembers == null || groupMembers.isEmpty()) {
			return;
		}
		List<Integer> memberIds = groupMembers.stream().map(obs -> obs.getId()).collect(Collectors.toList());
		NativeQuery query = getSessionFactory().getCurrentSession()
		        .createNativeQuery("UPDATE obs SET obs_group_id=:obsGroupId WHERE obs_id in (:members)");
		query.setParameter("obsGroupId", obsGroup.getObsId());
		query.setParameter("members", memberIds);
		query.executeUpdate();
	}
}
