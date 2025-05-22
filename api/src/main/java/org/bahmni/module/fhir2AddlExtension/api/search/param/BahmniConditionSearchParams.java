package org.bahmni.module.fhir2AddlExtension.api.search.param;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.bahmni.module.fhir2AddlExtension.api.BahmniFhirConstants;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.ConditionSearchParams;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import java.util.HashSet;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BahmniConditionSearchParams extends ConditionSearchParams {
	
	private TokenAndListParam verificationStatus;
	
	private StringParam category;
	
	public BahmniConditionSearchParams(ReferenceAndListParam patientParam, TokenAndListParam code,
	    TokenAndListParam clinicalStatus, DateRangeParam onsetDate, QuantityAndListParam onsetAge,
	    DateRangeParam recordedDate, TokenAndListParam id, TokenAndListParam verificationStatus, StringParam category,
	    DateRangeParam lastUpdated, SortSpec sort, HashSet<Include> includes) {
		
		super(patientParam, code, clinicalStatus, onsetDate, onsetAge, recordedDate, id, lastUpdated, sort, includes);
		this.verificationStatus = verificationStatus;
		this.category = category;
		
	}
	
	@Override
	public SearchParameterMap toSearchParameterMap() {
		return super.toSearchParameterMap()
		        .addParameter(BahmniFhirConstants.CONDITION_VERIFICATION_STATUS_SEARCH_HANDLER, verificationStatus)
		        .addParameter(FhirConstants.CATEGORY_SEARCH_HANDLER, category);
	}
}
