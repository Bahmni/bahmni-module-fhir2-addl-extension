package org.bahmni.module.fhir2addlextension.api.service.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirRelatedPersonDao;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniRelatedPersonSearchParams;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirRelatedPersonService;
import org.bahmni.module.fhir2addlextension.api.translator.BahmniRelatedPersonTranslator;
import org.hl7.fhir.r4.model.RelatedPerson;
import org.openmrs.Relationship;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.impl.BaseFhirService;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;

@Component
@Transactional
public class BahmniFhirRelatedPersonServiceImpl extends BaseFhirService<RelatedPerson, Relationship> implements BahmniFhirRelatedPersonService {
	
	private final BahmniFhirRelatedPersonDao dao;
	
	private final BahmniRelatedPersonTranslator translator;
	
	private final SearchQueryInclude<RelatedPerson> searchQueryInclude;
	
	private final SearchQuery<Relationship, RelatedPerson, BahmniFhirRelatedPersonDao, BahmniRelatedPersonTranslator, SearchQueryInclude<RelatedPerson>> searchQuery;
	
	@Autowired
	public BahmniFhirRelatedPersonServiceImpl(
	    BahmniFhirRelatedPersonDao dao,
	    BahmniRelatedPersonTranslator translator,
	    SearchQueryInclude<RelatedPerson> searchQueryInclude,
	    SearchQuery<Relationship, RelatedPerson, BahmniFhirRelatedPersonDao, BahmniRelatedPersonTranslator, SearchQueryInclude<RelatedPerson>> searchQuery) {
		this.dao = dao;
		this.translator = translator;
		this.searchQueryInclude = searchQueryInclude;
		this.searchQuery = searchQuery;
	}
	
	@Override
	protected FhirDao<Relationship> getDao() {
		return dao;
	}
	
	@Override
	protected OpenmrsFhirTranslator<Relationship, RelatedPerson> getTranslator() {
		return translator;
	}
	
	@Override
	public IBundleProvider searchByPatient(@Nonnull BahmniRelatedPersonSearchParams searchParams) {
		if (!searchParams.hasPatientReference()) {
			throw new InvalidRequestException("patient reference is required to search RelatedPerson");
		}
		String patientUuid = searchParams.extractPatientUuid();
		return searchQuery.getQueryResults(searchParams.toSearchParameterMap(), dao, new PerspectiveAwareTranslatorWrapper(
		        translator, patientUuid), searchQueryInclude);
	}
	
	@Override
	protected RelatedPerson applyUpdate(Relationship existing, RelatedPerson updatedRelatedPerson) {
		Relationship updated = translator.toOpenmrsType(existing, updatedRelatedPerson);
		Relationship saved = dao.createOrUpdate(updated);
		String focalPatientUuid = saved.getPersonB() != null ? saved.getPersonB().getUuid() : null;
		return translator.toFhirResource(saved, focalPatientUuid);
	}
	
	private static class PerspectiveAwareTranslatorWrapper implements BahmniRelatedPersonTranslator {
		
		private final BahmniRelatedPersonTranslator delegate;
		
		private final String patientUuid;
		
		PerspectiveAwareTranslatorWrapper(BahmniRelatedPersonTranslator delegate, String patientUuid) {
			this.delegate = delegate;
			this.patientUuid = patientUuid;
		}
		
		@Override
		public RelatedPerson toFhirResource(@Nonnull Relationship relationship) {
			return delegate.toFhirResource(relationship, patientUuid);
		}
		
		@Override
		public RelatedPerson toFhirResource(@Nonnull Relationship relationship, String subjectPatientUuid) {
			return delegate.toFhirResource(relationship, subjectPatientUuid);
		}
		
		@Override
		public Relationship toOpenmrsType(@Nonnull RelatedPerson relatedPerson) {
			return delegate.toOpenmrsType(relatedPerson);
		}
		
		@Override
		public Relationship toOpenmrsType(@Nonnull Relationship existing, @Nonnull RelatedPerson relatedPerson) {
			return delegate.toOpenmrsType(existing, relatedPerson);
		}
	}
}
