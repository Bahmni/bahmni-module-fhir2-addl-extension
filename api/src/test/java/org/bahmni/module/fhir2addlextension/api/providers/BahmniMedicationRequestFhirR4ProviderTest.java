package org.bahmni.module.fhir2addlextension.api.providers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bahmni.module.fhir2addlextension.api.search.param.BahmniMedicationRequestSearchParams;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.fhir2.api.FhirMedicationRequestService;

import java.util.HashSet;

@RunWith(MockitoJUnitRunner.class)
public class BahmniMedicationRequestFhirR4ProviderTest {
	
	@Mock
	private FhirMedicationRequestService fhirMedicationRequestService;
	
	@InjectMocks
	private BahmniMedicationRequestFhirR4Provider provider;
	
	@Test
	public void createMedicationRequest_shouldDelegateToService() {
		MedicationRequest request = new MedicationRequest();
		MedicationRequest created = new MedicationRequest();
		created.setId("new-id");
		when(fhirMedicationRequestService.create(request)).thenReturn(created);
		
		provider.createMedicationRequest(request);
		
		verify(fhirMedicationRequestService).create(request);
	}
	
	@Test
	public void searchForMedicationRequestsWithLocation_shouldCallSearchService() {
		when(fhirMedicationRequestService.searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class)))
		    .thenReturn(null);

		provider.searchForMedicationRequestsWithLocation(null, null, null, null, null, null, null, null, null, null,
		    null, null, new HashSet<>(), new HashSet<>(), null);

		verify(fhirMedicationRequestService).searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class));
	}
	
	@Test
	public void searchForMedicationRequestsWithLocation_shouldConvertEmptyIncludesToNull() {
		when(fhirMedicationRequestService.searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class)))
		    .thenReturn(null);

		provider.searchForMedicationRequestsWithLocation(null, null, null, null, null, null, null, null, null, null,
		    null, null, new HashSet<>(), new HashSet<>(), null);

		verify(fhirMedicationRequestService).searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class));
	}
	
	@Test
	public void searchForMedicationRequestsWithLocation_shouldUsePatientWhenProvided() {
		ca.uhn.fhir.rest.param.ReferenceAndListParam patientRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		    .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam()
		        .add(new ca.uhn.fhir.rest.param.ReferenceParam("Patient", "pat-123")));

		when(fhirMedicationRequestService.searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class)))
		    .thenReturn(null);

		provider.searchForMedicationRequestsWithLocation(patientRef, null, null, null, null, null, null, null, null, null,
		    null, null, new HashSet<>(), new HashSet<>(), null);

		verify(fhirMedicationRequestService).searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class));
	}
	
	@Test
	public void searchForMedicationRequestsWithLocation_shouldPreferPatientOverSubject() {
		ca.uhn.fhir.rest.param.ReferenceAndListParam patientRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		    .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam()
		        .add(new ca.uhn.fhir.rest.param.ReferenceParam("Patient", "pat-123")));
		ca.uhn.fhir.rest.param.ReferenceAndListParam subjectRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		    .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam()
		        .add(new ca.uhn.fhir.rest.param.ReferenceParam("Patient", "sub-456")));

		when(fhirMedicationRequestService.searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class)))
		    .thenReturn(null);

		provider.searchForMedicationRequestsWithLocation(patientRef, subjectRef, null, null, null, null, null, null, null,
		    null, null, null, new HashSet<>(), new HashSet<>(), null);

		verify(fhirMedicationRequestService).searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class));
	}
	
	@Test
	public void searchForMedicationRequestsWithLocation_shouldUseSubjectWhenPatientNull() {
		ca.uhn.fhir.rest.param.ReferenceAndListParam subjectRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		    .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam()
		        .add(new ca.uhn.fhir.rest.param.ReferenceParam("Patient", "sub-456")));

		when(fhirMedicationRequestService.searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class)))
		    .thenReturn(null);

		provider.searchForMedicationRequestsWithLocation(null, subjectRef, null, null, null, null, null, null, null, null,
		    null, null, new HashSet<>(), new HashSet<>(), null);

		verify(fhirMedicationRequestService).searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class));
	}
	
	@Test
	public void searchForMedicationRequestsWithLocation_shouldPassLocationAndSort() {
		ca.uhn.fhir.rest.param.ReferenceAndListParam locationRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		    .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam()
		        .add(new ca.uhn.fhir.rest.param.ReferenceParam("Location", "loc-789")));
		ca.uhn.fhir.rest.api.SortSpec sort = new ca.uhn.fhir.rest.api.SortSpec("status");

		when(fhirMedicationRequestService.searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class)))
		    .thenReturn(null);

		provider.searchForMedicationRequestsWithLocation(null, null, null, null, null, null, null, null, null, locationRef,
		    null, null, new HashSet<>(), new HashSet<>(), sort);

		verify(fhirMedicationRequestService).searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class));
	}
	
	@Test
	public void searchForMedicationRequestsWithLocation_allParameters() {
		ca.uhn.fhir.rest.param.ReferenceAndListParam patientRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		    .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam()
		        .add(new ca.uhn.fhir.rest.param.ReferenceParam("Patient", "pat-all")));
		ca.uhn.fhir.rest.param.ReferenceAndListParam encounterRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		    .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam()
		        .add(new ca.uhn.fhir.rest.param.ReferenceParam("Encounter", "enc-all")));
		ca.uhn.fhir.rest.param.TokenAndListParam code = new ca.uhn.fhir.rest.param.TokenAndListParam()
		    .addAnd(new ca.uhn.fhir.rest.param.TokenOrListParam().add("code-all"));
		ca.uhn.fhir.rest.param.TokenAndListParam status = new ca.uhn.fhir.rest.param.TokenAndListParam()
		    .addAnd(new ca.uhn.fhir.rest.param.TokenOrListParam().add("active"));
		ca.uhn.fhir.rest.param.ReferenceAndListParam locationRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		    .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam()
		        .add(new ca.uhn.fhir.rest.param.ReferenceParam("Location", "loc-all")));

		when(fhirMedicationRequestService.searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class)))
		    .thenReturn(null);

		provider.searchForMedicationRequestsWithLocation(patientRef, null, encounterRef, code, null, null, status, null,
		    null, locationRef, null, null, new HashSet<>(), new HashSet<>(), null);

		verify(fhirMedicationRequestService).searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class));
	}
	
	@Test
	public void searchForMedicationRequestsWithLocation_onlySubject() {
		ca.uhn.fhir.rest.param.ReferenceAndListParam subjectRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		    .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam()
		        .add(new ca.uhn.fhir.rest.param.ReferenceParam("Patient", "pat-sub")));

		when(fhirMedicationRequestService.searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class)))
		    .thenReturn(null);

		provider.searchForMedicationRequestsWithLocation(null, subjectRef, null, null, null, null, null, null, null, null,
		    null, null, new HashSet<>(), new HashSet<>(), null);

		verify(fhirMedicationRequestService).searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class));
	}
	
	@Test
	public void searchForMedicationRequestsWithLocation_withRequester() {
		ca.uhn.fhir.rest.param.ReferenceAndListParam requesterRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		    .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam()
		        .add(new ca.uhn.fhir.rest.param.ReferenceParam("Practitioner", "prac-789")));

		when(fhirMedicationRequestService.searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class)))
		    .thenReturn(null);

		provider.searchForMedicationRequestsWithLocation(null, null, null, null, requesterRef, null, null, null, null, null,
		    null, null, new HashSet<>(), new HashSet<>(), null);

		verify(fhirMedicationRequestService).searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class));
	}
	
	@Test
	public void searchForMedicationRequestsWithLocation_withStatusAndFulfillerStatus() {
		ca.uhn.fhir.rest.param.TokenAndListParam status = new ca.uhn.fhir.rest.param.TokenAndListParam()
		    .addAnd(new ca.uhn.fhir.rest.param.TokenOrListParam().add("active"));
		ca.uhn.fhir.rest.param.TokenAndListParam fulfillerStatus = new ca.uhn.fhir.rest.param.TokenAndListParam()
		    .addAnd(new ca.uhn.fhir.rest.param.TokenOrListParam().add("in-progress"));

		when(fhirMedicationRequestService.searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class)))
		    .thenReturn(null);

		provider.searchForMedicationRequestsWithLocation(null, null, null, null, null, null, null, status, fulfillerStatus, null,
		    null, null, new HashSet<>(), new HashSet<>(), null);

		verify(fhirMedicationRequestService).searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class));
	}
	
	@Test
	public void searchForMedicationRequestsWithLocation_withMultipleLocationRefs() {
		ca.uhn.fhir.rest.param.ReferenceAndListParam multiLocationRef = new ca.uhn.fhir.rest.param.ReferenceAndListParam()
		    .addAnd(new ca.uhn.fhir.rest.param.ReferenceOrListParam()
		        .add(new ca.uhn.fhir.rest.param.ReferenceParam("Location", "loc-1"))
		        .add(new ca.uhn.fhir.rest.param.ReferenceParam("Location", "loc-2")));

		when(fhirMedicationRequestService.searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class)))
		    .thenReturn(null);

		provider.searchForMedicationRequestsWithLocation(null, null, null, null, null, null, null, null, null, multiLocationRef,
		    null, null, new HashSet<>(), new HashSet<>(), null);

		verify(fhirMedicationRequestService).searchForMedicationRequests(any(BahmniMedicationRequestSearchParams.class));
	}
}
