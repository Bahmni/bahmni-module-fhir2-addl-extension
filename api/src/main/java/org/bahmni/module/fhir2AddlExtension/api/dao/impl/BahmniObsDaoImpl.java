package org.bahmni.module.fhir2AddlExtension.api.dao.impl;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniObsDao;
import org.hibernate.SessionFactory;
import org.hibernate.query.NativeQuery;
import org.openmrs.Obs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class BahmniObsDaoImpl implements BahmniObsDao {
	
	@Getter(AccessLevel.PUBLIC)
	@Setter(value = AccessLevel.PROTECTED, onMethod = @__({ @Autowired, @Qualifier("sessionFactory") }))
	private SessionFactory sessionFactory;
	
	@Override
    public void updateObsMember(Obs obsGroup, Set<Obs> groupMembers) {
        if (groupMembers == null || groupMembers.isEmpty()) {
            return;
        }
        List<Integer> memberIds = groupMembers.stream().map(obs -> obs.getId()).collect(Collectors.toList());
        NativeQuery query = sessionFactory.getCurrentSession()
                .createNativeQuery("UPDATE obs SET obs_group_id=:obsGroupId WHERE obs_id in (:members)");
        query.setParameter("obsGroupId", obsGroup.getObsId());
        query.setParameter("members", memberIds);
        query.executeUpdate();
    }
}
