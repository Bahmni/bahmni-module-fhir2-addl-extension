package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.bahmni.module.fhir2AddlExtension.api.dao.DocumentReferenceAttributeTypeDao;
import org.bahmni.module.fhir2AddlExtension.api.dao.DocumentReferenceDao;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReference;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReferenceAttributeType;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniOrderReferenceTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.DocumentReferenceExtensionTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.DocumentReferenceStatusTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.DocumentReferenceTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.impl.DefaultDocumentReferenceAttributeTranslatorImpl;
import org.bahmni.module.fhir2AddlExtension.api.translator.impl.DocumentReferenceExtensionTranslatorImpl;
import org.bahmni.module.fhir2AddlExtension.api.translator.impl.DocumentReferenceStatusTranslatorImpl;
import org.bahmni.module.fhir2AddlExtension.api.translator.impl.DocumentReferenceTranslatorImpl;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Enumerations;
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
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PatientReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.bahmni.module.fhir2AddlExtension.api.TestDataFactory.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BahmniFhirDocumentReferenceServiceImplTest {
	
	BahmniFhirDocumentReferenceServiceImpl documentReferenceService;
	
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
	private PractitionerReferenceTranslator<Provider> providerReferenceTranslator;
	
	@Mock
	private ContextDAO contextDAO;
	
	@Mock
	private UserContext userContext;
	
	@Mock
	private User user;
	
	@Mock
	private DocumentReferenceDao documentReferenceDao;
	
	@Mock
	private SearchQuery<FhirDocumentReference, DocumentReference, DocumentReferenceDao, DocumentReferenceTranslator, SearchQueryInclude<DocumentReference>> searchQuery;
	
	@Mock
	private SearchQueryInclude<DocumentReference> searchQueryInclude;
	
	@Mock
	private BahmniOrderReferenceTranslator basedOnReferenceTranslator;
	
	private DocumentReferenceTranslator translator;
	
	@Before
    public void setUp() {
        when(userContext.getAuthenticatedUser()).thenReturn(user);
        Context.setDAO(contextDAO);
        Context.openSession();
        Context.setUserContext(userContext);
        DocumentReferenceAttributeTypeDao attributeTypeDao = includeRetired -> supportedAttributes;
        DefaultDocumentReferenceAttributeTranslatorImpl defaultAttributeTranslator = new DefaultDocumentReferenceAttributeTranslatorImpl(attributeTypeDao);
        DocumentReferenceExtensionTranslator extensionTranslator = new DocumentReferenceExtensionTranslatorImpl(defaultAttributeTranslator);
        translator = new DocumentReferenceTranslatorImpl(patientReferenceTranslator,
                conceptTranslator, statusTranslator, encounterReferenceTranslator, providerReferenceTranslator, extensionTranslator, basedOnReferenceTranslator);
        documentReferenceService = new BahmniFhirDocumentReferenceServiceImpl(translator, documentReferenceDao, searchQueryInclude, searchQuery);
    }
	
	@Test(expected = InvalidRequestException.class)
	public void shouldThrowErrorForIncorrectStatusInCreateOperation() throws IOException {
		DocumentReference resource = (DocumentReference) loadResourceFromFile("example-document-reference.json");
		resource.setStatus(Enumerations.DocumentReferenceStatus.ENTEREDINERROR);
		documentReferenceService.create(resource);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void shouldThrowErrorForDifferentSubjectInUpdateOperation() throws IOException {
		DocumentReference resource = (DocumentReference) loadResourceFromFile("example-document-reference.json");
		when(conceptTranslator.toOpenmrsType(resource.getType())).thenReturn(
		    exampleConceptWithUuid("Prescription", "e53c2ed9-6c23-4683-ad9e-12c2a5d780d8"));
		when(conceptTranslator.toOpenmrsType(resource.getSecurityLabelFirstRep())).thenReturn(
		    exampleConceptWithUuid("All Clinicians", "a9a50608-0470-444d-aa44-3a03b63902b0"));
		when(providerReferenceTranslator.toOpenmrsType(resource.getAuthorFirstRep())).thenReturn(
		    exampleProviderWithUuid("Dr Neha", "05d6752c-5e08-11ef-8f7c-0242ac120002"));
		
		FhirDocumentReference document = translator.toOpenmrsType(resource);
		resource.getSubject().setReference("Patient/Random");
		//when(documentReferenceDao.get(document.getUuid())).thenReturn(document);
		documentReferenceService.update(document.getUuid(), resource);
	}
	
	@Test(expected = UnprocessableEntityException.class)
	public void shouldThrowErrorForInvalidAttachmentInCreateOperation() throws IOException {
		DocumentReference resource = (DocumentReference) loadResourceFromFile("example-document-reference.json");
		when(conceptTranslator.toOpenmrsType(resource.getType())).thenReturn(
		    exampleConceptWithUuid("Prescription", "e53c2ed9-6c23-4683-ad9e-12c2a5d780d8"));
		when(conceptTranslator.toOpenmrsType(resource.getSecurityLabelFirstRep())).thenReturn(
		    exampleConceptWithUuid("All Clinicians", "a9a50608-0470-444d-aa44-3a03b63902b0"));
		when(providerReferenceTranslator.toOpenmrsType(resource.getAuthorFirstRep())).thenReturn(
		    exampleProviderWithUuid("Dr Neha", "05d6752c-5e08-11ef-8f7c-0242ac120002"));
		resource.getContent().get(0).setAttachment(null);
		FhirDocumentReference document = translator.toOpenmrsType(resource);
	}
	
	@Test
	public void applyUpdate() {
		
	}
}
