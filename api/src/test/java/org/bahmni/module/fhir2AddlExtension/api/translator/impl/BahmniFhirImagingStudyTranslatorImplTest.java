package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.model.FhirImagingStudy;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirImagingStudyNote;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniFhirImagingStudyTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniServiceRequestReferenceTranslator;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.ImagingStudy;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Location;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.api.db.ContextDAO;
import org.openmrs.module.fhir2.api.translators.LocationReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.bahmni.module.fhir2AddlExtension.api.TestDataFactory.exampleProviderWithUuid;
import static org.bahmni.module.fhir2AddlExtension.api.TestDataFactory.loadResourceFromFile;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirImagingStudyTranslatorImplTest {
	
	private BahmniFhirImagingStudyTranslator translator;
	
	@Mock
	private BahmniServiceRequestReferenceTranslator basedOnReferenceTranslator;
	
	@Mock
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Mock
	private LocationReferenceTranslator locationReferenceTranslator;
	
	@Mock
	private PractitionerReferenceTranslator<Provider> practitionerReferenceTranslator;
	
	@Mock
	private ContextDAO contextDAO;
	
	@Mock
	private UserContext userContext;
	
	@Mock
	private User user;
	
	@Before
	public void setUp() {
		when(userContext.getAuthenticatedUser()).thenReturn(user);
		Context.setDAO(contextDAO);
		Context.openSession();
		Context.setUserContext(userContext);
		
		translator = new BahmniFhirImagingStudyTranslatorImpl(basedOnReferenceTranslator, patientReferenceTranslator,
		        locationReferenceTranslator, practitionerReferenceTranslator);
	}
	
	@Test
	public void toFhirResource_shouldTranslateBasicImagingStudyFields() {
		FhirImagingStudy study = new FhirImagingStudy();
		study.setUuid("test-uuid");
		study.setStudyInstanceUuid("urn:oid:2.16.124.113543.6003.1154777499");
		study.setStatus(FhirImagingStudy.FhirImagingStudyStatus.REGISTERED);
		study.setDescription("Test imaging study");
		study.setDateStarted(new Date());
		
		Patient patient = new Patient();
		patient.setUuid("patient-uuid");
		study.setSubject(patient);
		
		Order order = new Order();
		order.setUuid("order-uuid");
		study.setOrder(order);
		
		Location location = new Location();
		location.setUuid("location-uuid");
		study.setLocation(location);
		
		when(patientReferenceTranslator.toFhirResource(any(Patient.class)))
		        .thenReturn(new Reference("Patient/patient-uuid"));
		when(basedOnReferenceTranslator.toFhirResource(any(Order.class))).thenReturn(
		    new Reference("ServiceRequest/order-uuid"));
		when(locationReferenceTranslator.toFhirResource(any(Location.class))).thenReturn(
		    new Reference("Location/location-uuid"));
		
		ImagingStudy result = translator.toFhirResource(study);
		
		Assert.assertNotNull(result);
		Assert.assertEquals("test-uuid", result.getId());
		Assert.assertEquals(ImagingStudy.ImagingStudyStatus.REGISTERED, result.getStatus());
		Assert.assertEquals("Test imaging study", result.getDescription());
		Assert.assertNotNull(result.getStarted());
		Assert.assertEquals("Patient/patient-uuid", result.getSubject().getReference());
		Assert.assertEquals("ServiceRequest/order-uuid", result.getBasedOnFirstRep().getReference());
		Assert.assertEquals("Location/location-uuid", result.getLocation().getReference());
	}
	
	@Test
	public void toFhirResource_shouldTranslateStudyIdentifier() {
		FhirImagingStudy study = new FhirImagingStudy();
		study.setUuid("test-uuid");
		study.setStudyInstanceUuid("urn:oid:2.16.124.113543.6003.1154777499");
		study.setStatus(FhirImagingStudy.FhirImagingStudyStatus.REGISTERED);
		
		ImagingStudy result = translator.toFhirResource(study);
		
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.getIdentifier().size());
		Assert.assertEquals("urn:dicom:uid", result.getIdentifier().get(0).getSystem());
		Assert.assertEquals("urn:oid:2.16.124.113543.6003.1154777499", result.getIdentifier().get(0).getValue());
	}
	
	@Test
    public void toFhirResource_shouldTranslateNotesToAnnotations() {
        FhirImagingStudy study = new FhirImagingStudy();
        study.setUuid("test-uuid");
        study.setStudyInstanceUuid("urn:oid:2.16.124.113543.6003.1154777499");
        study.setStatus(FhirImagingStudy.FhirImagingStudyStatus.REGISTERED);

        Provider provider = exampleProviderWithUuid("Dr. Smith", "provider-uuid");
        Date noteDate = new Date();

        FhirImagingStudyNote note1 = new FhirImagingStudyNote();
        note1.setUuid("note-1-uuid");
        note1.setNote("First observation from radiologist");
        note1.setPerformer(provider);
        note1.setDateCreated(noteDate);

        FhirImagingStudyNote note2 = new FhirImagingStudyNote();
        note2.setUuid("note-2-uuid");
        note2.setNote("Second observation");
        note2.setPerformer(provider);
        note2.setDateCreated(noteDate);

        Set<FhirImagingStudyNote> notes = new HashSet<>();
        notes.add(note1);
        notes.add(note2);
        study.setNotes(notes);

        when(practitionerReferenceTranslator.toFhirResource(any(Provider.class)))
                .thenReturn(new Reference("Practitioner/provider-uuid"));

        ImagingStudy result = translator.toFhirResource(study);

        Assert.assertNotNull(result);
        Assert.assertTrue(result.hasNote());
        Assert.assertEquals(2, result.getNote().size());

        for (Annotation annotation : result.getNote()) {
            Assert.assertNotNull(annotation.getText());
            Assert.assertNotNull(annotation.getAuthorReference());
            Assert.assertEquals("Practitioner/provider-uuid", annotation.getAuthorReference().getReference());
            Assert.assertNotNull(annotation.getTime());
        }
    }
	
	@Test
    public void toFhirResource_shouldHandleEmptyNotes() {
        FhirImagingStudy study = new FhirImagingStudy();
        study.setUuid("test-uuid");
        study.setStudyInstanceUuid("urn:oid:2.16.124.113543.6003.1154777499");
        study.setStatus(FhirImagingStudy.FhirImagingStudyStatus.REGISTERED);
        study.setNotes(new HashSet<>());

        ImagingStudy result = translator.toFhirResource(study);

        Assert.assertNotNull(result);
        Assert.assertFalse(result.hasNote());
    }
	
	@Test
	public void toFhirResource_shouldHandleNullNotes() {
		FhirImagingStudy study = new FhirImagingStudy();
		study.setUuid("test-uuid");
		study.setStudyInstanceUuid("urn:oid:2.16.124.113543.6003.1154777499");
		study.setStatus(FhirImagingStudy.FhirImagingStudyStatus.REGISTERED);
		study.setNotes(null);
		
		ImagingStudy result = translator.toFhirResource(study);
		
		Assert.assertNotNull(result);
		Assert.assertFalse(result.hasNote());
	}
	
	@Test
	public void toOpenmrsType_shouldTranslateFromImagingStudyResource() throws IOException {
		ImagingStudy resource = (ImagingStudy) loadResourceFromFile("example-imaging-study-registered.json");
		
		Patient patient = new Patient();
		patient.setUuid("example-patient-id");
		
		Order order = new Order();
		order.setUuid("example-order-id");
		
		Location location = new Location();
		location.setUuid("example-radiology-center");
		
		when(patientReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(patient);
		when(basedOnReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(order);
		when(locationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(location);
		
		FhirImagingStudy result = translator.toOpenmrsType(resource);
		
		Assert.assertNotNull(result);
		Assert.assertEquals("urn:oid:2.16.124.113543.6003.1154777499.30246.19789.3503430045", result.getStudyInstanceUuid());
		Assert.assertEquals(FhirImagingStudy.FhirImagingStudyStatus.REGISTERED, result.getStatus());
		Assert.assertEquals("Imaging Study taken for XR Wrist", result.getDescription());
		Assert.assertEquals(patient, result.getSubject());
		Assert.assertEquals(order, result.getOrder());
		Assert.assertEquals(location, result.getLocation());
	}
	
	@Test
	public void toOpenmrsType_shouldTranslateAnnotationsToNotes() throws IOException {
		ImagingStudy resource = (ImagingStudy) loadResourceFromFile("example-imaging-study-with-notes.json");
		
		Provider provider = exampleProviderWithUuid("Dr. Radiologist", "provider-uuid");
		Patient patient = new Patient();
		Order order = new Order();
		Location location = new Location();
		
		when(practitionerReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(provider);
		when(patientReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(patient);
		when(basedOnReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(order);
		when(locationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(location);
		
		FhirImagingStudy result = translator.toOpenmrsType(resource);
		
		Assert.assertNotNull(result);
		Assert.assertNotNull(result.getNotes());
		Assert.assertEquals(2, result.getNotes().size());
		
		for (FhirImagingStudyNote note : result.getNotes()) {
			Assert.assertEquals(result, note.getImagingStudy());
			Assert.assertNotNull(note.getNote());
			Assert.assertNotNull(note.getPerformer());
			Assert.assertEquals(provider, note.getPerformer());
			Assert.assertNotNull(note.getDateCreated());
			Assert.assertNotNull(note.getCreator());
		}
	}
	
	@Test
	public void toOpenmrsType_shouldHandleAnnotationsWithoutAuthor() throws IOException, ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		Date noteDate = dateFormat.parse("2025-12-03T11:01:20+03:00");
		
		ImagingStudy resource = new ImagingStudy();
		resource.setId("test-uuid");
		resource.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);
		
		Annotation annotation = new Annotation();
		annotation.setText("Observation without author");
		annotation.setTime(noteDate);
		resource.addNote(annotation);
		
		FhirImagingStudy result = translator.toOpenmrsType(resource);
		
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.getNotes().size());
		
		FhirImagingStudyNote note = result.getNotes().iterator().next();
		Assert.assertEquals("Observation without author", note.getNote());
		Assert.assertNull(note.getPerformer());
		Assert.assertEquals(noteDate, note.getDateCreated());
	}
	
	@Test
	public void toOpenmrsType_shouldHandleAnnotationsWithoutTime() {
		ImagingStudy resource = new ImagingStudy();
		resource.setId("test-uuid");
		resource.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);
		
		Annotation annotation = new Annotation();
		annotation.setText("Observation without time");
		resource.addNote(annotation);
		
		FhirImagingStudy result = translator.toOpenmrsType(resource);
		
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.getNotes().size());
		
		FhirImagingStudyNote note = result.getNotes().iterator().next();
		Assert.assertEquals("Observation without time", note.getNote());
		Assert.assertNotNull(note.getDateCreated());
	}
	
	@Test
    public void toOpenmrsType_shouldClearExistingNotesWhenUpdating() throws IOException {
        FhirImagingStudy existingStudy = new FhirImagingStudy();
        existingStudy.setImagingStudyId(1);
        existingStudy.setUuid("existing-uuid");

        FhirImagingStudyNote oldNote = new FhirImagingStudyNote();
        oldNote.setNote("Old note that should be cleared");
        Set<FhirImagingStudyNote> oldNotes = new HashSet<>();
        oldNotes.add(oldNote);
        existingStudy.setNotes(oldNotes);

        ImagingStudy resource = new ImagingStudy();
        resource.setId("existing-uuid");
        resource.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);

        Annotation newAnnotation = new Annotation();
        newAnnotation.setText("New updated note");
        newAnnotation.setTime(new Date());
        resource.addNote(newAnnotation);

        FhirImagingStudy result = translator.toOpenmrsType(existingStudy, resource);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.getNotes().size());

        FhirImagingStudyNote note = result.getNotes().iterator().next();
        Assert.assertEquals("New updated note", note.getNote());
    }
	
	@Test
	public void toOpenmrsType_shouldHandleEmptyAnnotations() {
		ImagingStudy resource = new ImagingStudy();
		resource.setId("test-uuid");
		resource.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);
		
		FhirImagingStudy result = translator.toOpenmrsType(resource);
		
		Assert.assertNotNull(result);
		Assert.assertTrue(result.getNotes().isEmpty());
	}
	
	@Test
    public void toOpenmrsType_shouldTranslateMultipleAnnotations() throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

        ImagingStudy resource = new ImagingStudy();
        resource.setId("test-uuid");
        resource.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);

        Provider provider1 = exampleProviderWithUuid("Dr. First", "provider-1-uuid");
        Provider provider2 = exampleProviderWithUuid("Dr. Second", "provider-2-uuid");

        Annotation annotation1 = new Annotation();
        annotation1.setId("note-1");
        annotation1.setText("First radiologist observation");
        annotation1.setAuthor(new Reference("Practitioner/provider-1-uuid"));
        annotation1.setTime(dateFormat.parse("2025-12-03T11:01:20+03:00"));

        Annotation annotation2 = new Annotation();
        annotation2.setId("note-2");
        annotation2.setText("Second radiologist observation");
        annotation2.setAuthor(new Reference("Practitioner/provider-2-uuid"));
        annotation2.setTime(dateFormat.parse("2025-12-03T14:30:00+03:00"));

        Annotation annotation3 = new Annotation();
        annotation3.setId("note-3");
        annotation3.setText("Third observation");
        annotation3.setAuthor(new Reference("Practitioner/provider-1-uuid"));
        annotation3.setTime(dateFormat.parse("2025-12-04T09:15:00+03:00"));

        resource.addNote(annotation1);
        resource.addNote(annotation2);
        resource.addNote(annotation3);

        when(practitionerReferenceTranslator.toOpenmrsType(any(Reference.class))).thenAnswer(invocation -> {
            Reference ref = invocation.getArgument(0);
            if (ref.getReference().contains("provider-1-uuid")) {
                return provider1;
            } else {
                return provider2;
            }
        });

        FhirImagingStudy result = translator.toOpenmrsType(resource);

        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.getNotes().size());

        for (FhirImagingStudyNote note : result.getNotes()) {
            Assert.assertEquals(result, note.getImagingStudy());
            Assert.assertNotNull(note.getNote());
            Assert.assertNotNull(note.getPerformer());
            Assert.assertNotNull(note.getDateCreated());
        }
    }
	
	@Test
	public void toOpenmrsType_shouldHandleExistingNoteScenario() throws ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		
		Provider radiologist = exampleProviderWithUuid("Dr. Radiologist", "radiologist-uuid");
		
		FhirImagingStudy existingStudy = new FhirImagingStudy();
		existingStudy.setImagingStudyId(123);
		existingStudy.setUuid("study-uuid-456");
		existingStudy.setStudyInstanceUuid("1.2.826.0.1.3680043.8.498.1");
		existingStudy.setStatus(FhirImagingStudy.FhirImagingStudyStatus.AVAILABLE);
		
		FhirImagingStudyNote existingNote = new FhirImagingStudyNote();
		existingNote.setImagingStudyNoteId(10);
		existingNote.setUuid("existing-note-uuid");
		existingNote.setNote("Initial scan shows normal findings");
		existingNote.setPerformer(radiologist);
		existingNote.setImagingStudy(existingStudy);
		existingNote.setDateCreated(dateFormat.parse("2025-12-10T09:00:00+00:00"));
		
		Set<FhirImagingStudyNote> existingNotes = new HashSet<>();
		existingNotes.add(existingNote);
		existingStudy.setNotes(existingNotes);
		
		ImagingStudy updatedResource = new ImagingStudy();
		updatedResource.setId("study-uuid-456");
		updatedResource.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
		
		Annotation updatedAnnotation = new Annotation();
		updatedAnnotation.setId("existing-note-uuid");
		updatedAnnotation.setText("Scan reviewed - follow-up scheduled");
		updatedAnnotation.setAuthor(new Reference("Practitioner/radiologist-uuid"));
		updatedAnnotation.setTime(dateFormat.parse("2025-12-10T15:00:00+00:00"));
		updatedResource.addNote(updatedAnnotation);
		
		when(practitionerReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(radiologist);
		
		FhirImagingStudy result = translator.toOpenmrsType(existingStudy, updatedResource);
		
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.getNotes().size());
		
		FhirImagingStudyNote note = result.getNotes().iterator().next();
		Assert.assertEquals("existing-note-uuid", note.getUuid());
		Assert.assertEquals("Scan reviewed - follow-up scheduled", note.getNote());
		Assert.assertEquals(radiologist, note.getPerformer());
		Assert.assertEquals(result, note.getImagingStudy());
	}
	
	@Test
	public void toOpenmrsType_shouldHandleNewNoteScenario() throws ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		
		Provider radiologist = exampleProviderWithUuid("Dr. Radiologist", "radiologist-uuid");
		
		ImagingStudy newResource = new ImagingStudy();
		newResource.setId("new-study-uuid");
		newResource.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);
		
		Annotation newAnnotation = new Annotation();
		newAnnotation.setId("new-note-uuid");
		newAnnotation.setText("Chest X-ray ordered for suspected pneumonia");
		newAnnotation.setAuthor(new Reference("Practitioner/radiologist-uuid"));
		newAnnotation.setTime(dateFormat.parse("2025-12-15T08:30:00+00:00"));
		newResource.addNote(newAnnotation);
		
		when(practitionerReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(radiologist);
		
		FhirImagingStudy result = translator.toOpenmrsType(newResource);
		
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.getNotes().size());
		
		FhirImagingStudyNote note = result.getNotes().iterator().next();
		Assert.assertEquals("new-note-uuid", note.getUuid());
		Assert.assertEquals("Chest X-ray ordered for suspected pneumonia", note.getNote());
		Assert.assertEquals(radiologist, note.getPerformer());
		Assert.assertEquals(result, note.getImagingStudy());
	}
}
