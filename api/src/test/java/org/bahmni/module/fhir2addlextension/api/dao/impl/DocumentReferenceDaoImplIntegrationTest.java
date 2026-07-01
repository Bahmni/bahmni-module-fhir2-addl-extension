package org.bahmni.module.fhir2addlextension.api.dao.impl;

import org.bahmni.module.fhir2addlextension.api.model.FhirDocumentReference;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.openmrs.Encounter;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DocumentReferenceDaoImplIntegrationTest extends BaseModuleContextSensitiveTest {

	private static final String ENCOUNTER_UUID = "6519d653-393b-4118-9c83-a3715b82d4ac";

	private static final String PATIENT_UUID = "da7f524f-27ce-4bb2-86d6-6d1d05312bd5";

	@Autowired
	private SessionFactory sessionFactory;

	@Test
	public void shouldPersistDocumentReferenceWithAllFields() {
		Encounter encounter = Context.getEncounterService().getEncounterByUuid(ENCOUNTER_UUID);
		assertNotNull("Standard test encounter must exist", encounter);

		FhirDocumentReference docRef = new FhirDocumentReference();
		docRef.setUuid(UUID.randomUUID().toString());
		docRef.setSubject(Context.getPatientService().getPatientByUuid(PATIENT_UUID));
		docRef.setEncounter(encounter);
		docRef.setDocType(Context.getConceptService().getConcept(3));
		docRef.setStatus(FhirDocumentReference.FhirDocumentReferenceStatus.CURRENT);
		docRef.setDocStatus(FhirDocumentReference.FhirDocumentReferenceDocStatus.FINAL);
		docRef.setDescription("Test note");
		docRef.setCreator(Context.getUserContext().getAuthenticatedUser());
		docRef.setDateCreated(new Date());
		docRef.setVoided(false);

		Session session = sessionFactory.getCurrentSession();
		session.saveOrUpdate(docRef);
		session.flush();
		session.clear();

		FhirDocumentReference saved = (FhirDocumentReference) session.get(FhirDocumentReference.class,
		    docRef.getDocumentReferenceId());

		assertNotNull(saved);
		assertNotNull("encounter_id FK must be persisted", saved.getEncounter());
		assertEquals(ENCOUNTER_UUID, saved.getEncounter().getUuid());
		assertEquals("Test note", saved.getDescription());
		assertEquals(FhirDocumentReference.FhirDocumentReferenceStatus.CURRENT, saved.getStatus());
		assertEquals(FhirDocumentReference.FhirDocumentReferenceDocStatus.FINAL, saved.getDocStatus());
	}
}
