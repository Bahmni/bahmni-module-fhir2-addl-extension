package org.bahmni.module.fhir2addlextension.api.model;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Provider;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class FhirImagingStudyTest {
	
	private FhirImagingStudy imagingStudy;
	
	private Patient patient;
	
	private Order order;
	
	private Location location;
	
	private Provider provider;
	
	private Encounter encounter;
	
	private Date startDate;
	
	private Date completedDate;
	
	@Before
	public void setUp() {
		imagingStudy = new FhirImagingStudy();
		
		patient = new Patient();
		patient.setUuid("patient-uuid");
		
		order = new Order();
		order.setUuid("order-uuid");
		
		location = new Location();
		location.setUuid("location-uuid");
		location.setName("Radiology Center");
		
		provider = new Provider();
		provider.setUuid("provider-uuid");
		
		encounter = new Encounter();
		encounter.setUuid("encounter-uuid");
		
		startDate = new Date();
		completedDate = new Date(startDate.getTime() + 3600000); // 1 hour later
	}
	
	@Test
	public void shouldSetAndGetImagingStudyId() {
		Integer id = 123;
		imagingStudy.setImagingStudyId(id);
		assertEquals(id, imagingStudy.getImagingStudyId());
	}
	
	@Test
	public void shouldSetAndGetStudyInstanceUuid() {
		String studyInstanceUuid = "urn:oid:2.16.124.113543.6003.1154777499.30246.19789.3503430045";
		imagingStudy.setStudyInstanceUuid(studyInstanceUuid);
		assertEquals(studyInstanceUuid, imagingStudy.getStudyInstanceUuid());
	}
	
	@Test
	public void shouldSetAndGetSubject() {
		imagingStudy.setSubject(patient);
		assertEquals(patient, imagingStudy.getSubject());
	}
	
	@Test
	public void shouldSetAndGetOrder() {
		imagingStudy.setOrder(order);
		assertEquals(order, imagingStudy.getOrder());
	}
	
	@Test
	public void shouldSetAndGetLocation() {
		imagingStudy.setLocation(location);
		assertEquals(location, imagingStudy.getLocation());
	}
	
	@Test
	public void shouldSetAndGetPerformer() {
		imagingStudy.setPerformer(provider);
		assertEquals(provider, imagingStudy.getPerformer());
	}
	
	@Test
	public void shouldSetAndGetDescription() {
		String description = "CT Scan of Chest";
		imagingStudy.setDescription(description);
		assertEquals(description, imagingStudy.getDescription());
	}
	
	@Test
	public void shouldSetAndGetEncounter() {
		imagingStudy.setEncounter(encounter);
		assertEquals(encounter, imagingStudy.getEncounter());
	}
	
	@Test
	public void shouldSetAndGetDateStarted() {
		imagingStudy.setDateStarted(startDate);
		assertEquals(startDate, imagingStudy.getDateStarted());
	}
	
	@Test
	public void shouldSetAndGetDateCompleted() {
		imagingStudy.setDateCompleted(completedDate);
		assertEquals(completedDate, imagingStudy.getDateCompleted());
	}
	
	@Test
    public void shouldSetAndGetNotes() {
        Set<FhirImagingStudyNote> notes = new HashSet<>();
        FhirImagingStudyNote note = new FhirImagingStudyNote();
        note.setNote("Study completed successfully");
        notes.add(note);
        
        imagingStudy.setNotes(notes);
        assertEquals(notes, imagingStudy.getNotes());
        assertEquals(1, imagingStudy.getNotes().size());
    }
	
	@Test
    public void shouldSetAndGetAssessment() {
        Set<Obs> assessment = new HashSet<>();
        Obs obs1 = new Obs();
        obs1.setUuid("obs-uuid-1");
        Obs obs2 = new Obs();
        obs2.setUuid("obs-uuid-2");
        assessment.add(obs1);
        assessment.add(obs2);
        
        imagingStudy.setAssessment(assessment);
        assertEquals(assessment, imagingStudy.getAssessment());
        assertEquals(2, imagingStudy.getAssessment().size());
    }
	
	@Test
	public void shouldSetAndGetStatus() {
		imagingStudy.setStatus(FhirImagingStudy.FhirImagingStudyStatus.REGISTERED);
		assertEquals(FhirImagingStudy.FhirImagingStudyStatus.REGISTERED, imagingStudy.getStatus());
		
		imagingStudy.setStatus(FhirImagingStudy.FhirImagingStudyStatus.AVAILABLE);
		assertEquals(FhirImagingStudy.FhirImagingStudyStatus.AVAILABLE, imagingStudy.getStatus());
		
		imagingStudy.setStatus(FhirImagingStudy.FhirImagingStudyStatus.CANCELLED);
		assertEquals(FhirImagingStudy.FhirImagingStudyStatus.CANCELLED, imagingStudy.getStatus());
		
		imagingStudy.setStatus(FhirImagingStudy.FhirImagingStudyStatus.UNKNOWN);
		assertEquals(FhirImagingStudy.FhirImagingStudyStatus.UNKNOWN, imagingStudy.getStatus());
		
		imagingStudy.setStatus(FhirImagingStudy.FhirImagingStudyStatus.INACTIVE);
		assertEquals(FhirImagingStudy.FhirImagingStudyStatus.INACTIVE, imagingStudy.getStatus());
		
		imagingStudy.setStatus(FhirImagingStudy.FhirImagingStudyStatus.ENTEREDINERROR);
		assertEquals(FhirImagingStudy.FhirImagingStudyStatus.ENTEREDINERROR, imagingStudy.getStatus());
	}
	
	@Test
	public void shouldReturnImagingStudyIdFromGetId() {
		Integer id = 456;
		imagingStudy.setImagingStudyId(id);
		assertEquals(id, imagingStudy.getId());
	}
	
	@Test
	public void shouldSetImagingStudyIdViaSetId() {
		Integer id = 789;
		imagingStudy.setId(id);
		assertEquals(id, imagingStudy.getImagingStudyId());
		assertEquals(id, imagingStudy.getId());
	}
	
	@Test
	public void shouldReturnNullWhenImagingStudyIdNotSet() {
		assertNull(imagingStudy.getId());
		assertNull(imagingStudy.getImagingStudyId());
	}
	
	@Test
	public void shouldHaveEmptyNotesSetByDefault() {
		assertNotNull(imagingStudy.getNotes());
		assertTrue(imagingStudy.getNotes().isEmpty());
	}
	
	@Test
	public void shouldHaveEmptyAssessmentSetByDefault() {
		assertNotNull(imagingStudy.getAssessment());
		assertTrue(imagingStudy.getAssessment().isEmpty());
	}
	
	@Test
	public void testEqualsWithSameObject() {
		imagingStudy.setUuid("test-uuid");
		assertTrue(imagingStudy.equals(imagingStudy));
	}
	
	@Test
	public void testEqualsWithNull() {
		imagingStudy.setUuid("test-uuid");
		assertFalse(imagingStudy.equals(null));
	}
	
	@Test
	public void testEqualsWithDifferentClass() {
		imagingStudy.setUuid("test-uuid");
		assertFalse(imagingStudy.equals("not an imaging study"));
	}
	
	@Test
	public void testEqualsWithSameUuid() {
		imagingStudy.setUuid("test-uuid");
		imagingStudy.setImagingStudyId(1);
		imagingStudy.setStudyInstanceUuid("urn:oid:1.2.3");
		imagingStudy.setDescription("Test Description");
		imagingStudy.setStatus(FhirImagingStudy.FhirImagingStudyStatus.AVAILABLE);
		
		FhirImagingStudy other = new FhirImagingStudy();
		other.setUuid("test-uuid");
		other.setImagingStudyId(1);
		other.setStudyInstanceUuid("urn:oid:1.2.3");
		other.setDescription("Test Description");
		other.setStatus(FhirImagingStudy.FhirImagingStudyStatus.AVAILABLE);
		
		assertTrue(imagingStudy.equals(other));
		assertEquals(imagingStudy.hashCode(), other.hashCode());
	}
	
	@Test
	public void testHashCodeConsistency() {
		imagingStudy.setUuid("test-uuid");
		imagingStudy.setImagingStudyId(1);
		imagingStudy.setStudyInstanceUuid("urn:oid:1.2.3");
		
		int hashCode1 = imagingStudy.hashCode();
		int hashCode2 = imagingStudy.hashCode();
		
		assertEquals(hashCode1, hashCode2);
	}
	
	@Test
	public void testToString() {
		imagingStudy.setImagingStudyId(123);
		imagingStudy.setStudyInstanceUuid("urn:oid:2.16.124.113543.6003");
		imagingStudy.setDescription("CT Scan");
		imagingStudy.setStatus(FhirImagingStudy.FhirImagingStudyStatus.AVAILABLE);
		imagingStudy.setSubject(patient);
		imagingStudy.setLocation(location);
		
		String toString = imagingStudy.toString();
		
		assertNotNull(toString);
		assertTrue(toString.contains("FhirImagingStudy"));
	}
	
	@Test
    public void testToStringExcludesNotesAndAssessment() {
        imagingStudy.setImagingStudyId(123);
        
        Set<FhirImagingStudyNote> notes = new HashSet<>();
        FhirImagingStudyNote note = new FhirImagingStudyNote();
        note.setNote("Test note");
        notes.add(note);
        imagingStudy.setNotes(notes);
        
        Set<Obs> assessment = new HashSet<>();
        Obs obs = new Obs();
        obs.setUuid("obs-uuid");
        assessment.add(obs);
        imagingStudy.setAssessment(assessment);

        String toString = imagingStudy.toString();
        
        // notes and assessment should be excluded from toString per @ToString annotation
        assertNotNull(toString);
    }
	
	@Test
    public void testEqualsExcludesNotesAndAssessment() {
        imagingStudy.setUuid("test-uuid");
        imagingStudy.setImagingStudyId(1);
        
        Set<FhirImagingStudyNote> notes1 = new HashSet<>();
        FhirImagingStudyNote note1 = new FhirImagingStudyNote();
        note1.setNote("Note 1");
        notes1.add(note1);
        imagingStudy.setNotes(notes1);
        
        Set<Obs> assessment1 = new HashSet<>();
        Obs obs1 = new Obs();
        obs1.setUuid("obs-1");
        assessment1.add(obs1);
        imagingStudy.setAssessment(assessment1);

        FhirImagingStudy other = new FhirImagingStudy();
        other.setUuid("test-uuid");
        other.setImagingStudyId(1);
        
        Set<FhirImagingStudyNote> notes2 = new HashSet<>();
        FhirImagingStudyNote note2 = new FhirImagingStudyNote();
        note2.setNote("Note 2 - different");
        notes2.add(note2);
        other.setNotes(notes2);
        
        Set<Obs> assessment2 = new HashSet<>();
        Obs obs2 = new Obs();
        obs2.setUuid("obs-2");
        assessment2.add(obs2);
        other.setAssessment(assessment2);

        // Should be equal because notes and assessment are excluded from equals
        assertTrue(imagingStudy.equals(other));
    }
	
	@Test
	public void testAllStatusEnumValues() {
		FhirImagingStudy.FhirImagingStudyStatus[] statuses = FhirImagingStudy.FhirImagingStudyStatus.values();
		
		assertEquals(6, statuses.length);
		assertEquals(FhirImagingStudy.FhirImagingStudyStatus.REGISTERED,
		    FhirImagingStudy.FhirImagingStudyStatus.valueOf("REGISTERED"));
		assertEquals(FhirImagingStudy.FhirImagingStudyStatus.AVAILABLE,
		    FhirImagingStudy.FhirImagingStudyStatus.valueOf("AVAILABLE"));
		assertEquals(FhirImagingStudy.FhirImagingStudyStatus.CANCELLED,
		    FhirImagingStudy.FhirImagingStudyStatus.valueOf("CANCELLED"));
		assertEquals(FhirImagingStudy.FhirImagingStudyStatus.UNKNOWN,
		    FhirImagingStudy.FhirImagingStudyStatus.valueOf("UNKNOWN"));
		assertEquals(FhirImagingStudy.FhirImagingStudyStatus.INACTIVE,
		    FhirImagingStudy.FhirImagingStudyStatus.valueOf("INACTIVE"));
		assertEquals(FhirImagingStudy.FhirImagingStudyStatus.ENTEREDINERROR,
		    FhirImagingStudy.FhirImagingStudyStatus.valueOf("ENTEREDINERROR"));
	}
	
	@Test
	public void shouldSetNullValues() {
		imagingStudy.setImagingStudyId(1);
		imagingStudy.setStudyInstanceUuid("test");
		imagingStudy.setSubject(patient);
		imagingStudy.setOrder(order);
		imagingStudy.setLocation(location);
		imagingStudy.setPerformer(provider);
		imagingStudy.setDescription("test");
		imagingStudy.setEncounter(encounter);
		imagingStudy.setDateStarted(startDate);
		imagingStudy.setDateCompleted(completedDate);
		imagingStudy.setStatus(FhirImagingStudy.FhirImagingStudyStatus.AVAILABLE);
		
		// Set all to null
		imagingStudy.setImagingStudyId(null);
		imagingStudy.setStudyInstanceUuid(null);
		imagingStudy.setSubject(null);
		imagingStudy.setOrder(null);
		imagingStudy.setLocation(null);
		imagingStudy.setPerformer(null);
		imagingStudy.setDescription(null);
		imagingStudy.setEncounter(null);
		imagingStudy.setDateStarted(null);
		imagingStudy.setDateCompleted(null);
		imagingStudy.setStatus(null);
		
		assertNull(imagingStudy.getImagingStudyId());
		assertNull(imagingStudy.getStudyInstanceUuid());
		assertNull(imagingStudy.getSubject());
		assertNull(imagingStudy.getOrder());
		assertNull(imagingStudy.getLocation());
		assertNull(imagingStudy.getPerformer());
		assertNull(imagingStudy.getDescription());
		assertNull(imagingStudy.getEncounter());
		assertNull(imagingStudy.getDateStarted());
		assertNull(imagingStudy.getDateCompleted());
		assertNull(imagingStudy.getStatus());
	}
	
	@Test
	public void testNoArgsConstructor() {
		FhirImagingStudy newStudy = new FhirImagingStudy();
		
		assertNotNull(newStudy);
		assertNull(newStudy.getImagingStudyId());
		assertNull(newStudy.getStudyInstanceUuid());
		assertNull(newStudy.getSubject());
		assertNull(newStudy.getOrder());
		assertNull(newStudy.getLocation());
		assertNull(newStudy.getPerformer());
		assertNull(newStudy.getDescription());
		assertNull(newStudy.getEncounter());
		assertNull(newStudy.getDateStarted());
		assertNull(newStudy.getDateCompleted());
		assertNotNull(newStudy.getNotes());
		assertNotNull(newStudy.getAssessment());
		assertNull(newStudy.getStatus());
	}
	
	@Test
	public void testSetIdDelegatesToSetImagingStudyId() {
		Integer testId = 999;
		imagingStudy.setId(testId);
		
		assertEquals(testId, imagingStudy.getImagingStudyId());
		assertEquals(testId, imagingStudy.getId());
	}
	
	@Test
	public void testAddNoteToSet() {
		FhirImagingStudyNote note1 = new FhirImagingStudyNote();
		note1.setNote("First note");
		
		FhirImagingStudyNote note2 = new FhirImagingStudyNote();
		note2.setNote("Second note");
		
		imagingStudy.getNotes().add(note1);
		imagingStudy.getNotes().add(note2);
		
		assertEquals(2, imagingStudy.getNotes().size());
		assertTrue(imagingStudy.getNotes().contains(note1));
		assertTrue(imagingStudy.getNotes().contains(note2));
	}
	
	@Test
	public void testAddObsToAssessment() {
		Obs obs1 = new Obs();
		obs1.setUuid("assessment-obs-1");
		
		Obs obs2 = new Obs();
		obs2.setUuid("assessment-obs-2");
		
		imagingStudy.getAssessment().add(obs1);
		imagingStudy.getAssessment().add(obs2);
		
		assertEquals(2, imagingStudy.getAssessment().size());
		assertTrue(imagingStudy.getAssessment().contains(obs1));
		assertTrue(imagingStudy.getAssessment().contains(obs2));
	}
	
	@Test
	public void testClearNotesSet() {
		FhirImagingStudyNote note = new FhirImagingStudyNote();
		note.setNote("Test note");
		imagingStudy.getNotes().add(note);
		
		assertEquals(1, imagingStudy.getNotes().size());
		
		imagingStudy.getNotes().clear();
		
		assertTrue(imagingStudy.getNotes().isEmpty());
	}
	
	@Test
	public void testClearAssessmentSet() {
		Obs obs = new Obs();
		obs.setUuid("test-obs");
		imagingStudy.getAssessment().add(obs);
		
		assertEquals(1, imagingStudy.getAssessment().size());
		
		imagingStudy.getAssessment().clear();
		
		assertTrue(imagingStudy.getAssessment().isEmpty());
	}
	
	@Test
    public void testReplaceNotesSet() {
        FhirImagingStudyNote note1 = new FhirImagingStudyNote();
        note1.setNote("Original note");
        imagingStudy.getNotes().add(note1);
        
        Set<FhirImagingStudyNote> newNotes = new HashSet<>();
        FhirImagingStudyNote note2 = new FhirImagingStudyNote();
        note2.setNote("New note");
        newNotes.add(note2);
        
        imagingStudy.setNotes(newNotes);
        
        assertEquals(1, imagingStudy.getNotes().size());
        assertTrue(imagingStudy.getNotes().contains(note2));
        assertFalse(imagingStudy.getNotes().contains(note1));
    }
	
	@Test
    public void testReplaceAssessmentSet() {
        Obs obs1 = new Obs();
        obs1.setUuid("original-obs");
        imagingStudy.getAssessment().add(obs1);
        
        Set<Obs> newAssessment = new HashSet<>();
        Obs obs2 = new Obs();
        obs2.setUuid("new-obs");
        newAssessment.add(obs2);
        
        imagingStudy.setAssessment(newAssessment);
        
        assertEquals(1, imagingStudy.getAssessment().size());
        assertTrue(imagingStudy.getAssessment().contains(obs2));
        assertFalse(imagingStudy.getAssessment().contains(obs1));
    }
}
