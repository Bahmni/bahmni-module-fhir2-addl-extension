package org.bahmni.module.fhir2addlextension.api.providers;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.bahmni.module.fhir2addlextension.api.context.RequestContextHolder;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniObservationSearchParams;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirObservationService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.providers.r4.ObservationFhirResourceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("bahmniObservationFhirR4ResourceProvider")
@R4Provider
public class BahmniObservationFhirR4ResourceProvider extends ObservationFhirResourceProvider {
	
	@Autowired
	private BahmniFhirObservationService observationService;
	
	@Search
	public IBundleProvider searchObservation(
	        @OptionalParam(name = Observation.SP_PATIENT, chainWhitelist = { "", Patient.SP_IDENTIFIER, Patient.SP_NAME,
	                Patient.SP_GIVEN, Patient.SP_FAMILY }, targetTypes = Patient.class) ReferenceAndListParam patientReference,
	        @OptionalParam(name = Observation.SP_BASED_ON) StringAndListParam basedOnReference,
	        @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated, @Sort SortSpec sort) {
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(patientReference, basedOnReference,
		        lastUpdated, sort);
		return observationService.searchObservations(searchParams);
	}
	
	@Description(shortDefinition = "Retrieves all Observations for an encounter without paging limit", value = "This operation returns all Observations linked to the specified encounter as a Bundle, "
	        + "bypassing the default FHIR paging maximum limit.")
	@Operation(name = "$fetch-all", idempotent = true, type = Observation.class, returnParameters = { @OperationParam(name = "return", type = Bundle.class, min = 1, max = 1) })
	public Bundle getEverythingByEncounter(
	        @OperationParam(name = "encounter", min = 1, max = 1) ReferenceAndListParam encounterReference,
	        RequestDetails requestDetails) {
		if (encounterReference == null) {
			throw new InvalidRequestException("The 'encounter' parameter is required");
		}
		RequestContextHolder.setValue(requestDetails.getFhirServerBase());
		return observationService.fetchAllByEncounter(encounterReference);
	}
}
