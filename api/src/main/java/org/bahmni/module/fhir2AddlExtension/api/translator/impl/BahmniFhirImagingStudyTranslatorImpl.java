package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
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
import org.hl7.fhir.r4.model.Type;
import org.openmrs.Location;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.api.translators.LocationReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants.FHIR_EXT_IMAGING_STUDY_COMPLETION_DATE;
import static org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants.FHIR_EXT_IMAGING_STUDY_PERFORMER;

@Component
@Slf4j
public class BahmniFhirImagingStudyTranslatorImpl implements BahmniFhirImagingStudyTranslator {
	
	public static final String SYSTEM_DICOM_UID = "urn:dicom:uid";
	
	public static final String ERR_INVALID_LOCATION_REFERENCE = "Invalid location reference for imaging study";
	
	public static final String INVALID_PERFORMER_FOR_IMAGING_STUDY = "Invalid performer for Imaging Study";
	
	private final BahmniServiceRequestReferenceTranslator basedOnReferenceTranslator;
	
	private final PatientReferenceTranslator patientReferenceTranslator;
	
	private final LocationReferenceTranslator locationReferenceTranslator;
	
	private final PractitionerReferenceTranslator<Provider> practitionerReferenceTranslator;
	
	@Autowired
	public BahmniFhirImagingStudyTranslatorImpl(BahmniServiceRequestReferenceTranslator basedOnReferenceTranslator,
	    PatientReferenceTranslator patientReferenceTranslator, LocationReferenceTranslator locationReferenceTranslator,
	    PractitionerReferenceTranslator<Provider> practitionerReferenceTranslator) {
		this.basedOnReferenceTranslator = basedOnReferenceTranslator;
		this.patientReferenceTranslator = patientReferenceTranslator;
		this.locationReferenceTranslator = locationReferenceTranslator;
		this.practitionerReferenceTranslator = practitionerReferenceTranslator;
	}
	
	@Override
    public ImagingStudy toFhirResource(@Nonnull FhirImagingStudy study) {
        ImagingStudy resource = new ImagingStudy();
        resource.setId(study.getUuid());
        resource.addIdentifier(getStudyIdentifier(study));
        resource.setStatus(tFhirStatusType(study.getStatus()));
        resource.addBasedOn(basedOnReferenceTranslator.toFhirResource(study.getOrder()));
        resource.setSubject(patientReferenceTranslator.toFhirResource(study.getSubject()));
        resource.setLocation(locationReferenceTranslator.toFhirResource(study.getLocation()));
        resource.setDescription(study.getDescription());
        resource.setStarted(study.getDateStarted());
        if (study.getNotes() != null && !study.getNotes().isEmpty()) {
            resource.setNote(
                study.getNotes().stream().map(note -> {
                    Annotation annotation = new Annotation();
                    annotation.setId(note.getUuid());
                    if (note.getPerformer() != null) {
                        annotation.setAuthor(practitionerReferenceTranslator.toFhirResource(note.getPerformer()));
                    }
                    annotation.setTime(note.getDateCreated());
                    annotation.setText(note.getNote());
                    return annotation;
                }).collect(Collectors.toList()));
        }
        Optional.ofNullable(practitionerReferenceTranslator.toFhirResource(study.getPerformer()))
                .ifPresent(reference -> resource.addExtension(FHIR_EXT_IMAGING_STUDY_PERFORMER, reference));
        if (study.getDateCompleted() != null) {
            resource.addExtension(FHIR_EXT_IMAGING_STUDY_COMPLETION_DATE, new DateTimeType(study.getDateCompleted()));
        }
        return resource;
    }
	
	private Identifier getStudyIdentifier(FhirImagingStudy study) {
		String studyInstanceUuid = study.getStudyInstanceUuid();
		Identifier dicomStudyIdentifier = new Identifier();
		dicomStudyIdentifier.setSystem(SYSTEM_DICOM_UID);
		dicomStudyIdentifier.setValue(studyInstanceUuid);
		return dicomStudyIdentifier;
	}
	
	@Override
	public FhirImagingStudy toOpenmrsType(@Nonnull ImagingStudy resource) {
		FhirImagingStudy study = new FhirImagingStudy();
		study.setCreator(Context.getUserContext().getAuthenticatedUser());
		study.setDateCreated(new Date());
		return this.toOpenmrsType(study, resource);
	}
	
