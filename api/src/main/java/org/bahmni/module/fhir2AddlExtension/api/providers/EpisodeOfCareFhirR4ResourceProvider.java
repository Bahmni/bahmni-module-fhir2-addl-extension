package org.bahmni.module.fhir2AddlExtension.api.providers;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.bahmni.module.fhir2AddlExtension.api.service.BahmniFhirEpisodeOfCareService;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.EpisodeOfCare;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.providers.util.FhirProviderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component("bahmniEpisodeOfCareFhirR4ResourceProvider")
@R4Provider
@Primary
public class EpisodeOfCareFhirR4ResourceProvider implements IResourceProvider {
	
	final private BahmniFhirEpisodeOfCareService episodeOfCareService;
	
	@Autowired
	public EpisodeOfCareFhirR4ResourceProvider(BahmniFhirEpisodeOfCareService episodeOfCareService) {
		this.episodeOfCareService = episodeOfCareService;
	}
	
	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return EpisodeOfCare.class;
	}
	
	@Read
	public EpisodeOfCare getEpisodeOfCareByUuid(@IdParam IdType id) {
		EpisodeOfCare episodeOfCare = episodeOfCareService.get(id.getIdPart());
		if (episodeOfCare == null) {
			throw new ResourceNotFoundException("Could not find EpisodeOfCare with Id " + id.getIdPart());
		}
		return episodeOfCare;
	}
	
	@Create
	public MethodOutcome createEpisodeOfCare(@ResourceParam EpisodeOfCare episodeOfCare) {
		EpisodeOfCare createdEpisodeOfCare = episodeOfCareService.create(episodeOfCare);
		return FhirProviderUtils.buildCreate(createdEpisodeOfCare);
	}
	
	@Search
	public IBundleProvider searchEpisodesOfCare(@RequiredParam(name = EpisodeOfCare.SP_PATIENT, chainWhitelist = { "",
	        Patient.SP_IDENTIFIER }, targetTypes = Patient.class) ReferenceAndListParam patientReference) {
		return episodeOfCareService.episodesForPatient(patientReference);
	}
	
	@Update
	public MethodOutcome updateEpisodeOfCare(@IdParam IdType id, @ResourceParam EpisodeOfCare updatedEpisodeOfCare) {
		if (id == null || id.getIdPart() == null) {
			throw new InvalidRequestException("id must be specified to update");
		}
		updatedEpisodeOfCare.setId(id);
		return FhirProviderUtils.buildUpdate(episodeOfCareService.update(id.getIdPart(), updatedEpisodeOfCare));
	}
}
