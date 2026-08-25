package org.bahmni.module.fhir2addlextension.api.providers;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.apache.commons.collections.CollectionUtils;
import org.bahmni.module.fhir2addlextension.api.context.RequestContextHolder;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniObservationSearchParams;
import org.bahmni.module.fhir2addlextension.api.service.BahmniFhirObservationService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.api.search.param.ObservationSearchParams;
import org.openmrs.module.fhir2.providers.r4.ObservationFhirResourceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component("bahmniObservationFhirR4ResourceProvider")
@R4Provider
public class BahmniObservationFhirR4ResourceProvider extends ObservationFhirResourceProvider {
	
	@Autowired
	private BahmniFhirObservationService observationService;
	
	@Search
	public IBundleProvider searchObservation(
	        @OptionalParam(name = Observation.SP_PATIENT, chainWhitelist = { "", Patient.SP_IDENTIFIER, Patient.SP_NAME,
	                Patient.SP_GIVEN, Patient.SP_FAMILY }, targetTypes = Patient.class) ReferenceAndListParam patientReference,
	        @OptionalParam(name = Observation.SP_BASED_ON) ReferenceAndListParam basedOnReference,
	        @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated, @Sort SortSpec sort) {
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams(patientReference, basedOnReference,
		        lastUpdated, sort);
		return observationService.searchObservations(searchParams);
	}
	
	@Description(shortDefinition = "Retrieves all Observations matching the given search parameters without paging limit", value = "This operation returns all Observations matching the given search parameters as a Bundle, "
	        + "bypassing the default FHIR paging maximum limit.")
	@Operation(name = "$fetch-all", idempotent = true, type = Observation.class, returnParameters = { @OperationParam(name = "return", type = Bundle.class, min = 1, max = 1) })
	public Bundle searchAllObservation(
	        @OperationParam(name = "encounter", min = 1, max = 1) ReferenceAndListParam encounterReference,
	        @OperationParam(name = Observation.SP_BASED_ON, min = 0, max = 1) ReferenceAndListParam basedOnReference,
	        RequestDetails requestDetails) {
		if (encounterReference == null) {
			throw new InvalidRequestException("The 'encounter' parameter is required");
		}
		RequestContextHolder.setValue(requestDetails.getFhirServerBase());
		BahmniObservationSearchParams searchParams = new BahmniObservationSearchParams();
		searchParams.setEncounterReference(encounterReference);
		searchParams.setBasedOnReference(basedOnReference);
		return observationService.fetchAllObservation(searchParams);
	}
	
	/*
	- This override has been done to introduce additional Encounter param and include param to support filtering of Obs
	for a visit/episode.
	- HAPI Server does not support overrides of Operation Methods and it leads to the parent operation always being called. So this
	override changes the operation name from lastn to last-n.
	TODO: Remove this override once https://openmrs.atlassian.net/browse/FM2-697 is resolved and merged and module version is upgraded.

	 */
	@Operation(name = "last-n", idempotent = true, type = Observation.class, bundleType = BundleTypeEnum.SEARCHSET)
	public IBundleProvider getLastnObservations(
	        @OperationParam(name = "max") NumberParam max,
	        @OperationParam(name = Observation.SP_SUBJECT) ReferenceAndListParam subjectParam,
	        @OperationParam(name = Observation.SP_PATIENT) ReferenceAndListParam patientParam,
	        @OperationParam(name = Observation.SP_CATEGORY) TokenAndListParam category,
	        @OperationParam(name = Observation.SP_CODE) TokenAndListParam code,
	        @OperationParam(name = Observation.SP_ENCOUNTER) ReferenceAndListParam encounterParam,
	        @IncludeParam(allow = { "Observation:" + Observation.SP_ENCOUNTER, "Observation:" + Observation.SP_PATIENT,
	                "Observation:" + Observation.SP_HAS_MEMBER }) HashSet<Include> includes,
	        @IncludeParam(reverse = true, allow = { "Observation:" + Observation.SP_HAS_MEMBER }) HashSet<Include> revIncludes) {
		if (patientParam != null) {
			subjectParam = patientParam;
		}
		if (CollectionUtils.isEmpty(includes)) {
			includes = null;
		}
		if (CollectionUtils.isEmpty(revIncludes)) {
			revIncludes = null;
		}
		
		ObservationSearchParams searchParams = new ObservationSearchParams();
		searchParams.setPatient(subjectParam);
		searchParams.setCategory(category);
		searchParams.setEncounter(encounterParam);
		searchParams.setCode(code);
		searchParams.setIncludes(includes);
		searchParams.setRevIncludes(revIncludes);
		
		return observationService.getLastnObservations(max, searchParams);
	}
}