	@Override
	public FhirImagingStudy toOpenmrsType(@Nonnull FhirImagingStudy existingObject, @Nonnull ImagingStudy resource) {
		if (resource.hasIdentifier()) {
			String studyInstanceUuid = findStudyInstanceUuid(resource.getIdentifier());
			if (!StringUtils.isEmpty(studyInstanceUuid)) {
				existingObject.setStudyInstanceUuid(studyInstanceUuid);
			}
		}
		
		if (resource.hasStarted()) {
			existingObject.setDateStarted(resource.getStarted());
		}
		
		if (resource.hasSubject()) {
			existingObject.setSubject(patientReferenceTranslator.toOpenmrsType(resource.getSubject()));
		}
		
		if (resource.hasBasedOn()) {
			existingObject.setOrder(basedOnReferenceTranslator.toOpenmrsType(resource.getBasedOnFirstRep()));
		}
		
		if (resource.hasStatus()) {
			existingObject.setStatus(toOmrsStatusType(resource.getStatus()));
		}
		
		if (resource.hasLocation()) {
			Location location = locationReferenceTranslator.toOpenmrsType(resource.getLocation());
			if (location == null) {
				log.error(ERR_INVALID_LOCATION_REFERENCE);
				throw new InvalidRequestException(ERR_INVALID_LOCATION_REFERENCE);
			}
			existingObject.setLocation(location);
		}
		
		if (resource.hasDescription()) {
			existingObject.setDescription(resource.getDescription());
		}
		
		if (isExistingRecord(existingObject)) {
			existingObject.setChangedBy(Context.getUserContext().getAuthenticatedUser());
			existingObject.setDateChanged(new Date());
		}
		
		mapExtensionToStudyPerformer(existingObject, resource);
		mapExtensionToDateCompleted(existingObject, resource);
		mapAnnotationsToNotes(existingObject, resource);
		
		return existingObject;
	}
	
	private static void mapExtensionToDateCompleted(FhirImagingStudy existingObject, ImagingStudy resource) {
		List<Extension> completionDateExtns = resource.getExtensionsByUrl(FHIR_EXT_IMAGING_STUDY_COMPLETION_DATE);
		if (!completionDateExtns.isEmpty()) {
			Type value = completionDateExtns.get(0).getValue();
			if (value instanceof DateTimeType) {
				Date completionDate = ((DateTimeType) value).getValue();
				existingObject.setDateCompleted(completionDate);
			}
		}
	}
	
	private String findStudyInstanceUuid(List<Identifier> identifiers) {
		if (identifiers == null || identifiers.isEmpty()) {
			return null;
		}
		for (Identifier identifier : identifiers) {
			if (SYSTEM_DICOM_UID.equals(identifier.getSystem())) {
				return identifier.getValue();
			}
		}
		return null;
	}
	
	private FhirImagingStudy.FhirImagingStudyStatus toOmrsStatusType(ImagingStudy.ImagingStudyStatus status) {
		return FhirImagingStudy.FhirImagingStudyStatus.valueOf(status.name());
	}
	
	private ImagingStudy.ImagingStudyStatus tFhirStatusType(FhirImagingStudy.FhirImagingStudyStatus status) {
		return ImagingStudy.ImagingStudyStatus.valueOf(status.name());
	}
	
	private boolean isExistingRecord(FhirImagingStudy record) {
		return record.getImagingStudyId() != null && record.getImagingStudyId() != 0;
	}
	
	private void mapExtensionToStudyPerformer(FhirImagingStudy study, ImagingStudy resource) {
		List<Extension> performerExtensions = resource.getExtensionsByUrl(FHIR_EXT_IMAGING_STUDY_PERFORMER);
		if (performerExtensions.isEmpty()) {
			return;
		}
		Extension performerExt = performerExtensions.get(0);
		Type performerExtValue = performerExt.getValue();
		if (performerExtValue instanceof Reference) {
			Reference reference = (Reference) performerExtValue;
			Provider performer = practitionerReferenceTranslator.toOpenmrsType(reference);
			if (performer == null) {
				throw new InvalidRequestException(INVALID_PERFORMER_FOR_IMAGING_STUDY);
			}
			study.setPerformer(performer);
		}
	}
	
	private void mapAnnotationsToNotes(FhirImagingStudy study, ImagingStudy resource) {
		if (!resource.hasNote()) {
			return;
		}

		Map<String, FhirImagingStudyNote> existingNotesMap = new HashMap<>();
		if (study.getNotes() != null) {
			for (FhirImagingStudyNote existingNote : study.getNotes()) {
				if (existingNote.getUuid() != null) {
					existingNotesMap.put(existingNote.getUuid(), existingNote);
				}
			}
		}
		
		Set<String> incomingNoteUuids = new HashSet<>();
		
		for (Annotation annotation : resource.getNote()) {
			String noteUuid = annotation.getId();
			incomingNoteUuids.add(noteUuid);

			FhirImagingStudyNote note;
			
			if (noteUuid != null && existingNotesMap.containsKey(noteUuid)) {
				note = existingNotesMap.get(noteUuid);
				note.setChangedBy(Context.getUserContext().getAuthenticatedUser());
				note.setDateChanged(new Date());
			} else {
				note = new FhirImagingStudyNote();
				note.setImagingStudy(study);
				note.setCreator(Context.getUserContext().getAuthenticatedUser());
                note.setUuid(noteUuid);
                note.setDateCreated(new Date());
				study.getNotes().add(note);
			}

			if (annotation.hasText()) {
				note.setNote(annotation.getText());
			}
			
			if (annotation.hasAuthorReference()) {
				Provider performer = practitionerReferenceTranslator.toOpenmrsType(annotation.getAuthorReference());
				note.setPerformer(performer);
			}
			
			if (annotation.hasTime()) {
				note.setDateCreated(annotation.getTime());
			}
		}
		
		if (study.getNotes() != null) {
			study.getNotes().removeIf(note -> 
				note.getUuid() != null && !incomingNoteUuids.contains(note.getUuid())
			);
		}
	}
}
