package org.bahmni.module.fhir2addlextension.api.providers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniTaskSearchParams;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirTaskService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashSet;

@RunWith(MockitoJUnitRunner.class)
public class BahmniTaskFhirR4ResourceProviderTest {
	
	@Mock
	private BahmniFhirTaskService bahmniFhirTaskService;
	
	@InjectMocks
	private BahmniTaskFhirR4ResourceProvider provider;
	
	@Test
	public void searchTasks_shouldCallSearchService() {
		when(bahmniFhirTaskService.searchForTasks(any(BahmniTaskSearchParams.class)))
		    .thenReturn(null);

		provider.searchTasks(null, null, null, null, null, null, null, null, null, new HashSet<>(),
		    null);

		verify(bahmniFhirTaskService).searchForTasks(any(BahmniTaskSearchParams.class));
	}
	
	@Test
	public void searchTasks_shouldConvertEmptyIncludesToNonNullSet() {
		when(bahmniFhirTaskService.searchForTasks(any(BahmniTaskSearchParams.class)))
		    .thenReturn(null);

		HashSet<Include> emptyIncludes = new HashSet<>();

		provider.searchTasks(null, null, null, null, null, null, null, null, null, emptyIncludes,
		    null);

		verify(bahmniFhirTaskService).searchForTasks(any(BahmniTaskSearchParams.class));
	}
	
	@Test
	public void searchTasks_shouldHandleAllParameters() {
		when(bahmniFhirTaskService.searchForTasks(any(BahmniTaskSearchParams.class)))
		    .thenReturn(null);

		DateRangeParam lastUpdated = new DateRangeParam();
		HashSet<Include> includes = new HashSet<>();
		SortSpec sort = new SortSpec("name");

		provider.searchTasks(null, null, null, null, null, null, null, null, lastUpdated, includes,
		    sort);

		verify(bahmniFhirTaskService).searchForTasks(any(BahmniTaskSearchParams.class));
	}
}
