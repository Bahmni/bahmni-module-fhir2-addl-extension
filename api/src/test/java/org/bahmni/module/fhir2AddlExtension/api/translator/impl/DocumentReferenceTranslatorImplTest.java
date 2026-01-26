package org.bahmni.module.fhir2AddlExtension.api.translator.impl;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.bahmni.module.fhir2AddlExtension.api.dao.DocumentReferenceAttributeTypeDao;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReference;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReferenceAttributeType;
import org.bahmni.module.fhir2AddlExtension.api.translator.BahmniOrderReferenceTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.DocumentReferenceExtensionTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.DocumentReferenceStatusTranslator;
import org.bahmni.module.fhir2AddlExtension.api.translator.DocumentReferenceTranslator;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.Patient;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
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
	
	@Mock
	private BahmniOrderReferenceTranslator basedOnReferenceTranslator;
	
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
                extensionTranslator,
				basedOnReferenceTranslator);
    }
	
	@Test
	public void toFhirResource_shouldTranslateTypeForDocumentReference() {
		//        String uuid = UUID.randomUUID().toString();
		//        Concept prescription = exampleConcept("Prescription");
	}
	
	@Test
    public void toOpenmrsType_shouldTranslateFromDocumentReference() throws IOException {
        DocumentReference resource = (DocumentReference) loadResourceFromFile("example-document-reference.json");
		Reference orderRef = (Reference) resource.getExtensionByUrl("http://fhir.bahmni.org/ext/document-reference/based-on-service-request").getValue();
        when(conceptTranslator.toOpenmrsType(resource.getType())).thenReturn(exampleConceptWithUuid("Prescription", "e53c2ed9-6c23-4683-ad9e-12c2a5d780d8"));
        when(conceptTranslator.toOpenmrsType(resource.getSecurityLabelFirstRep())).thenReturn(exampleConceptWithUuid("All Clinicians", "a9a50608-0470-444d-aa44-3a03b63902b0"));
        when(providerReferenceTranslator.toOpenmrsType(resource.getAuthorFirstRep())).thenReturn(exampleProviderWithUuid("Dr Neha", "05d6752c-5e08-11ef-8f7c-0242ac120002"));
		Patient patient = new Patient();
		patient.setUuid("1357c6aa-3421-44f9-9a8f-391b6cbb88e4");
		Order order = new Order();
		order.setPatient(patient);
		order.setUuid("example-order-id");
		when(basedOnReferenceTranslator.toOpenmrsType(orderRef)).thenReturn(order);
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
		Assert.assertEquals("example-order-id", fhirDocumentReference.getOrder().getUuid());
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
	
	@Test
	public void toFhirResource_shouldMapContextPeriodStartAndEndDates() throws ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date startDate = dateFormat.parse("2025-10-31 18:30:00");
		Date endDate = dateFormat.parse("2025-11-21 18:30:00");
		
		FhirDocumentReference docRef = new FhirDocumentReference();
		docRef.setUuid("test-uuid");
		docRef.setMasterIdentifier("TEST123");
		docRef.setDateStarted(startDate);
		docRef.setDateEnded(endDate);
		docRef.setStatus(FhirDocumentReference.FhirDocumentReferenceStatus.CURRENT);
		docRef.setDocStatus(FhirDocumentReference.FhirDocumentReferenceDocStatus.PRELIMINARY);
		
		DocumentReference fhirResource = translator.toFhirResource(docRef);
		
		Assert.assertTrue(fhirResource.hasContext());
		Assert.assertTrue(fhirResource.getContext().hasPeriod());
		Assert.assertEquals(startDate, fhirResource.getContext().getPeriod().getStart());
		Assert.assertEquals(endDate, fhirResource.getContext().getPeriod().getEnd());
	}
	
	@Test
	public void toFhirResource_shouldMapContextPeriodWithOnlyStartDate() throws ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date startDate = dateFormat.parse("2025-10-31 18:30:00");
		
		FhirDocumentReference docRef = new FhirDocumentReference();
		docRef.setUuid("test-uuid");
		docRef.setMasterIdentifier("TEST123");
		docRef.setDateStarted(startDate);
		docRef.setStatus(FhirDocumentReference.FhirDocumentReferenceStatus.CURRENT);
		docRef.setDocStatus(FhirDocumentReference.FhirDocumentReferenceDocStatus.PRELIMINARY);
		
		DocumentReference fhirResource = translator.toFhirResource(docRef);
		
		Assert.assertTrue(fhirResource.hasContext());
		Assert.assertTrue(fhirResource.getContext().hasPeriod());
		Assert.assertEquals(startDate, fhirResource.getContext().getPeriod().getStart());
		Assert.assertNull(fhirResource.getContext().getPeriod().getEnd());
	}
	
	@Test
	public void toFhirResource_shouldMapContextPeriodWithOnlyEndDate() throws ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date endDate = dateFormat.parse("2025-11-21 18:30:00");
		
		FhirDocumentReference docRef = new FhirDocumentReference();
		docRef.setUuid("test-uuid");
		docRef.setMasterIdentifier("TEST123");
		docRef.setDateEnded(endDate);
		docRef.setStatus(FhirDocumentReference.FhirDocumentReferenceStatus.CURRENT);
		docRef.setDocStatus(FhirDocumentReference.FhirDocumentReferenceDocStatus.PRELIMINARY);
		
		DocumentReference fhirResource = translator.toFhirResource(docRef);
		
		Assert.assertTrue(fhirResource.hasContext());
		Assert.assertTrue(fhirResource.getContext().hasPeriod());
		Assert.assertNull(fhirResource.getContext().getPeriod().getStart());
		Assert.assertEquals(endDate, fhirResource.getContext().getPeriod().getEnd());
	}
	
	@Test
	public void toFhirResource_shouldMapContextWithEncounterAndPeriod() throws ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date startDate = dateFormat.parse("2025-10-31 18:30:00");
		Date endDate = dateFormat.parse("2025-11-21 18:30:00");
		
		Encounter encounter = new Encounter();
		encounter.setUuid("encounter-uuid");
		
		FhirDocumentReference docRef = new FhirDocumentReference();
		docRef.setUuid("test-uuid");
		docRef.setMasterIdentifier("TEST123");
		docRef.setEncounter(encounter);
		docRef.setDateStarted(startDate);
		docRef.setDateEnded(endDate);
		docRef.setStatus(FhirDocumentReference.FhirDocumentReferenceStatus.CURRENT);
		docRef.setDocStatus(FhirDocumentReference.FhirDocumentReferenceDocStatus.PRELIMINARY);
		
		org.hl7.fhir.r4.model.Reference encounterRef = new org.hl7.fhir.r4.model.Reference();
		encounterRef.setReference("Encounter/encounter-uuid");
		when(encounterReferenceTranslator.toFhirResource(encounter)).thenReturn(encounterRef);
		
		DocumentReference fhirResource = translator.toFhirResource(docRef);
		
		Assert.assertTrue(fhirResource.hasContext());
		Assert.assertTrue(fhirResource.getContext().hasEncounter());
		Assert.assertEquals(1, fhirResource.getContext().getEncounter().size());
		Assert.assertEquals("Encounter/encounter-uuid", fhirResource.getContext().getEncounter().get(0).getReference());
		Assert.assertTrue(fhirResource.getContext().hasPeriod());
		Assert.assertEquals(startDate, fhirResource.getContext().getPeriod().getStart());
		Assert.assertEquals(endDate, fhirResource.getContext().getPeriod().getEnd());
	}
	
	@Test
	public void toOpenmrsType_shouldMapContextPeriodFromFhirResource() throws ParseException, IOException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date startDate = dateFormat.parse("2025-10-31 18:30:00");
		Date endDate = dateFormat.parse("2025-11-21 18:30:00");
		
		DocumentReference resource = (DocumentReference) loadResourceFromFile("example-document-reference.json");
		Period period = new Period();
		period.setStart(startDate);
		period.setEnd(endDate);
		DocumentReference.DocumentReferenceContextComponent context = new DocumentReference.DocumentReferenceContextComponent();
		context.setPeriod(period);
		resource.setContext(context);
		
		when(conceptTranslator.toOpenmrsType(resource.getType())).thenReturn(
		    exampleConceptWithUuid("Prescription", "e53c2ed9-6c23-4683-ad9e-12c2a5d780d8"));
		when(conceptTranslator.toOpenmrsType(resource.getSecurityLabelFirstRep())).thenReturn(
		    exampleConceptWithUuid("All Clinicians", "a9a50608-0470-444d-aa44-3a03b63902b0"));
		when(providerReferenceTranslator.toOpenmrsType(resource.getAuthorFirstRep())).thenReturn(
		    exampleProviderWithUuid("Dr Neha", "05d6752c-5e08-11ef-8f7c-0242ac120002"));
		Reference orderRef = (Reference) resource.getExtensionByUrl(
		    "http://fhir.bahmni.org/ext/document-reference/based-on-service-request").getValue();
		Patient patient = new Patient();
		patient.setUuid("1357c6aa-3421-44f9-9a8f-391b6cbb88e4");
		Order order = new Order();
		order.setPatient(patient);
		order.setUuid("example-order-id");
		when(basedOnReferenceTranslator.toOpenmrsType(orderRef)).thenReturn(order);
		
		FhirDocumentReference fhirDocumentReference = translator.toOpenmrsType(resource);
		
		Assert.assertNotNull(fhirDocumentReference.getDateStarted());
		Assert.assertNotNull(fhirDocumentReference.getDateEnded());
		Assert.assertEquals(startDate, fhirDocumentReference.getDateStarted());
		Assert.assertEquals(endDate, fhirDocumentReference.getDateEnded());
	}
}
