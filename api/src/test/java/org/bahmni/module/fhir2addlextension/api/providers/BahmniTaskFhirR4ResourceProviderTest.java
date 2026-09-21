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
	
	@Test
	public void searchTasks_withBasedOnAndStatus() {
		ca.uhn.fhir.rest.param.ReferenceAndListParam basedOnRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		    .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam()
		        .add(new ca.uhn.fhir.rest.param.ReferenceParam("ServiceRequest", "sr-999")));
		ca.uhn.fhir.rest.param.TokenAndListParam status = new ca.uhn.fhir.rest.param.TokenAndListParam()
		    .addAnd(new ca.uhn.fhir.rest.param.TokenOrListParam().add("in-progress"));

		when(bahmniFhirTaskService.searchForTasks(any(BahmniTaskSearchParams.class)))
		    .thenReturn(null);

		provider.searchTasks(basedOnRef, null, null, null, status, null, null, null, null, new HashSet<>(),
		    null);

		verify(bahmniFhirTaskService).searchForTasks(any(BahmniTaskSearchParams.class));
	}
	
	@Test
	public void searchTasks_withAllParametersSet() {
		ca.uhn.fhir.rest.param.ReferenceAndListParam basedOnRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		    .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam()
		        .add(new ca.uhn.fhir.rest.param.ReferenceParam("ServiceRequest", "sr-111")));
		ca.uhn.fhir.rest.param.ReferenceAndListParam ownerRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		    .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam()
		        .add(new ca.uhn.fhir.rest.param.ReferenceParam("Practitioner", "prac-111")));
		ca.uhn.fhir.rest.param.ReferenceAndListParam forRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		    .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam()
		        .add(new ca.uhn.fhir.rest.param.ReferenceParam("Patient", "pat-111")));
		ca.uhn.fhir.rest.param.TokenAndListParam status = new ca.uhn.fhir.rest.param.TokenAndListParam()
		    .addAnd(new ca.uhn.fhir.rest.param.TokenOrListParam().add("requested"));

		when(bahmniFhirTaskService.searchForTasks(any(BahmniTaskSearchParams.class)))
		    .thenReturn(null);

		provider.searchTasks(basedOnRef, ownerRef, forRef, null, status, null, null, null, null, new HashSet<>(),
		    null);

		verify(bahmniFhirTaskService).searchForTasks(any(BahmniTaskSearchParams.class));
	}
	
	@Test
	public void searchTasks_withMultipleReferences() {
		ca.uhn.fhir.rest.param.ReferenceAndListParam multiOwner = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		    .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam()
		        .add(new ca.uhn.fhir.rest.param.ReferenceParam("Practitioner", "prac-1"))
		        .add(new ca.uhn.fhir.rest.param.ReferenceParam("Practitioner", "prac-2")));

		when(bahmniFhirTaskService.searchForTasks(any(BahmniTaskSearchParams.class)))
		    .thenReturn(null);

		provider.searchTasks(null, multiOwner, null, null, null, null, null, null, null, new HashSet<>(), null);

		verify(bahmniFhirTaskService).searchForTasks(any(BahmniTaskSearchParams.class));
	}
	
	@Test
	public void searchTasks_withFocusParameter() {
		ca.uhn.fhir.rest.param.ReferenceAndListParam focusRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		    .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam()
		        .add(new ca.uhn.fhir.rest.param.ReferenceParam("Observation", "obs-123")));

		when(bahmniFhirTaskService.searchForTasks(any(BahmniTaskSearchParams.class)))
		    .thenReturn(null);

		provider.searchTasks(null, null, null, focusRef, null, null, null, null, null, new HashSet<>(), null);

		verify(bahmniFhirTaskService).searchForTasks(any(BahmniTaskSearchParams.class));
	}
	
	@Test
	public void searchTasks_withEncounterAndName() {
		ca.uhn.fhir.rest.param.ReferenceAndListParam encounterRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		    .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam()
		        .add(new ca.uhn.fhir.rest.param.ReferenceParam("Encounter", "enc-456")));
		ca.uhn.fhir.rest.param.StringAndListParam name = new ca.uhn.fhir.rest.param.StringAndListParam()
		    .addAnd(new ca.uhn.fhir.rest.param.StringOrListParam()
		        .add(new ca.uhn.fhir.rest.param.StringParam("PatientHistory")));

		when(bahmniFhirTaskService.searchForTasks(any(BahmniTaskSearchParams.class)))
		    .thenReturn(null);

		provider.searchTasks(null, null, null, null, null, encounterRef, name, null, null, new HashSet<>(), null);

		verify(bahmniFhirTaskService).searchForTasks(any(BahmniTaskSearchParams.class));
	}
}
