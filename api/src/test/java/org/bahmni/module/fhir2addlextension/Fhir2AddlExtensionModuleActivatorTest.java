package org.bahmni.module.fhir2addlextension;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.GlobalPropertyListener;
import org.openmrs.api.context.Context;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class Fhir2AddlExtensionModuleActivatorTest {
	
	@Mock
	private AdministrationService administrationService;
	
	private Fhir2AddlExtensionModuleActivator activator;
	
	@Before
	public void setUp() {
		activator = new Fhir2AddlExtensionModuleActivator();
	}
	
	@Test
	public void shouldRegisterTheCacheInvalidatorAsAGlobalPropertyListener() {
		activator.registerCacheInvalidator(administrationService);
		
		ArgumentCaptor<GlobalPropertyListener> captor = ArgumentCaptor.forClass(GlobalPropertyListener.class);
		verify(administrationService).addGlobalPropertyListener(captor.capture());
		assertTrue(captor.getValue() instanceof FhirGlobalPropertyCacheInvalidator);
	}
	
	@Test
	public void shouldRemoveBeforeAddingSoRepeatedRegistrationDoesNotDuplicate() {
		activator.registerCacheInvalidator(administrationService);
		
		InOrder inOrder = inOrder(administrationService);
		inOrder.verify(administrationService).removeGlobalPropertyListener(any(GlobalPropertyListener.class));
		inOrder.verify(administrationService).addGlobalPropertyListener(any(GlobalPropertyListener.class));
	}
	
	@Test
	public void shouldReRegisterTheSameListenerOnEveryContextRefresh() {
		activator.registerCacheInvalidator(administrationService);
		activator.registerCacheInvalidator(administrationService);
		
		ArgumentCaptor<GlobalPropertyListener> captor = ArgumentCaptor.forClass(GlobalPropertyListener.class);
		verify(administrationService, times(2)).addGlobalPropertyListener(captor.capture());
		assertSame(captor.getAllValues().get(0), captor.getAllValues().get(1));
	}
	
	@Test
	public void shouldDeregisterTheCacheInvalidatorWithoutReAddingIt() {
		activator.deregisterCacheInvalidator(administrationService);
		
		verify(administrationService).removeGlobalPropertyListener(any(GlobalPropertyListener.class));
		verify(administrationService, never()).addGlobalPropertyListener(any(GlobalPropertyListener.class));
	}
	
	@Test
	public void shouldRegisterTheCacheInvalidatorWhenTheContextIsRefreshed() {
		try (MockedStatic<Context> mockedContext = Mockito.mockStatic(Context.class)) {
			mockedContext.when(Context::getAdministrationService).thenReturn(administrationService);
			
			activator.contextRefreshed();
			
			ArgumentCaptor<GlobalPropertyListener> captor = ArgumentCaptor.forClass(GlobalPropertyListener.class);
			verify(administrationService).addGlobalPropertyListener(captor.capture());
			assertTrue(captor.getValue() instanceof FhirGlobalPropertyCacheInvalidator);
		}
	}
	
	@Test
	public void shouldDeregisterTheCacheInvalidatorWhenTheModuleWillStop() {
		try (MockedStatic<Context> mockedContext = Mockito.mockStatic(Context.class)) {
			mockedContext.when(Context::getAdministrationService).thenReturn(administrationService);
			
			activator.willStop();
			
			verify(administrationService).removeGlobalPropertyListener(any(GlobalPropertyListener.class));
			verify(administrationService, never()).addGlobalPropertyListener(any(GlobalPropertyListener.class));
		}
	}
	
	@Test
	public void shouldReRegisterAfterAWillStopContextRefreshedCycle() {
		try (MockedStatic<Context> mockedContext = Mockito.mockStatic(Context.class)) {
			mockedContext.when(Context::getAdministrationService).thenReturn(administrationService);
			
			activator.contextRefreshed();
			activator.willStop();
			activator.contextRefreshed();
			
			ArgumentCaptor<GlobalPropertyListener> captor = ArgumentCaptor.forClass(GlobalPropertyListener.class);
			verify(administrationService, times(2)).addGlobalPropertyListener(captor.capture());
			assertSame(captor.getAllValues().get(0), captor.getAllValues().get(1));
		}
	}
}
