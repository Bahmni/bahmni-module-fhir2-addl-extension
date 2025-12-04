package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirImagingStudy;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniFhirImagingStudyTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniServiceRequestReferenceTranslator;
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
import java.util.List;
import java.util.Optional;

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
        Optional.ofNullable(practitionerReferenceTranslator.toFhirResource(study.getPerformer()))
                .ifPresent(reference -> resource.addExtension(FHIR_EXT_IMAGING_STUDY_PERFORMER, reference));
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
		
		return existingObject;
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
		return record.getImagingStudyId() == null || record.getImagingStudyId() == 0;
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
}
