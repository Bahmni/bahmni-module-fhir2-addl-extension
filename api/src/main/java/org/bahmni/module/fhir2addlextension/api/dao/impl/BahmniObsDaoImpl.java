package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringParam;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniObsDao;
import org.hibernate.query.NativeQuery;
import org.openmrs.Obs;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.dao.impl.FhirObservationDaoImpl;
import org.openmrs.module.fhir2.api.dao.internals.OpenmrsFhirCriteriaContext;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.persistence.criteria.Join;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Primary
public class BahmniObsDaoImpl extends FhirObservationDaoImpl implements BahmniObsDao {
	
	@Override
	protected <U> void setupSearchParams(OpenmrsFhirCriteriaContext<Obs, U> ctx, SearchParameterMap theParams) {
		super.setupSearchParams(ctx, theParams);
		theParams.getParameters().forEach(param -> {
			switch (param.getKey()) {
				case FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER:
					param.getValue().forEach(basedOnReference -> handleBasedOnReference(ctx, basedOnReference.getParam()));
					break;
			}
		});
		if (!theParams.getParameters(FhirConstants.LASTN_OBSERVATION_SEARCH_HANDLER).isEmpty()) {
			handleVoidable(ctx);
		}
	}
	
	private <U> void handleBasedOnReference(OpenmrsFhirCriteriaContext<Obs, U> ctx, Object basedOnReference) {
		if (basedOnReference == null) {
			return;
		}
		Join<?, ?> oJoin = ctx.getJoin("o").orElseGet(() -> ctx.addJoin("order", "o"));
		handleAndListParam(ctx.getCriteriaBuilder(), (ReferenceAndListParam) basedOnReference, param -> {
			String uuid = param.getIdPart();
			return getSearchQueryHelper().propertyLike(ctx, oJoin, "uuid", new StringParam(uuid));
		}).ifPresent(ctx::addPredicate);
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
