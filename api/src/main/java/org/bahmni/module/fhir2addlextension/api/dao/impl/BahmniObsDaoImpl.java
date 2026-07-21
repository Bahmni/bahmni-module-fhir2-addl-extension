package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniObsDao;
import org.hibernate.Criteria;
import org.hibernate.query.NativeQuery;
import org.openmrs.Obs;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.impl.BaseFhirDao;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class BahmniObsDaoImpl extends BaseFhirDao<Obs> implements BahmniObsDao {
	
	@Override
	protected void setupSearchParams(Criteria criteria, SearchParameterMap theParams) {
		super.setupSearchParams(criteria, theParams);
		theParams.getParameters().forEach(param -> {
			switch (param.getKey()) {
				case FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER:
					param.getValue().forEach(patientReference -> handlePatientReference(criteria,
					    (ReferenceAndListParam) patientReference.getParam(), "person"));
					break;
				case FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER:
					param.getValue().forEach(basedOnReference -> handleBasedOnReference(criteria,
					    basedOnReference.getParam()));
					break;
				case FhirConstants.COMMON_SEARCH_HANDLER:
					handleCommonSearchParameters(param.getValue()).ifPresent(criteria::add);
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
				String value = param.getValue();
				String uuid = value != null && value.contains("/") ? value.substring(value.lastIndexOf("/") + 1) : value;
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
