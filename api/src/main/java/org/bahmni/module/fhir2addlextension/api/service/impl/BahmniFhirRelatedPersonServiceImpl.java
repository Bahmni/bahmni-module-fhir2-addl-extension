package org.bahmni.module.fhir2addlextension.api.service.impl;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import org.bahmni.module.fhir2addlextension.api.dao.BahmniFhirRelatedPersonDao;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniRelatedPersonSearchParams;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirRelatedPersonService;
import org.bahmni.module.fhir2addlextension.api.translator.BahmniRelatedPersonTranslator;
import org.hl7.fhir.r4.model.RelatedPerson;
import org.openmrs.Relationship;
import org.openmrs.module.fhir2.api.dao.FhirDao;
import org.openmrs.module.fhir2.api.impl.BaseFhirService;
import org.openmrs.module.fhir2.api.search.param.RelatedPersonSearchParams;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Primary
@Transactional
public class BahmniFhirRelatedPersonServiceImpl extends BaseFhirService<RelatedPerson, Relationship> implements BahmniFhirRelatedPersonService {
	
	private final BahmniFhirRelatedPersonDao dao;
	
	private final BahmniRelatedPersonTranslator translator;
	
	@Autowired
	public BahmniFhirRelatedPersonServiceImpl(BahmniFhirRelatedPersonDao dao, BahmniRelatedPersonTranslator translator) {
		this.dao = dao;
		this.translator = translator;
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
		List<Relationship> relationships = dao.getSearchResults(searchParams.toSearchParameterMap());
		List<RelatedPerson> relatedPersons = relationships.stream()
		        .map(rel -> translator.toFhirResource(rel, patientUuid))
		        .collect(Collectors.toList());
		return new SimpleBundleProvider(relatedPersons);
	}
	
	@Override
	public IBundleProvider searchForRelatedPeople(@Nonnull RelatedPersonSearchParams searchParams) {
		throw new NotImplementedOperationException("Use patient= search parameter for RelatedPerson queries");
	}
	
	@Override
	protected RelatedPerson applyUpdate(Relationship existing, RelatedPerson updatedRelatedPerson) {
		Relationship updated = translator.toOpenmrsType(existing, updatedRelatedPerson);
		Relationship saved = dao.createOrUpdate(updated);
		return translator.toFhirResource(saved);
	}
}
