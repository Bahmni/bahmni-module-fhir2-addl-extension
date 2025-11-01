package org.bahmni.module.fhir2AddlExtension.api.service.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import lombok.extern.slf4j.Slf4j;
import org.bahmni.module.fhir2AddlExtension.api.dao.DocumentReferenceDao;
import org.bahmni.module.fhir2AddlExtension.api.model.FhirDocumentReference;
import org.bahmni.module.fhir2AddlExtension.api.search.param.BahmniDocumentReferenceSearchParams;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirDocumentReferenceService;
import org.bahmni.module.fhir2AddlExtension.api.translator.DocumentReferenceTranslator;
import org.hl7.fhir.r4.model.DocumentReference;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.dao.FhirPractitionerDao;
import org.openmrs.module.fhir2.api.impl.BaseFhirService;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Primary
@Transactional
@Slf4j
public class BahmniFhirDocumentReferenceServiceImpl extends BaseFhirService<DocumentReference, FhirDocumentReference> implements BahmniFhirDocumentReferenceService {
	
	public static final String MISSING_PATIENT_IDENTIFIER_OR_RES_ID = "Missing patient identifier/id or resource id";;
	
	private static final String PATIENT_IDENTIFIER_OR_REFERENCE_OR_RES_ID_MUST_BE_SPECIFIED = "You must specify patient identifier or reference or resource _id!";
	
	private DocumentReferenceDao documentReferenceDao;
	
	private DocumentReferenceTranslator documentReferenceTranslator;
	
	private FhirPractitionerDao practitionerDao;
	
	private final SearchQueryInclude<DocumentReference> searchQueryInclude;
	
	private final SearchQuery<FhirDocumentReference, DocumentReference, DocumentReferenceDao, DocumentReferenceTranslator, SearchQueryInclude<DocumentReference>> searchQuery;
	
	@Autowired
	public BahmniFhirDocumentReferenceServiceImpl(
	    DocumentReferenceTranslator documentReferenceTranslator,
	    DocumentReferenceDao documentReferenceDao,
	    FhirPractitionerDao practitionerDao,
	    SearchQueryInclude<DocumentReference> searchQueryInclude,
	    SearchQuery<FhirDocumentReference, DocumentReference, DocumentReferenceDao, DocumentReferenceTranslator, SearchQueryInclude<DocumentReference>> searchQuery) {
		this.documentReferenceTranslator = documentReferenceTranslator;
		this.documentReferenceDao = documentReferenceDao;
		this.practitionerDao = practitionerDao;
		this.searchQueryInclude = searchQueryInclude;
		this.searchQuery = searchQuery;
	}
	
	@Override
	public IBundleProvider searchDocumentReferences(BahmniDocumentReferenceSearchParams searchParams) {
		if (!searchParams.hasPatientReference() && !searchParams.hasId()) {
			logAndThrowUnsupportedExceptionForMissingPatientOrResourceId();
		}
		return searchQuery.getQueryResults(searchParams.toSearchParameterMap(), documentReferenceDao,
		    documentReferenceTranslator, searchQueryInclude);
		
	}
	
	private void logAndThrowUnsupportedExceptionForMissingPatientOrResourceId() {
		log.error(MISSING_PATIENT_IDENTIFIER_OR_RES_ID);
		throw new UnsupportedOperationException(PATIENT_IDENTIFIER_OR_REFERENCE_OR_RES_ID_MUST_BE_SPECIFIED);
	}
	
	@Override
	protected FhirDao<FhirDocumentReference> getDao() {
		return documentReferenceDao;
	}
	
	@Override
	protected OpenmrsFhirTranslator<FhirDocumentReference, DocumentReference> getTranslator() {
		return documentReferenceTranslator;
	}
}
