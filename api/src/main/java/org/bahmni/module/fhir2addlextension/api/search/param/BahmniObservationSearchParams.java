package org.bahmni.module.fhir2addlextension.api.search.param;

import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.StringOrListParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.BaseResourceSearchParams;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.util.StringUtils;

import java.util.Collections;

import static org.openmrs.module.fhir2.FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BahmniObservationSearchParams extends BaseResourceSearchParams {
	
	private ReferenceAndListParam patientReference;
	
	private StringAndListParam basedOnReference;
	
	public BahmniObservationSearchParams(ReferenceAndListParam patientReference, StringAndListParam basedOnReference,
	    DateRangeParam lastUpdated, SortSpec sort) {
		super(null, lastUpdated, sort, Collections.emptySet(), Collections.emptySet());
		this.patientReference = patientReference;
		this.basedOnReference = basedOnReference;
	}
	
	@Override
	public SearchParameterMap toSearchParameterMap() {
		SearchParameterMap searchParameterMap = baseSearchParameterMap();
		
		if (patientReference != null) {
			searchParameterMap.addParameter(PATIENT_REFERENCE_SEARCH_HANDLER, patientReference);
		}
		
		if (basedOnReference != null) {
			searchParameterMap.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		}
		
		return searchParameterMap;
	}
	
	public boolean hasPatientReference() {
		return hasReferenceParam(patientReference);
	}
	
	public boolean hasBasedOnReference() {
		return hasStringParam(basedOnReference);
	}
	
	private boolean hasReferenceParam(ReferenceAndListParam reference) {
		if ((reference == null) || reference.getValuesAsQueryTokens().isEmpty()) {
			return false;
		}
		
		boolean hasParam = false;
		for (ReferenceOrListParam referenceOrListParam : reference.getValuesAsQueryTokens()) {
			if (referenceOrListParam.getValuesAsQueryTokens().isEmpty() || 
			    referenceOrListParam.getValuesAsQueryTokens().stream()
			        .anyMatch(referenceParam -> StringUtils.isEmpty(referenceParam.getValue()))) {
				continue;
			}
			hasParam = true;
		}
		return hasParam;
	}
	
	private boolean hasStringParam(StringAndListParam stringParam) {
		if ((stringParam == null) || stringParam.getValuesAsQueryTokens().isEmpty()) {
			return false;
		}
		
		boolean hasParam = false;
		for (StringOrListParam stringOrListParam : stringParam.getValuesAsQueryTokens()) {
			if (stringOrListParam.getValuesAsQueryTokens().isEmpty() || 
			    stringOrListParam.getValuesAsQueryTokens().stream()
			        .anyMatch(param -> StringUtils.isEmpty(param.getValue()))) {
				continue;
			}
			hasParam = true;
		}
		return hasParam;
	}
}
