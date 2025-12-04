package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2AddlExtension.api.dao.BahmniFhirImagingStudyDao;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirImagingStudy;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirImagingStudyService;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniFhirImagingStudyTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniServiceRequestReferenceTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.impl.BahmniFhirImagingStudyTranslatorImpl;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.ImagingStudy;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Location;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.api.db.ContextDAO;
import org.openmrs.module.fhir2.api.translators.LocationReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;

import java.io.IOException;

import static org.bahmni.module.fhir2AddlExtension.api.TestDataFactory.loadResourceFromFile;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirImagingStudyServiceImplTest {
    @Mock
    private ContextDAO contextDAO;

    @Mock
    private UserContext userContext;

    @Mock
    private User user;

    @Mock
    private BahmniFhirImagingStudyDao imagingStudyDao;
    @Mock
    private BahmniServiceRequestReferenceTranslator basedOnReferenceTranslator;
    @Mock
    private PatientReferenceTranslator patientReferenceTranslator;
    @Mock
    private LocationReferenceTranslator locationReferenceTranslator;
    @Mock
    private PractitionerReferenceTranslator<Provider> practitionerReferenceTranslator;

    private BahmniFhirImagingStudyTranslator imagingStudyTranslator;
    private BahmniFhirImagingStudyService fhirImagingStudyService;

    @Before
    public void setUp() {
        when(userContext.getAuthenticatedUser()).thenReturn(user);
        Context.setDAO(contextDAO);
        Context.openSession();
        Context.setUserContext(userContext);

        imagingStudyTranslator = new BahmniFhirImagingStudyTranslatorImpl(basedOnReferenceTranslator, patientReferenceTranslator, locationReferenceTranslator, practitionerReferenceTranslator);
        fhirImagingStudyService = new BahmniFhirImagingStudyServiceImpl(imagingStudyDao, imagingStudyTranslator) {
            @Override
            protected void validateObject(FhirImagingStudy object) {
                //Done because baseFhirService implementation uses Context.getAdministrativeService()
                //we are not mocking Context.serviceContext
            }
        };
    }

    @Test
    public void shouldCreateImagingStudy() throws IOException {
        Location studyLocation = new Location();
        studyLocation.setName("Radiology Center");
        studyLocation.setUuid("example-radiology-center");

        IBaseResource fhirResource = loadResourceFromFile("example-imaging-study-registered.json");
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(locationReferenceTranslator.toOpenmrsType(
                ArgumentMatchers.argThat(reference -> {
                    return reference.getReference().equals("Location/example-radiology-center");
                })))
                .thenReturn(studyLocation);
        ImagingStudy imagingStudy = fhirImagingStudyService.create((ImagingStudy)  fhirResource);
        //we are not considering client id for resoruces
        Assert.assertFalse("Client Id is not accepted", imagingStudy.getId().equals("example-imaging-study"));
        Assert.assertEquals(ImagingStudy.ImagingStudyStatus.REGISTERED, imagingStudy.getStatus());
    }

    @Test
    public void shouldUpdateImagingStudy() throws IOException {
        Location studyLocation = new Location();
        studyLocation.setName("Radiology Center");
        studyLocation.setUuid("example-radiology-center");

        Provider performer = new Provider();
        performer.setUuid("example-technician-id");

        FhirImagingStudy existingStudy = new FhirImagingStudy();
        existingStudy.setStudyInstanceUuid("urn:oid:2.16.124.113543.6003.1154777499.30246.19789.3503430045");
        existingStudy.setStatus(FhirImagingStudy.FhirImagingStudyStatus.REGISTERED);

        IBaseResource fhirResource = loadResourceFromFile("example-imaging-study-performed.json");
        when(imagingStudyDao.get("example-imaging-study")).thenReturn(existingStudy);
        when(imagingStudyDao.createOrUpdate(any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(locationReferenceTranslator.toOpenmrsType(
                ArgumentMatchers.argThat(reference -> {
                    return reference.getReference().equals("Location/example-radiology-center");
                })))
                .thenReturn(studyLocation);
        when(practitionerReferenceTranslator.toOpenmrsType(
                ArgumentMatchers.argThat(reference -> {
                    return reference.getReference().equals("Practitioner/example-technician-id");
                })))
                .thenReturn(performer);
        when(practitionerReferenceTranslator.toFhirResource(
                ArgumentMatchers.argThat(provider -> {
                    return provider.getUuid().equals("example-technician-id");
                })))
                .thenReturn(new Reference("Practitioner/example-technician-id"));
        ImagingStudy imagingStudy = fhirImagingStudyService.update("example-imaging-study", (ImagingStudy)  fhirResource);
        Assert.assertEquals(ImagingStudy.ImagingStudyStatus.REGISTERED, imagingStudy.getStatus());
        Extension performerExt = imagingStudy.getExtensionByUrl(BahmniFhirConstants.FHIR_EXT_IMAGING_STUDY_PERFORMER);
        Assert.assertNotNull("Performer extension should not be null for Imaging Study", performerExt);
    }

}