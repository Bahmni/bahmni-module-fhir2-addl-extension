package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirImagingStudy;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniFhirImagingStudyTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniServiceRequestReferenceTranslator;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ImagingStudy;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component
@Slf4j
public class BahmniFhirImagingStudyTranslatorImpl implements BahmniFhirImagingStudyTranslator {

    public static final String URN_DICOM_UID = "urn:dicom:uid";
    private final BahmniServiceRequestReferenceTranslator basedOnReferenceTranslator;
    private final PatientReferenceTranslator patientReferenceTranslator;

    @Autowired
    public BahmniFhirImagingStudyTranslatorImpl(BahmniServiceRequestReferenceTranslator basedOnReferenceTranslator, PatientReferenceTranslator patientReferenceTranslator) {
        this.basedOnReferenceTranslator = basedOnReferenceTranslator;
        this.patientReferenceTranslator = patientReferenceTranslator;
    }

    @Override
    public ImagingStudy toFhirResource(@Nonnull FhirImagingStudy data) {
        ImagingStudy resource = new ImagingStudy();
        resource.setId(data.getUuid());

        String studyInstanceUuid = data.getStudyInstanceUuid();
        Identifier dicomStudyIdentifier = new Identifier();
        dicomStudyIdentifier.setSystem(URN_DICOM_UID);
        dicomStudyIdentifier.setValue(studyInstanceUuid);
        resource.addIdentifier(dicomStudyIdentifier);


        if (data.getOrder() != null) {
            resource.addBasedOn(basedOnReferenceTranslator.toFhirResource(data.getOrder()));
        }

        if (data.getSubject() != null) {
            resource.setSubject(patientReferenceTranslator.toFhirResource(data.getSubject()));
        }
        return resource;
    }

    @Override
    public FhirImagingStudy toOpenmrsType(@Nonnull ImagingStudy resource) {
        FhirImagingStudy study = new FhirImagingStudy();
        return this.toOpenmrsType(study, resource);
    }

    @Override
    public FhirImagingStudy toOpenmrsType(@Nonnull FhirImagingStudy existingObject, @Nonnull ImagingStudy resource) {

        if (resource.hasSubject()) {
            existingObject.setSubject(patientReferenceTranslator.toOpenmrsType(resource.getSubject()));
        }

        if (resource.hasBasedOn()) {
            existingObject.setOrder(basedOnReferenceTranslator.toOpenmrsType(resource.getBasedOnFirstRep()));
        }

        return existingObject;
    }
}
