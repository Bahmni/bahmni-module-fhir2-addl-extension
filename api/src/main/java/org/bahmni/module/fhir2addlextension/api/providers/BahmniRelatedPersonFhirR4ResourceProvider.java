package org.bahmni.module.fhir2addlextension.api.providers;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniRelatedPersonSearchParams;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirRelatedPersonService;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.RelatedPerson;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.providers.util.FhirProviderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component("bahmniRelatedPersonFhirR4ResourceProvider")
@R4Provider
@Primary
public class BahmniRelatedPersonFhirR4ResourceProvider implements IResourceProvider {
	
	private final BahmniFhirRelatedPersonService relatedPersonService;
	
	@Autowired
	public BahmniRelatedPersonFhirR4ResourceProvider(BahmniFhirRelatedPersonService relatedPersonService) {
		this.relatedPersonService = relatedPersonService;
	}
	
	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return RelatedPerson.class;
	}
	
	@Read
	public RelatedPerson getRelatedPersonById(@IdParam @Nonnull IdType id) {
		RelatedPerson relatedPerson = relatedPersonService.get(id.getIdPart());
		if (relatedPerson == null) {
			throw new ResourceNotFoundException("Could not find RelatedPerson with id " + id.getIdPart());
		}
		return relatedPerson;
	}
	
	@Create
	public MethodOutcome createRelatedPerson(@ResourceParam RelatedPerson relatedPerson) {
		if (!relatedPerson.hasPatient()) {
			throw new InvalidRequestException("RelatedPerson.patient reference is required");
		}
		if (!relatedPerson.hasRelationship()) {
			throw new InvalidRequestException("RelatedPerson.relationship is required");
		}
		return FhirProviderUtils.buildCreate(relatedPersonService.create(relatedPerson));
	}
	
	@Update
	public MethodOutcome updateRelatedPerson(@IdParam IdType id, @ResourceParam RelatedPerson relatedPerson) {
		if (id == null || id.getIdPart() == null) {
			throw new InvalidRequestException("id must be specified to update RelatedPerson");
		}
		relatedPerson.setId(id.getIdPart());
		return FhirProviderUtils.buildUpdate(relatedPersonService.update(id.getIdPart(), relatedPerson));
	}
	
	@Delete
	public void deleteRelatedPerson(@IdParam @Nonnull IdType id) {
		relatedPersonService.delete(id.getIdPart());
	}
	
	@Search
	public IBundleProvider searchRelatedPersons(
	        @OptionalParam(name = RelatedPerson.SP_PATIENT, chainWhitelist = { "" }, targetTypes = Patient.class) ReferenceAndListParam patient,
	        @OptionalParam(name = "_id") TokenAndListParam id,
	        @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated) {
		return relatedPersonService.searchByPatient(BahmniRelatedPersonSearchParams.builder().patientReference(patient)
		        .id(id).lastUpdated(lastUpdated).build());
	}
}
