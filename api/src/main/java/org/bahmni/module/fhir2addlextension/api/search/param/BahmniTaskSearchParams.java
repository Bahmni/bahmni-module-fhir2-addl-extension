package org.bahmni.module.fhir2addlextension.api.search.param;

import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;
import org.openmrs.module.fhir2.api.search.param.TaskSearchParams;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BahmniTaskSearchParams extends TaskSearchParams {
	
	private ReferenceAndListParam encounterReference;
	
	private StringAndListParam name;
	
	@Override
	public SearchParameterMap toSearchParameterMap() {
		SearchParameterMap map = super.toSearchParameterMap();
		if (encounterReference != null) {
			map.addParameter(FhirConstants.ENCOUNTER_REFERENCE_SEARCH_HANDLER, encounterReference);
		}
		if (name != null) {
			map.addParameter(BahmniFhirConstants.NAME_SEARCH_HANDLER, name);
		}
		return map;
	}
}
