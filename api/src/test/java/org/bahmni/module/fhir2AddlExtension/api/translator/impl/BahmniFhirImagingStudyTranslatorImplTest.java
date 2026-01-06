package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import org.bahmni.module.fhir2AddlExtension.api.model.FhirImagingStudy;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirImagingStudyNote;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniFhirImagingStudyTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniServiceRequestReferenceTranslator;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants.FHIR_EXT_IMAGING_STUDY_COMPLETION_DATE;
import static org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants.FHIR_EXT_IMAGING_STUDY_PERFORMER;
import static org.bahmni.module.fhir2AddlExtension.api.TestDataFactory.exampleProviderWithUuid;
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
	public void toFhirResource_shouldTranslateBasicFields() {
		FhirImagingStudy study = new FhirImagingStudy();
		study.setUuid("study-uuid");
		study.setStudyInstanceUuid("1.2.826.0.1.3680043.8.498.1");
		study.setStatus(FhirImagingStudy.FhirImagingStudyStatus.REGISTERED);
		study.setDescription("CT scan of chest");
		
		Date started = new Date();
		study.setDateStarted(started);
		
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
		Assert.assertEquals("study-uuid", result.getId());
		Assert.assertEquals(ImagingStudy.ImagingStudyStatus.REGISTERED, result.getStatus());
		Assert.assertEquals("CT scan of chest", result.getDescription());
		Assert.assertEquals(started, result.getStarted());
		Assert.assertEquals("Patient/patient-uuid", result.getSubject().getReference());
		Assert.assertEquals("ServiceRequest/order-uuid", result.getBasedOnFirstRep().getReference());
		Assert.assertEquals("Location/location-uuid", result.getLocation().getReference());
	}
	
	@Test
	public void toFhirResource_shouldTranslateDicomIdentifier() {
		FhirImagingStudy study = new FhirImagingStudy();
		study.setUuid("study-uuid");
		study.setStudyInstanceUuid("1.2.826.0.1.3680043.8.498.1767606801000");
		study.setStatus(FhirImagingStudy.FhirImagingStudyStatus.REGISTERED);
		
		ImagingStudy result = translator.toFhirResource(study);
		
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.getIdentifier().size());
		
		Identifier identifier = result.getIdentifier().get(0);
		Assert.assertEquals("urn:dicom:uid", identifier.getSystem());
		Assert.assertEquals("1.2.826.0.1.3680043.8.498.1767606801000", identifier.getValue());
	}
	
	@Test
	public void toFhirResource_shouldTranslatePerformerExtension() {
		FhirImagingStudy study = new FhirImagingStudy();
		study.setUuid("study-uuid");
		study.setStudyInstanceUuid("1.2.826.0.1.3680043.8.498.1");
		study.setStatus(FhirImagingStudy.FhirImagingStudyStatus.AVAILABLE);
		
		Provider performer = exampleProviderWithUuid("Dr. Naman", "provider-uuid");
		study.setPerformer(performer);
		
		when(practitionerReferenceTranslator.toFhirResource(any(Provider.class))).thenReturn(
		    new Reference("Practitioner/provider-uuid"));
		
		ImagingStudy result = translator.toFhirResource(study);
		
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.getExtensionsByUrl(FHIR_EXT_IMAGING_STUDY_PERFORMER).size());
		
		Extension performerExt = result.getExtensionsByUrl(FHIR_EXT_IMAGING_STUDY_PERFORMER).get(0);
		Assert.assertTrue(performerExt.getValue() instanceof Reference);
		Assert.assertEquals("Practitioner/provider-uuid", ((Reference) performerExt.getValue()).getReference());
	}
	
	@Test
	public void toFhirResource_shouldTranslateCompletionDateExtension() {
		FhirImagingStudy study = new FhirImagingStudy();
		study.setUuid("study-uuid");
		study.setStudyInstanceUuid("1.2.826.0.1.3680043.8.498.1");
		study.setStatus(FhirImagingStudy.FhirImagingStudyStatus.AVAILABLE);
		
		Date completionDate = new Date();
		study.setDateCompleted(completionDate);
		
		ImagingStudy result = translator.toFhirResource(study);
		
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.getExtensionsByUrl(FHIR_EXT_IMAGING_STUDY_COMPLETION_DATE).size());
		
		Extension completionExt = result.getExtensionsByUrl(FHIR_EXT_IMAGING_STUDY_COMPLETION_DATE).get(0);
		Assert.assertTrue(completionExt.getValue() instanceof DateTimeType);
		Assert.assertEquals(completionDate, ((DateTimeType) completionExt.getValue()).getValue());
	}
	
	@Test
	public void toFhirResource_shouldNotAddExtensionsWhenFieldsAreNull() {
		FhirImagingStudy study = new FhirImagingStudy();
		study.setUuid("study-uuid");
		study.setStudyInstanceUuid("1.2.826.0.1.3680043.8.498.1");
		study.setStatus(FhirImagingStudy.FhirImagingStudyStatus.REGISTERED);
		study.setPerformer(null);
		study.setDateCompleted(null);
		
		ImagingStudy result = translator.toFhirResource(study);
		
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.getExtensionsByUrl(FHIR_EXT_IMAGING_STUDY_PERFORMER).size());
		Assert.assertEquals(0, result.getExtensionsByUrl(FHIR_EXT_IMAGING_STUDY_COMPLETION_DATE).size());
	}
	
	@Test
	public void toFhirResource_shouldTranslateNotesToAnnotations() {
		FhirImagingStudy study = new FhirImagingStudy();
		study.setUuid("study-uuid");
		study.setStudyInstanceUuid("1.2.826.0.1.3680043.8.498.1");
		study.setStatus(FhirImagingStudy.FhirImagingStudyStatus.AVAILABLE);
		
		Provider provider = exampleProviderWithUuid("Dr. Naman", "provider-uuid");
		Date noteDate = new Date();
		
		FhirImagingStudyNote note1 = new FhirImagingStudyNote();
		note1.setUuid("note-1-uuid");
		note1.setNote("Findings: Normal chest anatomy");
		note1.setPerformer(provider);
		note1.setDateCreated(noteDate);
		
		FhirImagingStudyNote note2 = new FhirImagingStudyNote();
		note2.setUuid("note-2-uuid");
		note2.setNote("Additional observation: No abnormalities");
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
			Assert.assertNotNull(annotation.getId());
			Assert.assertNotNull(annotation.getText());
			Assert.assertTrue(annotation.hasAuthorReference());
			Assert.assertEquals("Practitioner/provider-uuid", annotation.getAuthorReference().getReference());
			Assert.assertEquals(noteDate, annotation.getTime());
		}
	}
	
	@Test
	public void toFhirResource_shouldHandleNotesWithoutPerformer() {
		FhirImagingStudy study = new FhirImagingStudy();
		study.setUuid("study-uuid");
		study.setStudyInstanceUuid("1.2.826.0.1.3680043.8.498.1");
		study.setStatus(FhirImagingStudy.FhirImagingStudyStatus.AVAILABLE);
		
		FhirImagingStudyNote note = new FhirImagingStudyNote();
		note.setUuid("note-uuid");
		note.setNote("Automated system note");
		note.setPerformer(null);
		note.setDateCreated(new Date());
		
		Set<FhirImagingStudyNote> notes = new HashSet<>();
		notes.add(note);
		study.setNotes(notes);
		
		ImagingStudy result = translator.toFhirResource(study);
		
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.getNote().size());
		
		Annotation annotation = result.getNote().get(0);
		Assert.assertEquals("Automated system note", annotation.getText());
		Assert.assertFalse(annotation.hasAuthorReference());
	}
	
	@Test
	public void toFhirResource_shouldHandleEmptyNotesCollection() {
		FhirImagingStudy study = new FhirImagingStudy();
		study.setUuid("study-uuid");
		study.setStudyInstanceUuid("1.2.826.0.1.3680043.8.498.1");
		study.setStatus(FhirImagingStudy.FhirImagingStudyStatus.REGISTERED);
		study.setNotes(new HashSet<>());
		
		ImagingStudy result = translator.toFhirResource(study);
		
		Assert.assertNotNull(result);
		Assert.assertFalse(result.hasNote());
	}
	
	@Test
	public void toFhirResource_shouldHandleNullNotesCollection() {
		FhirImagingStudy study = new FhirImagingStudy();
		study.setUuid("study-uuid");
		study.setStudyInstanceUuid("1.2.826.0.1.3680043.8.498.1");
		study.setStatus(FhirImagingStudy.FhirImagingStudyStatus.REGISTERED);
		study.setNotes(null);
		
		ImagingStudy result = translator.toFhirResource(study);
		
		Assert.assertNotNull(result);
		Assert.assertFalse(result.hasNote());
	}
	
	@Test
	public void toOpenmrsType_shouldTranslateBasicFields() {
		ImagingStudy resource = new ImagingStudy();
		resource.setId("study-uuid");
		resource.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
		resource.setDescription("MRI Brain scan");
		
		Identifier identifier = new Identifier();
		identifier.setSystem("urn:dicom:uid");
		identifier.setValue("1.2.826.0.1.3680043.8.498.9876543");
		resource.addIdentifier(identifier);
		
		Date started = new Date();
		resource.setStarted(started);
		
		Patient patient = new Patient();
		patient.setUuid("patient-uuid");
		
		Order order = new Order();
		order.setUuid("order-uuid");
		
		Location location = new Location();
		location.setUuid("location-uuid");
		
		when(patientReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(patient);
		when(basedOnReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(order);
		when(locationReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(location);
		
		resource.setSubject(new Reference("Patient/patient-uuid"));
		resource.addBasedOn(new Reference("ServiceRequest/order-uuid"));
		resource.setLocation(new Reference("Location/location-uuid"));
		
		FhirImagingStudy result = translator.toOpenmrsType(resource);
		
		Assert.assertNotNull(result);
		Assert.assertEquals("1.2.826.0.1.3680043.8.498.9876543", result.getStudyInstanceUuid());
		Assert.assertEquals(FhirImagingStudy.FhirImagingStudyStatus.AVAILABLE, result.getStatus());
		Assert.assertEquals("MRI Brain scan", result.getDescription());
		Assert.assertEquals(started, result.getDateStarted());
		Assert.assertEquals(patient, result.getSubject());
		Assert.assertEquals(order, result.getOrder());
		Assert.assertEquals(location, result.getLocation());
		Assert.assertNotNull(result.getCreator());
		Assert.assertNotNull(result.getDateCreated());
	}
	
	@Test
	public void toOpenmrsType_shouldExtractDicomIdentifier() {
		ImagingStudy resource = new ImagingStudy();
		resource.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);
		
		Identifier dicomId = new Identifier();
		dicomId.setSystem("urn:dicom:uid");
		dicomId.setValue("1.2.826.0.1.3680043.8.498.12345");
		resource.addIdentifier(dicomId);
		
		Identifier otherId = new Identifier();
		otherId.setSystem("http://example.org/ids");
		otherId.setValue("some-other-id");
		resource.addIdentifier(otherId);
		
		FhirImagingStudy result = translator.toOpenmrsType(resource);
		
		Assert.assertNotNull(result);
		Assert.assertEquals("1.2.826.0.1.3680043.8.498.12345", result.getStudyInstanceUuid());
	}
	
	@Test
	public void toOpenmrsType_shouldTranslateAllStatusTypes() {
		for (ImagingStudy.ImagingStudyStatus fhirStatus : ImagingStudy.ImagingStudyStatus.values()) {
			if (fhirStatus == ImagingStudy.ImagingStudyStatus.NULL) {
				continue;
			}
			
			ImagingStudy resource = new ImagingStudy();
			resource.setStatus(fhirStatus);
			
			FhirImagingStudy result = translator.toOpenmrsType(resource);
			
			Assert.assertNotNull(result);
			Assert.assertEquals(FhirImagingStudy.FhirImagingStudyStatus.valueOf(fhirStatus.name()), result.getStatus());
		}
	}
	
	@Test
	public void toOpenmrsType_shouldTranslatePerformerExtension() {
		ImagingStudy resource = new ImagingStudy();
		resource.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
		
		Provider provider = exampleProviderWithUuid("Dr. Smith", "provider-uuid");
		
		Extension performerExt = new Extension();
		performerExt.setUrl(FHIR_EXT_IMAGING_STUDY_PERFORMER);
		performerExt.setValue(new Reference("Practitioner/provider-uuid"));
		resource.addExtension(performerExt);
		
		when(practitionerReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(provider);
		
		FhirImagingStudy result = translator.toOpenmrsType(resource);
		
		Assert.assertNotNull(result);
		Assert.assertNotNull(result.getPerformer());
		Assert.assertEquals(provider, result.getPerformer());
	}
	
	@Test
	public void toOpenmrsType_shouldTranslateCompletionDateExtension() throws ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		Date completionDate = dateFormat.parse("2025-12-15T14:30:00+00:00");
		
		ImagingStudy resource = new ImagingStudy();
		resource.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
		
		Extension completionExt = new Extension();
		completionExt.setUrl(FHIR_EXT_IMAGING_STUDY_COMPLETION_DATE);
		completionExt.setValue(new DateTimeType(completionDate));
		resource.addExtension(completionExt);
		
		FhirImagingStudy result = translator.toOpenmrsType(resource);
		
		Assert.assertNotNull(result);
		Assert.assertNotNull(result.getDateCompleted());
		Assert.assertEquals(completionDate, result.getDateCompleted());
	}
	
	@Test
	public void toOpenmrsType_shouldTranslateAnnotationsToNotes() throws ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		
		ImagingStudy resource = new ImagingStudy();
		resource.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
		
		Provider provider = exampleProviderWithUuid("Dr. Radiologist", "provider-uuid");
		
		Annotation annotation1 = new Annotation();
		annotation1.setId("note-1");
		annotation1.setText("Initial findings normal");
		annotation1.setAuthor(new Reference("Practitioner/provider-uuid"));
		annotation1.setTime(dateFormat.parse("2025-12-10T10:00:00+00:00"));
		
		Annotation annotation2 = new Annotation();
		annotation2.setId("note-2");
		annotation2.setText("Follow-up scan recommended");
		annotation2.setAuthor(new Reference("Practitioner/provider-uuid"));
		annotation2.setTime(dateFormat.parse("2025-12-10T11:00:00+00:00"));
		
		resource.addNote(annotation1);
		resource.addNote(annotation2);
		
		when(practitionerReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(provider);
		
		FhirImagingStudy result = translator.toOpenmrsType(resource);
		
		Assert.assertNotNull(result);
		Assert.assertNotNull(result.getNotes());
		Assert.assertEquals(2, result.getNotes().size());
		
		for (FhirImagingStudyNote note : result.getNotes()) {
			Assert.assertNotNull(note.getUuid());
			Assert.assertNotNull(note.getNote());
			Assert.assertEquals(result, note.getImagingStudy());
			Assert.assertEquals(provider, note.getPerformer());
			Assert.assertNotNull(note.getDateCreated());
			Assert.assertNotNull(note.getCreator());
		}
	}
	
	@Test
	public void toOpenmrsType_shouldAddNewNotesToExistingStudy() throws ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		
		FhirImagingStudy existingStudy = new FhirImagingStudy();
		existingStudy.setImagingStudyId(1);
		existingStudy.setUuid("existing-uuid");
		existingStudy.setStudyInstanceUuid("1.2.826.0.1.3680043.8.498.1");
		existingStudy.setStatus(FhirImagingStudy.FhirImagingStudyStatus.AVAILABLE);
		
		FhirImagingStudyNote existingNote = new FhirImagingStudyNote();
		existingNote.setImagingStudyNoteId(1);
		existingNote.setUuid("existing-note-uuid");
		existingNote.setNote("Existing note");
		existingNote.setImagingStudy(existingStudy);
		
		Set<FhirImagingStudyNote> existingNotes = new HashSet<>();
		existingNotes.add(existingNote);
		existingStudy.setNotes(existingNotes);
		
		ImagingStudy resource = new ImagingStudy();
		resource.setId("existing-uuid");
		resource.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
		
		Annotation existingAnnotation = new Annotation();
		existingAnnotation.setId("existing-note-uuid");
		existingAnnotation.setText("Existing note");
		existingAnnotation.setTime(dateFormat.parse("2025-12-10T10:00:00+00:00"));
		
		Annotation newAnnotation = new Annotation();
		newAnnotation.setId("new-note-uuid");
		newAnnotation.setText("Newly added note");
		newAnnotation.setTime(dateFormat.parse("2025-12-10T12:00:00+00:00"));
		
		resource.addNote(existingAnnotation);
		resource.addNote(newAnnotation);
		
		FhirImagingStudy result = translator.toOpenmrsType(existingStudy, resource);
		
		Assert.assertNotNull(result);
		Assert.assertEquals(2, result.getNotes().size());
		
		boolean foundExisting = false;
		boolean foundNew = false;
		
		for (FhirImagingStudyNote note : result.getNotes()) {
			if ("existing-note-uuid".equals(note.getUuid())) {
				foundExisting = true;
				Assert.assertEquals("Existing note", note.getNote());
			} else if ("new-note-uuid".equals(note.getUuid())) {
				foundNew = true;
				Assert.assertEquals("Newly added note", note.getNote());
			}
		}
		
		Assert.assertTrue(foundExisting);
		Assert.assertTrue(foundNew);
	}
	
	@Test
	public void toOpenmrsType_shouldUpdateExistingNoteWhenUuidMatches() throws ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		
		Provider oldProvider = exampleProviderWithUuid("Dr. Old", "old-provider-uuid");
		Provider newProvider = exampleProviderWithUuid("Dr. New", "new-provider-uuid");
		
		FhirImagingStudy existingStudy = new FhirImagingStudy();
		existingStudy.setImagingStudyId(1);
		existingStudy.setUuid("study-uuid");
		existingStudy.setStatus(FhirImagingStudy.FhirImagingStudyStatus.AVAILABLE);
		
		FhirImagingStudyNote existingNote = new FhirImagingStudyNote();
		existingNote.setImagingStudyNoteId(1);
		existingNote.setUuid("note-to-update");
		existingNote.setNote("Original note text");
		existingNote.setPerformer(oldProvider);
		existingNote.setImagingStudy(existingStudy);
		
		Set<FhirImagingStudyNote> notes = new HashSet<>();
		notes.add(existingNote);
		existingStudy.setNotes(notes);
		
		ImagingStudy resource = new ImagingStudy();
		resource.setStatus(ImagingStudy.ImagingStudyStatus.AVAILABLE);
		
		Annotation updatedAnnotation = new Annotation();
		updatedAnnotation.setId("note-to-update");
		updatedAnnotation.setText("Updated note text");
		updatedAnnotation.setAuthor(new Reference("Practitioner/new-provider-uuid"));
		updatedAnnotation.setTime(dateFormat.parse("2025-12-11T15:00:00+00:00"));
		resource.addNote(updatedAnnotation);
		
		when(practitionerReferenceTranslator.toOpenmrsType(any(Reference.class))).thenReturn(newProvider);
		
		FhirImagingStudy result = translator.toOpenmrsType(existingStudy, resource);
		
		Assert.assertNotNull(result);
		Assert.assertEquals(1, result.getNotes().size());
		
		FhirImagingStudyNote updatedNote = result.getNotes().iterator().next();
		Assert.assertEquals("note-to-update", updatedNote.getUuid());
		Assert.assertEquals("Updated note text", updatedNote.getNote());
		Assert.assertEquals(newProvider, updatedNote.getPerformer());
		Assert.assertNotNull(updatedNote.getChangedBy());
		Assert.assertNotNull(updatedNote.getDateChanged());
	}
	
	@Test
	public void toOpenmrsType_shouldHandleEmptyAnnotations() {
		ImagingStudy resource = new ImagingStudy();
		resource.setStatus(ImagingStudy.ImagingStudyStatus.REGISTERED);
		
		FhirImagingStudy result = translator.toOpenmrsType(resource);
		
		Assert.assertNotNull(result);
		Assert.assertTrue(result.getNotes().isEmpty());
	}
}
