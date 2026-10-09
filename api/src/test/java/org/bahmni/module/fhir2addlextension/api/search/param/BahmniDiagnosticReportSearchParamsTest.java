package org.bahmni.module.fhir2addlextension.api.search.param;

import org.junit.Test;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;

public class BahmniDiagnosticReportSearchParamsTest {
	
	@Test
	public void toSearchParameterMap_shouldReturnValidMap() {
		BahmniDiagnosticReportSearchParams params = new BahmniDiagnosticReportSearchParams();
		SearchParameterMap map = params.toSearchParameterMap();
		
		assertThat(map, notNullValue());
	}
	
	@Test
	public void equals_shouldReturnTrueForIdenticalInstances() {
		BahmniDiagnosticReportSearchParams params1 = new BahmniDiagnosticReportSearchParams();
		BahmniDiagnosticReportSearchParams params2 = new BahmniDiagnosticReportSearchParams();
		
		assertThat(params1.equals(params2), equalTo(true));
	}
	
	@Test
	public void hashCode_shouldBeConsistent() {
		BahmniDiagnosticReportSearchParams params1 = new BahmniDiagnosticReportSearchParams();
		BahmniDiagnosticReportSearchParams params2 = new BahmniDiagnosticReportSearchParams();
		
		assertThat(params1.hashCode(), equalTo(params2.hashCode()));
	}
}
