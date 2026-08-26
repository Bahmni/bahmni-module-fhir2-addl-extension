package org.bahmni.module.fhir2addlextension.api.search.param;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.BaseResourceSearchParams;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import java.util.HashSet;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BahmniServiceRequestSearchParams extends BaseResourceSearchParams {
	
	private ReferenceAndListParam patientReference;
	
	private TokenAndListParam code;
	
	private ReferenceAndListParam encounterReference;
	
	private ReferenceAndListParam participantReference;
	
	private ReferenceAndListParam category;
	
	private ReferenceAndListParam basedOnReference;
	
	private DateRangeParam occurrence;
	
	public BahmniServiceRequestSearchParams(ReferenceAndListParam patientReference, TokenAndListParam code,
	    ReferenceAndListParam encounterReference, ReferenceAndListParam participantReference,
	    ReferenceAndListParam category, ReferenceAndListParam basedOnReference, DateRangeParam occurrence,
	    TokenAndListParam id, DateRangeParam lastUpdated, HashSet<Include> includes, HashSet<Include> revIncludes) {
		super(id, lastUpdated, null, includes, revIncludes);
		this.patientReference = patientReference;
		this.code = code;
		this.encounterReference = encounterReference;
		this.participantReference = participantReference;
		this.category = category;
		this.basedOnReference = basedOnReference;
		this.occurrence = occurrence;
	}
	
	@Override
	public SearchParameterMap toSearchParameterMap() {
		return baseSearchParameterMap().addParameter(FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER, patientReference)
		        .addParameter(FhirConstants.CODED_SEARCH_HANDLER, code)
		        .addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, encounterReference)
		        .addParameter(FhirConstants.PARTICIPANT_REFERENCE_SEARCH_HANDLER, participantReference)
		        .addParameter(FhirConstants.CATEGORY_SEARCH_HANDLER, category)
		        .addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference)
		        .addParameter(FhirConstants.DATE_RANGE_SEARCH_HANDLER, occurrence);
	}
}
