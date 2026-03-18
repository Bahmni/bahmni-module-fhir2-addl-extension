package org.bahmni.module.fhir2addlextension.api.providers;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirObservationService;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.api.search.param.ObservationSearchParams;
import org.openmrs.module.fhir2.api.util.FhirUtils;
import org.openmrs.module.fhir2.providers.r4.ObservationFhirResourceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("bahmniObservationFhirR4ResourceProvider")
@R4Provider
public class BahmniObservationFhirR4ResourceProvider extends ObservationFhirResourceProvider {
	
	@Autowired
	private BahmniFhirObservationService observationService;
	
	@Description(shortDefinition = "Retrieves all Observations for an encounter without paging limit", value = "This operation returns all Observations linked to the specified encounter as a Bundle, "
	        + "bypassing the default FHIR paging maximum limit.")
	@Operation(name = "$everything-by-encounter", idempotent = true, type = Observation.class, returnParameters = { @OperationParam(name = "return", type = Bundle.class, min = 1, max = 1) })
	public Bundle getEverythingByEncounter(
	        @OperationParam(name = "encounter", min = 1, max = 1) ReferenceAndListParam encounterReference,
	        RequestDetails requestDetails) {
		if (encounterReference == null) {
			throw new InvalidRequestException("The 'encounter' parameter is required");
		}
		
		ObservationSearchParams searchParams = new ObservationSearchParams();
		searchParams.setEncounter(encounterReference);
		
		IBundleProvider bundleProvider = observationService.searchForObservations(searchParams);
		List<IBaseResource> observations = bundleProvider.getResources(0, Integer.MAX_VALUE);
		
		String serverBase = requestDetails.getFhirServerBase();
		Bundle bundle = new Bundle();
		bundle.setId(FhirUtils.newUuid());
		bundle.getMeta().setLastUpdated(InstantDt.withCurrentTime().getValue());
		bundle.setType(Bundle.BundleType.SEARCHSET);
		bundle.setTotal(observations.size());
		for (IBaseResource resource : observations) {
			Observation obs = (Observation) resource;
			bundle.addEntry().setResource(obs).setFullUrl(serverBase + "/Observation/" + obs.getIdElement().getIdPart());
		}
		return bundle;
	}
}
