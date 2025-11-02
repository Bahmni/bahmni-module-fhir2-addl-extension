package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.bahmni.module.fhir2AddlExtension.api.dao.DocumentReferenceAttributeTypeDao;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReference;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReferenceAttributeType;
import org.bahmni.module.fhir2AddlExtension.api.translator.DocumentReferenceExtensionTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.DocumentReferenceStatusTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.DocumentReferenceTranslator;
import org.hl7.fhir.r4.model.DocumentReference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Encounter;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.api.db.ContextDAO;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.bahmni.module.fhir2AddlExtension.api.TestDataFactory.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DocumentReferenceTranslatorImplTest {
	
	private DocumentReferenceTranslator translator;
	
	@Mock
	private PatientReferenceTranslator patientReferenceTranslator;
	
	@Mock
	private ConceptTranslator conceptTranslator;
	
	@Mock
	private EncounterReferenceTranslator<Encounter> encounterReferenceTranslator;
	
	private DocumentReferenceStatusTranslator statusTranslator = new DocumentReferenceStatusTranslatorImpl();
	
	private List<FhirDocumentReferenceAttributeType> supportedAttributes = Arrays.asList(
	    exampleAttrTypeExternalOrganization(), exampleAttrTypeIsSelfSubmitted());
	
	@Mock
	PractitionerReferenceTranslator<Provider> providerReferenceTranslator;
	
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
        DocumentReferenceAttributeTypeDao attributeTypeDao = includeRetired -> supportedAttributes;
        DefaultDocumentReferenceAttributeTranslatorImpl defaultAttributeTranslator = new DefaultDocumentReferenceAttributeTranslatorImpl(attributeTypeDao);
        DocumentReferenceExtensionTranslator extensionTranslator = new DocumentReferenceExtensionTranslatorImpl(defaultAttributeTranslator);
        translator = new DocumentReferenceTranslatorImpl(
                patientReferenceTranslator,
                conceptTranslator,
                statusTranslator,
                encounterReferenceTranslator,
                providerReferenceTranslator,
                extensionTranslator);
    }
	
	@Test
	public void toFhirResource_shouldTranslateTypeForDocumentReference() {
		//        String uuid = UUID.randomUUID().toString();
		//        Concept prescription = exampleConcept("Prescription");
	}
	
	@Test
    public void toOpenmrsType_shouldTranslateFromDocumentReference() throws IOException {
        DocumentReference resource = (DocumentReference) loadResourceFromFile("example-document-reference.json");
        when(conceptTranslator.toOpenmrsType(resource.getType())).thenReturn(exampleConceptWithUuid("Prescription", "e53c2ed9-6c23-4683-ad9e-12c2a5d780d8"));
        when(conceptTranslator.toOpenmrsType(resource.getSecurityLabelFirstRep())).thenReturn(exampleConceptWithUuid("All Clinicians", "a9a50608-0470-444d-aa44-3a03b63902b0"));
        when(providerReferenceTranslator.toOpenmrsType(resource.getAuthorFirstRep())).thenReturn(exampleProviderWithUuid("Dr Neha", "05d6752c-5e08-11ef-8f7c-0242ac120002"));
        FhirDocumentReference fhirDocumentReference = translator.toOpenmrsType(resource);
        Assert.assertTrue(fhirDocumentReference != null);
        Assert.assertEquals(3, fhirDocumentReference.getContents().size());
        //asserting that none of the client assigned id is considered for content
        Set<String> assignedIds = resource.getContent().stream()
                .map(contentComponent -> contentComponent.getId())
                .filter(element -> element != null).collect(Collectors.toSet());
        Set<String> allocatedIds = fhirDocumentReference.getContents().stream()
                .map(content -> content.getUuid()).collect(Collectors.toSet());
        // Calculate Set assignedIds - Set allocatedIds
        Set<String> difference = assignedIds.stream()
                .filter(element -> allocatedIds.contains(element))
                .collect(Collectors.toSet());
        Assert.assertEquals(0, difference.size());
        Assert.assertEquals(1, fhirDocumentReference.getActiveAttributes().size());
        Assert.assertEquals("Good Health Clinic", fhirDocumentReference.getActiveAttributes().iterator().next().getValueReference());
    }
	
	@Test(expected = UnprocessableEntityException.class)
	public void toOpenmrsType_shouldNotTranslateContentIfAttachmentIsNotPassedForNewDocRef() throws IOException {
		DocumentReference resource = (DocumentReference) loadResourceFromFile("example-document-reference.json");
		when(conceptTranslator.toOpenmrsType(resource.getType())).thenReturn(
		    exampleConceptWithUuid("Prescription", "e53c2ed9-6c23-4683-ad9e-12c2a5d780d8"));
		when(conceptTranslator.toOpenmrsType(resource.getSecurityLabelFirstRep())).thenReturn(
		    exampleConceptWithUuid("All Clinicians", "a9a50608-0470-444d-aa44-3a03b63902b0"));
		when(providerReferenceTranslator.toOpenmrsType(resource.getAuthorFirstRep())).thenReturn(
		    exampleProviderWithUuid("Dr Neha", "05d6752c-5e08-11ef-8f7c-0242ac120002"));
		resource.getContent().get(0).setAttachment(null);
		try {
			FhirDocumentReference existing = translator.toOpenmrsType(resource);
		}
		catch (Exception e) {
			Assert.assertTrue(e instanceof UnprocessableEntityException);
			Assert.assertEquals("Invalid document attachment. Please ensure attachment has valid content-type and url",
			    ((UnprocessableEntityException) e).getMessage());
			throw new UnprocessableEntityException(e.getMessage());
		}
		
	}
	
	@Test
	public void toOpenmrsType_shouldTranslateContentIfAttachmentIsNotPassedForExistingDocRef() throws IOException {
        DocumentReference resource = (DocumentReference) loadResourceFromFile("example-document-reference.json");
        when(conceptTranslator.toOpenmrsType(resource.getType())).thenReturn(exampleConceptWithUuid("Prescription", "e53c2ed9-6c23-4683-ad9e-12c2a5d780d8"));
        when(conceptTranslator.toOpenmrsType(resource.getSecurityLabelFirstRep())).thenReturn(exampleConceptWithUuid("All Clinicians", "a9a50608-0470-444d-aa44-3a03b63902b0"));
        when(providerReferenceTranslator.toOpenmrsType(resource.getAuthorFirstRep())).thenReturn(exampleProviderWithUuid("Dr Neha", "05d6752c-5e08-11ef-8f7c-0242ac120002"));

        FhirDocumentReference existing = translator.toOpenmrsType(resource);
        Assert.assertEquals(3, existing.getContents().size());

        List<String> contentIds = existing.getContents().stream().map(c -> c.getUuid()).collect(Collectors.toList());
        //manually set ids, since this is not persisted on db for uuids to be assigned
        existing.setDocumentReferenceId(1);
        for (int i=0; i < contentIds.size(); i++) {
            resource.getContent().get(i).setId(contentIds.get(i));
        }

        resource.getContent().stream().filter(contentComponent -> contentComponent.getId().equals(contentIds.get(0))).findFirst().ifPresent(c -> c.setAttachment(null));
        FhirDocumentReference updated = translator.toOpenmrsType(existing, resource);
        Assert.assertEquals(3, existing.getContents().stream().filter(c -> !c.getVoided()).collect(Collectors.toList()).size());
    }
}
