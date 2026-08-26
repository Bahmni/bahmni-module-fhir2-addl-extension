package org.bahmni.module.fhir2addlextension.api.validators.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.r4.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class EncounterBundleValidatorImplTest {
	
	private EncounterBundleValidatorImpl validator;
	
	private Bundle validBundle;
	
	@Before
	public void setup() {
		validator = new EncounterBundleValidatorImpl();
		
		// Create a valid bundle with transaction type
		validBundle = new Bundle();
		validBundle.setType(Bundle.BundleType.TRANSACTION);
		
		// Add a valid Encounter entry
		Bundle.BundleEntryComponent encounterEntry = createValidBundleEntry(createEncounter());
		validBundle.addEntry(encounterEntry);
	}
	
	@Test
	public void shouldPassValidationForValidBundleType() {
		// When
		try {
			validator.validateBundleType(validBundle);
		}
		catch (Exception e) {
			fail();
		}
	}
	
	@Test
    public void shouldThrowExceptionWhenBundleTypeIsNotTransaction() {
        // Given
        Bundle invalidBundle = new Bundle();
        invalidBundle.setType(Bundle.BundleType.COLLECTION);

        // When & Then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> validator.validateBundleType(invalidBundle));
        assertEquals("Bundle type must be transaction", exception.getMessage());
    }
	
	@Test
	public void shouldPassValidationForValidBundleEntries() {
		try {
			validator.validateBundleType(validBundle);
		}
		catch (Exception e) {
			fail();
		}
		
	}
	
	@Test
    public void shouldAcceptBundleWithNoEncounterEntryWhenEveryEntryNamesAnExistingEncounter() {
        // Given a bundle with no Encounter entry, whose only entry references an encounter that
        // already exists server-side. This lets a caller attach resources to an existing encounter
        // without re-sending -- and so overwriting -- the encounter itself.
        Bundle bundleWithNoEncounter = new Bundle();
        bundleWithNoEncounter.setType(Bundle.BundleType.TRANSACTION);
        bundleWithNoEncounter.addEntry(createValidBundleEntry(createCondition()));

        // When & Then
        validator.validateBundleEntries(bundleWithNoEncounter);
    }

    @Test
    public void shouldAcceptEncounterlessBundleOfDocumentReferences() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);
        bundle.addEntry(createValidBundleEntry(createDocumentReference("Encounter/456")));
        bundle.addEntry(createValidBundleEntry(createDocumentReference("Encounter/456")));

        validator.validateBundleEntries(bundle);
    }

    @Test
    public void shouldThrowWhenEncounterlessBundleEntryUsesABundleLocalReference() {
        // A "urn:uuid:" reference can only be satisfied by an Encounter entry, which is absent here.
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);
        Bundle.BundleEntryComponent entry = createValidBundleEntry(createDocumentReference("urn:uuid:placeholder"));
        bundle.addEntry(entry);

        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> validator.validateBundleEntries(bundle));
        assertEquals(String.format(
                "Bundle has no Encounter entry, so entry [%s] must reference an existing encounter",
                entry.getFullUrl()), exception.getMessage());
    }

    @Test
    public void shouldThrowWhenEncounterlessBundleEntryHasNoEncounterReferenceAtAll() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);
        Condition conditionWithoutEncounter = new Condition();
        conditionWithoutEncounter.setSubject(new Reference("Patient/123"));
        Bundle.BundleEntryComponent entry = createValidBundleEntry(conditionWithoutEncounter);
        bundle.addEntry(entry);

        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> validator.validateBundleEntries(bundle));
        assertTrue(exception.getMessage().contains("must reference an existing encounter"));
    }
	
	@Test
    public void shouldThrowExceptionWhenBundleHasMultipleEncounterEntries() {
        // Given
        Bundle bundleWithMultipleEncounters = new Bundle();
        bundleWithMultipleEncounters.setType(Bundle.BundleType.TRANSACTION);

        // Add two Encounter entries
        Bundle.BundleEntryComponent encounterEntry1 = createValidBundleEntry(createEncounter());
        Bundle.BundleEntryComponent encounterEntry2 = createValidBundleEntry(createEncounter());
        bundleWithMultipleEncounters.addEntry(encounterEntry1);
        bundleWithMultipleEncounters.addEntry(encounterEntry2);

        // When & Then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> validator.validateBundleEntries(bundleWithMultipleEncounters));
        assertEquals("Encounter bundle should contain at most one Encounter entry. Found 2 instead.",
                exception.getMessage());
    }
	
	@Test
    public void shouldThrowExceptionWhenEntryMissingResource() {
        // Given
        Bundle bundleWithInvalidEntry = new Bundle();
        bundleWithInvalidEntry.setType(Bundle.BundleType.TRANSACTION);

        // Create an entry with missing resource
        Bundle.BundleEntryComponent invalidEntry = new Bundle.BundleEntryComponent();
        invalidEntry.setFullUrl("urn:uuid:test");
        invalidEntry.setRequest(new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.POST).setUrl("Encounter"));
        bundleWithInvalidEntry.addEntry(invalidEntry);

        // When & Then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> validator.validateBundleEntries(bundleWithInvalidEntry));
        assertEquals("Bundle entries must contain fullUrl, resource and request fields",
                exception.getMessage());
    }
	
	@Test
    public void shouldThrowExceptionWhenEntryMissingRequest() {
        // Given
        Bundle bundleWithInvalidEntry = new Bundle();
        bundleWithInvalidEntry.setType(Bundle.BundleType.TRANSACTION);

        // Create an entry with missing request
        Bundle.BundleEntryComponent invalidEntry = new Bundle.BundleEntryComponent();
        invalidEntry.setFullUrl("urn:uuid:test");
        invalidEntry.setResource(createEncounter());
        bundleWithInvalidEntry.addEntry(invalidEntry);

        // When & Then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> validator.validateBundleEntries(bundleWithInvalidEntry));
        assertEquals("Bundle entries must contain fullUrl, resource and request fields",
                exception.getMessage());
    }
	
	@Test
    public void shouldThrowExceptionWhenEntryMissingFullUrl() {
        // Given
        Bundle bundleWithInvalidEntry = new Bundle();
        bundleWithInvalidEntry.setType(Bundle.BundleType.TRANSACTION);

        // Create an entry with missing fullUrl
        Bundle.BundleEntryComponent invalidEntry = new Bundle.BundleEntryComponent();
        invalidEntry.setResource(createEncounter());
        invalidEntry.setRequest(new Bundle.BundleEntryRequestComponent().setMethod(Bundle.HTTPVerb.POST).setUrl("Encounter"));
        bundleWithInvalidEntry.addEntry(invalidEntry);

        // When & Then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> validator.validateBundleEntries(bundleWithInvalidEntry));
        assertEquals("Bundle entries must contain fullUrl, resource and request fields",
                exception.getMessage());
    }
	
	@Test
    public void shouldThrowExceptionWhenResourceTypeIsNotSupported() {
        // Given
        Bundle bundleWithUnsupportedResource = new Bundle();
        bundleWithUnsupportedResource.setType(Bundle.BundleType.TRANSACTION);

        // Add a valid Encounter entry
        Bundle.BundleEntryComponent encounterEntry = createValidBundleEntry(createEncounter());
        bundleWithUnsupportedResource.addEntry(encounterEntry);

        // Add an unsupported resource type (Observation)
        Bundle.BundleEntryComponent immunizationEntry = createValidBundleEntry(createImagingStudy());
        bundleWithUnsupportedResource.addEntry(immunizationEntry);

        // When & Then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> validator.validateBundleEntries(bundleWithUnsupportedResource));
        assertEquals("Entry of resource type ImagingStudy is not supported as part of Encounter Bundle",
                exception.getMessage());
    }
	
	@Test
	public void shouldPassValidationForAllSupportedResourceTypes() {
		// Given
		Bundle bundleWithAllSupportedTypes = new Bundle();
		bundleWithAllSupportedTypes.setType(Bundle.BundleType.TRANSACTION);
		
		// Add one of each supported resource type
		bundleWithAllSupportedTypes.addEntry(createValidBundleEntry(createEncounter()));
		bundleWithAllSupportedTypes.addEntry(createValidBundleEntry(createCondition()));
		bundleWithAllSupportedTypes.addEntry(createValidBundleEntry(createAllergyIntolerance()));
		bundleWithAllSupportedTypes.addEntry(createValidBundleEntry(createMedicationRequest()));
		bundleWithAllSupportedTypes.addEntry(createValidBundleEntry(createImmunization()));
		try {
			validator.validateBundleEntries(bundleWithAllSupportedTypes);
		}
		catch (Exception e) {
			fail();
		}
	}
	
	// Helper methods to create test resources
	
	private Bundle.BundleEntryComponent createValidBundleEntry(org.hl7.fhir.r4.model.Resource resource) {
		Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
		entry.setFullUrl("urn:uuid:" + java.util.UUID.randomUUID().toString());
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
	
	private DocumentReference createDocumentReference(String encounterReference) {
		DocumentReference documentReference = new DocumentReference();
		documentReference.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);
		documentReference.setSubject(new Reference("Patient/123"));
		DocumentReference.DocumentReferenceContextComponent context = new DocumentReference.DocumentReferenceContextComponent();
		context.addEncounter(new Reference(encounterReference));
		documentReference.setContext(context);
		return documentReference;
	}
	
	private Condition createCondition() {
		Condition condition = new Condition();
		condition.setSubject(new Reference("Patient/123"));
		condition.setEncounter(new Reference("Encounter/456"));
		return condition;
	}
	
	private AllergyIntolerance createAllergyIntolerance() {
		AllergyIntolerance allergyIntolerance = new AllergyIntolerance();
		allergyIntolerance.setPatient(new Reference("Patient/123"));
		allergyIntolerance.setEncounter(new Reference("Encounter/456"));
		return allergyIntolerance;
	}
	
	private Observation createObservation() {
		Observation observation = new Observation();
		observation.setStatus(Observation.ObservationStatus.PRELIMINARY);
		observation.setSubject(new Reference("Patient/123"));
		observation.setEncounter(new Reference("Encounter/456"));
		return observation;
	}
	
	private MedicationRequest createMedicationRequest() {
		MedicationRequest medicationRequest = new MedicationRequest();
		medicationRequest.setSubject(new Reference("Patient/123"));
		medicationRequest.setEncounter(new Reference("Encounter/456"));
		return medicationRequest;
	}
	
	private Immunization createImmunization() {
		Immunization immunization = new Immunization();
		immunization.setStatus(Immunization.ImmunizationStatus.COMPLETED);
		immunization.setPatient(new Reference("Patient/123"));
		immunization.setEncounter(new Reference("Encounter/456"));
		return immunization;
	}
	
	private ImagingStudy createImagingStudy() {
		ImagingStudy imagingStudy = new ImagingStudy();
		imagingStudy.setSubject(new Reference("Patient/123"));
		imagingStudy.setEncounter(new Reference("Encounter/456"));
		imagingStudy.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);
		return imagingStudy;
	}
}
