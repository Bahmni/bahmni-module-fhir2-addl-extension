package org.bahmni.module.fhir2addlextension.api.dao.impl;

import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReference;
import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReferenceAttribute;
import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReferenceContent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.api.db.ContextDAO;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DocumentReferenceDaoImplTest {
	
	@Spy
	private DocumentReferenceDaoImpl documentReferenceDao;
	
	private User user;
	
	@Before
	public void setUp() {
		user = new User();
		ContextDAO contextDAO = mock(ContextDAO.class);
		UserContext userContext = mock(UserContext.class);
		when(userContext.getAuthenticatedUser()).thenReturn(user);
		Context.setDAO(contextDAO);
		Context.openSession();
		Context.setUserContext(userContext);
	}
	
	@Test
	public void shouldVoidDocumentReferenceSuccessfully() {
		String uuid = "doc-uuid-123";
		String voidReason = "Duplicate entry";
		FhirDocumentReference existingDoc = new FhirDocumentReference();
		existingDoc.setUuid(uuid);
		
		doReturn(existingDoc).when(documentReferenceDao).createOrUpdate(existingDoc);
		
		documentReferenceDao.voidDocumentReference(existingDoc, voidReason);
		
		assertTrue(existingDoc.getVoided());
		assertEquals(FhirDocumentReference.FhirDocumentReferenceDocStatus.ENTEREDINERROR, existingDoc.getDocStatus());
		assertEquals(voidReason, existingDoc.getVoidReason());
		assertNotNull(existingDoc.getDateVoided());
		assertEquals(user, existingDoc.getVoidedBy());
		verify(documentReferenceDao).createOrUpdate(existingDoc);
	}
	
	@Test
	public void shouldVoidDocumentReferenceWithContentsAndAttributes() {
		String uuid = "doc-uuid-123";
		String voidReason = "Duplicate entry";
		FhirDocumentReference existingDoc = createDocumentReferenceWithChildren();
		existingDoc.setUuid(uuid);
		
		doReturn(existingDoc).when(documentReferenceDao).createOrUpdate(existingDoc);
		
		documentReferenceDao.voidDocumentReference(existingDoc, voidReason);
		
		assertDocumentReferenceVoided(existingDoc, voidReason);
		assertChildEntitiesVoided(existingDoc, voidReason);
		verify(documentReferenceDao).createOrUpdate(existingDoc);
	}
	
	@Test
	public void shouldSkipAlreadyVoidedChildEntities() {
		String uuid = "doc-uuid-123";
		String voidReason = "New void reason";
		FhirDocumentReference existingDoc = new FhirDocumentReference();
		existingDoc.setUuid(uuid);
		
		FhirDocumentReferenceContent alreadyVoidedContent = new FhirDocumentReferenceContent();
		alreadyVoidedContent.setVoided(true);
		alreadyVoidedContent.setVoidReason("Previously voided");
		existingDoc.addContent(alreadyVoidedContent);
		
		doReturn(existingDoc).when(documentReferenceDao).createOrUpdate(existingDoc);
		
		documentReferenceDao.voidDocumentReference(existingDoc, voidReason);
		
		assertTrue(existingDoc.getVoided());
		assertEquals(voidReason, existingDoc.getVoidReason());
		assertEquals("Previously voided", alreadyVoidedContent.getVoidReason());
	}
	
	private FhirDocumentReference createDocumentReferenceWithChildren() {
		FhirDocumentReference doc = new FhirDocumentReference();
		
		FhirDocumentReferenceContent content = new FhirDocumentReferenceContent();
		content.setVoided(false);
		doc.addContent(content);
		
		FhirDocumentReferenceAttribute attribute = new FhirDocumentReferenceAttribute();
		attribute.setVoided(false);
		doc.addAttribute(attribute);
		
		return doc;
	}
	
	private void assertDocumentReferenceVoided(FhirDocumentReference doc, String expectedReason) {
		assertTrue(doc.getVoided());
		assertEquals(FhirDocumentReference.FhirDocumentReferenceDocStatus.ENTEREDINERROR, doc.getDocStatus());
		assertEquals(expectedReason, doc.getVoidReason());
		assertNotNull(doc.getDateVoided());
		assertEquals(user, doc.getVoidedBy());
	}
	
	private void assertChildEntitiesVoided(FhirDocumentReference doc, String expectedReason) {
		doc.getContents().forEach(content -> {
			assertTrue(content.getVoided());
			assertEquals(expectedReason, content.getVoidReason());
			assertNotNull(content.getDateVoided());
			assertEquals(user, content.getVoidedBy());
		});
		
		doc.getAttributes().forEach(attribute -> {
			assertTrue(attribute.getVoided());
			assertEquals(expectedReason, attribute.getVoidReason());
			assertNotNull(attribute.getDateVoided());
			assertEquals(user, attribute.getVoidedBy());
		});
	}
}
