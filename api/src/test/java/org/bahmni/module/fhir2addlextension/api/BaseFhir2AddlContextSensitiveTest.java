package org.bahmni.module.fhir2addlextension.api;

import org.junit.Before;
import org.mockito.Mockito;
import org.openmrs.api.cache.CacheConfig;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.ContextDAO;
import org.openmrs.module.fhir2.api.util.FhirGlobalPropertyHolder;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ContextConfiguration;

import java.sql.SQLException;

/**
 * Base class for tests that run against the real Spring/Hibernate context and an in-memory H2
 * database. Each test method runs in a transaction that is rolled back afterwards.
 */
@ContextConfiguration(classes = { TestFhir2AddlSpringConfiguration.class }, inheritLocations = false)
public abstract class BaseFhir2AddlContextSensitiveTest extends BaseModuleContextSensitiveTest {
	
	@Autowired
	private CacheConfig cacheConfig;
	
	@Autowired
	private ContextDAO contextDAO;
	
	/**
	 * Several mock-based tests in this module call Context.setDAO(mock) /
	 * Context.setUserContext(mock) on the static, thread-local Context and never undo it. When such
	 * a test runs before a context-sensitive one, the latter would see a mock DAO and an
	 * unauthenticated user. Restore the real DAO (and a real user context) before the OpenMRS base
	 * class sets up and authenticates.
	 */
	@Override
	@Before
	public void baseSetupWithStandardDataAndAuthentication() throws SQLException {
		Context.setDAO(contextDAO);
		if (Context.isSessionOpen() && Mockito.mockingDetails(Context.getUserContext()).isMock()) {
			Context.openSession();
		}
		super.baseSetupWithStandardDataAndAuthentication();
	}
	
	@Before
	public void clearCaches() throws Exception {
		CacheManager cm = cacheConfig.apiCacheManager();
		cm.getCacheNames().forEach(name -> cm.getCache(name).clear());
		FhirGlobalPropertyHolder.reset();
	}
}
