package org.bahmni.module.fhir2addlextension.api.search.param;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.bahmni.module.fhir2addlextension.api.BahmniFhirConstants;
import org.openmrs.module.fhir2.api.search.param.MedicationRequestSearchParams;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class BahmniMedicationRequestSearchParams extends MedicationRequestSearchParams {
	
	private ReferenceAndListParam locationReference;
	
	private SortSpec sort;
	
	public BahmniMedicationRequestSearchParams(ReferenceAndListParam patientReference,
	    ReferenceAndListParam encounterReference, TokenAndListParam code, ReferenceAndListParam participantReference,
	    ReferenceAndListParam medicationReference, TokenAndListParam id, TokenAndListParam status,
	    TokenAndListParam fulfillerStatus, ReferenceAndListParam locationReference, DateRangeParam lastUpdated,
	    Set<Include> includes, Set<Include> revIncludes, SortSpec sort) {
		super(patientReference, encounterReference, code, participantReference, medicationReference, id, status,
		        fulfillerStatus, lastUpdated, includes, revIncludes);
		this.locationReference = locationReference;
		this.sort = sort;
	}
	
	@Override
	public SearchParameterMap toSearchParameterMap() {
		SearchParameterMap map = super.toSearchParameterMap();
		map.addParameter(BahmniFhirConstants.ORDER_LOCATION_SEARCH_HANDLER, locationReference);
		if (sort != null) {
			map.setSortSpec(sort);
		}
		return map;
	}
}
