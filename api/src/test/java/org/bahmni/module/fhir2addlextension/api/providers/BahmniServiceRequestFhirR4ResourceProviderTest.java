package org.bahmni.module.fhir2addlextension.api.providers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniServiceRequestSearchParams;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirServiceRequestService;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashSet;

@RunWith(MockitoJUnitRunner.class)
public class BahmniServiceRequestFhirR4ResourceProviderTest {
	
	@Mock
	private BahmniFhirServiceRequestService serviceRequestService;
	
	@InjectMocks
	private BahmniServiceRequestFhirR4ResourceProvider provider;
	
	@Test
	public void create_shouldDelegateToService() {
		ServiceRequest serviceRequest = new ServiceRequest();
		ServiceRequest created = new ServiceRequest();
		created.setId("new-id");
		when(serviceRequestService.create(serviceRequest)).thenReturn(created);
		
		provider.create(serviceRequest);
		
		verify(serviceRequestService).create(serviceRequest);
	}
	
	@Test
	public void searchForServiceRequests_shouldCallService() {
		when(serviceRequestService.searchForServiceRequestsWithCategory(any(BahmniServiceRequestSearchParams.class)))
		    .thenReturn(null);

		provider.searchForServiceRequests(null, null, null, null, null, null, null, null, null, null, null,
		    new HashSet<>(), new HashSet<>(), null);

		verify(serviceRequestService).searchForServiceRequestsWithCategory(any(BahmniServiceRequestSearchParams.class));
	}
	
	@Test
	public void searchForServiceRequests_shouldHandlePatientReference() {
		when(serviceRequestService.searchForServiceRequestsWithCategory(any(BahmniServiceRequestSearchParams.class)))
		    .thenReturn(null);

		ReferenceAndListParam patientRef = new ReferenceAndListParam()
		    .addAnd(new ReferenceOrListParam().add(new ReferenceParam("Patient", "patient-uuid")));

		provider.searchForServiceRequests(patientRef, null, null, null, null, null, null, null, null, null, null,
		    new HashSet<>(), new HashSet<>(), null);

		verify(serviceRequestService).searchForServiceRequestsWithCategory(any(BahmniServiceRequestSearchParams.class));
	}
	
	@Test
	public void searchForServiceRequests_shouldHandleSubjectReferenceWhenPatientIsNull() {
		when(serviceRequestService.searchForServiceRequestsWithCategory(any(BahmniServiceRequestSearchParams.class)))
		    .thenReturn(null);

		ReferenceAndListParam subjectRef = new ReferenceAndListParam()
		    .addAnd(new ReferenceOrListParam().add(new ReferenceParam("Patient", "subject-uuid")));

		provider.searchForServiceRequests(null, subjectRef, null, null, null, null, null, null, null, null, null,
		    new HashSet<>(), new HashSet<>(), null);

		verify(serviceRequestService).searchForServiceRequestsWithCategory(any(BahmniServiceRequestSearchParams.class));
	}
	
	@Test
	public void searchForServiceRequests_shouldConvertEmptyIncludesToNull() {
		when(serviceRequestService.searchForServiceRequestsWithCategory(any(BahmniServiceRequestSearchParams.class)))
		    .thenReturn(null);

		provider.searchForServiceRequests(null, null, null, null, null, null, null, null, null, null, null,
		    new HashSet<>(), new HashSet<>(), null);

		verify(serviceRequestService).searchForServiceRequestsWithCategory(any(BahmniServiceRequestSearchParams.class));
	}
	
	@Test
	public void searchForServiceRequests_shouldHandleAllParameters() {
		when(serviceRequestService.searchForServiceRequestsWithCategory(any(BahmniServiceRequestSearchParams.class)))
		    .thenReturn(null);

		ReferenceAndListParam patientRef = new ReferenceAndListParam()
		    .addAnd(new ReferenceOrListParam().add(new ReferenceParam("Patient", "patient-uuid")));

		provider.searchForServiceRequests(patientRef, null, null, null, null, null, null, null, null,
		    null, null, new HashSet<>(), new HashSet<>(), null);

		verify(serviceRequestService).searchForServiceRequestsWithCategory(any(BahmniServiceRequestSearchParams.class));
	}
	
	@Test
	public void searchForServiceRequestsByNumberOfVisits_shouldCallService() {
		when(serviceRequestService.searchForServiceRequestsByNumberOfVisits(any(), any(), any(), any(), any(), any()))
		    .thenReturn(null);

		ReferenceParam patientRef = new ReferenceParam("Patient", "patient-uuid");
		NumberParam numberOfVisits = new NumberParam(5);

		provider.searchForServiceRequestsByNumberOfVisits(patientRef, numberOfVisits, null, null, new HashSet<>(),
		    new HashSet<>());

		verify(serviceRequestService).searchForServiceRequestsByNumberOfVisits(any(), any(), any(), any(), any(), any());
	}
	
	@Test
	public void searchForServiceRequestsByNumberOfVisits_shouldConvertEmptyIncludesToNull() {
		when(serviceRequestService.searchForServiceRequestsByNumberOfVisits(any(), any(), any(), any(), any(), any()))
		    .thenReturn(null);

		ReferenceParam patientRef = new ReferenceParam("Patient", "patient-uuid");
		NumberParam numberOfVisits = new NumberParam(5);

		provider.searchForServiceRequestsByNumberOfVisits(patientRef, numberOfVisits, null, null, new HashSet<>(),
		    new HashSet<>());

		verify(serviceRequestService).searchForServiceRequestsByNumberOfVisits(any(), any(), any(), any(), any(), any());
	}
	
	@Test
	public void searchForServiceRequestsByNumberOfVisits_shouldHandleCategoryAndSort() {
		when(serviceRequestService.searchForServiceRequestsByNumberOfVisits(any(), any(), any(), any(), any(), any()))
		    .thenReturn(null);

		ReferenceParam patientRef = new ReferenceParam("Patient", "patient-uuid");
		NumberParam numberOfVisits = new NumberParam(10);
		ReferenceAndListParam categoryRef = new ReferenceAndListParam()
		    .addAnd(new ReferenceOrListParam().add(new ReferenceParam("category-uuid")));
		SortSpec sort = new SortSpec("dateTime");
		HashSet<Include> includes = new HashSet<>();
		includes.add(new Include("ServiceRequest:requester"));

		provider.searchForServiceRequestsByNumberOfVisits(patientRef, numberOfVisits, categoryRef, sort, includes,
		    new HashSet<>());

		verify(serviceRequestService).searchForServiceRequestsByNumberOfVisits(any(), any(), any(), any(), any(), any());
	}
}
