package org.bahmni.module.fhir2addlextension.api.dao.impl;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReference;
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
		
		doReturn(existingDoc).when(documentReferenceDao).get(uuid);
		doReturn(existingDoc).when(documentReferenceDao).createOrUpdate(existingDoc);
		
		documentReferenceDao.voidDocumentReference(uuid, voidReason);
		
		assertTrue(existingDoc.getVoided());
		assertEquals(FhirDocumentReference.FhirDocumentReferenceDocStatus.ENTEREDINERROR, existingDoc.getDocStatus());
		assertEquals(voidReason, existingDoc.getVoidReason());
		assertNotNull(existingDoc.getDateVoided());
		assertEquals(user, existingDoc.getVoidedBy());
		verify(documentReferenceDao).createOrUpdate(existingDoc);
	}
	
	@Test(expected = InvalidRequestException.class)
	public void shouldThrowErrorWhenVoidingNonExistentDocumentReference() {
		String uuid = "non-existent-uuid";
		
		doReturn(null).when(documentReferenceDao).get(uuid);
		
		documentReferenceDao.voidDocumentReference(uuid, "some reason");
	}
}
