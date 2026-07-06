package org.bahmni.module.fhir2addlextension.api.search.param;

import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.openmrs.module.fhir2.api.search.param.BaseResourceSearchParams;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.springframework.util.StringUtils;

import java.util.Collections;

import static org.openmrs.module.fhir2.FhirConstants.CODED_SEARCH_HANDLER;
import static org.openmrs.module.fhir2.FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER;
import static org.openmrs.module.fhir2.FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BahmniDocumentReferenceSearchParams extends BaseResourceSearchParams {
	
	private ReferenceAndListParam patientReference;
	
	private ReferenceAndListParam encounterReference;
	
	private TokenAndListParam type;
	
	public BahmniDocumentReferenceSearchParams(ReferenceAndListParam patientReference, TokenAndListParam id,
	    DateRangeParam lastUpdated, ReferenceAndListParam encounterReference, TokenAndListParam type, SortSpec sort) {
		super(id, lastUpdated, sort, Collections.emptySet(), Collections.emptySet());
		
		this.patientReference = patientReference;
		this.encounterReference = encounterReference;
		this.type = type;
	}
	
	@Override
	public SearchParameterMap toSearchParameterMap() {
		SearchParameterMap searchParameterMap = baseSearchParameterMap().addParameter(PATIENT_REFERENCE_SEARCH_HANDLER,
		    patientReference).addParameter(ENCOUNTER_REFERENCE_SEARCH_HANDLER, encounterReference);
		if (hasType()) {
			searchParameterMap.addParameter(CODED_SEARCH_HANDLER, type);
		}
		return searchParameterMap;
	}
	
	public boolean hasType() {
		return (type != null) && !type.getValuesAsQueryTokens().isEmpty();
	}
	
	public boolean hasPatientReference() {
		if ((patientReference == null) || patientReference.getValuesAsQueryTokens().isEmpty()) {
			return false;
		}
		boolean hasParam = false;
		for (ReferenceOrListParam referenceOrListParam : patientReference.getValuesAsQueryTokens()) {
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
	
	public boolean hasId() {
		TokenAndListParam idParam = getId();
		if (idParam == null)
			return false;
		return !idParam.getValuesAsQueryTokens().isEmpty();
	}
}
