package org.bahmni.module.fhir2addlextension.api.helper;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.bahmni.module.fhir2addlextension.api.TestDataFactory;
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
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class EncounterBundleEntriesHelperTest {
	
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
		List<Bundle.BundleEntryComponent> result = EncounterBundleEntriesHelper.orderEntriesByReference(null);
		
		// Then
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}
	
	@Test
	public void shouldReturnSameListWhenEntriesIsEmpty() {
		// When
		List<Bundle.BundleEntryComponent> result = EncounterBundleEntriesHelper.orderEntriesByReference(entries);
		
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
		List<Bundle.BundleEntryComponent> result = EncounterBundleEntriesHelper.orderEntriesByReference(entries);
		
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
		List<Bundle.BundleEntryComponent> result = EncounterBundleEntriesHelper.orderEntriesByReference(entries);
		
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
		List<Bundle.BundleEntryComponent> result = EncounterBundleEntriesHelper.orderEntriesByReference(entries);
		
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
		List<Bundle.BundleEntryComponent> result = EncounterBundleEntriesHelper.orderEntriesByReference(entries);
		
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
		List<Bundle.BundleEntryComponent> result = EncounterBundleEntriesHelper.orderEntriesByReference(entries);
		
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
		List<Bundle.BundleEntryComponent> result = EncounterBundleEntriesHelper.orderEntriesByReference(entries);
		
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
		Bundle.BundleEntryComponent result = EncounterBundleEntriesHelper
		        .resolveReferences(conditionEntry, processedEntries);
		
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
		Bundle.BundleEntryComponent result = EncounterBundleEntriesHelper.resolveReferences(allergyEntry, processedEntries);
		
		// Then
		AllergyIntolerance resultAllergy = (AllergyIntolerance) result.getResource();
		assertEquals(FhirConstants.ENCOUNTER, resultAllergy.getEncounter().getType());
		assertEquals(FhirConstants.ENCOUNTER + "/encounter-uuid", resultAllergy.getEncounter().getReference());
	}
	
	@Test
	public void shouldResolveConcreteEncounterReferenceInPlaceWhenBundleHasNoEncounterEntry() {
		// Given a DocumentReference naming an encounter that already exists server-side, and no
		// Encounter entry in the bundle to resolve against.
		DocumentReference documentReference = new DocumentReference();
		DocumentReference.DocumentReferenceContextComponent context = new DocumentReference.DocumentReferenceContextComponent();
		context.addEncounter(new Reference("Encounter/existing-uuid"));
		documentReference.setContext(context);
		Bundle.BundleEntryComponent documentEntry = createBundleEntry(documentReference, "urn:uuid:document");
		
		// When
		Bundle.BundleEntryComponent result = EncounterBundleEntriesHelper.resolveReferences(documentEntry,
		    processedEntries);
		
		// Then the reference survives intact rather than failing to resolve
		DocumentReference resultDocument = (DocumentReference) result.getResource();
		assertEquals(FhirConstants.ENCOUNTER + "/existing-uuid", resultDocument.getContext().getEncounterFirstRep()
		        .getReference());
	}
	
	@Test
	public void shouldStillFailForAnUnresolvedBundleLocalReference() {
		// A "urn:uuid:" placeholder that names no processed entry is a bundle integrity error and must
		// not be mistaken for a concrete server-side reference.
		DocumentReference documentReference = new DocumentReference();
		DocumentReference.DocumentReferenceContextComponent context = new DocumentReference.DocumentReferenceContextComponent();
		context.addEncounter(new Reference("urn:uuid:never-processed"));
		documentReference.setContext(context);
		Bundle.BundleEntryComponent documentEntry = createBundleEntry(documentReference, "urn:uuid:document");
		
		assertThrows(InternalErrorException.class,
		    () -> EncounterBundleEntriesHelper.resolveReferences(documentEntry, processedEntries));
	}
	
	@Test
	public void shouldNotDoublePrefixWhenAProcessedEncounterCarriesAQualifiedId() {
		DocumentReference documentReference = new DocumentReference();
		DocumentReference.DocumentReferenceContextComponent context = new DocumentReference.DocumentReferenceContextComponent();
		context.addEncounter(new Reference("urn:uuid:placeholder"));
		documentReference.setContext(context);
		Bundle.BundleEntryComponent documentEntry = createBundleEntry(documentReference, "urn:uuid:document");
		
		Encounter encounter = createEncounter();
		encounter.setId("Encounter/encounter-uuid");
		processedEntries.put("urn:uuid:placeholder", createBundleEntry(encounter, "urn:uuid:placeholder"));
		
		Bundle.BundleEntryComponent result = EncounterBundleEntriesHelper.resolveReferences(documentEntry,
		    processedEntries);
		
		DocumentReference resultDocument = (DocumentReference) result.getResource();
		assertEquals(FhirConstants.ENCOUNTER + "/encounter-uuid", resultDocument.getContext().getEncounterFirstRep()
		        .getReference());
	}
	
	@Test
	public void shouldResolveDocumentReferenceContextEncounterReference() {
		// Given
		DocumentReference documentReference = new DocumentReference();
		DocumentReference.DocumentReferenceContextComponent context = new DocumentReference.DocumentReferenceContextComponent();
		context.addEncounter(new Reference("urn:uuid:placeholder"));
		documentReference.setContext(context);
		Bundle.BundleEntryComponent documentEntry = createBundleEntry(documentReference, "urn:uuid:document");
		
		// Create a processed encounter entry
		Encounter encounter = createEncounter();
		encounter.setId("encounter-uuid");
		Bundle.BundleEntryComponent encounterEntry = createBundleEntry(encounter, "urn:uuid:placeholder");
		processedEntries.put("urn:uuid:placeholder", encounterEntry);
		
		// When
		Bundle.BundleEntryComponent result = EncounterBundleEntriesHelper.resolveReferences(documentEntry, processedEntries);
		
		// Then
		DocumentReference resultDocument = (DocumentReference) result.getResource();
		assertEquals(FhirConstants.ENCOUNTER, resultDocument.getContext().getEncounterFirstRep().getType());
		assertEquals(FhirConstants.ENCOUNTER + "/encounter-uuid", resultDocument.getContext().getEncounterFirstRep()
		        .getReference());
	}
	
	@Test
	public void shouldResolveAllDocumentReferenceContextEncounterReferences() {
		// Given a DocumentReference whose context references more than one encounter
		DocumentReference documentReference = new DocumentReference();
		DocumentReference.DocumentReferenceContextComponent context = new DocumentReference.DocumentReferenceContextComponent();
		context.addEncounter(new Reference("urn:uuid:placeholder-1"));
		context.addEncounter(new Reference("urn:uuid:placeholder-2"));
		documentReference.setContext(context);
		Bundle.BundleEntryComponent documentEntry = createBundleEntry(documentReference, "urn:uuid:document");
		
		Encounter encounterOne = createEncounter();
		encounterOne.setId("encounter-uuid-1");
		processedEntries.put("urn:uuid:placeholder-1", createBundleEntry(encounterOne, "urn:uuid:placeholder-1"));
		Encounter encounterTwo = createEncounter();
		encounterTwo.setId("encounter-uuid-2");
		processedEntries.put("urn:uuid:placeholder-2", createBundleEntry(encounterTwo, "urn:uuid:placeholder-2"));
		
		// When
		Bundle.BundleEntryComponent result = EncounterBundleEntriesHelper.resolveReferences(documentEntry, processedEntries);
		
		// Then every encounter reference is resolved, not just the first
		DocumentReference resultDocument = (DocumentReference) result.getResource();
		assertEquals(2, resultDocument.getContext().getEncounter().size());
		assertEquals(FhirConstants.ENCOUNTER + "/encounter-uuid-1", resultDocument.getContext().getEncounter().get(0)
		        .getReference());
		assertEquals(FhirConstants.ENCOUNTER + "/encounter-uuid-2", resultDocument.getContext().getEncounter().get(1)
		        .getReference());
	}
	
	@Test
	public void shouldOrderEncounterBeforeDocumentReferenceThatReferencesIt() {
		// Given
		DocumentReference documentReference = new DocumentReference();
		DocumentReference.DocumentReferenceContextComponent context = new DocumentReference.DocumentReferenceContextComponent();
		context.addEncounter(new Reference("urn:uuid:example-encounter"));
		documentReference.setContext(context);
		Bundle.BundleEntryComponent documentEntry = createBundleEntry(documentReference, "urn:uuid:document");
		
		Encounter encounter = createEncounter();
		encounter.setId("example-encounter");
		Bundle.BundleEntryComponent encounterEntry = createBundleEntry(encounter, "urn:uuid:example-encounter");
		
		// Add entries in an order where the dependency is not respected
		entries.add(documentEntry);
		entries.add(encounterEntry);
		
		// When
		List<Bundle.BundleEntryComponent> result = EncounterBundleEntriesHelper.orderEntriesByReference(entries);
		
		// Then
		assertEquals(2, result.size());
		// Encounter must come first since the DocumentReference references it via context.encounter
		assertEquals(encounterEntry, result.get(0));
		assertEquals(documentEntry, result.get(1));
	}
	
	@Test
	public void shouldLeaveDocumentReferenceUnchangedWhenItHasNoEncounterContext() {
		// No context at all -> hasContext() == false
		DocumentReference noContext = new DocumentReference();
		Bundle.BundleEntryComponent noContextEntry = createBundleEntry(noContext, "urn:uuid:doc-no-context");
		Bundle.BundleEntryComponent resultNoContext = EncounterBundleEntriesHelper.resolveReferences(noContextEntry,
		    processedEntries);
		assertFalse(((DocumentReference) resultNoContext.getResource()).hasContext());
		
		// Context present (non-empty) but no encounter -> hasContext() == true, hasEncounter() == false
		DocumentReference contextWithoutEncounter = new DocumentReference();
		DocumentReference.DocumentReferenceContextComponent context = new DocumentReference.DocumentReferenceContextComponent();
		context.addRelated(new Reference("Patient/related"));
		contextWithoutEncounter.setContext(context);
		Bundle.BundleEntryComponent contextWithoutEncounterEntry = createBundleEntry(contextWithoutEncounter,
		    "urn:uuid:doc-context-without-encounter");
		Bundle.BundleEntryComponent resultContextWithoutEncounter = EncounterBundleEntriesHelper.resolveReferences(
		    contextWithoutEncounterEntry, processedEntries);
		assertFalse(((DocumentReference) resultContextWithoutEncounter.getResource()).getContext().hasEncounter());
	}
	
	@Test
	public void shouldNotAddDependencyForDocumentReferenceWithoutEncounterContext() {
		// hasContext() == false
		DocumentReference noContext = new DocumentReference();
		Bundle.BundleEntryComponent noContextEntry = createBundleEntry(noContext, "urn:uuid:doc-no-context");
		// hasContext() == true (non-empty), hasEncounter() == false
		DocumentReference contextWithoutEncounter = new DocumentReference();
		DocumentReference.DocumentReferenceContextComponent context = new DocumentReference.DocumentReferenceContextComponent();
		context.addRelated(new Reference("Patient/related"));
		contextWithoutEncounter.setContext(context);
		Bundle.BundleEntryComponent emptyContextEntry = createBundleEntry(contextWithoutEncounter,
		    "urn:uuid:doc-context-without-encounter");
		
		entries.add(noContextEntry);
		entries.add(emptyContextEntry);
		
		List<Bundle.BundleEntryComponent> result = EncounterBundleEntriesHelper.orderEntriesByReference(entries);
		
		assertEquals(2, result.size());
		assertTrue(result.contains(noContextEntry));
		assertTrue(result.contains(emptyContextEntry));
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
		Bundle.BundleEntryComponent result = EncounterBundleEntriesHelper.resolveReferences(serviceRequestEntry,
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
		Bundle.BundleEntryComponent result = EncounterBundleEntriesHelper.resolveReferences(medicationRequestEntry,
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
		Bundle.BundleEntryComponent result = EncounterBundleEntriesHelper.resolveReferences(observationEntry,
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
		Bundle.BundleEntryComponent result = EncounterBundleEntriesHelper
		        .resolveReferences(conditionEntry, processedEntries);
		
		// Then
		Condition resultCondition = (Condition) result.getResource();
		// Verify the condition still doesn't have an encounter
		assertFalse(resultCondition.hasEncounter());
	}
	
	@Test
	public void shouldResolveImmunizationEncounterReference() {
		Immunization immunization = createImmunization();
		immunization.setEncounter(new Reference("urn:uuid:placeholder"));
		Bundle.BundleEntryComponent immunizationEntry = createBundleEntry(immunization, "urn:uuid:immunization");
		
		Encounter encounter = createEncounter();
		encounter.setId("encounter-uuid");
		Bundle.BundleEntryComponent encounterEntry = createBundleEntry(encounter, "urn:uuid:placeholder");
		processedEntries.put("urn:uuid:placeholder", encounterEntry);
		
		Bundle.BundleEntryComponent result = EncounterBundleEntriesHelper.resolveReferences(immunizationEntry,
		    processedEntries);
		
		Immunization resultImmunization = (Immunization) result.getResource();
		assertEquals(FhirConstants.ENCOUNTER, resultImmunization.getEncounter().getType());
		assertEquals(FhirConstants.ENCOUNTER + "/encounter-uuid", resultImmunization.getEncounter().getReference());
	}
	
	@Test
	public void shouldNotModifyImmunizationEntryWhenNoEncounterIsPresent() {
		Immunization immunization = createImmunization();
		assertFalse(immunization.hasEncounter());
		Bundle.BundleEntryComponent immunizationEntry = createBundleEntry(immunization, "urn:uuid:immunization");
		
		Bundle.BundleEntryComponent result = EncounterBundleEntriesHelper.resolveReferences(immunizationEntry,
		    processedEntries);
		
		Immunization resultImmunization = (Immunization) result.getResource();
		assertFalse(resultImmunization.hasEncounter());
	}
	
	@Test
	public void shouldOrderEntriesWithImmunizationDependency() {
		
		Bundle.BundleEntryComponent encounterEntry = createBundleEntry(createEncounter(), "urn:uuid:encounter");
		
		Immunization immunization = createImmunization();
		immunization.setEncounter(new Reference("urn:uuid:encounter"));
		Bundle.BundleEntryComponent immunizationEntry = createBundleEntry(immunization, "urn:uuid:immunization");
		
		entries.add(immunizationEntry);
		entries.add(encounterEntry);
		
		List<Bundle.BundleEntryComponent> result = EncounterBundleEntriesHelper.orderEntriesByReference(entries);
		
		assertEquals(2, result.size());
		assertEquals(encounterEntry, result.get(0));
		assertEquals(immunizationEntry, result.get(1));
	}
	
	// ──────────────────────────────────────────────────────────────────────────────
	// Tests for hasMember null-resource handling (DELETE obs in same bundle)
	// ──────────────────────────────────────────────────────────────────────────────
	
	@Test
	public void shouldSkipHasMemberReferenceWhenProcessedEntryResourceIsNull() {
		// Simulates a DELETE obs result — entry exists but resource is null
		Bundle.BundleEntryComponent deletedObsEntry = new Bundle.BundleEntryComponent();
		deletedObsEntry.setResource(null);
		deletedObsEntry.setRequest(new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.DELETE).setUrl(
		    "Observation/deleted-obs-uuid"));
		processedEntries.put("urn:uuid:deleted-obs", deletedObsEntry);
		
		Observation parentObs = createObservation();
		parentObs.setEncounter(new Reference("urn:uuid:encounter"));
		parentObs.addHasMember(new Reference("urn:uuid:deleted-obs"));
		Bundle.BundleEntryComponent parentEntry = createBundleEntry(parentObs, "urn:uuid:parent");
		
		Encounter encounter = createEncounter();
		encounter.setId("encounter-uuid");
		processedEntries.put("urn:uuid:encounter", createBundleEntry(encounter, "urn:uuid:encounter"));
		
		Bundle.BundleEntryComponent result = EncounterBundleEntriesHelper.resolveReferences(parentEntry, processedEntries);
		
		Observation resultObs = (Observation) result.getResource();
		assertTrue("hasMember should be empty after deleted ref is skipped", resultObs.getHasMember().isEmpty());
	}
	
	@Test(expected = InternalErrorException.class)
	public void shouldThrowExceptionWhenHasMemberReferenceIsAbsentFromProcessedEntries() {
		// hasMember references an obs whose entry is not in processedEntries at all —
		// this is a bundle integrity bug and must throw, consistent with other resource types.
		Observation parentObs = createObservation();
		parentObs.setEncounter(new Reference("urn:uuid:encounter"));
		parentObs.addHasMember(new Reference("urn:uuid:not-in-processed-map"));
		Bundle.BundleEntryComponent parentEntry = createBundleEntry(parentObs, "urn:uuid:parent");
		
		Encounter encounter = createEncounter();
		encounter.setId("encounter-uuid");
		processedEntries.put("urn:uuid:encounter", createBundleEntry(encounter, "urn:uuid:encounter"));
		
		EncounterBundleEntriesHelper.resolveReferences(parentEntry, processedEntries);
	}
	
	@Test
	public void shouldResolveOnlyValidHasMemberReferencesAndSkipDeleted() {
		// Mix: one valid POST/PUT child, one deleted (null resource)
		Observation validChild = createObservation();
		validChild.setId("valid-child-uuid");
		Bundle.BundleEntryComponent validChildEntry = createBundleEntry(validChild, "urn:uuid:valid-child");
		processedEntries.put("urn:uuid:valid-child", validChildEntry);
		
		Bundle.BundleEntryComponent deletedChildEntry = new Bundle.BundleEntryComponent();
		deletedChildEntry.setResource(null);
		deletedChildEntry.setRequest(new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.DELETE).setUrl(
		    "Observation/deleted-child-uuid"));
		processedEntries.put("urn:uuid:deleted-child", deletedChildEntry);
		
		Observation parentObs = createObservation();
		parentObs.setEncounter(new Reference("urn:uuid:encounter"));
		parentObs.addHasMember(new Reference("urn:uuid:valid-child"));
		parentObs.addHasMember(new Reference("urn:uuid:deleted-child"));
		Bundle.BundleEntryComponent parentEntry = createBundleEntry(parentObs, "urn:uuid:parent");
		
		Encounter encounter = createEncounter();
		encounter.setId("encounter-uuid");
		processedEntries.put("urn:uuid:encounter", createBundleEntry(encounter, "urn:uuid:encounter"));
		
		Bundle.BundleEntryComponent result = EncounterBundleEntriesHelper.resolveReferences(parentEntry, processedEntries);
		
		Observation resultObs = (Observation) result.getResource();
		assertEquals("Only the valid hasMember reference should remain", 1, resultObs.getHasMember().size());
		assertEquals(FhirConstants.OBSERVATION + "/valid-child-uuid", resultObs.getHasMember().get(0).getReference());
	}
	
	@Test
	public void shouldResolveAllHasMemberReferencesWhenAllAreValid() {
		Observation child1 = createObservation();
		child1.setId("child-uuid-1");
		processedEntries.put("urn:uuid:child1", createBundleEntry(child1, "urn:uuid:child1"));
		
		Observation child2 = createObservation();
		child2.setId("child-uuid-2");
		processedEntries.put("urn:uuid:child2", createBundleEntry(child2, "urn:uuid:child2"));
		
		Observation parentObs = createObservation();
		parentObs.setEncounter(new Reference("urn:uuid:encounter"));
		parentObs.addHasMember(new Reference("urn:uuid:child1"));
		parentObs.addHasMember(new Reference("urn:uuid:child2"));
		Bundle.BundleEntryComponent parentEntry = createBundleEntry(parentObs, "urn:uuid:parent");
		
		Encounter encounter = createEncounter();
		encounter.setId("encounter-uuid");
		processedEntries.put("urn:uuid:encounter", createBundleEntry(encounter, "urn:uuid:encounter"));
		
		Bundle.BundleEntryComponent result = EncounterBundleEntriesHelper.resolveReferences(parentEntry, processedEntries);
		
		Observation resultObs = (Observation) result.getResource();
		assertEquals(2, resultObs.getHasMember().size());
		assertEquals(FhirConstants.OBSERVATION + "/child-uuid-1", resultObs.getHasMember().get(0).getReference());
		assertEquals(FhirConstants.OBSERVATION + "/child-uuid-2", resultObs.getHasMember().get(1).getReference());
	}
	
	@Test
	public void shouldSetEmptyHasMemberWhenAllReferencedChildrenAreDeleted() {
		Bundle.BundleEntryComponent deletedChild1 = new Bundle.BundleEntryComponent();
		deletedChild1.setResource(null);
		deletedChild1.setRequest(new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.DELETE).setUrl(
		    "Observation/deleted1-uuid"));
		processedEntries.put("urn:uuid:deleted1", deletedChild1);
		
		Bundle.BundleEntryComponent deletedChild2 = new Bundle.BundleEntryComponent();
		deletedChild2.setResource(null);
		deletedChild2.setRequest(new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.DELETE).setUrl(
		    "Observation/deleted2-uuid"));
		processedEntries.put("urn:uuid:deleted2", deletedChild2);
		
		Observation parentObs = createObservation();
		parentObs.setEncounter(new Reference("urn:uuid:encounter"));
		parentObs.addHasMember(new Reference("urn:uuid:deleted1"));
		parentObs.addHasMember(new Reference("urn:uuid:deleted2"));
		Bundle.BundleEntryComponent parentEntry = createBundleEntry(parentObs, "urn:uuid:parent");
		
		Encounter encounter = createEncounter();
		encounter.setId("encounter-uuid");
		processedEntries.put("urn:uuid:encounter", createBundleEntry(encounter, "urn:uuid:encounter"));
		
		Bundle.BundleEntryComponent result = EncounterBundleEntriesHelper.resolveReferences(parentEntry, processedEntries);
		
		Observation resultObs = (Observation) result.getResource();
		assertTrue("hasMember should be empty when all referenced children are deleted", resultObs.getHasMember().isEmpty());
	}
	
	@Test(expected = InternalErrorException.class)
	public void shouldThrowWhenHasMemberEntryHasNullResourceButIsNotADeleteEntry() {
		// A processed entry whose resource is null but whose HTTP verb is NOT DELETE
		// is an unexpected state — the code must fail loudly, not silently drop the member.
		Bundle.BundleEntryComponent nonDeleteNullEntry = new Bundle.BundleEntryComponent();
		nonDeleteNullEntry.setResource(null);
		nonDeleteNullEntry.setRequest(new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.POST).setUrl(
		    "Observation"));
		processedEntries.put("urn:uuid:bad-entry", nonDeleteNullEntry);
		
		Observation parentObs = createObservation();
		parentObs.setEncounter(new Reference("urn:uuid:encounter"));
		parentObs.addHasMember(new Reference("urn:uuid:bad-entry"));
		Bundle.BundleEntryComponent parentEntry = createBundleEntry(parentObs, "urn:uuid:parent");
		
		Encounter encounter = createEncounter();
		encounter.setId("encounter-uuid");
		processedEntries.put("urn:uuid:encounter", createBundleEntry(encounter, "urn:uuid:encounter"));
		
		EncounterBundleEntriesHelper.resolveReferences(parentEntry, processedEntries);
	}
	
	@Test
	public void shouldExtractObservationUuidCorrectlyWhenResourceIdContainsResourceTypePrefix() {
		// Verifies BahmniFhirUtils.extractId() is used: if resource.getId() returns
		// "Observation/<uuid>" the final hasMember reference must not double-prefix to
		// "Observation/Observation/<uuid>".
		Observation child = createObservation();
		child.setId("Observation/child-obs-uuid"); // simulates getId() returning a prefixed value
		Bundle.BundleEntryComponent childEntry = createBundleEntry(child, "urn:uuid:child");
		processedEntries.put("urn:uuid:child", childEntry);
		
		Observation parentObs = createObservation();
		parentObs.setEncounter(new Reference("urn:uuid:encounter"));
		parentObs.addHasMember(new Reference("urn:uuid:child"));
		Bundle.BundleEntryComponent parentEntry = createBundleEntry(parentObs, "urn:uuid:parent");
		
		Encounter encounter = createEncounter();
		encounter.setId("encounter-uuid");
		processedEntries.put("urn:uuid:encounter", createBundleEntry(encounter, "urn:uuid:encounter"));
		
		Bundle.BundleEntryComponent result = EncounterBundleEntriesHelper.resolveReferences(parentEntry, processedEntries);
		
		Observation resultObs = (Observation) result.getResource();
		assertEquals(1, resultObs.getHasMember().size());
		// Must be "Observation/child-obs-uuid", NOT "Observation/Observation/child-obs-uuid"
		assertEquals(FhirConstants.OBSERVATION + "/child-obs-uuid", resultObs.getHasMember().get(0).getReference());
	}
	
	// ── Integration scenarios: BAH-4793 hasMember reference resolution ───────────
	// These two tests verify the two distinct null-check cases that were previously
	// folded into a single silent-skip.  Keeping them separate makes the contract explicit:
	//   • absent entry (processedEntry == null)  → InternalErrorException
	//   • DELETE result (resource == null)        → silent skip
	
	/**
	 * BAH-4793 fix #2 (NPE guard): when a parent obs POST references a child that was DELETE'd in
	 * the same bundle, the processed entry exists but has a null resource. resolveReferences must
	 * silently skip it — the hasMember list shrinks to only the surviving (non-deleted) children.
	 */
	@Test
	public void shouldSkipDeletedChildAndKeepRemainingHasMemberReferences() {
		// Valid child — POST result
		Observation validChild = createObservation();
		validChild.setId("surviving-child-uuid");
		processedEntries.put("urn:uuid:valid-child", createBundleEntry(validChild, "urn:uuid:valid-child"));
		
		// Deleted child — DELETE result produces a null resource entry
		Bundle.BundleEntryComponent deletedEntry = new Bundle.BundleEntryComponent();
		deletedEntry.setResource(null);
		deletedEntry.setRequest(new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.DELETE).setUrl(
		    "Observation/deleted-child-uuid"));
		processedEntries.put("urn:uuid:deleted-child", deletedEntry);
		
		Observation parentObs = createObservation();
		parentObs.setEncounter(new Reference("urn:uuid:encounter"));
		parentObs.addHasMember(new Reference("urn:uuid:valid-child"));
		parentObs.addHasMember(new Reference("urn:uuid:deleted-child"));
		Bundle.BundleEntryComponent parentEntry = createBundleEntry(parentObs, "urn:uuid:parent");
		
		Encounter encounter = createEncounter();
		encounter.setId("encounter-uuid");
		processedEntries.put("urn:uuid:encounter", createBundleEntry(encounter, "urn:uuid:encounter"));
		
		Bundle.BundleEntryComponent result = EncounterBundleEntriesHelper.resolveReferences(parentEntry, processedEntries);
		
		Observation resultObs = (Observation) result.getResource();
		assertEquals("Only the surviving child reference should remain", 1, resultObs.getHasMember().size());
		assertEquals(FhirConstants.OBSERVATION + "/surviving-child-uuid", resultObs.getHasMember().get(0).getReference());
	}
	
	/**
	 * BAH-4793 fix #2 (bundle integrity): when a hasMember reference points to an entry that is
	 * completely absent from processedEntries (not a DELETE, but a genuinely missing entry),
	 * resolveReferences must throw InternalErrorException — consistent with how all other resource
	 * types handle missing references via getIdForPlaceHolderReference.
	 */
	@Test(expected = InternalErrorException.class)
	public void shouldThrowWhenHasMemberReferencesAnEntryAbsentFromProcessedEntries() {
		Observation parentObs = createObservation();
		parentObs.setEncounter(new Reference("urn:uuid:encounter"));
		parentObs.addHasMember(new Reference("urn:uuid:missing-from-bundle"));
		Bundle.BundleEntryComponent parentEntry = createBundleEntry(parentObs, "urn:uuid:parent");
		
		Encounter encounter = createEncounter();
		encounter.setId("encounter-uuid");
		processedEntries.put("urn:uuid:encounter", createBundleEntry(encounter, "urn:uuid:encounter"));
		
		// "urn:uuid:missing-from-bundle" is not in processedEntries at all — must throw
		EncounterBundleEntriesHelper.resolveReferences(parentEntry, processedEntries);
	}
	
	// ──────────────────────────────────────────────────────────────────────────────
	
	@Test(expected = InternalErrorException.class)
	public void shouldThrowExceptionWhenReferencedEntryNotFound() {
		// Given
		Condition condition = createCondition();
		condition.setEncounter(new Reference("urn:uuid:nonexistent"));
		Bundle.BundleEntryComponent conditionEntry = createBundleEntry(condition, "urn:uuid:condition");
		
		// When - should throw exception
		EncounterBundleEntriesHelper.resolveReferences(conditionEntry, processedEntries);
	}
	
	@Test
	public void shouldReturnDeleteEntryUnchangedWithoutResolvingReferences() {
		// Given - DELETE entry whose encounter reference is NOT in processedEntries
		// (a POST entry with the same missing reference would throw InternalErrorException)
		AllergyIntolerance allergyIntolerance = createAllergyIntolerance();
		allergyIntolerance.setEncounter(new Reference("urn:uuid:nonexistent-encounter"));
		Bundle.BundleEntryComponent deleteEntry = new Bundle.BundleEntryComponent();
		deleteEntry.setFullUrl("urn:uuid:allergy");
		deleteEntry.setResource(allergyIntolerance);
		deleteEntry.setRequest(new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.DELETE).setUrl(
		    "AllergyIntolerance/some-uuid"));
		
		// When - should NOT throw even though the reference cannot be resolved
		Bundle.BundleEntryComponent result = EncounterBundleEntriesHelper.resolveReferences(deleteEntry, processedEntries);
		
		// Then - same entry returned unchanged
		assertSame(deleteEntry, result);
		assertEquals("urn:uuid:nonexistent-encounter", ((AllergyIntolerance) result.getResource()).getEncounter()
		        .getReference());
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
	
	private Immunization createImmunization() {
		Immunization immunization = new Immunization();
		immunization.setStatus(Immunization.ImmunizationStatus.COMPLETED);
		immunization.setPatient(new Reference("Patient/123"));
		return immunization;
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
		List<Bundle.BundleEntryComponent> result = EncounterBundleEntriesHelper.orderEntriesByReference(entries);
		
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
		Bundle reportBundle = TestDataFactory.loadDiagnosticReportBundle("example-diagnostic-report-bundle-with-encounter-reference-nested-results.json");
		List<Observation> observations = reportBundle.getEntry().stream()
				.map(Bundle.BundleEntryComponent::getResource)
				.filter(resource -> resource != null && resource.getResourceType().name().equals("Observation"))
				.map(resource -> (Observation) resource )
				.collect(Collectors.toList());
		List<Observation> list = EncounterBundleEntriesHelper.sortObservationsByDepth(observations);
		Assert.assertEquals("urn:uuid:49a86246-4004-42eb-9bdc-f542f93f9228", list.get(0).getId());
		Assert.assertEquals("urn:uuid:60613a43-c4cb-4502-b3e2-cf9215feaa70", list.get(1).getId());
		Assert.assertEquals("Observation/29b5f5c4-b256-4f8f-809b-f87d8384b5cb", list.get(2).getId());
	}
}
