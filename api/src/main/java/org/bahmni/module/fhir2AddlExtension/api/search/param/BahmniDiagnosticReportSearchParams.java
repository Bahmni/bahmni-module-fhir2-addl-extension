package org.bahmni.module.fhir2AddlExtension.api.search.param;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.DiagnosticReportSearchParams;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.util.StringUtils;

import java.util.HashSet;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BahmniDiagnosticReportSearchParams extends DiagnosticReportSearchParams {
	
	private ReferenceAndListParam basedOnReference;
	
	public BahmniDiagnosticReportSearchParams(ReferenceAndListParam encounterReference,
	    ReferenceAndListParam patientReference, ReferenceAndListParam basedOnReference, DateRangeParam issueDate,
	    TokenAndListParam code, ReferenceAndListParam result, TokenAndListParam id, DateRangeParam lastUpdated,
	    SortSpec sort, HashSet<Include> includes) {
		super(encounterReference, patientReference, issueDate, code, result, id, lastUpdated, sort, includes);
		this.basedOnReference = basedOnReference;
	}
	
	@Override
	public SearchParameterMap toSearchParameterMap() {
		SearchParameterMap searchParameterMap = super.toSearchParameterMap();
		if (!hasBasedOnReference()) {
			return searchParameterMap;
		} else {
			searchParameterMap.addParameter(FhirConstants.BASED_ON_REFERENCE_SEARCH_HANDLER, basedOnReference);
		}
		return searchParameterMap;
	}
	
	public boolean hasBasedOnReference() {
        if ((basedOnReference == null) || basedOnReference.getValuesAsQueryTokens().isEmpty()) {
            return false;
        }
        boolean hasParam = false;
        for (ReferenceOrListParam referenceOrListParam : basedOnReference.getValuesAsQueryTokens()) {
            if (referenceOrListParam.getValuesAsQueryTokens().isEmpty()) {
                continue;
            }
            boolean match = referenceOrListParam.getValuesAsQueryTokens().stream().anyMatch(referenceParam -> {
                return StringUtils.isEmpty(referenceParam.getValue());
            });
            if (match) {
                continue;
            }
            hasParam = true;
        }
        return hasParam;
    }
}
