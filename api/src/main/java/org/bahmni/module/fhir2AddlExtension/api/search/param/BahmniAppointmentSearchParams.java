package org.bahmni.module.fhir2AddlExtension.api.search.param;

import ca.uhn.fhir.model.api.Include;
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
import java.util.HashSet;

import static org.openmrs.module.fhir2.FhirConstants.DATE_RANGE_SEARCH_HANDLER;
import static org.openmrs.module.fhir2.FhirConstants.PATIENT_REFERENCE_SEARCH_HANDLER;
import static org.openmrs.module.fhir2.FhirConstants.STATUS_SEARCH_HANDLER;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BahmniAppointmentSearchParams extends BaseResourceSearchParams {
	
	private ReferenceAndListParam patientReference;
	
	private TokenAndListParam status;
	
	private DateRangeParam date;
	
	@Builder
	public BahmniAppointmentSearchParams(ReferenceAndListParam patientReference, TokenAndListParam status,
	    DateRangeParam date, TokenAndListParam id, DateRangeParam lastUpdated, SortSpec sort) {
		super(id, lastUpdated, sort, Collections.emptySet(), Collections.emptySet());
		this.patientReference = patientReference;
		this.status = status;
		this.date = date;
	}
	
	@Override
	public SearchParameterMap toSearchParameterMap() {
		return baseSearchParameterMap().addParameter(PATIENT_REFERENCE_SEARCH_HANDLER, patientReference)
		        .addParameter(STATUS_SEARCH_HANDLER, status).addParameter(DATE_RANGE_SEARCH_HANDLER, date);
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
	
	public boolean hasStatus() {
		TokenAndListParam statusParam = getStatus();
		if (statusParam == null) {
			return false;
		}
		return !statusParam.getValuesAsQueryTokens().isEmpty();
	}
	
	public boolean hasId() {
		TokenAndListParam idParam = getId();
		if (idParam == null) {
			return false;
		}
		return !idParam.getValuesAsQueryTokens().isEmpty();
	}
	
	public boolean hasDate() {
		DateRangeParam dateParam = getDate();
		if (dateParam == null) {
			return false;
		}
		return dateParam.getLowerBound() != null || dateParam.getUpperBound() != null;
	}
}
