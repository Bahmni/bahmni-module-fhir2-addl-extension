package org.bahmni.module.fhir2addlextension.api.service.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirRelatedPersonDao;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniRelatedPersonSearchParams;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirRelatedPersonService;
import org.bahmni.module.fhir2addlextension.api.translator.BahmniRelatedPersonTranslator;
import org.hl7.fhir.r4.model.RelatedPerson;
import org.openmrs.Relationship;
import org.openmrs.api.PersonService;
import org.openmrs.module.fhir2.api.search.SearchQuery;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.List;

@Component
@Transactional
public class BahmniFhirRelatedPersonServiceImpl implements BahmniFhirRelatedPersonService {
	
	private final BahmniFhirRelatedPersonDao dao;
	
	private final BahmniRelatedPersonTranslator translator;
	
	private final SearchQueryInclude<RelatedPerson> searchQueryInclude;
	
	private final SearchQuery<Relationship, RelatedPerson, BahmniFhirRelatedPersonDao, BahmniRelatedPersonTranslator, SearchQueryInclude<RelatedPerson>> searchQuery;
	
	private final PersonService personService;
	
	@Autowired
	public BahmniFhirRelatedPersonServiceImpl(
	    BahmniFhirRelatedPersonDao dao,
	    BahmniRelatedPersonTranslator translator,
	    SearchQueryInclude<RelatedPerson> searchQueryInclude,
	    SearchQuery<Relationship, RelatedPerson, BahmniFhirRelatedPersonDao, BahmniRelatedPersonTranslator, SearchQueryInclude<RelatedPerson>> searchQuery,
	    PersonService personService) {
		this.dao = dao;
		this.translator = translator;
		this.searchQueryInclude = searchQueryInclude;
		this.searchQuery = searchQuery;
		this.personService = personService;
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
	public RelatedPerson get(@Nonnull String uuid) {
		Relationship relationship = dao.get(uuid);
		if (relationship == null) {
			throw new ResourceNotFoundException("RelatedPerson not found: " + uuid);
		}
		return translator.toFhirResource(relationship);
	}
	
	@Override
	public RelatedPerson create(@Nonnull RelatedPerson relatedPerson) {
		Relationship relationship = translator.toOpenmrsType(relatedPerson);
		validateNoDuplicate(relationship);
		Relationship saved = dao.createOrUpdate(relationship);
		return translator.toFhirResource(saved);
	}
	
	private void validateNoDuplicate(Relationship newRel) {
		if (newRel.getPersonA() == null || newRel.getPersonB() == null || newRel.getRelationshipType() == null) {
			return;
		}
		List<Relationship> existing = personService.getRelationships(newRel.getPersonA(), newRel.getPersonB(),
		    newRel.getRelationshipType());
		if (existing == null) {
			return;
		}
		for (Relationship rel : existing) {
			if (!rel.getVoided() && periodsOverlap(rel, newRel)) {
				throw new UnprocessableEntityException("A relationship of this type between these patients already exists");
			}
		}
	}
	
	private boolean periodsOverlap(Relationship a, Relationship b) {
		Date now = new Date();
		// A relationship with a past end date is historical — no conflict with a new one
		if (a.getEndDate() != null && a.getEndDate().before(now)) {
			return false;
		}
		if (b.getEndDate() != null && b.getEndDate().before(now)) {
			return false;
		}
		return true;
	}
	
	@Override
	public RelatedPerson update(@Nonnull String uuid, @Nonnull RelatedPerson relatedPerson) {
		Relationship existing = dao.get(uuid);
		if (existing == null) {
			throw new ResourceNotFoundException("RelatedPerson not found: " + uuid);
		}
		Relationship updated = translator.toOpenmrsType(existing, relatedPerson);
		Relationship saved = dao.createOrUpdate(updated);
		String focalPatientUuid = saved.getPersonB() != null ? saved.getPersonB().getUuid() : null;
		return translator.toFhirResource(saved, focalPatientUuid);
	}
	
	@Override
	public void delete(@Nonnull String uuid) {
		dao.delete(uuid);
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
