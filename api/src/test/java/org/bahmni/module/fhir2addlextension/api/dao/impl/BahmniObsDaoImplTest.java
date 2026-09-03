package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.NativeQuery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Obs;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniObsDaoImplTest {
	
	private static final String ORDER_UUID = "7d96f25c-4949-4f72-9931-d808fbc226de";
	
	@Mock
	private SessionFactory sessionFactory;
	
	@Mock
	private Session session;
	
	@Mock
	private Criteria criteria;
	
	@Mock
	private NativeQuery nativeQuery;
	
	private BahmniObsDaoImpl dao;
	
	@Before
	public void setUp() {
		dao = new BahmniObsDaoImpl();
		ReflectionTestUtils.setField(dao, "sessionFactory", sessionFactory);
		
		when(sessionFactory.getCurrentSession()).thenReturn(session);
		when(criteria.add(any())).thenReturn(criteria);
	}
	
	@Test
	public void setupSearchParams_shouldHandleBasedOnReference() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam().setValue(ORDER_UUID)));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		
		dao.setupSearchParams(criteria, theParams);
		
		verify(criteria).add(any());
	}
	
	@Test
	public void setupSearchParams_shouldHandleBasedOnReferenceWithServiceRequestPrefix() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
		        .add(new ReferenceParam("ServiceRequest/" + ORDER_UUID)));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		
		dao.setupSearchParams(criteria, theParams);
		
		verify(criteria).add(any());
	}
	
	@Test
	public void setupSearchParams_shouldHandleNullBasedOnReference() {
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, null);
		
		dao.setupSearchParams(criteria, theParams);
		
		verify(criteria, times(0)).createAlias("order", "o");
	}
	
	@Test
	public void setupSearchParams_shouldHandleEmptySearchParameters() {
		SearchParameterMap theParams = new SearchParameterMap();
		
		dao.setupSearchParams(criteria, theParams);
		
		verify(criteria, times(0)).add(any());
	}
	
	@Test
	public void setupSearchParams_shouldHandleMultipleBasedOnReferences() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam();
		ReferenceOrListParam orListParam = new ReferenceOrListParam();
		orListParam.add(new ReferenceParam("order-uuid-1"));
		orListParam.add(new ReferenceParam("order-uuid-2"));
		basedOnReference.addAnd(orListParam);
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		
		dao.setupSearchParams(criteria, theParams);
		
		verify(criteria).add(any());
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
	public void setupSearchParams_shouldHandleMultipleAndConditionsForBasedOn() {
		ReferenceAndListParam basedOnReference = new ReferenceAndListParam();
		basedOnReference.addAnd(new ReferenceOrListParam().add(new ReferenceParam("order-uuid-1")));
		basedOnReference.addAnd(new ReferenceOrListParam().add(new ReferenceParam("order-uuid-2")));
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		
		dao.setupSearchParams(criteria, theParams);
		
		verify(criteria, times(1)).add(any());
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
	
	@Test
	public void getSearchResults_withLastnHandler_shouldFilterOutVoidedObs() {
		Obs nonVoided = new Obs();
		nonVoided.setVoided(false);
		Obs voided = new Obs();
		voided.setVoided(true);
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.LASTN_OBSERVATION_SEARCH_HANDLER, "1");
		
		BahmniObsDaoImpl spyDao = spy(dao);
		doReturn(Arrays.asList(nonVoided, voided)).when(spyDao).superGetSearchResults(theParams);
		
		List<Obs> result = spyDao.getSearchResults(theParams);
		
		assertThat(result, hasSize(1));
		assertThat(result.get(0).getVoided(), equalTo(false));
	}
	
	@Test
	public void getSearchResults_withLastnHandler_andAllVoided_shouldReturnEmptyList() {
		Obs voided1 = new Obs();
		voided1.setVoided(true);
		Obs voided2 = new Obs();
		voided2.setVoided(true);
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.LASTN_OBSERVATION_SEARCH_HANDLER, "1");
		
		BahmniObsDaoImpl spyDao = spy(dao);
		doReturn(Arrays.asList(voided1, voided2)).when(spyDao).superGetSearchResults(theParams);
		
		List<Obs> result = spyDao.getSearchResults(theParams);
		
		assertThat(result, hasSize(0));
	}
	
	@Test
	public void getSearchResults_withoutLastnHandler_shouldDelegateToParent() {
		Obs nonVoided = new Obs();
		nonVoided.setVoided(false);
		Obs voided = new Obs();
		voided.setVoided(true);
		List<Obs> allObs = Arrays.asList(nonVoided, voided);
		
		SearchParameterMap theParams = new SearchParameterMap();
		
		BahmniObsDaoImpl spyDao = spy(dao);
		doReturn(allObs).when(spyDao).superGetSearchResults(theParams);
		
		List<Obs> result = spyDao.getSearchResults(theParams);
		
		assertThat(result, hasSize(2));
	}
	
	@Test
	public void getSearchResultsCount_withLastnHandler_shouldCountOnlyNonVoidedObs() {
		Obs nonVoided1 = new Obs();
		nonVoided1.setVoided(false);
		Obs nonVoided2 = new Obs();
		nonVoided2.setVoided(false);
		Obs voided = new Obs();
		voided.setVoided(true);
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.LASTN_OBSERVATION_SEARCH_HANDLER, "1");
		
		BahmniObsDaoImpl spyDao = spy(dao);
		doReturn(Arrays.asList(nonVoided1, nonVoided2, voided)).when(spyDao).superGetSearchResults(theParams);
		
		int count = spyDao.getSearchResultsCount(theParams);
		
		assertThat(count, equalTo(2));
	}
	
	@Test
	public void getSearchResultsCount_withLastnHandler_andAllVoided_shouldReturnZero() {
		Obs voided = new Obs();
		voided.setVoided(true);
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.addParameter(FhirConstants.LASTN_OBSERVATION_SEARCH_HANDLER, "1");
		
		BahmniObsDaoImpl spyDao = spy(dao);
		doReturn(Collections.singletonList(voided)).when(spyDao).superGetSearchResults(theParams);
		
		int count = spyDao.getSearchResultsCount(theParams);
		
		assertThat(count, equalTo(0));
	}
	
	@Test
	public void getSearchResultsCount_withoutLastnHandler_shouldDelegateToParent() {
		SearchParameterMap theParams = new SearchParameterMap();
		
		BahmniObsDaoImpl spyDao = spy(dao);
		doReturn(5).when(spyDao).superGetSearchResultsCount(theParams);
		
		int count = spyDao.getSearchResultsCount(theParams);
		
		assertThat(count, equalTo(5));
	}
}
