package org.bahmni.module.fhir2AddlExtension.api.helper;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.bahmni.module.fhir2AddlExtension.api.TestDataFactory;
import org.bahmni.module.fhir2AddlExtension.api.domain.DiagnosticReportBundle;
import org.bahmni.module.fhir2AddlExtension.api.utils.BahmniFhirUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.fhir2.FhirConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class ConsultationBundleEntriesHelperTest {
	
	private List<Bundle.BundleEntryComponent> entries;
	
	private Map<String, Bundle.BundleEntryComponent> processedEntries;
	
	@Before
    public void setup() {
        entries = new ArrayList<>();
        processedEntries = new HashMap<>();
    }
	
	// Tests for orderEntriesByReference method
	
	@Test
	public void shouldReturnEmptyListWhenEntriesIsNull() {
		// When
		List<Bundle.BundleEntryComponent> result = ConsultationBundleEntriesHelper.orderEntriesByReference(null);
		
		// Then
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}
	
	@Test
	public void shouldReturnSameListWhenEntriesIsEmpty() {
		// When
		List<Bundle.BundleEntryComponent> result = ConsultationBundleEntriesHelper.orderEntriesByReference(entries);
		
		// Then
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}
	
	@Test
	public void shouldReturnSameEntryWhenOnlyOneEntryExists() {
		// Given
		Bundle.BundleEntryComponent entry = createBundleEntry(createEncounter(), "urn:uuid:encounter");
		entries.add(entry);
		
		// When
		List<Bundle.BundleEntryComponent> result = ConsultationBundleEntriesHelper.orderEntriesByReference(entries);
		
		// Then
		assertEquals(1, result.size());
		assertEquals(entry, result.get(0));
	}
	
	@Test
	public void shouldOrderEntriesBasedOnDependencies() {
		// Given
		// Create an encounter entry
		Bundle.BundleEntryComponent encounterEntry = createBundleEntry(createEncounter(), "urn:uuid:encounter");
		
		// Create a condition that references the encounter
		Condition condition = createCondition();
		condition.setEncounter(new Reference("urn:uuid:encounter"));
		Bundle.BundleEntryComponent conditionEntry = createBundleEntry(condition, "urn:uuid:condition");
		
		// Create an allergy intolerance that references the encounter
		AllergyIntolerance allergyIntolerance = createAllergyIntolerance();
		allergyIntolerance.setEncounter(new Reference("urn:uuid:encounter"));
		Bundle.BundleEntryComponent allergyEntry = createBundleEntry(allergyIntolerance, "urn:uuid:allergy");
		
		// Add entries in an order where dependencies are not respected
		entries.add(conditionEntry);
		entries.add(allergyEntry);
		entries.add(encounterEntry);
		
		// When
		List<Bundle.BundleEntryComponent> result = ConsultationBundleEntriesHelper.orderEntriesByReference(entries);
		
		// Then
		assertEquals(3, result.size());
		// Encounter should be first since it's referenced by the others
		assertEquals(encounterEntry, result.get(0));
		// The other two can be in any order since they both depend only on the encounter
		assertTrue(result.contains(conditionEntry));
		assertTrue(result.contains(allergyEntry));
	}
	
	@Test
	public void shouldHandleCircularDependencies() {
		// Given
		// Create entries with circular dependencies
		Bundle.BundleEntryComponent entry1 = createBundleEntry(createEncounter(), "urn:uuid:entry1");
		
		Condition condition = createCondition();
		condition.setEncounter(new Reference("urn:uuid:entry1"));
		Bundle.BundleEntryComponent entry2 = createBundleEntry(condition, "urn:uuid:entry2");
		
		// Artificially create a circular dependency (not realistic but tests the code)
		Encounter encounter = (Encounter) entry1.getResource();
		encounter.addReasonReference(new Reference("urn:uuid:entry2"));
		
		entries.add(entry1);
		entries.add(entry2);
		
		// When
		List<Bundle.BundleEntryComponent> result = ConsultationBundleEntriesHelper.orderEntriesByReference(entries);
		
		// Then
		assertEquals(2, result.size());
		assertTrue(result.contains(entry1));
		assertTrue(result.contains(entry2));
	}
	
	@Test
	public void shouldHandleEntriesWithoutFullUrl() {
		// Given
		Bundle.BundleEntryComponent entryWithoutFullUrl = new Bundle.BundleEntryComponent();
		entryWithoutFullUrl.setResource(createEncounter());
		// No fullUrl set
		
		Bundle.BundleEntryComponent normalEntry = createBundleEntry(createCondition(), "urn:uuid:condition");
		
		entries.add(entryWithoutFullUrl);
		entries.add(normalEntry);
		
		// When
		List<Bundle.BundleEntryComponent> result = ConsultationBundleEntriesHelper.orderEntriesByReference(entries);
		
		// Then
		assertEquals(2, result.size());
		// Entry without fullUrl should not cause issues
		assertTrue(result.contains(normalEntry));
	}
	
	@Test
	public void shouldHandleEntriesWithoutResource() {
		// Given
		Bundle.BundleEntryComponent entryWithoutResource = new Bundle.BundleEntryComponent();
		entryWithoutResource.setFullUrl("urn:uuid:empty");
		// No resource set
		
		Bundle.BundleEntryComponent normalEntry = createBundleEntry(createEncounter(), "urn:uuid:encounter");
		
		entries.add(entryWithoutResource);
		entries.add(normalEntry);
		
		// When
		List<Bundle.BundleEntryComponent> result = ConsultationBundleEntriesHelper.orderEntriesByReference(entries);
		
		// Then
		assertEquals(2, result.size());
		// Entry without resource should not cause issues
		assertTrue(result.contains(normalEntry));
	}
	
	@Test
	public void shouldOrderComplexDependencyChain() {
		// Given
		// Create an encounter entry
		Bundle.BundleEntryComponent encounterEntry = createBundleEntry(createEncounter(), "urn:uuid:encounter");
		
		// Create a condition that references the encounter
		Condition condition = createCondition();
		condition.setEncounter(new Reference("urn:uuid:encounter"));
		Bundle.BundleEntryComponent conditionEntry = createBundleEntry(condition, "urn:uuid:condition");
		
		// Create a service request that references the encounter
		ServiceRequest serviceRequest = createServiceRequest();
		serviceRequest.setEncounter(new Reference("urn:uuid:encounter"));
		Bundle.BundleEntryComponent serviceRequestEntry = createBundleEntry(serviceRequest, "urn:uuid:service");
		
		// Create a medication request that references the encounter
		MedicationRequest medicationRequest = createMedicationRequest();
		medicationRequest.setEncounter(new Reference("urn:uuid:encounter"));
		Bundle.BundleEntryComponent medicationRequestEntry = createBundleEntry(medicationRequest, "urn:uuid:medication");
		
		// Add entries in random order
		entries.add(medicationRequestEntry);
		entries.add(conditionEntry);
		entries.add(serviceRequestEntry);
		entries.add(encounterEntry);
		
		// When
		List<Bundle.BundleEntryComponent> result = ConsultationBundleEntriesHelper.orderEntriesByReference(entries);
		
		// Then
		assertEquals(4, result.size());
		// Encounter should be first since it's referenced by all others
		assertEquals(encounterEntry, result.get(0));
		// The other three can be in any order after the encounter
		assertTrue(result.indexOf(conditionEntry) > result.indexOf(encounterEntry));
		assertTrue(result.indexOf(serviceRequestEntry) > result.indexOf(encounterEntry));
		assertTrue(result.indexOf(medicationRequestEntry) > result.indexOf(encounterEntry));
	}
	
	// Tests for resolveReferences method
	
	@Test
	public void shouldResolveConditionEncounterReference() {
		// Given
		Condition condition = createCondition();
		condition.setEncounter(new Reference("urn:uuid:placeholder"));
		Bundle.BundleEntryComponent conditionEntry = createBundleEntry(condition, "urn:uuid:condition");
		
		// Create a processed encounter entry
		Encounter encounter = createEncounter();
		encounter.setId("encounter-uuid");
		Bundle.BundleEntryComponent encounterEntry = createBundleEntry(encounter, "urn:uuid:placeholder");
		processedEntries.put("urn:uuid:placeholder", encounterEntry);
		
		// When
		Bundle.BundleEntryComponent result = ConsultationBundleEntriesHelper.resolveReferences(conditionEntry,
		    processedEntries);
		
		// Then
		Condition resultCondition = (Condition) result.getResource();
		assertEquals(FhirConstants.ENCOUNTER, resultCondition.getEncounter().getType());
		assertEquals(FhirConstants.ENCOUNTER + "/encounter-uuid", resultCondition.getEncounter().getReference());
	}
	
	@Test
	public void shouldResolveAllergyIntoleranceEncounterReference() {
		// Given
		AllergyIntolerance allergyIntolerance = createAllergyIntolerance();
		allergyIntolerance.setEncounter(new Reference("urn:uuid:placeholder"));
		Bundle.BundleEntryComponent allergyEntry = createBundleEntry(allergyIntolerance, "urn:uuid:allergy");
		
		// Create a processed encounter entry
		Encounter encounter = createEncounter();
		encounter.setId("encounter-uuid");
		Bundle.BundleEntryComponent encounterEntry = createBundleEntry(encounter, "urn:uuid:placeholder");
		processedEntries.put("urn:uuid:placeholder", encounterEntry);
		
		// When
		Bundle.BundleEntryComponent result = ConsultationBundleEntriesHelper.resolveReferences(allergyEntry,
		    processedEntries);
		
		// Then
		AllergyIntolerance resultAllergy = (AllergyIntolerance) result.getResource();
		assertEquals(FhirConstants.ENCOUNTER, resultAllergy.getEncounter().getType());
		assertEquals(FhirConstants.ENCOUNTER + "/encounter-uuid", resultAllergy.getEncounter().getReference());
	}
	
	@Test
	public void shouldResolveServiceRequestEncounterReference() {
		// Given
		ServiceRequest serviceRequest = createServiceRequest();
		serviceRequest.setEncounter(new Reference("urn:uuid:placeholder"));
		Bundle.BundleEntryComponent serviceRequestEntry = createBundleEntry(serviceRequest, "urn:uuid:allergy");
		
		// Create a processed encounter entry
		Encounter encounter = createEncounter();
		encounter.setId("encounter-uuid");
		Bundle.BundleEntryComponent encounterEntry = createBundleEntry(encounter, "urn:uuid:placeholder");
		processedEntries.put("urn:uuid:placeholder", encounterEntry);
		
		// When
		Bundle.BundleEntryComponent result = ConsultationBundleEntriesHelper.resolveReferences(serviceRequestEntry,
		    processedEntries);
		
		// Then
		ServiceRequest resultAllergy = (ServiceRequest) result.getResource();
		assertEquals(FhirConstants.ENCOUNTER, resultAllergy.getEncounter().getType());
		assertEquals(FhirConstants.ENCOUNTER + "/encounter-uuid", resultAllergy.getEncounter().getReference());
	}
	
	@Test
	public void shouldResolveMedicationRequestEncounterReference() {
		// Given
		MedicationRequest medicationRequest = createMedicationRequest();
		medicationRequest.setEncounter(new Reference("urn:uuid:placeholder"));
		Bundle.BundleEntryComponent medicationRequestEntry = createBundleEntry(medicationRequest, "urn:uuid:medication");
		
		// Create a processed encounter entry
		Encounter encounter = createEncounter();
		encounter.setId("encounter-uuid");
		Bundle.BundleEntryComponent encounterEntry = createBundleEntry(encounter, "urn:uuid:placeholder");
		processedEntries.put("urn:uuid:placeholder", encounterEntry);
		
		// When
		Bundle.BundleEntryComponent result = ConsultationBundleEntriesHelper.resolveReferences(medicationRequestEntry,
		    processedEntries);
		
		// Then
		MedicationRequest resultMedicationRequest = (MedicationRequest) result.getResource();
		assertEquals(FhirConstants.ENCOUNTER, resultMedicationRequest.getEncounter().getType());
		assertEquals(FhirConstants.ENCOUNTER + "/encounter-uuid", resultMedicationRequest.getEncounter().getReference());
	}
	
	@Test
	public void shouldResolveObservationEncounterReference() {
		// Given
		Observation observation = createObservation();
		observation.setEncounter(new Reference("urn:uuid:placeholder"));
		Bundle.BundleEntryComponent observationEntry = createBundleEntry(observation, "urn:uuid:observation");
		
		// Create a processed encounter entry
		Encounter encounter = createEncounter();
		encounter.setId("encounter-uuid");
		Bundle.BundleEntryComponent encounterEntry = createBundleEntry(encounter, "urn:uuid:placeholder");
		processedEntries.put("urn:uuid:placeholder", encounterEntry);
		
		// When
		Bundle.BundleEntryComponent result = ConsultationBundleEntriesHelper.resolveReferences(observationEntry,
		    processedEntries);
		
		// Then
		Observation resultObservation = (Observation) result.getResource();
		assertEquals(FhirConstants.ENCOUNTER + "/encounter-uuid", resultObservation.getEncounter().getReference());
	}
	
	@Test
	public void shouldNotModifyEntryWhenResourceDoesNotHaveEncounter() {
		// Given
		Condition condition = createCondition();
		// Verify condition doesn't have an encounter
		assertFalse(condition.hasEncounter());
		Bundle.BundleEntryComponent conditionEntry = createBundleEntry(condition, "urn:uuid:condition");
		
		// When
		Bundle.BundleEntryComponent result = ConsultationBundleEntriesHelper.resolveReferences(conditionEntry,
		    processedEntries);
		
		// Then
		Condition resultCondition = (Condition) result.getResource();
		// Verify the condition still doesn't have an encounter
		assertFalse(resultCondition.hasEncounter());
	}
	
	@Test(expected = InternalErrorException.class)
	public void shouldThrowExceptionWhenReferencedEntryNotFound() {
		// Given
		Condition condition = createCondition();
		condition.setEncounter(new Reference("urn:uuid:nonexistent"));
		Bundle.BundleEntryComponent conditionEntry = createBundleEntry(condition, "urn:uuid:condition");
		
		// When - should throw exception
		ConsultationBundleEntriesHelper.resolveReferences(conditionEntry, processedEntries);
	}
	
	// Helper methods to create test resources
	
	private Bundle.BundleEntryComponent createBundleEntry(Resource resource, String fullUrl) {
		Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
		entry.setFullUrl(fullUrl);
		entry.setResource(resource);
		entry.setRequest(new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.POST).setUrl(
		    resource.getResourceType().name()));
		return entry;
	}
	
	private Encounter createEncounter() {
		Encounter encounter = new Encounter();
		encounter.setStatus(Encounter.EncounterStatus.INPROGRESS);
		encounter.setSubject(new Reference("Patient/123"));
		return encounter;
	}
	
	private Condition createCondition() {
		Condition condition = new Condition();
		condition.setSubject(new Reference("Patient/123"));
		return condition;
	}
	
	private AllergyIntolerance createAllergyIntolerance() {
		AllergyIntolerance allergyIntolerance = new AllergyIntolerance();
		allergyIntolerance.setPatient(new Reference("Patient/123"));
		return allergyIntolerance;
	}
	
	private ServiceRequest createServiceRequest() {
		ServiceRequest serviceRequest = new ServiceRequest();
		serviceRequest.setSubject(new Reference("Patient/123"));
		return serviceRequest;
	}
	
	private MedicationRequest createMedicationRequest() {
		MedicationRequest medicationRequest = new MedicationRequest();
		medicationRequest.setSubject(new Reference("Patient/123"));
		medicationRequest.setStatus(MedicationRequest.MedicationRequestStatus.ACTIVE);
		medicationRequest.setIntent(MedicationRequest.MedicationRequestIntent.ORDER);
		return medicationRequest;
	}
	
	private Observation createObservation() {
		Observation observation = new Observation();
		observation.setStatus(Observation.ObservationStatus.PRELIMINARY);
		observation.setSubject(new Reference("Patient/123"));
		return observation;
	}
	
	@Test
	public void shouldResolveMemberObservationReferences() {
		// Given
		Observation systolicObs = createObservation();
		systolicObs.setId("systolicObs"); //to assist in debugging
		systolicObs.setEncounter(new Reference("urn:uuid:example-encounter"));
		Bundle.BundleEntryComponent systolicObsEntry = createBundleEntry(systolicObs, "urn:uuid:systolicObs");
		
		Observation diastolicObs = createObservation();
		diastolicObs.setId("diastolicObs"); //to assist in debugging
		diastolicObs.setEncounter(new Reference("urn:uuid:example-encounter"));
		Bundle.BundleEntryComponent diastolicObsEntry = createBundleEntry(diastolicObs, "urn:uuid:diastolicObs");
		
		Observation bpObs = createObservation();
		bpObs.setId("bpObs"); //to assist in debugging
		bpObs.setEncounter(new Reference("urn:uuid:example-encounter"));
		bpObs.addHasMember(new Reference("urn:uuid:systolicObs"));
		bpObs.addHasMember(new Reference("urn:uuid:diastolicObs"));
		Bundle.BundleEntryComponent bpObsEntry = createBundleEntry(bpObs, "urn:uuid:bpObs");
		
		Encounter encounter = createEncounter();
		encounter.setId("example-encounter"); //to assist in debugging
		Bundle.BundleEntryComponent encounterEntry = createBundleEntry(encounter, "urn:uuid:example-encounter");
		
		// Add entries in an order where dependencies are not respected
		entries.add(bpObsEntry);
		entries.add(systolicObsEntry);
		entries.add(diastolicObsEntry);
		entries.add(encounterEntry);
		
		// When
		List<Bundle.BundleEntryComponent> result = ConsultationBundleEntriesHelper.orderEntriesByReference(entries);
		
		// Then
		assertEquals(4, result.size());
		// Encounter should be first since it's referenced by the others
		assertEquals(encounterEntry, result.get(0));
		assertEquals(bpObsEntry, result.get(3));
		
	}
	
	@Test
	public void shouldSortObservationsByDependencies() throws IOException {
		//the following is the order in the json file for observations
		//29b5f5c4-b256-4f8f-809b-f87d8384b5cb
		//49a86246-4004-42eb-9bdc-f542f93f9228
		//60613a43-c4cb-4502-b3e2-cf9215feaa70
		DiagnosticReportBundle reportBundle = TestDataFactory.loadDiagnosticReportBundle("example-diagnostic-report-bundle-with-encounter-reference-nested-results.json");
		List<Observation> observations = reportBundle.getEntry().stream()
				.map(Bundle.BundleEntryComponent::getResource)
				.filter(resource -> resource != null && resource.getResourceType().name().equals("Observation"))
				.map(resource -> (Observation) resource )
				.collect(Collectors.toList());
		List<Observation> list = ConsultationBundleEntriesHelper.sortObservationsByDepth(observations);
		Assert.assertEquals("urn:uuid:49a86246-4004-42eb-9bdc-f542f93f9228", list.get(0).getId());
		Assert.assertEquals("urn:uuid:60613a43-c4cb-4502-b3e2-cf9215feaa70", list.get(1).getId());
		Assert.assertEquals("Observation/29b5f5c4-b256-4f8f-809b-f87d8384b5cb", list.get(2).getId());
	}
}
