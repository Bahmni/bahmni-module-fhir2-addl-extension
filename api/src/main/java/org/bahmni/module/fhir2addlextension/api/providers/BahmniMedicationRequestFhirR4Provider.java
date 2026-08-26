package org.bahmni.module.fhir2addlextension.api.providers;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IncludeParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Sort;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.bahmni.module.fhir2addlextension.api.search.param.BahmniMedicationRequestSearchParams;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationDispense;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.FhirMedicationRequestService;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2.providers.r4.MedicationRequestFhirResourceProvider;
import org.openmrs.module.fhir2.providers.util.FhirProviderUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
@R4Provider
public class BahmniMedicationRequestFhirR4Provider extends MedicationRequestFhirResourceProvider {
	
	@Setter(onMethod_ = @Autowired)
	private FhirMedicationRequestService fhirMedicationRequestService;
	
	@Create
	public MethodOutcome createMedicationRequest(@ResourceParam MedicationRequest medicationRequest) {
		return FhirProviderUtils.buildCreate(fhirMedicationRequestService.create(medicationRequest));
	}
	
	@Search
	public IBundleProvider searchForMedicationRequestsWithLocation(
	        @OptionalParam(name = MedicationRequest.SP_PATIENT, chainWhitelist = { "", Patient.SP_IDENTIFIER,
	                Patient.SP_GIVEN, Patient.SP_FAMILY, Patient.SP_NAME }, targetTypes = Patient.class) ReferenceAndListParam patientReference,
	        @OptionalParam(name = MedicationRequest.SP_SUBJECT, chainWhitelist = { "", Patient.SP_IDENTIFIER,
	                Patient.SP_GIVEN, Patient.SP_FAMILY, Patient.SP_NAME }, targetTypes = Patient.class) ReferenceAndListParam subjectReference,
	        @OptionalParam(name = MedicationRequest.SP_ENCOUNTER, chainWhitelist = { "" }, targetTypes = Encounter.class) ReferenceAndListParam encounterReference,
	        @OptionalParam(name = MedicationRequest.SP_CODE) TokenAndListParam code,
	        @OptionalParam(name = MedicationRequest.SP_REQUESTER, chainWhitelist = { "", Practitioner.SP_IDENTIFIER,
	                Practitioner.SP_GIVEN, Practitioner.SP_FAMILY, Practitioner.SP_NAME }, targetTypes = Practitioner.class) ReferenceAndListParam participantReference,
	        @OptionalParam(name = MedicationRequest.SP_MEDICATION, chainWhitelist = { "" }, targetTypes = Medication.class) ReferenceAndListParam medicationReference,
	        @OptionalParam(name = MedicationRequest.SP_RES_ID) TokenAndListParam id,
	        @OptionalParam(name = MedicationRequest.SP_STATUS) TokenAndListParam status,
	        @OptionalParam(name = FhirConstants.SP_FULFILLER_STATUS) TokenAndListParam fulfillerStatus,
	        @OptionalParam(name = BahmniFhirConstants.SP_ORDER_LOCATION, chainWhitelist = { "" }, targetTypes = Location.class) ReferenceAndListParam locationReference,
	        @OptionalParam(name = "_lastUpdated") DateRangeParam lastUpdated,
	        @IncludeParam(allow = { "MedicationRequest:" + MedicationRequest.SP_MEDICATION,
	                "MedicationRequest:" + MedicationRequest.SP_REQUESTER,
	                "MedicationRequest:" + MedicationRequest.SP_PATIENT,
	                "MedicationRequest:" + MedicationRequest.SP_ENCOUNTER }) HashSet<Include> includes,
	        @IncludeParam(reverse = true, allow = { "MedicationDispense:" + MedicationDispense.SP_PRESCRIPTION }) HashSet<Include> revIncludes,
	        @Sort SortSpec sort) {
		if (patientReference == null) {
			patientReference = subjectReference;
		}
		
		if (CollectionUtils.isEmpty(includes)) {
			includes = null;
		}
		
		if (CollectionUtils.isEmpty(revIncludes)) {
			revIncludes = null;
		}
		
		return fhirMedicationRequestService.searchForMedicationRequests(new BahmniMedicationRequestSearchParams(
		        patientReference, encounterReference, code, participantReference, medicationReference, id, status,
		        fulfillerStatus, locationReference, lastUpdated, includes, revIncludes, sort));
	}
	
}
