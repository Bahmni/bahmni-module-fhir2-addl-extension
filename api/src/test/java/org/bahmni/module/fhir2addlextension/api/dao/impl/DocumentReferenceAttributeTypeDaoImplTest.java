package org.bahmni.module.fhir2addlextension.api.dao.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.hasItem;

import org.bahmni.module.fhir2addlextension.api.BaseFhir2AddlContextSensitiveTest;
import org.bahmni.module.fhir2addlextension.api.dao.DocumentReferenceAttributeTypeDao;
import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReferenceAttributeType;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.stream.Collectors;

public class DocumentReferenceAttributeTypeDaoImplTest extends BaseFhir2AddlContextSensitiveTest {
	
	private static final String DATASET = "org/bahmni/module/fhir2addlextension/api/dao/impl/DocumentReferenceDaoImplTest_data.xml";
	
	@Autowired
	private DocumentReferenceAttributeTypeDao dao;
	
	@Autowired
	@Qualifier("sessionFactory")
	private SessionFactory sessionFactory;
	
	@Before
	public void loadDataset() throws Exception {
		executeDataSet(DATASET);
	}
	
	@Test
	public void getAttributeTypes_excludingRetired_returnsOnlyActiveTypes() {
		assertThat(names(dao.getAttributeTypes(false)), containsInAnyOrder("Active type A", "Active type B"));
	}
	
	@Test
	public void getAttributeTypes_includingRetired_returnsActiveAndRetiredTypes() {
		assertThat(names(dao.getAttributeTypes(true)),
		    containsInAnyOrder("Active type A", "Active type B", "Retired type C"));
	}
	
	@Test
	public void getAttributeTypes_mapsAllColumnsOfTheType() {
		FhirDocumentReferenceAttributeType typeB = dao.getAttributeTypes(true).stream()
		        .filter(t -> "Active type B".equals(t.getName())).findFirst().get();
		FhirDocumentReferenceAttributeType typeC = dao.getAttributeTypes(true).stream()
		        .filter(t -> "Retired type C".equals(t.getName())).findFirst().get();

		assertThat(typeB.getUuid(), equalTo("drat-9002-0000-0000-0000-000000000002"));
		assertThat(typeB.getDocumentReferenceAttributeTypeId(), equalTo(9002));
		assertThat(typeB.getMinOccurs(), equalTo(0));
		assertThat(typeB.getMaxOccurs(), equalTo(1));
		assertThat(typeB.getRetired(), equalTo(false));
		assertThat(typeC.getRetired(), equalTo(true));
		assertThat(typeC.getRetireReason(), equalTo("obsolete"));
		assertThat(typeC.getRetiredBy(), notNullValue());
	}
	
	@Test
	public void getAttributeTypes_isCachedPerIncludeRetiredFlag() {
		assertThat(dao.getAttributeTypes(false), hasSize(2));
		assertThat(dao.getAttributeTypes(true), hasSize(3));
		
		sessionFactory
		        .getCurrentSession()
		        .createSQLQuery(
		            "update document_reference_attribute_type set retired = true where uuid = 'drat-9001-0000-0000-0000-000000000001'")
		        .executeUpdate();
		sessionFactory.getCurrentSession().clear();
		
		assertThat("served from cache, so the update is not yet visible", dao.getAttributeTypes(false), hasSize(2));
		assertThat(names(dao.getAttributeTypes(false)), hasItem("Active type A"));
	}
	
	@Test
	public void getAttributeTypes_afterCacheCleared_reflectsNewlyRetiredType() {
		assertThat(dao.getAttributeTypes(false), hasSize(2));
		sessionFactory
		        .getCurrentSession()
		        .createSQLQuery(
		            "update document_reference_attribute_type set retired = true where uuid = 'drat-9001-0000-0000-0000-000000000001'")
		        .executeUpdate();
		sessionFactory.getCurrentSession().clear();
		org.springframework.cache.CacheManager cm = applicationContext.getBean("apiCacheManager",
		    org.springframework.cache.CacheManager.class);
		cm.getCache("fhir2addlextensionDocAttributeType").clear();
		
		assertThat(names(dao.getAttributeTypes(false)), not(hasItem("Active type A")));
	}
	
	private List<String> names(List<FhirDocumentReferenceAttributeType> types) {
		return types.stream().map(FhirDocumentReferenceAttributeType::getName).collect(Collectors.toList());
	}
}
