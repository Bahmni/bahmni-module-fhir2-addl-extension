package org.bahmni.module.fhir2AddlExtension.api.helper;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.hl7.fhir.r4.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.fhir2.FhirConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
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
		
		// Create a medication request that references the encounter
		MedicationRequest medicationRequest = createMedicationRequest();
		medicationRequest.setEncounter(new Reference("urn:uuid:encounter"));
		Bundle.BundleEntryComponent medicationRequestEntry = createBundleEntry(medicationRequest, "urn:uuid:medication");
		
		// Add entries in an order where dependencies are not respected
		entries.add(conditionEntry);
		entries.add(medicationRequestEntry);
		entries.add(encounterEntry);
		
		// When
		List<Bundle.BundleEntryComponent> result = ConsultationBundleEntriesHelper.orderEntriesByReference(entries);
		
		// Then
		assertEquals(3, result.size());
		// Encounter should be first since it's referenced by the others
		assertEquals(encounterEntry, result.get(0));
		// The other two can be in any order since they both depend only on the encounter
		assertTrue(result.contains(conditionEntry));
		assertTrue(result.contains(medicationRequestEntry));
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
	public void shouldResolveMedicationRequestEncounterReference() {
		// Given
		MedicationRequest medicationRequest = createMedicationRequest();
		medicationRequest.setEncounter(new Reference("urn:uuid:placeholder"));
		Bundle.BundleEntryComponent medicationEntry = createBundleEntry(medicationRequest, "urn:uuid:medication");
		
		// Create a processed encounter entry
		Encounter encounter = createEncounter();
		encounter.setId("encounter-uuid");
		Bundle.BundleEntryComponent encounterEntry = createBundleEntry(encounter, "urn:uuid:placeholder");
		processedEntries.put("urn:uuid:placeholder", encounterEntry);
		
		// When
		Bundle.BundleEntryComponent result = ConsultationBundleEntriesHelper.resolveReferences(medicationEntry,
		    processedEntries);
		
		// Then
		MedicationRequest resultMedication = (MedicationRequest) result.getResource();
		assertEquals(FhirConstants.ENCOUNTER, resultMedication.getEncounter().getType());
		assertEquals(FhirConstants.ENCOUNTER + "/encounter-uuid", resultMedication.getEncounter().getReference());
	}
	
	@Test
	public void shouldNotModifyEntryWhenResourceTypeIsNotSupported() {
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
		// Reference should not be modified
		assertEquals("urn:uuid:placeholder", resultObservation.getEncounter().getReference());
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
}
