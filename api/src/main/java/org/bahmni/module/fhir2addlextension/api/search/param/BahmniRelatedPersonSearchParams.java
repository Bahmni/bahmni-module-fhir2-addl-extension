package org.bahmni.module.fhir2addlextension.api.search.param;

import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.openmrs.module.fhir2.api.search.param.BaseResourceSearchParams;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.util.StringUtils;

import java.util.Collections;

import static org.openmrs.module.fhir2.FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BahmniRelatedPersonSearchParams extends BaseResourceSearchParams {
	
	private ReferenceAndListParam patientReference;
	
	@Builder
	public BahmniRelatedPersonSearchParams(ReferenceAndListParam patientReference, TokenAndListParam id,
	    DateRangeParam lastUpdated, SortSpec sort) {
		super(id, lastUpdated, sort, Collections.emptySet(), Collections.emptySet());
		this.patientReference = patientReference;
	}
	
	@Override
	public SearchParameterMap toSearchParameterMap() {
		return baseSearchParameterMap().addParameter(PATIENT_REFERENCE_SEARCH_HANDLER, patientReference);
	}
	
	public boolean hasPatientReference() {
		if (patientReference == null || patientReference.getValuesAsQueryTokens().isEmpty()) {
			return false;
		}
		for (ReferenceOrListParam orParam : patientReference.getValuesAsQueryTokens()) {
			boolean hasValue = orParam.getValuesAsQueryTokens().stream()
			        .anyMatch(token -> !StringUtils.isEmpty(token.getValue()));
			if (hasValue)
				return true;
		}
		return false;
	}
	
	public String extractPatientUuid() {
		if (patientReference == null)
			return null;
		return patientReference.getValuesAsQueryTokens().stream()
		        .flatMap(or -> or.getValuesAsQueryTokens().stream())
		        .map(token -> token.getIdPart())
		        .filter(id -> id != null && !id.isEmpty())
		        .findFirst()
		        .orElse(null);
	}
}
