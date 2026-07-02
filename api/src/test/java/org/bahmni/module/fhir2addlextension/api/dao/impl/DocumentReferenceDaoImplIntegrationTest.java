package org.bahmni.module.fhir2addlextension.api.dao.impl;

import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReference;
import org.h2.jdbcx.JdbcDataSource;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.orm.hibernate5.LocalSessionFactoryBuilder;

import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Boots a real Hibernate SessionFactory over the OpenMRS model plus this module's mappings against
 * an in-memory H2 database. This exercises the load-bearing {@code encounter -> encounter_id}
 * many-to-one in DocumentReference.hbm.xml: a wrong column, class FQN or field name fails at
 * SessionFactory startup here, which the Mockito-based unit tests cannot catch.
 */
public class DocumentReferenceDaoImplIntegrationTest {
	
	private static SessionFactory sessionFactory;
	
	@BeforeClass
	public static void buildSessionFactory() throws Exception {
		JdbcDataSource dataSource = new JdbcDataSource();
		dataSource.setUrl("jdbc:h2:mem:docref;DB_CLOSE_DELAY=-1");
		dataSource.setUser("sa");
		
		Properties properties = new Properties();
		properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
		properties.put("hibernate.hbm2ddl.auto", "create");
		properties.put("hibernate.search.autoregister_listeners", "false");
		
		LocalSessionFactoryBuilder builder = new LocalSessionFactoryBuilder(dataSource);
		for (Resource mapping : new PathMatchingResourcePatternResolver()
		        .getResources("classpath*:org/openmrs/api/db/hibernate/*.hbm.xml")) {
			builder.addInputStream(mapping.getInputStream());
		}
		builder.scanPackages("org.openmrs");
		builder.addResource("DocumentReference.hbm.xml");
		builder.addResource("DocumentReferenceContent.hbm.xml");
		builder.addResource("DocumentReferenceAttribute.hbm.xml");
		builder.addResource("DocumentReferenceAttributeType.hbm.xml");
		builder.addProperties(properties);
		
		sessionFactory = builder.buildSessionFactory();
	}
	
	@AfterClass
	public static void closeSessionFactory() {
		if (sessionFactory != null) {
			sessionFactory.close();
		}
	}
	
	@Test
	public void shouldMapEncounterToEncounterIdColumn() {
		AbstractEntityPersister persister = (AbstractEntityPersister) ((SessionFactoryImplementor) sessionFactory)
		        .getMetamodel().entityPersister(FhirDocumentReference.class.getName());
		
		String[] encounterColumns = persister.getPropertyColumnNames("encounter");
		
		assertNotNull(encounterColumns);
		assertEquals(1, encounterColumns.length);
		assertEquals("encounter_id", encounterColumns[0]);
		assertEquals("org.openmrs.Encounter", persister.getPropertyType("encounter").getReturnedClass().getName());
	}
}
