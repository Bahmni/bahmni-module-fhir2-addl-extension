package org.bahmni.module.fhir2addlextension.api.dao.impl;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.NativeQuery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Obs;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniObsDaoImplTest {
	
	@Mock
	private SessionFactory sessionFactory;
	
	@Mock
	private Session session;
	
	@Mock
	private NativeQuery nativeQuery;
	
	private BahmniObsDaoImpl dao;
	
	@Before
	public void setUp() {
		dao = new BahmniObsDaoImpl();
		ReflectionTestUtils.setField(dao, "sessionFactory", sessionFactory);
		
		when(sessionFactory.getCurrentSession()).thenReturn(session);
	}
	
	@Test
	public void updateObsMember_shouldUpdateObsGroupId() {
		Obs obsGroup = new Obs();
		obsGroup.setObsId(100);

		Obs member1 = new Obs();
		member1.setId(1);
		Obs member2 = new Obs();
		member2.setId(2);

		Set<Obs> groupMembers = new HashSet<>();
		groupMembers.add(member1);
		groupMembers.add(member2);

		when(session.createNativeQuery(anyString())).thenReturn(nativeQuery);
		when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
		when(nativeQuery.executeUpdate()).thenReturn(2);

		dao.updateObsMember(obsGroup, groupMembers);

		verify(session).createNativeQuery("UPDATE obs SET obs_group_id=:obsGroupId WHERE obs_id in (:members)");
		verify(nativeQuery).setParameter(eq("obsGroupId"), eq(100));
		verify(nativeQuery).setParameter(eq("members"), any());
		verify(nativeQuery).executeUpdate();
	}
	
	@Test
	public void updateObsMember_shouldHandleNullGroupMembers() {
		Obs obsGroup = new Obs();
		obsGroup.setObsId(100);
		
		dao.updateObsMember(obsGroup, null);
		
		verify(session, times(0)).createNativeQuery(anyString());
	}
	
	@Test
	public void updateObsMember_shouldHandleEmptyGroupMembers() {
		Obs obsGroup = new Obs();
		obsGroup.setObsId(100);

		Set<Obs> groupMembers = new HashSet<>();

		dao.updateObsMember(obsGroup, groupMembers);

		verify(session, times(0)).createNativeQuery(anyString());
	}
	
	@Test
	public void updateObsMember_shouldHandleSingleMember() {
		Obs obsGroup = new Obs();
		obsGroup.setObsId(50);

		Obs member = new Obs();
		member.setId(10);

		Set<Obs> groupMembers = new HashSet<>();
		groupMembers.add(member);

		when(session.createNativeQuery(anyString())).thenReturn(nativeQuery);
		when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
		when(nativeQuery.executeUpdate()).thenReturn(1);

		dao.updateObsMember(obsGroup, groupMembers);

		verify(nativeQuery).setParameter(eq("obsGroupId"), eq(50));
		verify(nativeQuery).executeUpdate();
	}
}
