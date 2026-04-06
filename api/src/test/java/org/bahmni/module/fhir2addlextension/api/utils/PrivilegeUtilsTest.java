package org.bahmni.module.fhir2addlextension.api.utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.User;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.api.db.ContextDAO;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PrivilegeUtilsTest {
	
	private static final String TEST_PRIVILEGE = "Get Diagnostic Report";
	
	@Mock
	private ContextDAO contextDAO;
	
	@Mock
	private UserContext userContext;
	
	@Mock
	private User user;
	
	@Before
	public void setUp() {
		Context.setDAO(contextDAO);
		Context.openSession();
		Context.setUserContext(userContext);
	}
	
	@After
	public void tearDown() {
		Context.closeSession();
	}
	
	@Test(expected = APIAuthenticationException.class)
	public void shouldThrowWhenUserIsNotAuthenticated() {
		when(userContext.getAuthenticatedUser()).thenReturn(null);
		
		PrivilegeUtils.requirePrivilege(TEST_PRIVILEGE);
	}
	
	@Test(expected = APIAuthenticationException.class)
	public void shouldThrowWhenUserLacksPrivilege() {
		when(userContext.getAuthenticatedUser()).thenReturn(user);
		when(user.hasPrivilege(TEST_PRIVILEGE)).thenReturn(false);
		
		PrivilegeUtils.requirePrivilege(TEST_PRIVILEGE);
	}
	
	@Test
	public void shouldNotThrowWhenUserHasPrivilege() {
		when(userContext.getAuthenticatedUser()).thenReturn(user);
		when(user.hasPrivilege(TEST_PRIVILEGE)).thenReturn(true);
		
		PrivilegeUtils.requirePrivilege(TEST_PRIVILEGE);
	}
}
